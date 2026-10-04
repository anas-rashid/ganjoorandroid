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

## Checksums

```
76121689da069d6ea2e2c6587d4b371ceb51d21bfe069a583d346d3da970337a  ganjoor-0.1.0.apk
43d78b773efca2b1aa2bf790c689b3086389d66c4ea0ffb72c9dba8bddb50770  ganjoor-0.2.0.apk
5163554eb01845c131c31a9da7bdd748170bb9b5f82edaece754b4cf40b02b0c  ganjoor-0.2.1.apk
```

Installing: `adb install -r releases/ganjoor-<version>.apk`. A phone holding a *debug* build has
to have it uninstalled first — Android will not replace a debug signature with a release one.
