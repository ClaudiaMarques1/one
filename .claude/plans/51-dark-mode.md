# Plan: #51 - dark / light mode toggle (+ dark mode for every section)

Issue: https://github.com/ClaudiaMarques1/one/issues/51 (idea #3, follows #46 profile drawer + Settings)
Branch: `feature/cmarques/51-dark-mode` from `develop` (`8eabe71`).
Type: backend + front-facing (see `rules/test-execution.md`).

## Handover

- `OneApp-designs/Settings/design_handoff_dark_mode/README.md` - conversion rule + token tables for
  every section (+ `.dc.html` per section).
- `OneApp-designs/Home/design_handoff_home/README.md` - Home, drawer, Account details, Settings dark
  (2a-2d, hand-tuned tokens) and the Theme setting wiring.
- Both live under `Downloads/OneApp-designs/`; the session needs `/add-dir` on that folder to read them.

## Decisions

- Setting: `ThemeMode { SYSTEM, LIGHT, DARK }`, default `SYSTEM`, stored in the existing
  `settings/backend/DataStoreSettingsRepository.kt` (string key), exposed by `SettingsViewModel`.
- `OneTheme(darkTheme = ...)` resolved in `MainActivity` from the setting; `dynamicColor = false`
  so section accents aren't replaced by the wallpaper palette.
- Section tokens in `ui/theme/Color.kt` move into a `OneColors` data class (Light / Dark) provided
  through a CompositionLocal (`OneTheme.colors.workoutInk`), since top-level vals can't switch.
- `SectionTheme.withAccent` gets a dark branch (dark accent primary, `#1E1E1E` on-primary, darker
  containers). `NeutralTheme` gets a dark scheme (2a-2d tokens).
- `MonochromeTheme`: check usages (SignIn?) and decide whether it follows dark too - TBD from handover.

- **Changed from the issue text**: tokens are NOT read as `OneTheme.colors.workoutInk`. `Color.kt`
  keeps the existing names (`WarmSurface`, `WorkoutInk` ...) as `@Composable @ReadOnlyComposable`
  getters over `LocalOneColors` - ~270 call sites unchanged; the compiler flags non-composable uses
  (4 fixed: `habitCard()`, `workoutCard()`, `labelColors()`, `ColumnHeaderStyle`).
- `OneColors` instances: `LightOneColors`, `DarkOneColors` (dark-mode handoff tables, WarmHint =
  #8B8783 per its contrast note), `HomeLightOneColors` / `HomeDarkOneColors` (home handoff 2a-2d,
  hand-tuned). `NeutralTheme` swaps in the Home set; `OneTheme` / `NeutralTheme` share `OneColorsTheme`.
- Accents: `enum SectionAccent(light, dark)`; `Destination.accent: SectionAccent?`,
  `SectionTheme(accent: SectionAccent)`; `CalendarAccent` etc. are getters resolving light/dark.
- New tokens: `CardSurface` (white / #1E1E1E / Home #1F1D1B), `DrawerSurface`, `NeutralAvatarInitials`,
  `NeutralSwitchOnThumb`, `WorkoutSelectedRow`, `ScrimColor`.
- `dynamicColor` parameter removed from `OneTheme` entirely (never wanted).
- `MainActivity` re-applies `enableEdgeToEdge(SystemBarStyle...)` from the resolved theme, else
  Light-on-a-dark-phone leaves white status-bar icons.
- `MonochromeTheme` (SignInScreen) stays light - not in either handover. Ask the user.

## Checklist

- [x] Read both handovers; token mapping is in `ui/theme/Color.kt`
- [x] `ThemeMode` in `SettingsRepository` + DataStore impl + `SettingsViewModel` (+ fake, tests)
- [x] Settings -> Appearance -> Theme segmented control (Light / Dark / System)
- [x] `MainActivity` / `OneTheme` wiring, no dynamic colour, system-bar icons follow the app
- [x] `OneColors` Light/Dark + CompositionLocal (named tokens are composable getters)
- [x] `SectionTheme` dark branch; `NeutralTheme` dark; nav bar selected = dark accent; drawer scrim 60%
- [x] Home + drawer + Account + Settings (2a-2d) - checked on the emulator in dark, light, Light-override
- [x] Calendar (2a): `CalendarColors` moved to `ui/theme/CalendarColors.kt` as Light/Dark palettes
- [x] Diary journal (1a, 2a): private literals -> getters over tokens + `ui/theme/DiaryColors.kt`
      (soreness stays "Diary purple", the handoff default; the dark README reds are the Warm red option)
- [x] Goals time picker (1a): white fills -> `CardSurface`
- [x] Workout (1a-1d, 2b, 3a) + exercise picker: `onPrimary`, `CardSurface`, new `OnWorkoutDeep` (rest bar)
- [x] Meals placeholder: checked in dark
- [x] Calendar legend: "Both" removed (Meals joins the grid); Goals list padded 16/16/24
- [x] `/verify` (all Checks ticked, Manual by the user); screenshots in `docs/screenshots/51-dark-mode/` + on the issue
- [ ] PR to `develop` with `Closes #51` - user review

## Next step

PR open for review; merge only on the user's explicit go-ahead. Open question for later: give the
sign-in screen (MonochromeTheme) a dark version.
