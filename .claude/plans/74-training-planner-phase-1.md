# Plan: #74 - Training planner phase 1 (routine data + schedule builder)

Issue: https://github.com/ClaudiaMarques1/one/issues/74 (phase 1 of 4; card In Progress)
Branch: `feature/cmarques/74-training-planner-phase-1` from `develop` (`5c2a3b7`, after #72 / PR #73).
Type: backend + data. No UI.

## Handover

`OneApp-designs/Calendar/design_handoff_training_planner/README.md` (2026-11-24) - sections "Data model",
"Building the schedule", "Day states", "Behaviour". Screens 5a / 4a come in phases 2 / 3.

## Phases (user, 2026-11-25: phased tickets, like Meals)

1. **#74** - data + schedule builder + tests, no UI.
2. Todo card - 5a in Calendar (week card, planned / missed outlines, day sheet Training plan + Missed).
3. Todo card - 4a Training plan sheet (Routine + Upcoming tabs, Save plan). Real Firestore read/write
   Manual step lives here.
4. Todo card - Meals reads training days from the plan (today: a logged workout only,
   `MealsViewModel.kt:29-31`).
Ticket 2 (own card, In Progress column): reminders, nudge, Google Calendar.

## Decisions

- Package `workout/planner/` (`backend/` now, `frontend/` for 4a in phase 3). Calendar and Meals read
  it the way they already read `StrengthRepository` (`CalendarViewModel.kt:13`, `MealsViewModel.kt:31`).
- Firestore: `users/{uid}/trainingRoutines/{effectiveFrom}` + `users/{uid}/trainingOverrides/{sourceDate}`
  via `FirebaseModule.userCollection` - existing owner-only rule covers both.
- **Routine versions, not one doc** (2026-11-25, changed from the issue's `trainingPlan/routine`): the handover
  says past days keep Done / Missed when the routine changes; one doc would rewrite the past. Each date uses
  the latest version with `effectiveFrom` <= it; saving twice on one day overwrites. 4a saves with
  `effectiveFrom = today`. Issue body to update.
- Day states beyond the handover table: a workout logged **today** is Done (not Planned); a skipped session
  in the past is Skipped (not Missed); a split counts turns from `splitAnchor` (blank = `effectiveFrom`).
- Stale overrides (routine no longer trains that day, not `added`) are dropped at build time, not deleted.
- Move targets: same week, today or later, no other session - a skipped session still blocks its day.
- Stored shape is Firestore-friendly (strings): dates ISO `yyyy-MM-dd`, times `HH:mm`, days as
  `DayOfWeek.name`, template = `WorkoutTemplate.id`. The builder parses into `java.time`.
- Reminder / nudge / Google Calendar sync fields stored now with the handover defaults (1 hour, on,
  off) so ticket 2 doesn't change the shape.
- "Added" workout on a rest day defaults to the first template at 18:00 (handover: Push day 18:00).
- Week for "move" follows the week-start setting - the builder takes the first `DayOfWeek` as a param.

## Open (ask when it comes up)

- A routine pointing at a deleted template: show as "Workout" or drop the session? (phase 2 UI)

## Checklist

- [x] Models + stored shape (`workout/planner/backend/TrainingPlan.kt`)
- [x] `TrainingPlanRepository` + `FirestoreTrainingPlanRepository`
- [x] Schedule builder + day states + move targets (`TrainingSchedule.kt`)
- [x] Tests: `TrainingScheduleTest` (16) + `FakeTrainingPlanRepository`
- [x] `test lint assembleDebug` pass locally (2026-11-25)
- [ ] Issue body: storage path -> routine versions; `/verify`, commit, PR

## Next step

Update #74's body (storage), `/verify` #74, commit, push, PR (draft) - confirm as one batch.
