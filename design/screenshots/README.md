# Screenshots

Captured on an Android 11 phone emulator at 1080×2340, interface in Persian, light theme,
app version 0.5.0. Reusable anywhere — the store listing, the site, a README.

| | |
|---|---|
| `01-home.png` | the poet shelf, pinned poets first |
| `02-poet.png` | a poet's books |
| `03-ghazal-list.png` | a book's poems, each with its opening line |
| `04-reader.png` | the reader: couplets, breadcrumbs, metre, recitation |
| `05-settings-themes.png` | reading settings — the six themes and the OLED switch |
| `06-settings-language.png` | reading settings — weight, size with a live preview, language |
| `07-language-menu.png` | the globe in the top bar: Persian, Urdu, English |

The store listing for `fa` keeps its own older set under
`fastlane/metadata/android/fa/images/phoneScreenshots/`, which covers nastaliq, sepia, OLED,
offline downloads, bookmarks and licences. Those are not superseded by these; between them
there are more than the eight Play allows, so choosing the final eight is a judgement call.

`../assets/store/` holds the Play graphics: the 512×512 icon, and the 1024×500 feature
graphic with the SVG it is rendered from —

    rsvg-convert -w 1024 -h 500 feature-graphic.svg -o feature-graphic-1024x500.png

It needs Noto Naskh Arabic visible to fontconfig, or the Persian will not shape.
