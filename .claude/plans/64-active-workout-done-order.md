# #64 Active workout: move an exercise up once a set is ticked and its rest ends

Branch `feature/cmarques/64-active-workout-done-order` from develop `99eff84`. Issue #64 (card In Progress).
#65 (Do Not Disturb) is also In Progress, at the user's request - it gets its own branch after this one.

## Decisions

- **User's rule (clarified 2026-11-23, after two misreads):** tick a set of an exercise further down ->
  once that rest ends (timer 0 or Skip) the card moves up, under the cards already started; none
  started -> very top. Not "every set done" (first version, `85055e1`), not sets moving inside a card
  (tried and dropped the same day).
- **Started** = `ActiveExercise.hasDoneSet` (any set ticked). Order in `doneOrder: Int?` (1 = first to
  move up...), not in the list order - the list keeps the template's order for saving / picker / duplicate.
- `withDoneOrder(restingId)` (pure, `ActiveWorkout.kt`) runs on every change in
  `StrengthViewModel.updateExercises` with `restState.instanceId`: numbers newly started exercises except
  the resting one, clears it when no set is ticked. `RestState.instanceId` (defaulted) was added for this.
- `toggleSetDone` sets the rest *before* updating exercises, so the ticked exercise waits; a new rest from
  another exercise releases the previous one. `endRest()` (Skip + timer 0) and `adjustRest` (-30 can end
  it) re-run the order. Rest Off -> moves immediately.
- UI: `inDisplayOrder()` + `Modifier.animateItem()`; `requestScrollToItem(0)` when already at the top
  (else the moved card ends up cut off above).
- **Keyboard (user report, same session):** number pad covered kg/reps fields - `imePadding()` on the
  `Box` around the workout `LazyColumn`, same as the Settings fix in #53.
  The empty gap above the keyboard (bottom bar height counted twice) - fixed app-wide with
  `consumeWindowInsets(padding)` on the NavHost in `OneNavHost.kt`.

## Checklist

- [x] `hasDoneSet`, `withDoneOrder(restingId)`, `RestState.instanceId`, ViewModel rest hooks + unit/VM tests
- [x] `ActiveWorkoutContent`: display order + `animateItem()`; `imePadding()` for the keyboard
- [x] Checks pass (test, lint, assembleDebug); installed on emulator; issue #64 title/text + Manual 1-8
  rewritten; old screenshots removed
- [ ] User runs Manual 1-8 on #64; commit; new screenshots; PR `Closes #64`, CI, card -> In Review

## Next step (session of 2026-11-23)

1. **#64**: user runs Manual 1-8 (exercise moves after rest + keyboard). Then commit, new screenshots in
   `docs/screenshots/64-active-workout-done-order/` + on #64, PR to develop with `Closes #64`, wait for CI (`gh pr checks`), card -> In Review, then wait for "merge".
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
