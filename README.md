# Ganjoor for Android

An Android reader for [Ganjoor](https://ganjoor.net), the open archive of Persian poetry —
240 poets and ~135,000 poems, laid out for comfortable long-form reading in Persian, Urdu
and Arabic script.

Jetpack Compose, Material 3, `minSdk 24`.

## What's here

**Reading.** Poems render as couplets: the two hemistichs of each line stack on a phone, the
first aligned to the start of the line and the second to the end, the way Ganjoor itself reads.
Prose sections (Golestan, for example) fill the column instead. The whole app lays out
right-to-left.

**Fonts.** Two bundled Noto families, switchable while reading:

| Font | File | Note |
|---|---|---|
| Naskh | `res/font/noto_naskh_arabic.ttf` | Covers Persian, Urdu, Arabic and Latin — also carries the UI |
| Nastaliq | `res/font/noto_nastaliq_urdu.ttf` | Traditional hanging script, needs ~2.4× leading (see `readingStyle`) |

Both are variable fonts shipped at their default weight; Android synthesises bold, since
variable axes would need API 26+. Licences are in [`licenses/`](licenses/) (SIL OFL 1.1).

**Themes.** Five modes, persisted across launches: System, Light, Dark, Sepia and Sepia night.
The two sepia schemes are warm paper tones for long sessions — the dark one has no blue cast.

**Text size.** A slider from 14 to 40 sp with a live preview in the settings sheet.

**Summaries.** Ganjoor publishes AI-generated summaries for some poems and couplets. They are
off by default and labelled as AI-generated wherever they appear.

## Where the poems come from

There is no backend. Every "endpoint" is a JSON file in
[`anas-rashid/ganjoor-data`](https://github.com/anas-rashid/ganjoor-data), served over
jsDelivr's CDN, and addressed by the poem's own Ganjoor URL:

```
/hafez/ghazal/sh1  ->  poets/hafez/ghazal/sh1.json
/hafez/ghazal      ->  poets/hafez/ghazal/_cat.json
```

Because paths are URLs, the app never touches the numeric id indexes. Responses land in a 64 MB
OkHttp disk cache, so anything already read stays readable offline.

See [`data/Ganjoor.kt`](app/src/main/java/com/ganjoor/android/data/Ganjoor.kt) — the client is
about thirty lines.

## Build

```sh
./gradlew :app:assembleDebug      # APK
./gradlew :app:testDebugUnitTest  # couplet grouping tests
```

## Layout

```
data/Ganjoor.kt        models, couplet grouping, the static-file client
ui/GanjoorApp.kt       nav graph; routes carry Ganjoor URLs
ui/PoetsScreen.kt      poet grid with name filter
ui/CategoryScreen.kt   collections and poem lists
ui/PoemScreen.kt       the reader
ui/ReadingSettings.kt  theme / font / size sheet
ui/Settings.kt         preferences
ui/Load.kt             fetch + loading + retry
ui/theme/              colour schemes and typography
```

Single module, no DI framework, no ViewModels yet — screens fetch through `Load` and lean on the
HTTP cache. `ponytail:` comments mark the deliberate shortcuts and what would replace them.

## Not built yet

- **Search.** The data set has no search index; this needs either a client-side index or
  `api.ganjoor.net`'s `/api/ganjoor/poems/search`.
- **Bookmarks and reading position.** Nothing is stored locally beyond preferences.
- **Recitations.** Ganjoor has audio for many poems; it isn't in this data set.
- **Offline download.** The HTTP cache covers what you've read, not a whole divan on demand.

## Licensing

The app code is MIT (see [`LICENSE`](LICENSE)). Two things worth knowing about what it builds on:

- **The poetry** is classical Persian verse, long out of copyright.
- **The data set** ([`ganjoor/ganjoor-data`](https://github.com/ganjoor/ganjoor-data)) carries no
  licence file. Worth asking upstream to add an explicit one.
- **[GanjoorService](https://github.com/ganjoor/GanjoorService)**, Ganjoor's own backend and site,
  is GPL-3.0. This app uses none of its code — only data over HTTPS — so it is not a derivative
  work of it.

Fonts are SIL OFL 1.1, which permits bundling in an application.

Not affiliated with or endorsed by Ganjoor.
