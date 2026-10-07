# Couplet

One line of poetry: two hemistichs stacked on a phone, the first pushed to the start of the line and the second to the end — the way Ganjoor itself reads.

- **Consumer provides:** the couplet's verses (each with its position), the reading style from settings (`poem-naskh` or `poem-nastaliq` at the reader's size and weight), and handlers for a tapped word and for the actions.
- Alignment by verse position: first hemistich `start`, second `end`, centred verses `center`, prose (single/paragraph/comment) `justify` so Golestan and Nowruznameh fill the column.
- `space-6` above and below each couplet. Text is `on-surface`.
- Tapping a word opens the dictionary sheet; tapping between words, or the 32px chevron (`touch-compact`, 20px icon, `on-surface-variant`), opens the couplet's actions: **Save this passage**, **Copy**, **Share** as TextButtons. Save keeps the link back to the poem; Copy doesn't.
- Long-press belongs to text selection — never bind actions to it here.
- With summaries on, Ganjoor's couplet summary sits underneath in `body-small` / `on-surface-variant`.
- Nastaliq needs `2.4` leading or its swashes clip the line above; naskh uses `1.8`.
