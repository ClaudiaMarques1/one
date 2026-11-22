# Plan: #58 - Meals phase 3 (Week plan, 1d)

Issue: https://github.com/ClaudiaMarques1/one/issues/58 (idea #12, phase 3 of 4)
Branch: `feature/cmarques/58-meals-phase-3` from `develop` (`2909e71`, after #56 phase 2).
Type: backend + data + front-facing. Builds on #53 / #56 (`.claude/plans/53-…`, `56-…`).

## Handover

`OneApp-designs/Meals/design_handoff_meals/README.md` section **1d** (Week).

## Decisions

- **Entry / navigation**: `calendar_view_week` icon in the weekly view header -> `Destination.MealsWeek`
  (route `meals/week`, outside the bottom nav), wrapped in `SectionTheme(SectionAccent.Meals)`.
- **Own ViewModel** `MealsWeekViewModel` (default-arg repositories + the settings repo via a factory,
  like `MealsViewModel`), with its own selected week. Shares the pure helpers in `meals/backend`.
- **Week data**: `MealsRepository.observeMealsBetween(from, to)` (single-field range on `date`) and
  `observeRestDayChoices(from, to)` (range on the doc id = date). Training per day uses the same rule
  as `buildMealsDay` (logged workout, or rest day switch off).
- **Pure `buildWeekPlan`**: per day -> PAST / TODAY / FUTURE, eaten / planned kcal, target, onTarget
  (|eaten - target| <= 5% of target), B L D S chip states, workout label. Tested.
- **Copy** (tomorrow, to date, week) = `addMeals` with copies (new ids, `eaten = false`, new date);
  added to what's there. **Clear day** = new `deleteMeals(ids)` in one `WriteBatch`.
  Copy week = one `addMeals` batch for all 7 days (+7 days each).
- **Range label** uses a plain hyphen (`16 - 22 Nov`), repo style, where the mock has an en dash.
- **Chip letters** use a trimmed, centred line height so B L D S sit in the middle of the 18dp chips.
- Week plan / Shopping icons: only Week plan now; Shopping list arrives in phase 4.

## Checklist

- [x] Repository: `observeMealsBetween`, `observeRestDayChoices`, `deleteMeals` (+ Firestore, fake)
- [x] `buildWeekPlan` + copy helpers + labels (`2,280 of 2,300 kcal`, `1,060 eaten · 1,960 planned`, `15 – 21 Nov`), tests
- [x] `MealsWeekViewModel` (week, arrows, copy to tomorrow / date, clear day, copy week), tests
- [x] Week screen: top bar, day card rows (date block, summary, chips, training, menu), legend, footer, dialogs
- [x] Navigation: `Destination.MealsWeek`, `OneNavHost`, header icon in the weekly view
- [ ] Dark check (Manual step 10); `/verify`, screenshots to `docs/screenshots/58-meals-phase-3/` + issue, PR `Closes #58`

## Next step

User runs the 10 Manual steps on #58; then PR `Closes #58`.
