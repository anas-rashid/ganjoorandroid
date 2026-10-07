# SettingToggle

A setting with a one-line explanation and a Material Switch.

- **Consumer provides:** a title, a note and the checked state.
- Title in `body-large` / `on-surface`; the note under it in `body-small` / `on-surface-variant`; the Switch at the end. `space-12` above each toggle.
- The note says what the setting does *and* when it applies ("Applies to the dark themes; saves power on OLED screens"). Put a toggle beside the choice it modifies — OLED sits right under the theme chips.
- Switch on: `primary` track, `on-primary` thumb. Off: `surface-container-highest` track with an `outline` border and thumb.
