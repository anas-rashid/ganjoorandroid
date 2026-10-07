# Couplet

One line of poetry: two hemistichs stacked on a phone, the first pushed to the start of the line and the second to the end — the way Ganjoor itself reads.

- **Consumer provides:** the couplet's verses (each with its position), the reading style from settings (`poem-naskh` or `poem-nastaliq` at the reader's size and weight), and handlers for a tapped word and for the actions.
- Alignment by verse position: first hemistich `start`, second `end`, centred verses `center`, prose (single/paragraph/comment) `justify` so Golestan and Nowruznameh fill the column.
- Every line of verse sits in its own **card**: `surface-container-high` (`surface-container-highest` on OLED black, where the usual step is all but black), `radius-md` (12dp) corners, 12dp/8dp inner padding and 4dp above and below. The card holds the couplet's options chevron, its actions and its summary, so they visibly belong to it. Text is `on-surface`, at least 6:1 on the card in every theme.
- Prose (Single, Paragraph or Comment positions: Golestan, Nowruznameh) has no card; it keeps `space-6` above and below. A paragraph in a box would read as a quotation.
- The cards are the same on phones, tablets and foldables (wireframes 4–9).
- Tapping a word opens the dictionary sheet; tapping between words, or the 32px chevron (`touch-compact`, 20px icon, `on-surface-variant`), opens the couplet's actions: **Save this passage**, **Copy**, **Share** as TextButtons. Save keeps the link back to the poem; Copy doesn't.
- Long-press belongs to text selection — never bind actions to it here.
- With summaries on, Ganjoor's couplet summary sits underneath in `body-small` / `on-surface-variant`.
- Nastaliq needs `2.4` leading or its swashes clip the line above; naskh uses `1.8`.
