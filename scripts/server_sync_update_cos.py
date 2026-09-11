#!/usr/bin/env python3
"""Copy one verified immutable Sonorus release from local storage to private COS."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import re
import subprocess
import tempfile
from pathlib import Path

APPLICATION_IDS = {
    "debug": "io.github.cluno1.sonorus.debug",
    "stable": "io.github.cluno1.sonorus",
}
BUCKET = "sonorus-updates-1328751369"
REGION = "ap-guangzhou"
UPDATE_ROOT = Path("/srv/sonorus-updates")
PUBLIC_KEY_ROOT = Path("/etc/sonorus-update")
SAFE_APK = re.compile(r"^[A-Za-z0-9._-]+\.apk$")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonical_json(manifest: dict[str, object]) -> bytes:
    payload = dict(manifest)
    payload.pop("manifestSignature", None)
    return json.dumps(
        payload,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode()


def verify_signature(manifest: dict[str, object], channel: str) -> None:
    public_key = PUBLIC_KEY_ROOT / f"{channel}-manifest-public.pem"
    if not public_key.is_file():
        raise SystemExit(f"{channel} manifest public key is not installed")
    try:
        signature = base64.b64decode(str(manifest["manifestSignature"]), validate=True)
    except (KeyError, ValueError):
        raise SystemExit("manifest signature is missing or invalid") from None
    if len(signature) != 64 or manifest.get("signatureAlgorithm") != "Ed25519":
        raise SystemExit("manifest does not use a valid Ed25519 signature")
    with tempfile.TemporaryDirectory(prefix="sonorus-cos-signature-") as directory:
        work = Path(directory)
        payload_path = work / "manifest.json"
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
                str(public_key),
                "-rawin",
                "-in",
                str(payload_path),
                "-sigfile",
                str(signature_path),
            ],
            check=True,
            stdout=subprocess.DEVNULL,
        )


def validated_assets(channel: str, version: int) -> list[tuple[Path, str, str]]:
    release = UPDATE_ROOT / channel / "releases" / str(version)
    manifest_path = release / "manifest.json"
    try:
        manifest = json.loads(manifest_path.read_bytes())
    except (FileNotFoundError, json.JSONDecodeError):
        raise SystemExit("immutable release manifest is missing or invalid") from None
    if (
        manifest.get("schemaVersion") != 1
        or manifest.get("channel") != channel
        or manifest.get("applicationId") != APPLICATION_IDS[channel]
        or manifest.get("versionCode") != version
    ):
        raise SystemExit("immutable release manifest identity is invalid")
    verify_signature(manifest, channel)
    assets = manifest.get("assets")
    if not isinstance(assets, list) or not assets:
        raise SystemExit("immutable release manifest contains no assets")
    verified: list[tuple[Path, str, str]] = []
    for asset in assets:
        name = asset.get("fileName") if isinstance(asset, dict) else None
        digest = str(asset.get("sha256", "")) if isinstance(asset, dict) else ""
        if not isinstance(name, str) or not SAFE_APK.fullmatch(name):
            raise SystemExit("immutable release manifest contains an unsafe APK name")
        if not re.fullmatch(r"[0-9a-f]{64}", digest):
            raise SystemExit("immutable release manifest contains an invalid APK digest")
        path = release / name
        if (
            not path.is_file()
            or path.stat().st_size != asset.get("sizeBytes")
            or sha256(path) != digest
        ):
            raise SystemExit(f"immutable release APK failed validation: {name}")
        verified.append((path, f"{channel}/releases/{version}/{digest}", digest))
    return verified


def response_status(error: Exception) -> int | None:
    getter = getattr(error, "get_status_code", None)
    if callable(getter):
        try:
            return int(getter())
        except (TypeError, ValueError):
            return None
    return None


def verify_cos_object(client: object, key: str, expected_size: int, expected_sha: str) -> None:
    response = client.get_object(Bucket=BUCKET, Key=key)
    stream = response["Body"].get_raw_stream()
    digest = hashlib.sha256()
    size = 0
    while True:
        chunk = stream.read(1024 * 1024)
        if not chunk:
            break
        size += len(chunk)
        digest.update(chunk)
    if size != expected_size or digest.hexdigest() != expected_sha:
        raise SystemExit(f"COS object failed full read-back verification: {key}")


def sync_asset(client: object, path: Path, key: str, digest: str) -> str:
    exists = False
    try:
        head = client.head_object(Bucket=BUCKET, Key=key)
        exists = True
    except Exception as error:
        if response_status(error) != 404:
            raise
    if exists:
        content_length = int(head.get("Content-Length", head.get("ContentLength", -1)))
        if content_length != path.stat().st_size:
            raise SystemExit(f"immutable COS object has a different size: {key}")
    else:
        client.upload_file(
            Bucket=BUCKET,
            Key=key,
            LocalFilePath=str(path),
            PartSize=8,
            MAXThread=4,
            EnableMD5=True,
            ContentType="application/octet-stream",
            ContentDisposition="attachment",
            StorageClass="STANDARD",
            Metadata={"sha256": digest},
        )
    head = client.head_object(Bucket=BUCKET, Key=key)
    if (
        int(head.get("Content-Length", head.get("ContentLength", -1))) != path.stat().st_size
        or head.get("Content-Type", head.get("ContentType")) != "application/octet-stream"
        or head.get("Content-Disposition", head.get("ContentDisposition")) != "attachment"
        or head.get("x-cos-storage-class", "STANDARD") != "STANDARD"
    ):
        raise SystemExit(f"COS object metadata failed validation: {key}")
    verify_cos_object(client, key, path.stat().st_size, digest)
    return "verified" if exists else "uploaded"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--channel", choices=("debug", "stable"), required=True)
    parser.add_argument("--version", type=int, required=True)
    args = parser.parse_args()
    if args.version <= 0:
        raise SystemExit("version must be positive")
    secret_id = os.environ.get("RHYTHM_COS_SECRET_ID", "")
    secret_key = os.environ.get("RHYTHM_COS_SECRET_KEY", "")
    if not secret_id or not secret_key:
        raise SystemExit("COS credentials are not configured")

    from qcloud_cos import CosConfig, CosS3Client

    client = CosS3Client(
        CosConfig(
            Region=REGION,
            SecretId=secret_id,
            SecretKey=secret_key,
            Token=os.environ.get("RHYTHM_COS_SESSION_TOKEN"),
        )
    )
    assets = validated_assets(args.channel, args.version)
    outcomes = [sync_asset(client, path, key, digest) for path, key, digest in assets]
    print(
        f"COS sync complete: channel={args.channel} version={args.version} "
        f"assets={len(assets)} outcomes={','.join(outcomes)}"
    )


if __name__ == "__main__":
    main()
