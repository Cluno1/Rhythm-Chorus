#!/usr/bin/env python3
"""Verify signed Sonorus release APK identities and write SHA-256 sidecars."""

from __future__ import annotations

import argparse
import subprocess
from pathlib import Path

from build_update_manifest import (
    file_sha256,
    normalize_digest,
    parse_badging,
    signer_digest,
)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--application-id", required=True)
    parser.add_argument("--expected-certificate-sha256", required=True)
    parser.add_argument("--aapt", default="aapt")
    parser.add_argument("--apksigner", default="apksigner")
    parser.add_argument("apk", nargs="+")
    args = parser.parse_args()

    expected_certificate = normalize_digest(args.expected_certificate_sha256)
    observed_certificates: set[str] = set()
    for value in args.apk:
        apk = Path(value).resolve(strict=True)
        if apk.suffix != ".apk":
            raise SystemExit(f"release input is not an APK: {apk.name}")
        subprocess.run(
            [args.apksigner, "verify", "--verbose", str(apk)],
            check=True,
            stdout=subprocess.DEVNULL,
        )
        application_id, version_code, version_name, _ = parse_badging(
            apk, args.aapt
        )
        if application_id != args.application_id:
            raise SystemExit(
                f"unexpected application ID for {apk.name}: {application_id}"
            )
        certificate = signer_digest(apk, args.apksigner)
        if certificate != expected_certificate:
            raise SystemExit(f"unexpected signing certificate for {apk.name}")
        observed_certificates.add(certificate)
        digest = file_sha256(apk)
        apk.with_name(f"{apk.name}.sha256").write_text(f"{digest}  {apk.name}\n")
        print(
            f"verified {apk.name}: applicationId={application_id} "
            f"versionCode={version_code} versionName={version_name} sha256={digest}"
        )

    if len(observed_certificates) != 1:
        raise SystemExit("release APKs do not share exactly one signing certificate")


if __name__ == "__main__":
    main()
