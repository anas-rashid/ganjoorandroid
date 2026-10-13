# Builds

Only the newest release is kept here, and it is the only supported one. Earlier builds have
been withdrawn and receive no updates; the table below records what each of them added.

Every build is signed with the same release key. Up to 0.5.1 they also shared one
applicationId, so any of those upgrades any other in place without losing bookmarks.

0.6.0 renamed the app and changed its applicationId. If you are on an earlier version,
uninstall it and install 0.6.0 — Android will not replace it for you, and the old app receives
no further updates. Bookmarks and downloaded poets do not carry across; see "If you have the
old Ganjoor app" in the main README before you remove it.

Each is reproducible: check out the commit and run `./gradlew assembleRelease` (from 0.5.0,
`assembleFossRelease` and `assemblePlayRelease`). F-Droid builds from source and signs with its
own key, so these are for direct installation only.

From 0.5.0 every release comes as two APKs. **foss** has no Google code at all and reaches Google
Translate through your own Cloud key; **play** translates on the phone with Google's ML Kit and
needs Google Play Services. Either upgrades the other.

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

## Checksums

```
b42e0c5b159396f85ce78e83dac0f5edff030e5bb834ee9b4fa8ec9e9510208c  khazeena-0.6.0-foss.apk
6cf2e73662e8ff2d458c5033497f525a867d28ba11cf3609f5ac1a3f131e6f12  khazeena-0.6.0-play.apk
```

Installing: `adb install -r releases/khazeena-<version>-foss.apk`. A phone holding a *debug* build has
to have it uninstalled first — Android will not replace a debug signature with a release one.
