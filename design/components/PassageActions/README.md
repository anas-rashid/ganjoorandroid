# PassageActions

The row of TextButtons under an opened couplet: save the passage, copy it, share it.

- **Consumer provides:** the passage (excerpt plus the poem's URL and title).
- TextButtons in `label-large`, `space-8` apart. **Save** reads in the content colour until saved, then turns `primary` and says **Saved** — the state is in the word, not only the colour.
- **Share** carries the standard Android share glyph at `icon-inline` (18px): people recognise the shape before the word.
- The same row appears in the word sheet, so a passage can be saved from wherever the reader is.
