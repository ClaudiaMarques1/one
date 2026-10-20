# Plan: Workout section redesign

Branch: `feat/workout-redesign` (from `develop` at `49d9b79`). No issue number yet.
Handover: `C:\Users\claud\Downloads\OneApp-designs\Workouts\design_handoff_workout\` (`README.md` is the
spec; `Workout Redesign.dc.html` is the interactive reference). Outside the repo - `/add-dir` it.
Build **1a** home, **1b** start sheet, **1c** active workout, **1d** template builder, **2b** exercise
picker. Not 1e / 2a / 2c.

## Decisions (user, 2026-10-11)

- **Finish with unticked sets: ask each time.** If every set is done, save straight away; otherwise a
  dialog: "Save only done sets" / "Save all sets" / "Keep going". Exercises left with no sets are
  dropped. Finish stays disabled until at least one set is done (as in the mock).
- **Reorder:** add `sh.calvin.reorderable` (version catalog) for the template builder's drag handle;
  `WorkoutTemplateViewModel.moveDraftExercise(from, to)`.
- **Extras:** confirm dialog before deleting a template; haptic (`LongPress`) when rest ends; rest time
  saved on `TemplateExercise` so the next workout starts with it. Card tap does nothing (not chosen).
- **Vibration setting:** on by default in this PR. The toggle lives in the profile/settings work below.
- **Shipping:** one PR, a commit per screen; user reviews on the emulator before the PR.

## Data / state (from the handover, adjusted)

- `StrengthSet.done` - kept out of Firestore (`@get:Exclude` on the getter; Serializable for
  SavedStateHandle). `StrengthExercise.restSeconds` (0 = Off, default 90) and `instanceId` (stable key
  for rest, previous values and Compose keys); `instanceId` and `restSeconds` not saved to sessions.
- `TemplateExercise.restSeconds` (saved; default 90) - carried into the session and back on finish.
- `previousSets` captured at session start (SavedStateHandle), keyed by `instanceId`.
- Rest timer in `StrengthViewModel`: `restState: StateFlow<RestState?>` (`exerciseName`,
  `totalSeconds`, `endsAtMillis`), wall-clock based, in SavedStateHandle. Pure helpers (ticks,
  -30/+30, summaries, volume, week count, last done) in backend files with unit tests.
- Firestore model changes need `rules.keep` checked (classes are already kept; new fields are fine).

## Checklist (one commit each)

- [ ] Data + ViewModels: models, rest state, previous sets, done toggling + fill-from-previous, add set
      copies last, remove set, finish modes, rest per exercise, template `moveDraftExercise`,
      home helpers. Tests.
- [ ] 1a Home + 1b start sheet (+ delete confirm)
- [ ] 1c Active workout (+ rest bar, rest sheet, finish dialog, discard confirm, haptic, long-press remove set)
- [ ] 1d Template builder (+ reorderable dependency)
- [ ] 2b Exercise picker (search across all categories, counts on the rail, selection tray)
- [ ] `./gradlew assembleDebug test lint`, install on emulator for the user's review, then PR

## Follow-up: profile drawer + settings (own branch/PR, after this one)

User request: on Home (dashboard), a round profile picture; tapping it opens a left-side drawer with the
profile picture, user name, **Account Details** and **Settings**. Settings holds "Vibrate when rest
ends", stored per user in Firestore (`users/{uid}/settings`, via a SettingsRepository; existing rule
covers it). No design yet - plain M3 with existing tokens. Then wire the rest timer to the setting.

## Status at end of 2026-10-11 (resume here)

Everything below is **uncommitted** on `feat/workout-redesign` (on disk, not lost by shutdown). Nothing built
or tested yet.

Written:
- `sessions/backend/ActiveWorkout.kt` - in-progress workout model kept apart from Firestore models
  (`ActiveSet` holds typed text + done; `ActiveExercise` has instanceId, restSeconds, previousSets;
  `RestState`; `FinishMode`), plus pure helpers: parse/format kg+reps, toggleSetDone (fills empty
  fields from previous), withAddedSet, set counts, setsToSave / toSessionExercises / toTemplateExercises,
  rest remaining / elapsedFraction / adjusted(-30/+30).
  Decision: no `done`/`instanceId` on StrengthSet/StrengthExercise, so Firestore models stay unchanged.
- `templates/backend/WorkoutTemplate.kt` - `DEFAULT_REST_SECONDS = 90`, `TemplateExercise.restSeconds`.
- `sessions/backend/StrengthViewModel.kt` - rewritten: keyed by instanceId; clock param; restState in
  SavedStateHandle; toggleSetDone starts rest; adjustRest/endRest; setRestSeconds; hasUnfinishedSets;
  finishSession(mode). (Old saveSession/updateSet/addSet(index) are gone.)
- `templates/backend/WorkoutTemplateViewModel.kt` - `moveDraftExercise(from, to)`.
- `workout/backend/WorkoutStats.kt` - sessionsThisWeek, lastDoneByTemplate, recentSessions, setCount, volumeKg.
- Tests: `ActiveWorkoutTest`, rewritten `StrengthViewModelTest`, `WorkoutStatsTest`.

## Next step (tomorrow)

1. Fix two wrong expectations in `ActiveWorkoutTest` "minus 30 ... plus 30" test:
   `adjusted(-30, nowMillis = 10_000)` -> endsAt **70_000** (not 50_000);
   `adjusted(30, nowMillis = 90_000)` -> endsAt **130_000** (not 120_000).
2. Add a `moveDraftExercise` test to `WorkoutTemplateViewModelTest`.
3. Build 1c active workout UI against the new ViewModel (the old `ActiveWorkoutContent` and
   `WorkoutScreen` wiring won't compile until then) - data + 1c go in **one commit**.
   UI labels (rest "2:00"/"Off", elapsed mm:ss, Previous "60 kg × 8"/"BW × 12", set summaries
   "3 × 10 · 35 kg", home meta) belong in frontend label files with tests.
4. Then 1a+1b, 1d (+ `sh.calvin.reorderable` in the version catalog), 2b - a commit each.
5. `./gradlew assembleDebug test lint`, install on the emulator for the user, then PR.

Also parked: `feature/cmarques/37-testing-environment` has commit `5eaf34d` (lint workflow + docs),
**not pushed**; the android-conventions / pr-review skill updates are still to do (see that plan).
