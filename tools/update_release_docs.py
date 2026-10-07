#!/usr/bin/env python3
"""
Bring the download line in README.md and the checksum list in releases/README.md
into step with whatever version app/build.gradle.kts declares.

Run it after building and copying the signed APK into releases/:

    ./gradlew :app:assembleRelease
    cp app/build/outputs/apk/release/ganjoor-<v>-release.apk releases/ganjoor-<v>.apk
    python3 tools/update_release_docs.py

It reads the version from the build file rather than taking an argument, so the
documentation can only ever describe the release the build actually produces.
ReleaseDocsTest fails if either file falls behind, so forgetting this is caught
rather than shipped.
"""
import hashlib
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent


def version() -> str:
    build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    m = re.search(r'versionName\s*=\s*"([^"]+)"', build)
    if not m:
        sys.exit("could not find versionName in app/build.gradle.kts")
    return m.group(1)


def main() -> None:
    v = version()
    apk = ROOT / "releases" / f"ganjoor-{v}.apk"
    if not apk.exists():
        sys.exit(f"{apk.relative_to(ROOT)} is missing — build and copy it in first")

    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    megabytes = apk.stat().st_size / 1_000_000

    # README.md — the download heading and the line of facts under it.
    readme = ROOT / "README.md"
    text = readme.read_text(encoding="utf-8")
    text, n = re.subn(
        r"### ⬇ \[Download ganjoor-[^\]]+\]\(releases/ganjoor-[^)]+\)",
        f"### ⬇ [Download ganjoor-{v}.apk](releases/ganjoor-{v}.apk)",
        text,
        count=1,
    )
    if n != 1:
        sys.exit("could not find the download heading in README.md")
    short = f"{digest[:8]}…{digest[-11:]}"
    text, n = re.subn(
        r"Android 7\.0 and up · [^·]+· \[older releases\]\(releases/\) · `sha256 [^`]+`",
        f"Android 7.0 and up · {megabytes:.1f} MB · [older releases](releases/) · `sha256 {short}`",
        text,
        count=1,
    )
    if n != 1:
        sys.exit("could not find the download facts line in README.md")
    readme.write_text(text, encoding="utf-8")

    # releases/README.md — one checksum line per archived APK, in version order.
    index = ROOT / "releases" / "README.md"
    listing = index.read_text(encoding="utf-8")
    line = f"{digest}  ganjoor-{v}.apk"
    if line not in listing:
        # replace an existing line for this version, or append to the block
        existing = re.search(rf"^[0-9a-f]{{64}}  ganjoor-{re.escape(v)}\.apk$", listing, re.M)
        if existing:
            listing = listing[: existing.start()] + line + listing[existing.end():]
        else:
            last = None
            for last in re.finditer(r"^[0-9a-f]{64}  ganjoor-[^\n]+$", listing, re.M):
                pass
            if not last:
                sys.exit("could not find the checksum block in releases/README.md")
            listing = listing[: last.end()] + "\n" + line + listing[last.end():]
        index.write_text(listing, encoding="utf-8")

    print(f"{v}  {megabytes:.1f} MB  {digest}")
    print("README.md and releases/README.md are in step")


if __name__ == "__main__":
    main()
