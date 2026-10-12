# Builds

> **On this branch the APKs are not here.** `fdroid` is the lineage F-Droid builds from, and
> its scanner reads the checked-out tree — a pile of APKs sitting in it invites exactly the
> question their policy asks, whether the app is really built from the source beside them. The
> builds live on [GitHub Releases](https://github.com/anas-rashid/ganjoorandroid/releases); the
> checksums below still describe them, and `main` still archives them.

Every release, kept so a version can be reinstalled without rebuilding it. All are signed with
the same release key, so any of them upgrades any other in place without losing bookmarks.

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

## Checksums

```
76121689da069d6ea2e2c6587d4b371ceb51d21bfe069a583d346d3da970337a  ganjoor-0.1.0.apk
43d78b773efca2b1aa2bf790c689b3086389d66c4ea0ffb72c9dba8bddb50770  ganjoor-0.2.0.apk
5163554eb01845c131c31a9da7bdd748170bb9b5f82edaece754b4cf40b02b0c  ganjoor-0.2.1.apk
e473269044710813d2cf4119de663fe180f887239df7bdb1ccee0239ef84fd3e  ganjoor-0.2.2.apk
9c38d5cacf9e809c2189ba581f8aa6eb4072d4fa0316f00d66434461cf4b1459  ganjoor-0.3.0.apk
1c64cefa0fb2b51bf73386460ec1b88bda3e20832fa6926c2f7f1fb863ab9fc7  ganjoor-0.4.0.apk
6c85da41bf1416e34d6cb9c9eda9b4867e01b6793f21e476a6e3ece357615daa  ganjoor-0.4.1.apk
843b01c2c6192e1d0fd667200b06b2a3e59e42e6b388646f6bca41bba8671aa0  ganjoor-0.5.0-foss.apk
3cd9dd814c9b4fc1699e0b44c864a148e9430e13d98b601e65091bc3c927e79d  ganjoor-0.5.0-play.apk
54a77ad94a85672f19f040607466d2ead15560590e5cdcefda512104319e80a4  ganjoor-0.5.1-foss.apk
8617c63bf2e2427d3a29d7f826a7d2603f14695cf13b629946815692826ab6f4  ganjoor-0.5.1-play.apk
```

Installing: `adb install -r releases/ganjoor-<version>.apk`. A phone holding a *debug* build has
to have it uninstalled first — Android will not replace a debug signature with a release one.
