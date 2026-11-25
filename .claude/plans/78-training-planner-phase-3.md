# Plan: #78 - Training planner phase 3 (planned/missed in Calendar + day editor)

Issue: https://github.com/ClaudiaMarques1/one/issues/78 (phase 3 of 4; card In Progress)
Branch: `feature/cmarques/78-training-planner-phase-3` from `develop` (`6dd961c`, after #76 / PR #77).
Type: backend + data + front-facing.

## Handover

`OneApp-designs/Calendar/design_handoff_training_planner/README.md` - "5a" sections 4 (grid fill + outline),
5 (legend + hint), 6 (day sheet: Training plan card, Missed card); "4a" Upcoming tab; "Behaviour" (moves
only to free days this week; overrides stay on their original date). The Google Calendar action in the
editor and reminders are ticket 2.

## Decisions

- **Dark planned pip `#4F7499`** (user, 2026-11-25), instead of the handover's `#84B0D8`, which was too close
  to done `#8DC4FF` (#76 QA step 8). Light stays `#9CC9F2`.
- Day states for the grid come from `TrainingPlanViewModel` (it already has routines, overrides, templates,
  logged dates); Calendar passes them into the grid like the week card - `CalendarViewModel` stays as is.
- The editor writes `DayOverride`s keyed by the session's original date (#74); Reset / Remove deletes the
  override. Step limits 05:00-23:00 by 30 min, as the handover.
- Planned sessions in Week / Day views: out of scope (handover "Try next").

## Checklist

- [x] Dark planned pip `#4F7499`
- [x] `DayEditor` (pure: editor state, status line, skip note, Upcoming rows, time steps) + 7 tests;
      ViewModel `monthStates`, `dayEditor`, day actions, `PlanTab` + 7 tests (51 planner tests total)
- [x] Grid outlines (overlay above the tint) + planned band + legend + hint
- [x] Day sheet Training plan + Missed cards (`planner/frontend/DayPlanEditor.kt`, slot in `DaySheet`)
- [x] 4a Routine / Upcoming segmented + Upcoming list with the shared editor
- [x] README + CLAUDE.md package lines brought up to date (user, 2026-11-25)
- [x] `/verify` + QA pass (8 pass, 2 needs you; every day change undone in the app), commit, PR

## Implementation notes

- `ui/dashedBorder` got optional `dash` / `gap` (defaults unchanged). It clips half the stroke at the
  edge, so the cells / legend pass `width = 3.dp` to show the handover's 1.5dp.
- The cell outline is a `matchParentSize` overlay - as a modifier before `.background()` the tint
  painted over it (first emulator look: outlines invisible).
- With an outline, the today / selected ring is drawn inset (padding 3dp) instead of as the border.
- A move or reset re-points the open day sheet (`follow`) so the sheet stays on the session.
- Future-day empty state is hidden in the month day sheet (the Training plan card covers it); Week /
  Day keep it.

## Next step

User's Manual steps on #78 + CI, then PR ready / In Review. After merge: phase 4 (Meals reads the plan).
