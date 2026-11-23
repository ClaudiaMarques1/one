# #65 Workout: Do Not Disturb during a workout

Branch `feature/cmarques/65-workout-do-not-disturb` from develop `16f0478` (after #64 / PR #66 merged).
Issue #65 (card In Progress).

## Decisions (user, 2026-11-23)

- **Level: Priority only** (`INTERRUPTION_FILTER_PRIORITY`) - the phone's own DND exceptions still ring
  (alarms, starred contacts, repeat callers).
- **App killed mid-workout: restore on next launch.** A fresh launch (`savedInstanceState == null`) can't
  have a running workout (it lives in `StrengthViewModel`'s SavedStateHandle), so `MainActivity` restores.
- Setting `dndDuringWorkout` in `SettingsRepository` / DataStore, **off by default**, Settings -> Workout
  switch under "Vibrate when rest ends". Turning it on without access opens
  `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`; the setting is stored on return only if access was granted.
  Turning it off restores DND straight away (also mid-workout).
- `ACCESS_NOTIFICATION_POLICY` in the manifest (normal permission; access itself is granted in system settings).
- `notifications/WorkoutDoNotDisturb.kt` (platform helper like `RestVibrator`; in flat `notifications/` because Settings uses it too):
  `turnOn()` only when DND is off (filter ALL) and access granted - remembers the previous filter in its own
  small DataStore (`workout_dnd`) so it survives process death; `restore()` puts it back only if we
  changed it and it's still Priority (the user changing DND by hand wins). Pure decisions
  `dndFilterOnStart` / `dndFilterOnRestore` are unit tested.
- `WorkoutScreen`: `LaunchedEffect(workoutActive, dndDuringWorkout)` - active + on -> `turnOn()`; not
  active -> `restore()`. No restore on the setting's initial `false` while active (that would flicker DND
  off when coming back to the tab); leaving the tab mid-workout does nothing.

## Checklist

- [x] Setting in repo/DataStore/Fake + `SettingsViewModel` + tests; summary text + test
- [x] `WorkoutDoNotDisturb` + pure decision tests; manifest permission
- [x] Settings switch row with access flow; `WorkoutScreen` effect; `MainActivity` restore on fresh launch
- [x] Checks (test, lint, assembleDebug), security checklist + `/security-review` (no findings), emulator run by Claude
  (on / kill + relaunch restores / discard restores / already-on stays on); Manual 1-6 ticked by user
- [ ] PR to develop `Closes #65`, CI, card -> In Review

## Next step

PR open for review; merge only on the user's "merge". Then the Todo list continues top-down: "Workout: play
the exercise GIF when tapping its name", #48 Recent list, schedule training days, Meals servings,
Search Recipe, Meals in Calendar.

Note from emulator testing: once the stored setting read false after automated taps that had misfired; a
clean replay of the same flow persisted true every time - not reproduced, likely a stray tap.
