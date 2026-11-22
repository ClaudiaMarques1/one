# #64 Active workout: move completed exercises up

Branch `feature/cmarques/64-active-workout-done-order` from develop `99eff84`. Issue #64 (card In Progress).
#65 (Do Not Disturb) is also In Progress, at the user's request - it gets its own branch after this one.

## Decisions

- **Complete** = the exercise has sets and every one is ticked (`ActiveExercise.isComplete`).
- **Order lives in `doneOrder: Int?`** on `ActiveExercise` (1 = first finished, 2 = next...), not in the
  list order. The list keeps the template's order, so saving still writes the template back in its own
  order, and the exercise picker / duplicate keep working on positions as before.
- `withDoneOrder()` (pure, in `ActiveWorkout.kt`) runs on every change in `StrengthViewModel.updateExercises`:
  numbers newly complete exercises after the done ones, clears it from any no longer complete (set
  unticked, removed or added).
- `inDisplayOrder()`: done ones first by `doneOrder`, then the rest in list order. Unticking puts an
  exercise back in its own place among the still-to-do ones (not the top of them - the issue text
  is updated to match).
- UI: `ActiveWorkoutContent` renders `exercises.inDisplayOrder()` with `Modifier.animateItem()` (Compose BOM
  2024.12.01 has it) so cards slide. LazyColumn keeps the first visible card's key in place, so the next
  exercise to do stays in view.
- Saved session (`toSessionExercises`) uses list order = template order.

## Checklist

- [x] `isComplete`, `doneOrder`, `withDoneOrder()`, `inDisplayOrder()` + unit tests
- [x] `StrengthViewModel.updateExercises` applies `withDoneOrder()` + ViewModel test
- [x] `ActiveWorkoutContent`: display order + `animateItem()`; stays at the top when already there (`requestScrollToItem(0)` - without it the moved card ended up cut off above)
- [x] Checks pass (test, lint, assembleDebug); emulator run of Manual 1-3 by Claude; screenshots in `docs/screenshots/64-active-workout-done-order/` + issue, PR `Closes #64`

- [ ] User ticks Manual steps 1-6 on #64; then PR `Closes #64`, CI, card -> In Review

## Next step (session of 2026-11-23)

1. **#64**: give the user the 6 Manual steps (Claude already ran 1-3 on the emulator: screenshots in
   `docs/screenshots/64-active-workout-done-order/`, posted on #64). Once ticked: PR to develop with
   `Closes #64`, wait for CI (`gh pr checks`), card -> In Review, then wait for "merge".
2. **#65 Do Not Disturb** (card already In Progress, issue has Checks): branch
   `feature/cmarques/65-workout-do-not-disturb` from develop after #64 merges, plan
   `.claude/plans/65-workout-do-not-disturb.md`. Proposed approach, to confirm with the user first:
   - Settings -> Workout switch "Do Not Disturb during workouts", off by default (DataStore, like vibrate).
   - `ACCESS_NOTIFICATION_POLICY` in the manifest (normal permission); access is granted in system
     settings (`Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`), checked with
     `NotificationManager.isNotificationPolicyAccessGranted`.
   - Start workout -> remember the current interruption filter, set `INTERRUPTION_FILTER_PRIORITY`;
     finish / discard -> restore it. Only restore if we changed it (DND already on stays on).
   - Open questions: Priority vs total silence; what happens if the app is killed mid-workout (restore
     on next launch when no session is active?). Platform change -> run `.claude/rules/security.md`
     (manifest) + `/security-review`.
3. Then the Todo list continues top-down: "Workout: play the exercise GIF when tapping its name",
   #48 Recent list, schedule training days, Meals servings, Search Recipe, Meals in Calendar.
