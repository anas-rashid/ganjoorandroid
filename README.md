# Ganjoor for Android

<div dir="rtl">گنجور — خوانندهٔ شعر پارسی برای اندروید</div>

An Android reader for [Ganjoor](https://ganjoor.net), the open archive of Persian poetry —
240 poets and ~135,000 poems, laid out for comfortable long-form reading in Persian, Urdu
and Arabic script, online or fully offline.

Jetpack Compose, Material 3, `minSdk 24`. No account, no tracking, no server of its own.

## Reading

Poems are set as couplets: the two hemistichs of each line stack on a phone, the first aligned
to the start of the line and the second to the end, the way Ganjoor itself reads. Prose sections
— Golestan, Nowruznameh — fill the column instead. Category listings show each poem's opening
line under its title, because "Ghazal 237" tells you nothing.

Tap a couplet to save that passage or copy it; a saved passage keeps a tappable link back to the
poem it came from, which a plain copy would lose. The whole poem can be bookmarked from the top
bar, and any span of text can be selected and copied the usual way.

The whole interface lays out and navigates right-to-left, whichever UI language is chosen.

## Fonts

| Font | Used for | Licence |
|---|---|---|
| Noto Naskh Arabic | Poems, and the Persian/Urdu interface | OFL 1.1 |
| Noto Nastaliq Urdu | Poems, when nastaliq is selected | OFL 1.1 |
| [Libron](https://github.com/nicoverbruggen/libron) | The English interface only | OFL 1.1 |

Naskh is the default. Both Arabic-script faces are variable fonts registered at four weights, so
the text can be thickened — thin naskh strokes wash out on a lit screen, especially in the dark
themes. Real axis interpolation needs API 26+; below that Android synthesises the heavier
weights. Libron is a reading serif and never touches the poems: content is always naskh or
nastaliq, whatever language the interface is in.

Licences are in [`licenses/`](licenses/).

## Themes, size, language

Five themes, persisted: System, Light, Dark, Sepia and Sepia night. The two sepia schemes are
warm paper tones for long sessions; the dark one has no blue cast. Text size runs 14–40 sp with a
live preview in the settings sheet.

The interface speaks **Persian, Urdu and English**, Persian by default regardless of the phone's
locale. Switch from the globe in the top bar or from the settings sheet.

## Offline

Download a poet — or every poet — and read with no connection at all. Downloads are listed with
each poet's portrait and tick boxes for picking several at once; the screen shows how much space
they take and lets you delete any of them. Offline mode then refuses the network entirely and
reads only what's on the device, saying so plainly when you open something that was never
downloaded, rather than blaming your connection.

Downloads run one poet at a time and are resumable: anything already on disk is skipped, so
restarting an interrupted download picks up where it left off.

## Where the poems come from

There is no backend. Every "endpoint" is a JSON file in
[`anas-rashid/ganjoor-data`](https://github.com/anas-rashid/ganjoor-data), served over
jsDelivr's CDN, and addressed by the poem's own Ganjoor URL:

```
/hafez/ghazal/sh1  ->  poets/hafez/ghazal/sh1.json
/hafez/ghazal      ->  poets/hafez/ghazal/_cat.json
```

Because paths are URLs, the app never touches the numeric id indexes. Downloaded files mirror
that same layout under `filesDir/offline`, which is why offline mode is a single lookup rather
than a parallel code path. Everything else lands in a 64 MB OkHttp disk cache.

One exception: opening lines aren't in the data set, so they're fetched best-effort from
`api.ganjoor.net` and cached. Adding an `Excerpt` field to `_cat.json` upstream would remove
that dependency — see the `ponytail:` note in `Ganjoor.kt`.

## Build

```sh
./gradlew :app:assembleDebug      # APK
./gradlew :app:testDebugUnitTest  # couplet grouping tests
```

Release signing is optional. Drop a `keystore.properties` next to `settings.gradle.kts` with
`storeFile`, `storePassword`, `keyAlias` and `keyPassword` to sign locally; without it the
release build still succeeds, unsigned. The file and any `*.jks` are gitignored.

## Layout

```
data/Ganjoor.kt        models, couplet grouping, the static-file client
data/Offline.kt        downloaded poems on disk
data/Downloads.kt      download queue and progress
data/Bookmarks.kt      saved poems and passages
ui/GanjoorApp.kt       nav graph; routes carry Ganjoor URLs
ui/PoetsScreen.kt      poet grid, search, sort
ui/CategoryScreen.kt   collections and poem lists
ui/PoemScreen.kt       the reader
ui/DownloadsScreen.kt  multi-select offline downloads
ui/BookmarksScreen.kt  saved poems and passages
ui/ReadingSettings.kt  theme / font / weight / size / language / offline
ui/theme/              colour schemes and typography
```

Single module, no DI framework, no ViewModels yet — screens fetch through `Load` and lean on the
cache. `ponytail:` comments mark the deliberate shortcuts and what would replace them.

## Not built yet

- **Search within poems.** The data set has no search index; this needs either a client-side
  index or `api.ganjoor.net`'s `/api/ganjoor/poems/search`.
- **Reading position.** Bookmarks are saved, but not where you stopped reading.
- **Recitations.** Ganjoor has audio for many poems; it isn't in this data set.

## Publishing to F-Droid

The build already meets the [quick start
guide](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/):

- MIT licensed, with a `LICENSE` file.
- Every dependency is FOSS (AndroidX, Kotlin, OkHttp, Coil, Accompanist). No Play Services, no
  Firebase, no analytics, no trackers. Only the `INTERNET` permission, and cleartext disabled.
- `versionCode`/`versionName` are literals in `app/build.gradle.kts`, not derived from git.
- Dependency versions are all pinned; no version ranges.
- `dependenciesInfo` is switched off — that blob is signed with a Google key and isn't
  reproducible, so F-Droid rejects APKs carrying it.
- The build succeeds with no keystore, so F-Droid can sign with its own key.
- Store listing lives in `fastlane/metadata/android/{en-US,fa,ur}/`. Drop screenshots into each
  locale's `images/phoneScreenshots/`.

Two things to do before submitting: tag a release (`v0.1.0`), and check that F-Droid's build
server supports **AGP 9.4.1** — it is new, and that is the most likely thing to hold up a merge.

## Licensing

The app code is MIT (see [`LICENSE`](LICENSE)). What it builds on:

- **The poetry** is classical Persian verse, long out of copyright.
- **The data set** ([`ganjoor/ganjoor-data`](https://github.com/ganjoor/ganjoor-data)) carries no
  licence file at all. Worth asking upstream to add an explicit one.
- **[GanjoorService](https://github.com/ganjoor/GanjoorService)**, Ganjoor's own backend and site,
  is GPL-3.0. This app uses none of its code — only data over HTTPS — so it is not a derivative
  work of it. Nothing in Ganjoor's repositories restricts AI-assisted use.
- **The icon** is original: an eight-point shamsa, the star that tiles Persian architecture.
  Ganjoor's own app icons are unlicensed and are their mark, so they are not used here.

Not affiliated with or endorsed by Ganjoor.
