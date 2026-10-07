# Breadcrumbs

The poem's path at the top of the reader — poet » book » section » this poem — with every ancestor tappable.

- **Consumer provides:** the poem's full title and URL; crumbs are split from them.
- `title-medium`. Ancestors are `primary` and open that category; the current poem is `on-surface` and not a link; the ` » ` separators are `on-surface-variant`.
- Wraps onto further lines (FlowRow) instead of truncating — a long Golestan path stays readable.
- If no crumbs can be derived, show the full title as plain `title-medium` text.
- Below it: the recitation player, then the metre line in `body-small` / `on-surface-variant`.
