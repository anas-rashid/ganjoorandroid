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

## Tablet, landscape

`tablet/` holds the same app on an Android 16 Pixel Tablet emulator at 2560×1600, landscape,
Persian. These are also the `fa` and `en-US` listings' `tenInchScreenshots/`.

| | |
|---|---|
| `01-home.png` | the poet shelf, eight to a row |
| `02-poet.png` | a poet opened: poets narrowed to a column on the right, their books beside it |
| `03-poems.png` | a book's poems as a third column, and as cards in the page |
| `04-reading.png` | a poem with the columns narrowed, two hemistichs to a line |
| `05-reader-view.png` | the columns hidden, leaving the poem |
| `06-dictionary.png` | the dictionary in a panel beside the poem rather than over it |
| `07-settings.png` | reading settings in the same panel |
| `08-sepia-night-nastaliq.png` | the reader in sepia night, set in nastaliq |

`../assets/store/` holds the Play graphics: the 512×512 icon, and the 1024×500 feature
graphic with the SVG it is rendered from —

    rsvg-convert -w 1024 -h 500 feature-graphic.svg -o feature-graphic-1024x500.png

It needs Noto Naskh Arabic visible to fontconfig, or the Persian will not shape.
