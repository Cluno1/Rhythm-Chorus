#!/usr/bin/env python3
"""Publish one locally verified Sonorus update bundle over SSH."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import re
import subprocess
import tarfile
import tempfile
import uuid
from pathlib import Path

SAFE_TARGET = re.compile(r"^[A-Za-z0-9_.-]+@[A-Za-z0-9_.-]+$")
SAFE_ROOT = re.compile(r"^/[A-Za-z0-9_./-]+$")
SAFE_FILE = re.compile(r"^[A-Za-z0-9._-]+\.apk$")
APPLICATION_IDS = {
    "debug": "io.github.cluno1.sonorus.debug",
    "stable": "io.github.cluno1.sonorus",
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalize_digest(value: str) -> str:
    digest = value.replace(":", "").strip().lower()
    if not re.fullmatch(r"[0-9a-f]{64}", digest):
        raise SystemExit("expected certificate must be a SHA-256 fingerprint")
    return digest


def validate_bundle(
    root: Path,
    channel: str,
    expected_certificate_sha256: str,
) -> tuple[int, Path, list[Path]]:
    latest = root / channel / "latest.json"
    try:
        raw = latest.read_bytes()
        manifest = json.loads(raw)
        version = int(manifest["versionCode"])
    except (FileNotFoundError, KeyError, TypeError, ValueError, json.JSONDecodeError):
        raise SystemExit("latest.json is missing or invalid") from None
    release = root / channel / "releases" / str(version)
    if (
        version <= 0
        or manifest.get("schemaVersion") != 1
        or manifest.get("channel") != channel
        or manifest.get("applicationId") != APPLICATION_IDS[channel]
        or normalize_digest(str(manifest.get("signingCertificateSha256", "")))
        != normalize_digest(expected_certificate_sha256)
        or manifest.get("signatureAlgorithm") != "Ed25519"
    ):
        raise SystemExit("manifest channel/version does not match the requested track")
    try:
        signature = base64.b64decode(manifest["manifestSignature"], validate=True)
    except (KeyError, TypeError, ValueError):
        raise SystemExit("manifest signature is missing or invalid") from None
    if len(signature) != 64:
        raise SystemExit("manifest signature must contain exactly 64 bytes")
    if (release / "manifest.json").read_bytes() != raw:
        raise SystemExit(
            "latest.json must exactly match the immutable release manifest"
        )
    files = [release / "manifest.json"]
    assets = manifest.get("assets")
    if not isinstance(assets, list) or not assets:
        raise SystemExit("manifest has no assets")
    for asset in assets:
        name = asset.get("fileName") if isinstance(asset, dict) else None
        if not isinstance(name, str) or not SAFE_FILE.fullmatch(name):
            raise SystemExit("manifest contains an unsafe APK file name")
        path = release / name
        if not path.is_file() or path.stat().st_size != asset.get("sizeBytes"):
            raise SystemExit(f"asset size does not match manifest: {name}")
        if sha256(path) != str(asset.get("sha256", "")).lower():
            raise SystemExit(f"asset SHA-256 does not match manifest: {name}")
        files.append(path)
    return version, release, files


REMOTE_INSTALLER = r"""
import hashlib, json, os, pathlib, shutil, sys, tarfile

remote_root, channel, version_text, archive_text, nonce, application_id, certificate = sys.argv[1:]
root = pathlib.Path(remote_root).resolve()
archive = pathlib.Path(archive_text)
channel_root = root / channel
releases = channel_root / "releases"
release = releases / version_text
incoming = releases / (".incoming-" + version_text + "-" + nonce)
latest = channel_root / "latest.json"
latest_tmp = channel_root / (".latest-" + nonce + ".json")

def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()

