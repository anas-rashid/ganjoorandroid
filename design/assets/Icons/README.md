# Icons

The app uses **Material Icons, filled style**: `Icons.Default.*` from Compose for back (auto-mirrored), home, search, settings, share, favourite or favourite-border, check-circle and the keyboard arrows. It also keeps the vector drawables below, which are copies of Material symbols for glyphs Compose's core set lacks.

All of them are single-ink SVGs drawn in ink `#191c1c` (light `on-surface`). In the app they are tinted at runtime: actions in `on-surface-variant`, the toggled state in `primary`, and the download tick in `downloaded`. When you use these files in `<img>`, swap the fill for the colour you need.

| File | Material name | Where |
|---|---|---|
| ic-download.svg | download | Downloads in the top bar; download a poet |
| ic-pin.svg | push_pin | Pin mark on a pinned poet, 16px |
| ic-view-grid.svg / ic-view-list.svg | grid_view / view_list | Poets view toggle |
| ic-language.svg | language | UI language switcher |
| ic-share.svg | share | Share a poem or passage |
| ic-lookup.svg | search | Dictionary entry in the selection menu |
| ic-ask.svg | auto_awesome | Ask an assistant |
| ic-pause.svg | pause | Recitation player |
