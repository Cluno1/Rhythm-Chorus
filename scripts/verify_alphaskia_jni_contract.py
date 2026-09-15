#!/usr/bin/env python3
"""Fail when R8 changes Java names consumed by libalphaskiajni.so."""

from __future__ import annotations

import argparse
import re
from pathlib import Path


PACKAGE_PREFIX = "alphaTab.alphaSkia."
HANDLE_CLASS = f"{PACKAGE_PREFIX}AlphaSkiaNative"
CLASS_MAPPING = re.compile(r"^(\S+) -> (\S+):$")


def original_member_name(mapping_left: str) -> str:
    signature = mapping_left.split("(", 1)[0]
    token = signature.rsplit(" ", 1)[-1]
    return token.rsplit(":", 1)[-1]


def verify_mapping(path: Path) -> tuple[int, int]:
    current_class: str | None = None
    class_count = 0
    member_count = 0
    renamed: list[str] = []

    for raw_line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        class_match = CLASS_MAPPING.fullmatch(raw_line)
        if class_match:
            original_class, output_class = class_match.groups()
            current_class = original_class if original_class.startswith(PACKAGE_PREFIX) else None
            if current_class is not None:
                class_count += 1
                if original_class != output_class:
                    renamed.append(f"{original_class} -> {output_class}")
            continue

        if current_class is None:
            continue
        stripped = raw_line.strip()
        if not stripped or stripped.startswith("#") or " -> " not in stripped:
            continue
        mapping_left, output_name = stripped.rsplit(" -> ", 1)
        original_name = original_member_name(mapping_left)
        member_count += 1
        if original_name != output_name:
            renamed.append(f"{current_class}.{original_name} -> {output_name}")

    if class_count == 0:
        raise SystemExit(f"No {PACKAGE_PREFIX} classes found in {path}")
    if renamed:
        raise SystemExit(
            "alphaSkia JNI classes or members renamed by R8:\n  "
            + "\n  ".join(sorted(set(renamed)))
        )
    return class_count, member_count


def verify_seeds(path: Path) -> None:
    required = f"{HANDLE_CLASS}: long handle"
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    if required not in lines:
        raise SystemExit(f"Required JNI field {HANDLE_CLASS}.handle:J is absent from R8 seeds {path}")


def verify_usage(path: Path) -> None:
    removed_handle = f"{HANDLE_CLASS}:\n    long handle"
    text = path.read_text(encoding="utf-8", errors="replace")
    if removed_handle in text:
        raise SystemExit(f"Required JNI field {HANDLE_CLASS}.handle:J was removed according to {path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mapping", type=Path, required=True)
    parser.add_argument("--seeds", type=Path, required=True)
    parser.add_argument("--usage", type=Path, required=True)
    args = parser.parse_args()
    classes, members = verify_mapping(args.mapping)
    verify_seeds(args.seeds)
    verify_usage(args.usage)
    print(
        f"verified {classes} alphaSkia JNI classes and {members} members in R8 mapping; "
        "AlphaSkiaNative.handle:J retained in R8 seeds and usage"
    )


if __name__ == "__main__":
    main()
