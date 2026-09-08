#!/usr/bin/env python3
"""Validate one signed Sonorus APK and create an immutable signed update manifest."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import re
import shutil
import subprocess
import tempfile
from pathlib import Path

APPLICATION_IDS = {
    "debug": "io.github.cluno1.sonorus.debug",
    "stable": "io.github.cluno1.sonorus",
}


def command(*args: str) -> str:
    return subprocess.run(args, check=True, text=True, capture_output=True).stdout


def canonical_json(value: object) -> bytes:
    return json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode()


def normalize_digest(value: str) -> str:
    digest = value.replace(":", "").strip().lower()
    if not re.fullmatch(r"[0-9a-f]{64}", digest):
        raise SystemExit("certificate SHA-256 must be 64 hexadecimal characters")
    return digest


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def parse_badging(apk: Path, aapt: str) -> tuple[str, int, str, int]:
    output = command(aapt, "dump", "badging", str(apk))
    package = re.search(
        r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",
        output,
        re.MULTILINE,
    )
    sdk = re.search(r"^sdkVersion:'(\d+)'", output, re.MULTILINE)
    if package is None or sdk is None:
        raise SystemExit("aapt could not read package/version/minSdk from APK")
    return package.group(1), int(package.group(2)), package.group(3), int(sdk.group(1))


def signer_digest(apk: Path, apksigner: str) -> str:
    result = subprocess.run(
        [apksigner, "verify", "--print-certs", str(apk)],
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    matches = re.findall(
        r"^(?:Signer #\d+|V\d+ Signer): certificate SHA-256 digest:"
        r"\s*([0-9a-fA-F:]{64,95})\s*$",
        result.stdout,
        re.MULTILINE,
    )
    if not matches:
        raise SystemExit(
            "apksigner did not report a signer SHA-256; output was:\n"
            + result.stdout.strip()
        )
    digests = {normalize_digest(match) for match in matches}
    if len(digests) != 1:
        raise SystemExit("release APK must have exactly one current signer")
    return digests.pop()


def sign(payload: bytes, key: Path, openssl: str) -> str:
    with tempfile.TemporaryDirectory(prefix="sonorus-update-sign-") as directory:
        source = Path(directory) / "manifest.canonical.json"
        signature = Path(directory) / "manifest.sig"
        source.write_bytes(payload)
        subprocess.run(
            [
                openssl,
                "pkeyutl",
                "-sign",
                "-rawin",
                "-inkey",
                str(key),
                "-in",
                str(source),
                "-out",
                str(signature),
            ],
            check=True,
        )
        raw = signature.read_bytes()
    if len(raw) != 64:
        raise SystemExit("Ed25519 signature must be exactly 64 bytes")
    return base64.b64encode(raw).decode()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--channel", choices=sorted(APPLICATION_IDS), required=True)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument(
        "--abi",
        choices=("arm64-v8a", "armeabi-v7a", "x86_64", "x86", "universal"),
        required=True,
    )
    parser.add_argument(
        "--manifest-key",
        type=Path,
        required=True,
        help="PEM Ed25519 private key outside Git",
    )
    parser.add_argument("--expected-certificate-sha256", required=True)
    parser.add_argument("--published-at", required=True, help="UTC ISO-8601 timestamp")
    parser.add_argument("--release-note", action="append", default=[])
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--aapt", default="aapt")
    parser.add_argument("--apksigner", default="apksigner")
    parser.add_argument("--openssl", default="openssl")
    args = parser.parse_args()

    apk = args.apk.resolve(strict=True)
    key = args.manifest_key.resolve(strict=True)
    if not apk.name.endswith(".apk") or not re.fullmatch(
        r"[A-Za-z0-9._-]+\.apk", apk.name
    ):
        raise SystemExit("APK file name must be portable and end in .apk")
    subprocess.run([args.apksigner, "verify", "--verbose", str(apk)], check=True)
    application_id, version_code, version_name, minimum_sdk = parse_badging(
        apk, args.aapt
    )
    if application_id != APPLICATION_IDS[args.channel]:
        raise SystemExit(
            f"{args.channel} APK must use {APPLICATION_IDS[args.channel]}, got {application_id}"
        )
    certificate = signer_digest(apk, args.apksigner)
    if certificate != normalize_digest(args.expected_certificate_sha256):
        raise SystemExit("APK signer does not match the frozen certificate")

    release_parent = args.output_root / args.channel / "releases"
    release_dir = release_parent / str(version_code)
    latest = args.output_root / args.channel / "latest.json"
    if release_dir.exists():
        raise SystemExit(f"immutable release directory already exists: {release_dir}")
    release_parent.mkdir(parents=True, exist_ok=True)
    staging = Path(tempfile.mkdtemp(prefix=f".{version_code}-", dir=release_parent))
    target_apk = staging / apk.name
    shutil.copyfile(apk, target_apk)
    size = target_apk.stat().st_size
    sha256 = file_sha256(target_apk)
    manifest: dict[str, object] = {
        "schemaVersion": 1,
        "channel": args.channel,
        "applicationId": application_id,
        "signingCertificateSha256": certificate,
        "versionCode": version_code,
        "versionName": version_name,
        "publishedAt": args.published_at,
        "minimumAndroidSdk": minimum_sdk,
        "mandatory": False,
        "releaseNotes": args.release_note,
        "assets": [
            {
                "abi": args.abi,
                "fileName": apk.name,
                "url": f"/v2/app-updates/files/{version_code}/{apk.name}",
                "sizeBytes": size,
                "sha256": sha256,
            }
        ],
        "signatureAlgorithm": "Ed25519",
    }
    manifest["manifestSignature"] = sign(canonical_json(manifest), key, args.openssl)
    encoded = json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    (staging / "manifest.json").write_text(encoded)
    staging.replace(release_dir)
    latest.parent.mkdir(parents=True, exist_ok=True)
    temporary_latest = latest.with_suffix(".json.tmp")
    temporary_latest.write_text(encoded)
    temporary_latest.replace(latest)
    print(f"created {release_dir}")
    print(f"latest pointer: {latest}")
    print(f"APK SHA-256: {sha256}")


if __name__ == "__main__":
    main()
