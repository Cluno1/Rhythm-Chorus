#!/usr/bin/env python3
"""Validate a Sonorus stable version and print the non-destructive release commands."""

import re
import subprocess
import sys

version = sys.argv[1] if len(sys.argv) > 1 else "1.0.0"
match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)", version)
if not match:
    raise SystemExit("usage: prepare_release.py MAJOR.MINOR.PATCH")
major, minor, patch = map(int, match.groups())
if minor > 999 or patch > 999:
    raise SystemExit("minor and patch must be <= 999")
version_code = major * 1_000_000 + minor * 1_000 + patch

subprocess.run(["scripts/check_sonorus_brand.py"], check=True)
subprocess.run(["scripts/verify_brand_assets.sh"], check=True)
print(f"Sonorus {version} -> versionCode {version_code}")
print(f"Dry run: scripts/release_dry_run.sh {version}")
print(f"After review: git tag -a v{version} -m 'Sonorus {version}' && git push origin v{version}")
print("Pushing the tag runs the signed GitHub Release workflow; this script does not tag or publish anything.")
