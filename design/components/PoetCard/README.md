# PoetCard

One poet in the home grid: a round portrait over their name, in a filled card.

- **Consumer provides:** the poet (name, portrait URL), whether they are pinned, and click / long-click handlers.
- Filled Card: `surface-container-highest`, `radius-md`, `space-12` padding, `space-8` gap. The grid is adaptive with columns at least `poet-grid-min` (132px), `space-12` gutters.
- The portrait is an `avatar-card` (84px) disc on `secondary-container`; the poet's initial (`headline-medium`, `on-secondary-container`) sits *underneath* the photo so a missing portrait is never a hole.
- Name in `title-medium`, centred, up to two lines.
- Long-press pins the poet; a pinned poet shows the 16px pin (`icon-pin`, `primary`) before the name. The "Pinned" sort puts them first.
