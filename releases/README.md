# Builds

Only the newest release is kept here, and it is the only supported one. Earlier builds have
been withdrawn and receive no updates; the table below records what each of them added.

Every build is signed with the same release key. Up to 0.5.1 they also shared one
applicationId, so any of those upgrades any other in place without losing bookmarks.

0.6.0 renamed the app and changed its applicationId. If you are on an earlier version,
uninstall it and install the current build — Android will not replace it for you, and the old app receives
no further updates. Bookmarks and downloaded poets do not carry across; see "If you have the
old Ganjoor app" in the main README before you remove it.

Each is reproducible: check out the commit and run `./gradlew assembleRelease` (from 0.5.0,
`assembleFossRelease` and `assemblePlayRelease`). F-Droid builds from source and signs with its
own key, so these are for direct installation only.

From 0.5.0 every release comes as two APKs. **foss** has no Google code at all and reaches Google
Translate through your own Cloud key; **play** translates on the phone with Google's ML Kit and
needs Google Play Services. Either upgrades the other.

Beside them is the **play** build as an app bundle (`.aab`), the file Google Play Console takes.
A phone cannot install it; it is kept here so the bundle on Play can be checked against its
checksum.

| Version | Code | Commit | What it added |
| --- | --- | --- | --- |
| 0.1.0 | 1 | `9445f44` | The base app: browsing, bookmarks, offline, themes, the bundled dictionary with pronunciation, and Ganjoor's chapter order. |
| 0.2.0 | 2 | `d92445f` | Share a poem, couplet or selection; look a word up from any app's selection menu; the optional AI assistant. |
| 0.2.1 | 3 | `ec05989` | Assistant answers in the page rather than over it, a visible chevron for each couplet's actions, and answers remembered so scrolling doesn't re-ask. |
| 0.2.2 | 4 | `d7568d1` | Credits Material Symbols for the glyphs, and states the terms around the optional assistant. |
| 0.3.0 | 5 | `5d12a1c` | Pin poets to the home screen: three views (pinned, Ganjoor's order, alphabetical), as cards or a list, with a download control on each row. |
| 0.4.0 | 6 | `270c28c` | Columns on tablets and unfolded foldables; the dictionary and reading settings beside the poem rather than over it; couplets in cards, two hemistichs to a line where there is room; skeletons while pages load; a seek bar on recitations. |
| 0.4.1 | 7 | `3899694` | The font weight control works on every device: the app was asking the bundled faces for an italic axis they do not have, and a font may refuse the whole request rather than the part it cannot honour. |
| 0.5.0 | 8 | `v0.5.0` | Ganjoor's AI descriptions translated in the page for Urdu and English readers, by Google Translate or your AI assistant, always labelled; presets for hosted AI APIs; two builds, foss and play; icons for a couplet's actions, and the bookmark glyph everywhere. |
| 0.5.1 | 9 | `v0.5.1` | The privacy policy reads inside the app. Built so F-Droid can compile the identical APK and publish it under this project's own signature: AGP no longer writes the building checkout's git revision into the APK. |
| 0.6.0 | 10 | `v0.6.0` | Renamed to Khazeena. Ganjoor is ganjoor.net's name and this is an independent, unofficial reader for it; the app now has its own name in each script, and its own applicationId. Installs alongside the old app rather than replacing it. |
| 0.6.1 | 11 | `v0.6.1` | Downloads continue in the background with a progress notification and a per-poet queue; partial downloads are counted and retried; a failed download no longer crashes the app; poets the CDN stalled on load from GitHub instead; a recitation lights the couplet being read; the language switch in settings applies at once. |

## Checksums

```
e1b88ed5b4ec7cc94de4d46e04d5612b51843a15beae909d97ec3db8693a7920  khazeena-0.6.1-foss.apk
f84349b9f6fa6f54b0ecf6faaf03d7b4a5bb92c918ab9000377c909f74d0dbdc  khazeena-0.6.1-play.apk
dff354e929363fc6b7846462b8c4498b6d4b0b4f9d428bf4b8e5a21149ccc1a0  khazeena-0.6.1-play.aab
```

Installing: `adb install -r releases/khazeena-<version>-foss.apk`. A phone holding a *debug* build has
to have it uninstalled first — Android will not replace a debug signature with a release one.
