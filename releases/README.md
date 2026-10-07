# Builds

Every release, kept so a version can be reinstalled without rebuilding it. All are signed with
the same release key, so any of them upgrades any other in place without losing bookmarks.

Each is reproducible: check out the commit and run `./gradlew assembleRelease`. F-Droid builds
from source and signs with its own key, so these are for direct installation only.

| Version | Code | Commit | What it added |
| --- | --- | --- | --- |
| 0.1.0 | 1 | `9445f44` | The base app: browsing, bookmarks, offline, themes, the bundled dictionary with pronunciation, and Ganjoor's chapter order. |
| 0.2.0 | 2 | `d92445f` | Share a poem, couplet or selection; look a word up from any app's selection menu; the optional AI assistant. |
| 0.2.1 | 3 | `ec05989` | Assistant answers in the page rather than over it, a visible chevron for each couplet's actions, and answers remembered so scrolling doesn't re-ask. |
| 0.2.2 | 4 | `d7568d1` | Credits Material Symbols for the glyphs, and states the terms around the optional assistant. |
| 0.3.0 | 5 | `5d12a1c` | Pin poets to the home screen: three views (pinned, Ganjoor's order, alphabetical), as cards or a list, with a download control on each row. |
| 0.4.0 | 6 | `270c28c` | Columns on tablets and unfolded foldables; the dictionary and reading settings beside the poem rather than over it; couplets in cards, two hemistichs to a line where there is room; skeletons while pages load; a seek bar on recitations. |

## Checksums

```
76121689da069d6ea2e2c6587d4b371ceb51d21bfe069a583d346d3da970337a  ganjoor-0.1.0.apk
43d78b773efca2b1aa2bf790c689b3086389d66c4ea0ffb72c9dba8bddb50770  ganjoor-0.2.0.apk
5163554eb01845c131c31a9da7bdd748170bb9b5f82edaece754b4cf40b02b0c  ganjoor-0.2.1.apk
e473269044710813d2cf4119de663fe180f887239df7bdb1ccee0239ef84fd3e  ganjoor-0.2.2.apk
9c38d5cacf9e809c2189ba581f8aa6eb4072d4fa0316f00d66434461cf4b1459  ganjoor-0.3.0.apk
1c64cefa0fb2b51bf73386460ec1b88bda3e20832fa6926c2f7f1fb863ab9fc7  ganjoor-0.4.0.apk
```

Installing: `adb install -r releases/ganjoor-<version>.apk`. A phone holding a *debug* build has
to have it uninstalled first — Android will not replace a debug signature with a release one.
