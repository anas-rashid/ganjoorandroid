# ColumnBrowser

The layout for tablets and unfolded foldables. A narrow column of poets sits on the right. Beside it is a column for each level of the open book (books › chapters › poems), and the open page fills the rest of the screen on the left. Phones keep today's navigation. Built in `app/src/main/java/com/ganjoor/android/ui/ColumnBrowser.kt`; wireframes are in `design/wireframes/` (1–8).

## Window sizes

| Width | Layout |
|---|---|
| < 600dp (phones, a folded foldable) | Unchanged: one screen at a time. |
| 600–839dp (small tablets, a book-style foldable open in portrait) | Poets column, the **newest** list column only (its header has a back arrow to the level above), and the page. |
| ≥ 840dp (tablets, an open foldable in landscape) | Poets column, up to three list columns, and the page. |

Both layouts use the same navigation routes, so folding or unfolding keeps your place.

## Columns, right to left

1. **Poets** (`surface-container`, 96dp; 80dp once two or more lists are open or a poem is being read): a portrait disc (56dp, or 44dp when narrow) with the name underneath in `label-medium`. The selected poet gets a `secondary-container` tile and a 2dp `primary` ring around the portrait. The poets appear in the same order as on the home screen, pinned poets first.
2. **List columns** (`surface-container-low`): a poet's books, a book's chapters, a chapter's poems. The header shows the title of what the column lists. Selecting a row replaces every column to its left. Each column keeps its own scroll position.
3. **The page** (`surface`): the usual TopAppBar, then the breadcrumbs, then the content at a centred measure (680dp for a poem, 760dp for a book's cards).

## Columns give their room to the page

| Column | Browsing | Reading a poem |
|---|---|---|
| Newest | 224dp: `body-large` titles, poems show their first line, and a chevron where a row opens another column | 168dp, terse |
| One back | 168dp, terse | 132dp, terse |
| Two back | 132dp, terse | 132dp, terse |

**Terse** means `body-medium` titles wrapped to two lines and cut with an ellipsis, with no first lines and no chevrons. Width changes animate over 300ms. A new column opens out from zero width.

## Reader view

- The **hide** button (`menu_open`) is the first action in the page's top bar. It slides the columns away to the right, and the page takes the whole width, still centred.
- While the columns are hidden, a **floating button** (`menu`) at the bottom-right corner brings them back exactly as they were.
- The choice is saved (`columnsHidden` in Settings).

## Couplets

When the page is at least 640dp wide, a Right+Left couplet sits on one line: the first half at the start, the second at the end, 32dp apart. When it's narrower, couplets stack as on the phone. Centred verses and prose always keep their own lines.

## Dictionary

On large screens, tapping a word opens **WordPanel** on the left instead of the bottom sheet:
- `surface-container-low`, 360dp wide, with a "Dictionary" header, a ✕ button, and the same lookup and couplet actions as the sheet.
- The poem moves over to make room, so nothing being read is covered.
- The tapped word stays highlighted in the verse (`secondary-container` / `on-secondary-container`), on phones too.
- ✕ or Back closes the panel.

## Reading settings

On large screens the gear opens **ReadingSettingsPanel** on the left, where the dictionary opens, instead of the bottom sheet:
- It has the same width and surface as the dictionary panel, with a "Reading settings" header and a ✕ button. The settings and their order are the same as in the sheet.
- The page stays in view beside the panel, so a change of theme, font, weight or size shows on the poem as it's made.
- While the panel is open, the poets and list columns fold away so the page keeps its room. When it closes they come back, but only if they were showing before: this never changes the saved reader-view choice, and the floating "show the list" button stays hidden while the panel is open.
- ✕ or Back closes it. Phones keep the bottom sheet.

## Loading

Nothing covers the whole screen while it loads, on phones as well:
- Only the part that is waiting shows a skeleton shaped like its content (`Skeleton.kt`): list rows in a column, portrait discs in the poets column, cards on a book's page, couplets on a poem.
- The real top bar stays visible above the skeleton, so Back already works.
- The skeletons pulse softly, and the content fades in over them when it arrives.

## Not yet done

- Lining the page edge up with a foldable's hinge (`FoldingFeature`, Jetpack WindowManager).
