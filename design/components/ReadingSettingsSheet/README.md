# ReadingSettingsSheet

The Material bottom sheet behind the gear icon: theme, OLED, font, weight, text size with a live preview, language, offline mode and summaries.

- **Consumer provides:** nothing; it reads and writes the app's settings.
- `surface-container-low` with `radius-xl` top corners; `space-20` side padding, `space-8` between rows, `space-32` at the bottom. It opens half-height and scrolls, inset above the navigation bar.
- Order: Theme chips → OLED toggle → Font chips → Weight chips → Text size slider (14–40, steps of 2) → **live preview** of a hemistich in the chosen reading style → Language chips → Offline mode → Show summaries → AI assistant, About & licences as TextButtons.
- Every group has a **SectionLabel**. Changes apply immediately; there is no Save.
- Changing language recreates the activity.
- On tablets and unfolded foldables the same settings open as a panel on the left instead (`ReadingSettingsPanel`, see ColumnBrowser), so the poem stays in view while they change.
