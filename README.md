# Ganjoor for Android

<div dir="rtl">گنجور — خوانندهٔ شعر پارسی برای اندروید</div>

> **Unofficial.** This is an independent, unofficial Android client for
> [ganjoor.net](https://ganjoor.net). It is a **separate project** from ganjoor.net and its
> GitHub repositories ([`ganjoor/ganjoor`](https://github.com/ganjoor),
> `GanjoorService`, and the rest), built and maintained by **Muhammad Anas Rashid**. The owners of
> ganjoor.net do not run or manage it. Please direct any issue with this app
> [here](https://github.com/anas-rashid/ganjoorandroid/issues) — never to the Ganjoor project.

An Android reader for [Ganjoor](https://ganjoor.net), the open archive of Persian poetry —
240 poets and ~135,000 poems, laid out for comfortable long-form reading in Persian, Urdu
and Arabic script, online or fully offline.

### ⬇ [Download ganjoor-0.4.0.apk](releases/ganjoor-0.4.0.apk)

Android 7.0 and up · 31.7 MB · [older releases](releases/) · `sha256 1c64cefa…fb863ab9fc7`

Android will call the developer unknown and offer to install anyway — it says that about every
app installed outside a store. Allow installs from your browser once and it will go through.
Every release is signed with the same key, so a new one upgrades the last without losing your
bookmarks. [Build it yourself](#build) instead if you would rather.

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

On a tablet or an unfolded foldable (600dp and wider) the poets become a narrow column on the right,
with the open poet's books, chapters and poems in columns beside it and the page in the rest of the
screen. The columns narrow while a poem is open and can be hidden for a reader view. The dictionary
and reading settings open in a panel to the left of the poem instead of over it. Phones are unchanged.

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

Six themes, persisted: System, Light, Dark, Sepia, Sepia night, and Black for OLED panels,
where an unlit pixel costs no power at all. The two sepia schemes are
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

## Dictionary

Tap any word in a poem for its meaning. The bundled database layers two sources — neither is
enough alone:

| | coverage of real poem vocabulary |
|---|---|
| Daneshjoo alone | 71% |
| **Both, with the lookup chain** | **88%** |

Measured over every distinct word in five poems (Hafez ×2, Golestan, Masnavi, a Khayyam rubaʿi).
13 of the 55 remaining misses are Arabic lines quoted inside Persian poems, so Persian coverage
is about 91%.

Lookup widens until something matches: the word as written, then the lemma it inflects from,
then with an affix stripped, then the parts of a ZWNJ compound. The lemma step is what makes
classical verse readable — `افتاد` is only findable as `افتادن`, and Wiktionary ships 149,589
form→lemma pairs that make that possible.

Each entry carries its pronunciation where Wiktionary has one — 101,306 of them, tagged with the
variety, Classical Persian first, because a word in a 14th-century ghazal was not said the way
Tehran says it now. Urdu Wiktionary adds the vowelled spelling and the syllable split in Urdu
script. When nothing matches at all, the sheet offers near words ranked by shared letters.

Poems that Ganjoor has recordings for show a play button, streamed rather than stored: a famous
ghazal often has a dozen readings, and downloading them would dwarf the poems. It is the one
part of the app that needs a connection, and it simply doesn't appear without one.

See [`tools/README.md`](tools/README.md) to rebuild it, and for the licensing of each source.

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

## Open source this app is built on

Every third-party project this app uses, with its repository and terms. None of these projects
endorse or maintain this app. Full licence texts are in [`licenses/`](licenses/) and ship inside
the APK.

### Content and data

| Project | Repository / source | Terms | How it is used |
|---|---|---|---|
| Ganjoor | [ganjoor.net](https://ganjoor.net) | Classical Persian verse, long out of copyright | The poems themselves |
| GanjoorService | [ganjoor/GanjoorService](https://github.com/ganjoor/GanjoorService) | GPL-3.0 | **No code used.** Only `api.ganjoor.net` over HTTPS, for poem search, opening lines and poet portraits — so this app is not a derivative work |
| ganjoor-data | [anas-rashid/ganjoor-data](https://github.com/anas-rashid/ganjoor-data) | **No licence file stated** | The static JSON export every poem is read from |

### Dictionary

| Project | Repository / source | Terms | How it is used |
|---|---|---|---|
| Daneshjoo Dictionary | [0xdolan/Daneshjoo](https://github.com/0xdolan/Daneshjoo) | Repository states MIT (see caveat below) | Persian → English definitions |
| Wiktionary | [en.wiktionary.org](https://en.wiktionary.org) | CC BY-SA 3.0 | Persian/Urdu/Arabic → English definitions, IPA, form→lemma index |
| Urdu Wiktionary | [ur.wiktionary.org](https://ur.wiktionary.org) | CC BY-SA 3.0 | The only Urdu → Urdu definitions, plus vowelled spelling and syllable split |
| wiktextract / kaikki.org | [tatuylonen/wiktextract](https://github.com/tatuylonen/wiktextract) | See repository | Produces the machine-readable Wiktionary exports the build consumes |
| readmdict | [readmdict](https://pypi.org/project/readmdict/) | See project | Build-time only — reads Daneshjoo's `.mdx` |

Because four of the five dictionary sources are CC BY-SA 3.0, **the generated `dictionary.db` is
CC BY-SA 3.0**. The Daneshjoo caveat: the repository states MIT, but the underlying lexicon is a
published Iranian dictionary, so that relicensing is worth verifying before relying on it.

### Fonts

| Project | Repository | Terms |
|---|---|---|
| Noto Naskh Arabic | [notofonts/arabic](https://github.com/notofonts/arabic) | SIL OFL 1.1 |
| Noto Nastaliq Urdu | [notofonts/nastaliq](https://github.com/notofonts/nastaliq) | SIL OFL 1.1 |
| Libron | [nicoverbruggen/libron](https://github.com/nicoverbruggen/libron) | SIL OFL 1.1 |

### Libraries and icons

All Apache-2.0.

| Project | Repository |
|---|---|
| Jetpack Compose, AndroidX (core, activity, lifecycle, navigation) | [androidx/androidx](https://github.com/androidx/androidx) |
| Kotlin standard library | [JetBrains/kotlin](https://github.com/JetBrains/kotlin) |
| kotlinx.serialization | [Kotlin/kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) |
| kotlinx.coroutines | [Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) |
| OkHttp | [square/okhttp](https://github.com/square/okhttp) |
| Okio | [square/okio](https://github.com/square/okio) |
| Coil 3 | [coil-kt/coil](https://github.com/coil-kt/coil) |
| Accompanist (drawablepainter, via Coil) | [google/accompanist](https://github.com/google/accompanist) |
| Material Symbols (share, lookup, assistant glyphs, copied as vector paths) | [google/material-design-icons](https://github.com/google/material-design-icons) |

No Google Play Services, Firebase, analytics or trackers. The only permission requested is
`INTERNET`.

## Licensing

The app code is MIT (see [`LICENSE`](LICENSE)). Every other component — the poems, the three
bundled fonts, and each library — is credited with its terms in
[`licenses/README.md`](licenses/README.md), and the full licence texts ship inside the APK,
readable at **Reading settings → About & licences**.

The two things worth knowing up front: the **data set** carries no licence file, and
**GanjoorService** is GPL-3.0 but none of its code is used here — only data over HTTPS — so this
app is not a derivative work of it.

This app is an **unofficial, independent** client, built and maintained by **Muhammad Anas Rashid**. It is
a wholly separate project from ganjoor.net and the Ganjoor GitHub repositories, and the owners of
ganjoor.net do not run or manage it. The same statement is shown in the app itself, at **Reading
settings → About & licences**.
