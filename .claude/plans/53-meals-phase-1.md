# Plan: #53 - Meals phase 1 (Today checklist, Add meal, water, targets)

Issue: https://github.com/ClaudiaMarques1/one/issues/53 (idea #12, phase 1 of 4)
Branch: `feature/cmarques/53-meals-phase-1` from `develop` (`1462faf`, after #51 dark mode).
Type: backend + data + front-facing.

## Handover

`OneApp-designs/Meals/design_handoff_meals/README.md` (re-exported 2026-11-22, full token tables, mock
data, state model) + `Meals Redesign.dc.html`. Phase 1 builds **1a** and **1c** (Favourites + New tabs).
Meal settings: the README points to `Home Redesign.dc.html` 1d, which was never exported -> user chose
**plain rows in the existing Settings screen** (same card style as Theme / Vibrate).

## Decisions

- **Data (Firestore, `users/{uid}/...`, covered by the owner-only rule):**
  - `meals`: `PlannedMeal(id, date "yyyy-MM-dd", slot (MealSlot name), name, kcal, proteinG, carbsG,
    fatG, eaten, createdAt)`. Meal values are copied in (not a `mealId` reference as in the README's
    sketch), so editing/deleting a favourite never rewrites past days. Query `whereEqualTo("date")`.
  - `favourites`: `FavouriteMeal(id, name, kcal, proteinG, carbsG, fatG, createdAt)`.
  - `water`: doc id = date key, `WaterLog(glasses)`.
  - Adding from the sheet is one `WriteBatch` (several planned meals, plus the favourite when "Save
    to favourites" is on) - one user action, one atomic write.
- **Settings (local DataStore, `SettingsRepository`):** `MealSettings(enabledSlots, trainingTargets,
  restTargets, waterGlasses)`. Slot times and Today view wait for phase 2 (timeline) - they only
  matter there. Models live in `meals/backend`; `settings/backend` stores them (same cross-feature
  shape as Workout reading the vibrate setting).
- **Training day:** a strength session logged for that date (`StrengthRepository`, as Calendar does)
  until workout scheduling exists. Banner copy without a time: `Leg day · targets raised for training`.
- **Day navigation (requested in testing):** < / > arrows on the header move `MealsViewModel`'s selected
  date; title reads Today / Tomorrow / Yesterday / weekday and tapping it returns to today.
- **Settings keyboard bug (emulator video):** the Settings scroll column had no `imePadding()`, so with
  edge-to-edge the number pad covered the rest-day fields. Fixed by padding the scroll area for the IME.
- **Empty-slot copy:** `Add from favourites` until phase 4 adds Recipes (handoff copy is "... or recipes").
- **Removing a meal:** not in the mock; long-press a meal row -> menu with `Remove` (destructive).
  Without it a mistaken add can't be undone.
- **Accent:** `SectionAccent.Meals` light becomes `#2E7D32` (handover accent fill; was `#43A047`).
  Dark values from the design conversion (`__dkHex`), like the other sections: accent `#91E091`,
  eaten `#82D583`, deep `#A5E9A4`, container `#1A221A`, soft `#141814`, planned/dashed `#305333`.
  (README suggests `#81C784` / `#A5D6A7` "confirm with design" - conversion keeps sections consistent.)
- **Logic kept pure and tested:** day totals / slots / training (`MealsDay`), water tap rule,
  macro line (`640 kcal · P 48 · C 70 · F 16`, en-GB separators), litres label (`1.25 of 2.5 L`).

## Checklist

- [x] `meals/backend`: models, `MealsRepository` + `FirestoreMealsRepository`, `MealsDay` builder, `MealsViewModel`
- [x] `settings/backend`: `MealSettings` in `SettingsRepository` + DataStore + `SettingsViewModel`
- [x] Tests: `FakeMealsRepository`, `MealsViewModelTest`, `MealsDayTest`, label tests, settings tests
- [x] `ui/theme/MealsColors.kt` (light/dark), `SectionAccent.Meals` -> `#2E7D32`
- [x] 1a Today checklist screen (header, training banner, summary, slot cards, empty slot, water)
- [x] 1c Add meal sheet (Favourites search + multi-pick, New form, footer states)
- [x] Settings -> Meals rows (slots, training / rest targets, water target)
- [ ] `/verify`, screenshots to `docs/screenshots/53-meals-phase-1/` + issue, PR with `Closes #53`

## Next step

User runs the Manual steps on #53 (they write real Firestore data); then screenshots of a filled day,
PR with `Closes #53`.
