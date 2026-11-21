# Plan: #46 - Home redesign, profile drawer, Account details, Settings

Branch: `feature/cmarques/46-profile-drawer` (from `develop` at `4beb5f1`, after PR #42 merged).
Issue #46 (idea #11), card In Progress on board "one".
Handover: `C:\Users\claud\Downloads\OneApp-designs\Home\design_handoff_home\` (`README.md` spec,
`Home Redesign.dc.html` reference). Build **1a-1d light** only.

## Decisions (user, 2026-11-21)

- **Settings are local on the device** (no internet dependency): `androidx.datastore:datastore-preferences`
  via the version catalog. Not Firestore - the #11 card said Firestore before the handover.
- **Light only in #46.** The Theme row (Light / Dark / System) and every dark variant, including Home's
  2a-2d, move to the dark-mode card (idea #3, "Settings: dark / light mode toggle (+ dark mode for every
  section)"), done straight after. Its card body lists both handovers and the code changes.

## Design reading (from the HTML, not only the README)

- The drawer overlays the **whole screen, bottom nav included** - so `ModalNavigationDrawer` wraps the
  Scaffold in `OneNavHost`, not just Home. Gestures only while open (swipe-to-close), opened from the avatar.
- Nav icons: outlined when unselected, filled when selected (Material Symbols FILL 0/1); unselected ink
  `#49454F` weight 500, selected weight 600. Nav bar `#F2F0ED`, 80 tall. Home's indicator `#E2DFDB` and
  ink `#1D1B20`; accent tabs keep `accent.copy(alpha = 0.18f)`.
- The mock's Workout card says "3 workouts this week", but the README says keep today's summaries -
  following the README (Workout stays "Coming soon"); possible follow-up.

## Layout

- `auth/backend/UserProfile.kt` - plain profile model + `SignInMethod` from provider ids, so no screen
  imports a Firebase class; `AuthViewModel.profile: StateFlow<UserProfile?>`.
- `auth/frontend/AccountDetailsScreen.kt` (+ labels) - Sign out moves here.
- `settings/backend/` - `SettingsRepository` interface, `DataStoreSettingsRepository`, `SettingsViewModel`
  (repository injected through a `viewModelFactory`, as `StrengthViewModel` is); `settings/frontend/SettingsScreen.kt`.
  The `preferencesDataStore` delegate is a process-wide singleton by necessity (two DataStores on one
  file throw) - note it next to FirebaseModule in architecture.md.
- `dashboard/frontend/` - restyled `DashboardScreen` + `ProfileDrawerContent`.
- `ui/ProfileAvatar.kt` + `ui/ProfileLabels.kt` (initials) - shared by dashboard and auth.
- `ui/theme/` - neutral tokens + `NeutralTheme` (light scheme, `primary = #1D1B20`) for Home and its
  sub-screens; SignIn keeps `MonochromeTheme`.
- `navigation/` - `Destination.Account` / `Destination.Settings` (not in the bottom nav; bar hidden on
  them), `selectedIcon` per destination.
- Workout: `WorkoutScreen` reads the setting through `SettingsViewModel` (a ViewModel, not another
  feature's repository) and passes `vibrateOnRestEnd` to the rest bar.

## Checklist

- [x] Data: DataStore dependency, settings repository + ViewModel + fake + tests; `UserProfile` + tests
- [x] 1a Home + nav bar restyle
- [x] 1b Profile drawer
- [x] 1c Account details (Sign out moved)
- [x] 1d Settings (vibrate) + rest bar respects it
- [x] Docs: architecture.md (DataStore singleton, settings/ package), CLAUDE.md tree, README
- [ ] `/verify`, Manual steps, PR to `develop` (`Closes #46`)

## Status at 2026-11-21 (resume here)

All of 1a-1d built and committed; `:app:compileDebugKotlin --rerun test lint assembleDebug` pass; installed
on the emulator (Account details checked by screenshot: matches 1c, Google photo shows). Draft PR open.
One commit covers 1a-1d because the drawer lives in OneNavHost and links to both new routes.
Shared bits went to `ui/`: `ProfileAvatar`, `profileInitials`, `BackTopBar`, `theme/NeutralTheme`.
The always-loaded docs stay at 219 lines (CLAUDE.md intro reflowed to pay for the `settings/` line).

## Next step

1. User runs the 8 Manual steps on #46 (emulator has the build).
2. `/verify`, then PR ready + card In Review.
3. After merge: dark mode (idea #3 card - already lists both handovers and the code changes).
