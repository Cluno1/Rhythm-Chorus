#!/usr/bin/env bash
set -euo pipefail

version="${1:-1.0.0}"
if [[ ! "$version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "usage: $0 MAJOR.MINOR.PATCH" >&2
  exit 2
fi
major="${BASH_REMATCH[1]}"; minor="${BASH_REMATCH[2]}"; patch="${BASH_REMATCH[3]}"
if (( minor > 999 || patch > 999 )); then
  echo "minor and patch must be <= 999" >&2
  exit 2
fi
code=$((major * 1000000 + minor * 1000 + patch))

scripts/check_sonorus_brand.py
scripts/verify_brand_assets.sh
./gradlew testGithubDebugUnitTest lintGithubRelease assembleGithubRelease \
  -PallowDebugReleaseSigning=true \
  -PversionNameOverride="$version" \
  -PversionCodeOverride="$code" \
  -PreleaseDateOverride="$(date -u +%F)"

output=app/build/outputs/apk/github/release
shopt -s nullglob
apks=("$output"/Sonorus-*githubRelease*.apk)
test ${#apks[@]} -gt 0
for apk in "${apks[@]}"; do shasum -a 256 "$apk"; done
