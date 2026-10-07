# DownloadStatus

Fetch a poet's poems, or say they are already here — three states, no menu.

- **Consumer provides:** the poet's slug; state comes from the download queue and the offline store.
- **Idle:** the download arrow in `on-surface-variant`, tap to start.
- **Downloading:** an 18px (`icon-inline`) progress ring, `space-2` stroke, in `primary`; tap cancels.
- **Saved:** the CheckCircle in `downloaded` green — a tick shape as well as a colour. It sits in a 48px (`touch`) box even though it is not tappable, so ticks and arrows line up down the column.
- Deleting is never offered here; it lives on the Downloads screen next to the sizes. A tap beside a poet's name should never throw their poems away.
