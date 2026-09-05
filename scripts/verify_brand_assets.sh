#!/usr/bin/env bash
set -euo pipefail
root_dir="$(cd "$(dirname "$0")/.." && pwd)"
exec python3 "$root_dir/scripts/verify_brand_assets.py"
