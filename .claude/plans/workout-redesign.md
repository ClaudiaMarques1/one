# Plan: Workout section redesign

Branch: `feat/workout-redesign` (from `develop` at `49d9b79`). Issue #43 (In Progress on project board "one").
Handover: `~/Downloads/OneApp-designs/Workouts/design_handoff_workout/` (`README.md` is the
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
- [x] 1a Home + 1b start sheet (+ delete confirm)
- [x] 1c Active workout (+ rest bar, rest sheet, finish dialog, discard confirm, haptic, long-press remove set)
- [x] 1d Template builder (+ reorderable dependency)
- [x] 2b Exercise picker (search across all categories, counts on the rail, selection tray)
- [x] `./gradlew assembleDebug test lint`, install on emulator for the user's review, then mark
      draft PR #42 ready (`gh pr ready 42`)

## Follow-up: profile drawer + settings (own branch/PR, after this one)

User request: on Home (dashboard), a round profile picture; tapping it opens a left-side drawer with the
profile picture, user name, **Account Details** and **Settings**. Settings holds "Vibrate when rest
ends", stored per user in Firestore (`users/{uid}/settings`, via a SettingsRepository; existing rule
covers it). No design yet - plain M3 with existing tokens. Then wire the rest timer to the setting.

## Status at 2026-11-21 (resume here)

**All five screens built and verified.** `/verify` at `69e344e`: test (169), lint, assembleDebug pass;
all 14 Manual steps ticked by the user on the emulator. PR #42 marked ready and #43 moved to In Review -
waiting on the user's GitHub review; merge only on their explicit "merge". Next after merge: the
profile drawer + settings follow-up (idea #11) on its own branch.

Data + ViewModel layer and all five screens committed on `feat/workout-redesign`; PR #42 against
`develop`. Sources live under `app/src/*/kotlin/` (PR #41); `develop` merged at `e190b02`.
#43 now has its `## Checks` section (14 numbered Manual steps covering all five screens).

1c landed the branch compiling again (it had failed in CI since `030bbdd` - stale local incremental
output had hidden it). Verified with `compileDebugKotlin --rerun` then `test lint assembleDebug`.

Written (see commits for detail):
- `sessions/backend/ActiveWorkout.kt` - in-progress workout model kept apart from Firestore models.
  Decision: no `done`/`instanceId` on StrengthSet/StrengthExercise, so Firestore models stay unchanged.
- `templates/backend/WorkoutTemplate.kt` - `DEFAULT_REST_SECONDS = 90`, `TemplateExercise.restSeconds`.
- `sessions/backend/StrengthViewModel.kt` - keyed by instanceId; old saveSession/updateSet/addSet(index) gone.
- `templates/backend/WorkoutTemplateViewModel.kt` - `moveDraftExercise(from, to)`.
- `workout/backend/WorkoutStats.kt` - home-screen helpers.
- `sessions/frontend/ActiveWorkoutContent.kt` - 1c: header (elapsed, progress, Finish), exercise cards
  (rest chip, overflow Duplicate/Remove, set grid, long-press Remove set), floating rest bar (ticks on
  the wall clock, haptic at 0 unless returning >2s late), rest sheet, finish + discard dialogs. Swipe
  shortcuts dropped (menu is primary per spec), so `SwipeDeleteBackground` was removed.
- `sessions/frontend/ActiveWorkoutLabels.kt` (+ test) - elapsed, rest, Previous, progress labels.
- `ui/theme/Color.kt` - `Workout*` tokens and the extra warm neutrals (`WarmBodyText`, `WarmHint`,
  `WarmField`, `WarmDisabledFill`) from the handoff.

## Next step

1. ~~Push, confirm CI~~ - 1c green on PR #42 (2026-11-21). Re-check `gh pr checks 42` after each push.
2. ~~1a Home + 1b start sheet~~ - done: `WorkoutHomeContent` (no inner Scaffold - OneNavHost's padding
   covers insets), `StartWorkoutSheet`, `WorkoutHomeLabels` + `templates/frontend/TemplateLabels.setSummary`
   (1d reuses it), delete confirm. Strings are literals like the other redesigned screens; the unused
   `workout_*` string resources were removed. Start sheet says "last done Thursday" only within a week,
   else "last done 8 Oct".
3. ~~1d~~ - done: `sh.calvin.reorderable` 2.4.3 (catalog `reorderable`; 2.4.x matches Compose BOM 2024.12).
   Uses the non-lazy `ReorderableColumn` inside the scrolling body - in 2.4.3 its content lambda is
   `ReorderableScope` (call `Modifier.draggableHandle()` directly; there is no `ReorderableItem` for it).
   Move applies on settle via `moveDraftExercise`. Row keys are name + occurrence, since templates can
   hold the same exercise twice. New token `WarmHandle`.
4. ~~2b~~ - done: filtering/counts/selection order in `exercise/backend/ExerciseSearch.kt` (+ test),
   labels in `ExercisePickerLabels.kt` (+ test). Fixed on the way: `onConfirm` returned catalog order,
   which would have undone a 1d reorder whenever exercises were added - it now returns selection order
   (existing exercises first, in their order). Back clears an active search first.
5. ~~`/verify`, Manual steps, `gh pr ready 42`~~ - done 2026-11-21. README Workout section rewritten for
   the redesign (its "animated GIF demos" claim was already wrong - GIFs are idea #1).
6. User reviews PR #42 on GitHub; merge on their go-ahead.
