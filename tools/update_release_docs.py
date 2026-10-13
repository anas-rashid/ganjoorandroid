#!/usr/bin/env python3
"""
Bring the downloads in README.md and the checksum list in releases/README.md
into step with whatever version app/build.gradle.kts declares.

Every release ships as two APKs, one per flavour:

    foss  no Google code at all; Google Translate through your own Cloud key
    play  Google Translate on the phone with ML Kit; the Play Store build

Run it after building both and copying them into releases/:

    ./gradlew :app:assembleFossRelease :app:assemblePlayRelease
    cp app/build/outputs/apk/foss/release/khazeena-<v>-foss-release.apk releases/khazeena-<v>-foss.apk
    cp app/build/outputs/apk/play/release/khazeena-<v>-play-release.apk releases/khazeena-<v>-play.apk
    python3 tools/update_release_docs.py

Google Play itself takes a bundle instead: ./gradlew :app:bundlePlayRelease.

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

# Flavour, and the one line that tells a reader which to pick.
FLAVOURS = [
    ("foss", "no Google code; Google Translate with your own Cloud key"),
    ("play", "translates on the phone with Google's ML Kit; needs Google Play Services"),
]


def version() -> str:
    build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    m = re.search(r'versionName\s*=\s*"([^"]+)"', build)
    if not m:
        sys.exit("could not find versionName in app/build.gradle.kts")
    return m.group(1)


def main() -> None:
    v = version()
    lines = []
    checksums = []
    for flavour, note in FLAVOURS:
        name = f"khazeena-{v}-{flavour}.apk"
        apk = ROOT / "releases" / name
        if not apk.exists():
            sys.exit(f"releases/{name} is missing — build and copy it in first")
        digest = hashlib.sha256(apk.read_bytes()).hexdigest()
        megabytes = apk.stat().st_size / 1_000_000
        short = f"{digest[:8]}…{digest[-11:]}"
        lines.append(f"- [**{name}**](releases/{name}) · {megabytes:.1f} MB · `sha256 {short}` · {note}")
        checksums.append(f"{digest}  {name}")
        print(f"{name}  {megabytes:.1f} MB  {digest}")

    # README.md — the download heading, one line per APK, and the line of facts under them.
    readme = ROOT / "README.md"
    text = readme.read_text(encoding="utf-8")
    block = (
        f"### ⬇ Download ganjoor {v}\n\n"
        + "\n".join(lines)
        + "\n\nAndroid 7.0 and up · [older releases](releases/)\n"
    )
    text, n = re.subn(r"^### ⬇ .*?^Android 7\.0 and up[^\n]*\n", lambda _: block, text, count=1, flags=re.M | re.S)
    if n != 1:
        sys.exit("could not find the download block in README.md")
    readme.write_text(text, encoding="utf-8")

    # releases/README.md — one checksum line per archived APK, in version order.
    index = ROOT / "releases" / "README.md"
    listing = index.read_text(encoding="utf-8")
    for line in checksums:
        name = line.split("  ", 1)[1]
        existing = re.search(rf"^[0-9a-f]{{64}}  {re.escape(name)}$", listing, re.M)
        if existing:
            listing = listing[: existing.start()] + line + listing[existing.end():]
            continue
        last = None
        for last in re.finditer(r"^[0-9a-f]{64}  khazeena-[^\n]+$", listing, re.M):
            pass
        if not last:
            sys.exit("could not find the checksum block in releases/README.md")
        listing = listing[: last.end()] + "\n" + line + listing[last.end():]
    index.write_text(listing, encoding="utf-8")

    print("README.md and releases/README.md are in step")


if __name__ == "__main__":
    main()
