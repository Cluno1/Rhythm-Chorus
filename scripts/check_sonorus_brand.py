#!/usr/bin/env python3
"""Reject accidental upstream branding in Sonorus user-facing/release files."""

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
TARGETS = [
    ROOT / "app/src/main/res",
    ROOT / "app/src/debug/res",
    ROOT / "app/src/main/AndroidManifest.xml",
    ROOT / "README.md",
    ROOT / ".github/workflows",
]
LEGAL_MARKERS = (
    "upstream",
    "unofficial derivative",
    "based on",
    "spdx-filecopyrighttext",
    "cromaguy/rhythm",
    "rhythm authors",
    "team chromahub",
)

violations: list[str] = []
for target in TARGETS:
    files = [target] if target.is_file() else [p for p in target.rglob("*") if p.is_file()]
    for path in files:
        if path.suffix.lower() not in {".xml", ".md", ".yml", ".yaml", ".txt"}:
            continue
        for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            lower = line.lower()
            if ("Rhythm" in line or "ChromaHub" in line) and not any(marker in lower for marker in LEGAL_MARKERS):
                # Internal resource/style/class identifiers remain stable compatibility names.
                if any(token in line for token in ("@style/Theme.Rhythm", ".Rhythm")):
                    continue
                violations.append(f"{path.relative_to(ROOT)}:{line_number}: {line.strip()}")

# Kotlin has many legal/internal Rhythm identifiers that must remain stable, so scan only
# concrete identity and newly-created user-visible filename regressions here.
for path in (ROOT / "app/src").rglob("*.kt"):
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if "chromahub.rhythm.app" in line or '"rhythm_backup_' in line:
            violations.append(f"{path.relative_to(ROOT)}:{line_number}: {line.strip()}")

if violations:
    print("Unexpected upstream brand references:", *violations, sep="\n", file=sys.stderr)
    raise SystemExit(1)
print("Sonorus brand scan passed")
