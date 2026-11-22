# Plan: #62 - Settings split into sections (Appearance, Workout, Meals)

Issue: https://github.com/ClaudiaMarques1/one/issues/62 (requested 2026-11-22 during Meals phase 4)
Branch: `feature/cmarques/62-settings-sections` from `develop` (`1b62362`, after #60).
Type: front-facing (Settings screens, navigation). No storage change.

## Decisions

- **Top level** (`SettingsScreen`): one card per section - icon, title, summary of current values,
  chevron. Summaries are pure + tested (`settingsSummaries`).
- **Sections**: `SettingsSection { APPEARANCE, WORKOUT, MEALS }` with route key, title, icon.
  `SettingsSectionScreen(section)` renders the existing controls: `ThemeCard`, the vibrate row,
  `MealSettingsSection` - moved, not changed. Meals keeps `imePadding()` (keyboard fix from #53).
- **Navigation**: `Destination.SettingsSection` route `settings/{section}`; back pops to the list.
  Both screens get their own `SettingsViewModel` via `settingsViewModel()` (same DataStore), as today.
- Not in a design handoff (Home 1d only covered the single screen) - same neutral card / row style.

## Checklist

- [x] `SettingsSection` + summaries (+ tests)
- [x] Settings list screen + section screen, controls moved
- [x] Route `settings/{section}` in `Destination` / `OneNavHost`
- [ ] Dark check (Manual 6); `/verify`, screenshots to `docs/screenshots/62-settings-sections/` + issue, PR `Closes #62`

## Next step

User runs the 6 Manual steps on #62; then PR `Closes #62`.
