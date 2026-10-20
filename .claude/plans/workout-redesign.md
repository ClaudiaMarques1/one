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

- [x] Data + ViewModels: models, rest state, previous sets, done toggling + fill-from-previous, add set
      copies last, remove set, finish modes, rest per exercise, template `moveDraftExercise`,
      home helpers. Tests. (Landed as 3 commits `4470148`, `d114f49`, `38ef8b8` - not bundled with 1c.)
- [ ] 1a Home + 1b start sheet (+ delete confirm)
- [ ] 1c Active workout (+ rest bar, rest sheet, finish dialog, discard confirm, haptic, long-press remove set)
- [ ] 1d Template builder (+ reorderable dependency)
- [ ] 2b Exercise picker (search across all categories, counts on the rail, selection tray)
- [ ] `./gradlew assembleDebug test lint`, install on emulator for the user's review, then mark
      draft PR #42 ready (`gh pr ready 42`)

## Follow-up: profile drawer + settings (own branch/PR, after this one)

User request: on Home (dashboard), a round profile picture; tapping it opens a left-side drawer with the
profile picture, user name, **Account Details** and **Settings**. Settings holds "Vibrate when rest
ends", stored per user in Firestore (`users/{uid}/settings`, via a SettingsRepository; existing rule
covers it). No design yet - plain M3 with existing tokens. Then wire the rest timer to the setting.

## Status at 2026-10-20 (resume here)

Data + ViewModel layer committed and pushed on `feat/workout-redesign`; draft PR #42 open against
`develop`. Branch has `develop` merged in (`cf4ee82`, PR #41): sources now live under
`app/src/*/kotlin/`, not `java/`. `./gradlew assembleDebug test lint` all pass (140 tests) - the old
`ActiveWorkoutContent`/`WorkoutScreen` wiring does compile against the new ViewModel.

Done since 2026-10-11: the two `ActiveWorkoutTest` expectations fixed (70_000, 130_000); ktlint
line-length fix in `StrengthViewModel.duplicateExercise`.

Written (see commits for detail):
- `sessions/backend/ActiveWorkout.kt` - in-progress workout model kept apart from Firestore models.
  Decision: no `done`/`instanceId` on StrengthSet/StrengthExercise, so Firestore models stay unchanged.
- `templates/backend/WorkoutTemplate.kt` - `DEFAULT_REST_SECONDS = 90`, `TemplateExercise.restSeconds`.
- `sessions/backend/StrengthViewModel.kt` - keyed by instanceId; old saveSession/updateSet/addSet(index) gone.
- `templates/backend/WorkoutTemplateViewModel.kt` - `moveDraftExercise(from, to)`.
- `workout/backend/WorkoutStats.kt` - home-screen helpers.

## Next step

1. Add a `moveDraftExercise` test to `WorkoutTemplateViewModelTest`.
2. Build 1c active workout UI against the new ViewModel. UI labels (rest "2:00"/"Off", elapsed mm:ss,
   Previous "60 kg × 8"/"BW × 12", set summaries "3 × 10 · 35 kg", home meta) belong in frontend label
   files with tests.
3. Then 1a+1b, 1d (+ `sh.calvin.reorderable` in the version catalog), 2b - a commit each.
4. `./gradlew assembleDebug test lint`, install on the emulator for the user, then `gh pr ready 42`.
