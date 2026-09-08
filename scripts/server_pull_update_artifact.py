#!/usr/bin/env python3
"""Pull a GitHub Actions artifact through the server proxy and publish it."""

from __future__ import annotations

import argparse
import base64
import fcntl
import hashlib
import json
import os
import re
import shutil
import stat
import subprocess
import tempfile
import urllib.parse
import zipfile
from pathlib import Path, PurePosixPath

CHANNEL = "debug"
APPLICATION_ID = "io.github.cluno1.sonorus.debug"
CERTIFICATE_SHA256 = (
    "4b165f85d181c2c36d293545e18028874c4aece8f900d5b69f99fd7ed04d0fdb"
)
UPDATE_ROOT = Path("/srv/sonorus-updates")
PUBLIC_KEY = Path("/etc/sonorus-update/debug-manifest-public.pem")
PROXY = "http://127.0.0.1:7890"
ALLOWED_DOWNLOAD_SUFFIXES = (
    ".blob.core.windows.net",
    ".actions.githubusercontent.com",
)
SAFE_APK = re.compile(r"^[A-Za-z0-9._-]+\.apk$")
MAX_ARCHIVE_BYTES = 256 * 1024 * 1024
MAX_EXTRACTED_BYTES = 384 * 1024 * 1024
MAX_MEMBERS = 16


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalize_digest(value: str) -> str:
    digest = value.removeprefix("sha256:").replace(":", "").strip().lower()
    if not re.fullmatch(r"[0-9a-f]{64}", digest):
        raise SystemExit("expected digest must be a SHA-256 value")
    return digest


def read_download_url() -> str:
    value = input().strip()
    if not value or len(value) > 8192 or any(char in value for char in '"\r\n'):
        raise SystemExit("artifact download URL is missing or invalid")
    parsed = urllib.parse.urlsplit(value)
    hostname = (parsed.hostname or "").lower()
    if (
        parsed.scheme != "https"
        or parsed.username is not None
        or parsed.password is not None
        or parsed.port not in (None, 443)
        or not any(hostname.endswith(suffix) for suffix in ALLOWED_DOWNLOAD_SUFFIXES)
    ):
        raise SystemExit("artifact download URL does not use an allowed HTTPS host")
    return value


def pull(url: str, destination: Path) -> None:
    destination.unlink(missing_ok=True)
    config = f'url = "{url}"\n'
    try:
        subprocess.run(
            [
                "curl",
                "--fail",
                "--silent",
                "--show-error",
                "--location",
                "--proxy",
                PROXY,
                "--connect-timeout",
                "20",
                "--max-time",
                "1200",
                "--retry",
                "2",
                "--retry-delay",
                "3",
                "--output",
                str(destination),
                "--config",
                "-",
            ],
            input=config,
            text=True,
            check=True,
        )
    except BaseException:
        destination.unlink(missing_ok=True)
        raise
    if destination.stat().st_size > MAX_ARCHIVE_BYTES:
        destination.unlink(missing_ok=True)
        raise SystemExit("artifact archive exceeds the configured size limit")


def safe_members(bundle: zipfile.ZipFile) -> list[zipfile.ZipInfo]:
    members = bundle.infolist()
    if not members or len(members) > MAX_MEMBERS:
        raise SystemExit("artifact contains an unexpected number of entries")
    total = 0
    for member in members:
        path = PurePosixPath(member.filename)
        mode = member.external_attr >> 16
        if (
            path.is_absolute()
            or ".." in path.parts
            or "\\" in member.filename
            or stat.S_ISLNK(mode)
        ):
            raise SystemExit("artifact contains an unsafe ZIP entry")
        total += member.file_size
    if total > MAX_EXTRACTED_BYTES:
        raise SystemExit("artifact expands beyond the configured size limit")
    return members


def extract(bundle: zipfile.ZipFile, members: list[zipfile.ZipInfo], root: Path) -> None:
    for member in members:
        target = root.joinpath(*PurePosixPath(member.filename).parts)
        if member.is_dir():
            target.mkdir(parents=True, exist_ok=True)
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        with bundle.open(member) as source, target.open("xb") as destination:
            shutil.copyfileobj(source, destination, 1024 * 1024)


