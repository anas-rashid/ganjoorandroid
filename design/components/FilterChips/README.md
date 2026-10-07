# FilterChips

A wrapping row of single-choice Material FilterChips — poet sort order on the home screen, and theme, font, weight and language in the reading settings sheet.

- **Consumer provides:** the options, the selected one, and a label for each.
- 32px tall, `radius-sm`, `label-large`, `space-8` apart, wrapping (FlowRow).
- Selected: `secondary-container` fill, `on-secondary-container` label and a check mark. Unselected: transparent with a 1px border.
- Exactly one is always selected; these are a choice, not filters that can all be off.
- Language chips label each language in its own script (فارسی · اردو · English), whatever the UI language.
