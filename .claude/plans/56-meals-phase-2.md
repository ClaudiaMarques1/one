# Plan: #56 - Meals phase 2 (Today timeline view, 1b)

Issue: https://github.com/ClaudiaMarques1/one/issues/56 (idea #12, phase 2 of 4)
Branch: `feature/cmarques/56-meals-phase-2` from `develop` (`1778f98`, after #53 phase 1).
Type: backend + front-facing. Builds on #53 (`.claude/plans/53-meals-phase-1.md`).

## Handover

`OneApp-designs/Meals/design_handoff_meals/README.md` section **1b** (and "Meal slots and targets").

## Decisions

- **Slot times** in `MealSettings.slotTimes` (minutes of day per slot, handover defaults) + a time
  picker per slot in Settings -> Meals. Timeline order = slot time; workout row placed by its time.
- **Workout row** at the logged session's `createdAt` local time; name only (no duration data).
- **Week start**: `MealSettings.weekStart` (MONDAY / SUNDAY), default from the device locale.
- **Week arrows** (< / >) in the 1b header; Week plan / Shopping icons wait for phases 3 / 4.
- **Today view**: `MealSettings.todayView` CHECKLIST / TIMELINE (default CHECKLIST). `MealsScreen`
  switches on it; both views read the same selected date from `MealsViewModel`.
- **Week dots**: new `MealsRepository.observeMealDates(from, to)` - a range query on `date` (single
  field, no composite index), emitting the set of dates holding any meal.
- **Timeline model** kept pure + tested: `buildTimeline(day, slotTimes, workout)` -> ordered rows
  (meal, empty slot, workout) with eaten flags for the rail; `kcalLeft`, `morePlanned`.
- **Timeline rows in one list item:** the LazyColumn's 12dp item spacing broke the rail between rows
  (seen on the emulator), so the timeline renders as one Column item.
- **Summary label:** `kcal left today` on today, `kcal left` on any other day.
- **Water -/+**: one glass per tap, clamped 0..max (the README says 0-10; clamp at
  `MAX_WATER_GLASSES` so a raised target still works).

## Checklist

- [x] `MealSettings`: slotTimes, weekStart, todayView (+ DataStore keys, defaults, tests)
- [x] `MealsRepository.observeMealDates` (+ Firestore, fake), ViewModel `week` state, selectDate, week arrows, water +/-
- [x] Pure timeline builder + labels (`1,440`, `kcal left today`, `1,060 eaten of 2,500 · 900 more planned`, `108 g left`, times)
- [x] 1b screen: fixed header (month, week arrows, week strip), summary card, timeline rail, Eat button, workout row, empty slot, water card
- [x] Settings -> Meals: Today view, week start, slot times
- [x] Dark mode check
- [ ] `/verify`, screenshots to `docs/screenshots/56-meals-phase-2/` + issue, PR `Closes #56`

## Next step

User runs the Manual steps on #56 (Eat / water / Add write real data); then PR `Closes #56`.
