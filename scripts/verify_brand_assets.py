#!/usr/bin/env python3
"""Portable structural checks for the versioned Sonorus image assets."""

from hashlib import sha256
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[1]
MASTER = ROOT / "assets/brand/sonorus-master.png"
EXPECTED = (ROOT / "assets/brand/sonorus-master.sha256").read_text().split()[0]


def png_info(path: Path) -> tuple[int, int, int]:
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise SystemExit(f"not a PNG: {path}")
    width, height, _, color_type = struct.unpack(">IIBB", data[16:26])
    return width, height, color_type


if sha256(MASTER.read_bytes()).hexdigest() != EXPECTED:
    raise SystemExit("Sonorus master hash changed")
if png_info(MASTER)[:2] != (1024, 1024):
    raise SystemExit("Sonorus master must remain 1024x1024")

alpha_pngs = [
    ROOT / "app/src/main/res/drawable-nodpi/sonorus_symbol.png",
    ROOT / "app/src/main/res/drawable-nodpi/sonorus_symbol_monochrome.png",
    ROOT / "app/src/main/res/drawable-xxxhdpi/ic_notification.png",
]
for path in alpha_pngs:
    if png_info(path)[2] not in (4, 6):
        raise SystemExit(f"asset has no alpha channel: {path}")

required = alpha_pngs + [
    ROOT / f"app/src/main/res/mipmap-{density}/ic_launcher.webp"
    for density in ("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
]
for path in required:
    if not path.is_file() or path.stat().st_size == 0:
        raise SystemExit(f"missing brand asset: {path}")

print("Sonorus brand assets verified")