try:
    releases.mkdir(parents=True, exist_ok=True)
    if incoming.exists():
        shutil.rmtree(incoming)
    incoming.mkdir()
    with tarfile.open(archive, "r:gz") as bundle:
        members = bundle.getmembers()
        if any(not member.isfile() or "/" in member.name or "\\" in member.name for member in members):
            raise SystemExit("bundle contains an unsafe entry")
        for member in members:
            source = bundle.extractfile(member)
            if source is None:
                raise SystemExit("bundle entry cannot be read")
            with (incoming / member.name).open("wb") as destination:
                shutil.copyfileobj(source, destination)
    manifest_raw = (incoming / "manifest.json").read_bytes()
    manifest = json.loads(manifest_raw)
    if (manifest.get("channel") != channel or str(manifest.get("versionCode")) != version_text
            or manifest.get("applicationId") != application_id
            or manifest.get("signingCertificateSha256") != certificate):
        raise SystemExit("remote manifest identity mismatch")
    for asset in manifest.get("assets", []):
        path = incoming / asset["fileName"]
        if not path.is_file() or path.stat().st_size != asset["sizeBytes"] or digest(path) != asset["sha256"]:
            raise SystemExit("remote asset verification failed")
    if latest.exists():
        current = int(json.loads(latest.read_bytes())["versionCode"])
        if current >= int(version_text):
            raise SystemExit(f"versionCode must increase: current={current}, candidate={version_text}")
    if release.exists():
        if (release / "manifest.json").read_bytes() != manifest_raw:
            raise SystemExit("immutable release directory already exists with different content")
        for asset in manifest.get("assets", []):
            path = release / asset["fileName"]
            if not path.is_file() or path.stat().st_size != asset["sizeBytes"] or digest(path) != asset["sha256"]:
                raise SystemExit("existing immutable release failed verification")
        shutil.rmtree(incoming)
    else:
        os.replace(incoming, release)
    latest_tmp.write_bytes(manifest_raw)
    os.replace(latest_tmp, latest)
finally:
    if incoming.exists():
        shutil.rmtree(incoming)
    latest_tmp.unlink(missing_ok=True)
    archive.unlink(missing_ok=True)
"""


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--channel", choices=("debug", "stable"), required=True)
    parser.add_argument("--bundle-root", type=Path, required=True)
    parser.add_argument(
        "--ssh-target", required=True, help="deployment-user@SSH-host"
    )
    parser.add_argument("--remote-root", default="/srv/sonorus-updates")
    parser.add_argument("--expected-certificate-sha256", required=True)
    args = parser.parse_args()

    if not SAFE_TARGET.fullmatch(args.ssh_target):
        raise SystemExit("SSH target must be a simple user@host value")
    if (
        args.remote_root == "/"
        or not SAFE_ROOT.fullmatch(args.remote_root)
        or ".." in Path(args.remote_root).parts
    ):
        raise SystemExit("remote root must be a safe absolute directory below /")
    certificate = normalize_digest(args.expected_certificate_sha256)
    version, _, files = validate_bundle(
        args.bundle_root.resolve(), args.channel, certificate
    )
    nonce = uuid.uuid4().hex
    remote_archive = (
        f"{args.remote_root}/{args.channel}/.upload-{version}-{nonce}.tar.gz"
    )

    subprocess.run(
        ["ssh", args.ssh_target, "mkdir", "-p", f"{args.remote_root}/{args.channel}"],
        check=True,
    )
    with tempfile.NamedTemporaryFile(
        prefix="sonorus-update-", suffix=".tar.gz"
    ) as temporary:
        with tarfile.open(fileobj=temporary, mode="w:gz") as bundle:
            for path in files:
                info = bundle.gettarinfo(str(path), arcname=path.name)
                with path.open("rb") as stream:
                    bundle.addfile(info, stream)
        temporary.flush()
        subprocess.run(
            ["scp", temporary.name, f"{args.ssh_target}:{remote_archive}"], check=True
        )

    command = [
        "ssh",
        args.ssh_target,
        "python3",
        "-",
        args.remote_root,
        args.channel,
        str(version),
        remote_archive,
        nonce,
        APPLICATION_IDS[args.channel],
        certificate,
    ]
    subprocess.run(command, input=REMOTE_INSTALLER, text=True, check=True)
    print(
        f"published {args.channel} versionCode {version} to {args.ssh_target}:{args.remote_root}"
    )


if __name__ == "__main__":
    main()