def canonical_json(manifest: dict[str, object]) -> bytes:
    payload = dict(manifest)
    payload.pop("manifestSignature", None)
    return json.dumps(
        payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode()


def verify_signature(manifest: dict[str, object], work: Path) -> None:
    try:
        signature = base64.b64decode(
            str(manifest["manifestSignature"]), validate=True
        )
    except (KeyError, ValueError):
        raise SystemExit("manifest signature is missing or invalid") from None
    if len(signature) != 64 or manifest.get("signatureAlgorithm") != "Ed25519":
        raise SystemExit("manifest does not use a valid Ed25519 signature")
    payload_path = work / "manifest.canonical.json"
    signature_path = work / "manifest.sig"
    payload_path.write_bytes(canonical_json(manifest))
    signature_path.write_bytes(signature)
    subprocess.run(
        [
            "openssl",
            "pkeyutl",
            "-verify",
            "-pubin",
            "-inkey",
            str(PUBLIC_KEY),
            "-rawin",
            "-in",
            str(payload_path),
            "-sigfile",
            str(signature_path),
        ],
        check=True,
        stdout=subprocess.DEVNULL,
    )


def validate(root: Path) -> tuple[int, bytes, Path, list[Path]]:
    latest_path = root / "latest.json"
    try:
        latest_raw = latest_path.read_bytes()
        manifest = json.loads(latest_raw)
        version = int(manifest["versionCode"])
    except (FileNotFoundError, KeyError, TypeError, ValueError, json.JSONDecodeError):
        raise SystemExit("artifact latest.json is missing or invalid") from None
    release = root / "releases" / str(version)
    release_manifest = release / "manifest.json"
    if (
        version <= 0
        or manifest.get("schemaVersion") != 1
        or manifest.get("channel") != CHANNEL
        or manifest.get("applicationId") != APPLICATION_ID
        or normalize_digest(str(manifest.get("signingCertificateSha256", "")))
        != CERTIFICATE_SHA256
        or release_manifest.read_bytes() != latest_raw
    ):
        raise SystemExit("artifact manifest identity or immutable copy is invalid")
    verify_signature(manifest, root)
    assets = manifest.get("assets")
    if not isinstance(assets, list) or not assets:
        raise SystemExit("artifact manifest contains no assets")
    asset_paths: list[Path] = []
    expected_entries = {"latest.json", f"releases/{version}/manifest.json"}
    for asset in assets:
        name = asset.get("fileName") if isinstance(asset, dict) else None
        if not isinstance(name, str) or not SAFE_APK.fullmatch(name):
            raise SystemExit("artifact manifest contains an unsafe APK name")
        path = release / name
        if (
            not path.is_file()
            or path.stat().st_size != asset.get("sizeBytes")
            or sha256(path) != str(asset.get("sha256", "")).lower()
        ):
            raise SystemExit("artifact APK does not match its signed manifest")
        asset_paths.append(path)
        expected_entries.add(f"releases/{version}/{name}")
    actual_entries = {
        path.relative_to(root).as_posix() for path in root.rglob("*") if path.is_file()
    }
    actual_entries -= {"manifest.canonical.json", "manifest.sig"}
    if actual_entries != expected_entries:
        raise SystemExit("artifact contains files outside the signed update bundle")
    return version, latest_raw, release, [release_manifest, *asset_paths]


def same_release(release: Path, source_files: list[Path]) -> bool:
    if not release.is_dir():
        return False
    for source in source_files:
        target = release / source.name
        if (
            not target.is_file()
            or target.stat().st_size != source.stat().st_size
            or sha256(target) != sha256(source)
        ):
            return False
    return True


def publish(version: int, latest_raw: bytes, source: Path, files: list[Path]) -> str:
    channel_root = UPDATE_ROOT / CHANNEL
    releases = channel_root / "releases"
    release = releases / str(version)
    latest = channel_root / "latest.json"
    current = int(json.loads(latest.read_bytes())["versionCode"]) if latest.exists() else 0
    if current > version:
        raise SystemExit(f"versionCode must increase: current={current}, candidate={version}")
    if release.exists() and not same_release(release, files):
        raise SystemExit("immutable release directory already contains different content")
    if current == version:
        if release.exists() and latest.read_bytes() == latest_raw:
            return "already-published"
        raise SystemExit("current version exists but does not match the artifact")
    releases.mkdir(parents=True, exist_ok=True)
    if not release.exists():
        os.replace(source, release)
    latest_tmp = channel_root / f".latest-{os.getpid()}.json"
    try:
        latest_tmp.write_bytes(latest_raw)
        os.replace(latest_tmp, latest)
    finally:
        latest_tmp.unlink(missing_ok=True)
    return "published"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact-id", type=int, required=True)
    parser.add_argument("--artifact-sha256", required=True)
    parser.add_argument("--source-run-id", type=int, required=True)
    parser.add_argument("--source-sha", required=True)
    args = parser.parse_args()
    if args.artifact_id <= 0 or args.source_run_id <= 0:
        raise SystemExit("artifact and run IDs must be positive")
    if not re.fullmatch(r"[0-9a-f]{40}", args.source_sha.lower()):
        raise SystemExit("source SHA must contain 40 hexadecimal characters")
    expected_archive_sha256 = normalize_digest(args.artifact_sha256)
    url = read_download_url()
    channel_root = UPDATE_ROOT / CHANNEL
    channel_root.mkdir(parents=True, exist_ok=True)
    lock_path = channel_root / ".artifact-pull.lock"
    archive = channel_root / f".artifact-{args.artifact_id}.zip"
    with lock_path.open("a+") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        try:
            pull(url, archive)
            if sha256(archive) != expected_archive_sha256:
                raise SystemExit("downloaded artifact SHA-256 does not match GitHub")
            with tempfile.TemporaryDirectory(
                prefix=".artifact-extract-", dir=channel_root
            ) as directory:
                extracted = Path(directory)
                with zipfile.ZipFile(archive) as bundle:
                    members = safe_members(bundle)
                    extract(bundle, members, extracted)
                version, latest_raw, release, files = validate(extracted)
                result = publish(version, latest_raw, release, files)
            print(
                f"{result} debug versionCode {version} from "
                f"run {args.source_run_id} artifact {args.artifact_id}"
            )
        finally:
            archive.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
