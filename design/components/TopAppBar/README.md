# TopAppBar

The Material 3 small top app bar every screen uses: back arrow, a one-line title, then a row of icon actions.

- **Consumer provides:** the title (a poem title, a category, or the app name گنجور), an optional up action, and the actions for that screen.
- Title in `title-large`, ellipsized to one line. The navigation icon is `on-surface`; action icons are `on-surface-variant`, 24px in a 48px `touch` target.
- A toggled action (a saved bookmark heart) turns `primary`, so the state is visible without reading the label.
- Action order on the poem screen: Home, Share, Bookmark, Reading settings. On the poets screen: view toggle, language, bookmarks, downloads, reading settings. Reading settings is always last, so it is always in the same place.
- The arrow is auto-mirrored: in the RTL layout it points right.
- Don't add a menu overflow; every action has its own icon and content description.
