# Screenshots

Captured on an Android 16 phone emulator at 1080×2400, interface in Persian, light theme unless
noted, app version 0.6.0 (Khazeena). Reusable anywhere — the store listing, the site, a README.

| | |
|---|---|
| `01-home.png` | the poet shelf, pinned poets first |
| `02-poet.png` | a poet's books |
| `03-poems.png` | a book's poems, each with its opening line |
| `04-reader.png` | the reader: couplets in cards, breadcrumbs, metre, recitation; a saved couplet marked |
| `05-couplet.png` | a couplet's actions as icons — save (filled once saved), copy, share, ask |
| `06-dictionary.png` | a tapped word: pronunciations, meanings with their source, the couplet's actions |
| `07-settings.png` | reading settings — themes, the OLED switch, naskh or nastaliq |
| `08-sepia-nastaliq.png` | the reader in the sepia theme, set in nastaliq |

These are also the `fa` and `en-US` store listings, under
`fastlane/metadata/android/*/images/phoneScreenshots/` — eight, Play's maximum. Not shown:
offline downloads, bookmarks, licences, and the Urdu and English interfaces.

`../assets/store/` holds the Play graphics: the 512×512 icon, and the 1024×500 feature
graphic with the SVG it is rendered from —

    rsvg-convert -w 1024 -h 500 feature-graphic.svg -o feature-graphic-1024x500.png

It needs Noto Naskh Arabic visible to fontconfig, or the Persian will not shape.
