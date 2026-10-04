# Licences

Everything this app is built from, and the terms it comes under. The app itself is MIT; see
[`../LICENSE`](../LICENSE).

The full text of each licence below also ships **inside the APK**, under
`assets/licenses/`, and is readable in the app at **Reading settings → About & licences**. The
SIL Open Font License requires the licence to travel with the software, so the font licences are
bundled rather than merely linked.

## Content

| What | Source | Terms |
|---|---|---|
| The poems | [ganjoor.net](https://ganjoor.net) | Classical Persian verse, long out of copyright |
| The data export | [ganjoor-data](https://github.com/anas-rashid/ganjoor-data) | **No licence file.** Worth asking upstream to add one |
| Poem search, first lines | `api.ganjoor.net` | A public web service. Its implementation, [GanjoorService](https://github.com/ganjoor/GanjoorService), is GPL-3.0 — this app uses none of that code, only data over HTTPS, so it is not a derivative work |
| AI summaries | Ganjoor's own feature, shown only when enabled | Labelled in the app as AI-generated |

Nothing in Ganjoor's repositories restricts AI-assisted use.

## The AI assistant

Optional, off by default, and the app ships no vendor SDK, no API key and no default endpoint.
Nothing is sent anywhere until a reader configures a server themselves.

| What | Terms |
| --- | --- |
| The client | Ours, MIT. It speaks the OpenAI chat-completions shape, which is a request format, not anyone's code, plus Anthropic's `/v1/messages` shape for Claude |
| A self-hosted server (Ollama, LM Studio, llama.cpp, LocalAI) | Each is separately installed free software; this app only makes HTTP requests to it |
| A hosted service (OpenAI, DeepSeek, Gemini, Claude) | Whatever the reader agreed with that service. Their terms and privacy policy govern what they do with the text sent |
| What a model writes back | Generated text, not Ganjoor's and not ours. Shown as an aid, never stored next to the poem |

For F-Droid: nothing here is a non-free dependency, since no such code ships. A reader *may*
point it at a non-free network service, which is why the setting is off by default and the
privacy note sits on its own settings page.

## Dictionary

Five sources, each row in the database tagged with the one it came from so the app can name it
and so any of them can be dropped without rebuilding the others.

| Source | Direction | Licence |
|---|---|---|
| [Wiktionary](https://en.wiktionary.org) | Persian → English | CC BY-SA 3.0 |
| [Wiktionary](https://en.wiktionary.org) | Urdu → English | CC BY-SA 3.0 |
| [Urdu Wiktionary](https://ur.wiktionary.org) | Urdu → Urdu | CC BY-SA 3.0 |
| [Wiktionary](https://en.wiktionary.org) | Arabic → English | CC BY-SA 3.0 |
| [Daneshjoo](https://github.com/0xdolan/Daneshjoo) | Persian → English | Repository states MIT |

Pronunciation — IPA with the variety it belongs to, and Urdu Wiktionary's vowelled spelling and
syllable split — comes from the same Wiktionary exports and carries the same licence.

Because four of the five are CC BY-SA 3.0, **the generated `dictionary.db` is CC BY-SA 3.0**.
Attribution is shown in the app beside every definition. See [`../tools/README.md`](../tools/README.md)
to rebuild it.

The Daneshjoo entry is worth a caveat: the repository states MIT, but the underlying lexicon is
a published Iranian dictionary, so that relicensing is worth verifying before relying on it.

## Fonts

| Font | Copyright | Licence | File |
|---|---|---|---|
| Noto Naskh Arabic | The Noto Project Authors | SIL OFL 1.1 | [`OFL-NotoNaskhArabic.txt`](OFL-NotoNaskhArabic.txt) |
| Noto Nastaliq Urdu | The Noto Project Authors | SIL OFL 1.1 | [`OFL-NotoNastaliqUrdu.txt`](OFL-NotoNastaliqUrdu.txt) |
| [Libron](https://github.com/nicoverbruggen/libron) | Nico Verbruggen, after Readerly and Newsreader (Production Type) | SIL OFL 1.1 | [`OFL-Libron.txt`](OFL-Libron.txt) |

## Libraries

Every dependency is Apache-2.0. The full text is in
[`../app/src/main/assets/licenses/apache-2.0.txt`](../app/src/main/assets/licenses/apache-2.0.txt).

| Library | Copyright |
|---|---|
| Jetpack Compose, AndroidX (core, activity, lifecycle, navigation) | The Android Open Source Project |
| Material Components / Material icons | The Android Open Source Project |
| Accompanist (drawablepainter, pulled in by Coil) | The Android Open Source Project |
| Kotlin standard library, kotlinx.serialization, kotlinx.coroutines | JetBrains |
| OkHttp, Okio | Square, Inc. |
| Coil 3 | Coil Contributors |

No Google Play Services, Firebase, analytics or tracking of any kind. The only Android
permission requested is `INTERNET`.

## Icons

The share, lookup and assistant glyphs in `app/src/main/res/drawable/` are drawn from
[Material Symbols](https://fonts.google.com/icons) path data, copied into our own vector files
rather than pulled in as a dependency. Apache 2.0, the same terms as the Material icons listed
under Libraries. The assistant glyph is Material's `auto_awesome`; the sparkle that has come to
mean "ask a model" is used by Gemini and others, but their marks are trademarks and none is used
here.

## Artwork

The launcher icon — an eight-point *shamsa* — is original work for this project, MIT like the
rest of the code. Ganjoor's own app icons are unlicensed and are their mark; they are not used
here.

## Poet portraits

Fetched at runtime from `api.ganjoor.net`. They are Ganjoor's own illustrations of
long-dead poets, displayed as served and never redistributed in the APK.
