# BottomSearchBar

The poets search, docked at the *bottom* of the home screen where a one-handed thumb reaches.

- **Consumer provides:** the query and handlers for change and for "search poems".
- A Surface at tonal elevation 3 (`surface-tonal-3`), `space-12` / `space-8` padding, holding an OutlinedTextField (`radius-xs`, `outline` border, leading search icon) labelled "Search poets".
- Typing filters the poets in place. Once there is a query, a row appears above the field — "Search poems for “…”" with a `primary` search icon — that carries the same words into full-text poem search.
- Rides above the keyboard (ime padding) and the navigation bar.
