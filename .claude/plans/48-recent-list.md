# #48 Workout: Recent list + session detail (3a)

Branch `feature/cmarques/48-recent-list` from develop `49cf97e` (after #65 / PR #67 merged). Issue #48 (card
In Progress). Designs in `OneApp-designs/Workouts/recent/`:
- `design_handoff_workout/README.md` - 1a "Recent workouts" rows (month in the date block, duration meta).
- `design_handoff_workout_session_detail/README.md` - 3a session detail, light + dark (the issue's image).

The GIF idea was skipped for now: the user is asking Gym visual for permission (2026-11-23). It stays in Todo.

## Scope (user, 2026-11-23): everything in the handoff

Rows + detail screen + Repeat + overflow (Edit sets, Save as template, Delete) + notes (a way to write
them, so the Notes card has content). Bodyweight display from the exercise catalog's equipment.

## Decisions

- **Session data** (`StrengthSession`, defaulted so old Firestore docs still map): `startedAt: Long?`
  (workout start, set on Finish from `sessionStartTimeMillis`; `createdAt` = end, from the VM `clock()`)
  and `notes: String`. Old sessions: no times, no duration ("13 sets" row meta, "-" duration tile).
- **Notes:** a Notes card in the active workout (1c) below the exercises, kept in SavedStateHandle,
  saved on Finish; also editable in Edit sets. Not in the 1c mock - kept plain (card + multi-line field).
- **Route:** `workout/session/{sessionId}` (`Destination.WorkoutSession`), pushed, no bottom nav.
  `SessionDetailViewModel` (sessions + templates repos, `sessionId` from the nav SavedStateHandle) builds
  the UI state. Repeat uses the Workout tab's `StrengthViewModel` via the Workout back-stack entry, then
  pops back to the Workout tab.
- **Repeat:** new active session with the session's exercises; Previous = this session's sets; rest and
  template id from the template with the same name if there is one (so Finish updates it), else default rest.
- **Comparison:** vs the latest earlier session (date, then createdAt) with the same `templateName` that
  has that exercise. Top weight up/down -> `+5 kg vs 1 Oct` (up colour) / `-2.5 kg vs 1 Oct` (secondary);
  same top weight -> total reps `+1 rep` (up) / `-2 reps` (secondary); else `Same as 1 Oct`. None -> hidden.
- **PR:** weight x reps > every earlier set of that exercise (earlier sessions + earlier sets in the same
  session). Needs at least one earlier session with the exercise - otherwise a first workout would be all
  PRs. 0 kg sets never count.
- **Bodyweight:** names whose catalog equipment is "body weight" -> `Bodyweight × 10` / `Bodyweight + 10 kg
  × 10`; other exercises `80 kg × 6`.
- **Edit sets:** edit mode on the same screen - per exercise the 1c set rows (kg/reps fields, Previous =
  the value before editing, no tick), Add set, long-press Remove set, notes field; Save / Cancel. Saving
  uses `setsToSave(ALL_SETS)` (empty field -> previous value). `StrengthRepository.updateSession`.
- **Save as template:** `addTemplate` with the session's exercises and sets; toast "Saved as template".
- **Delete:** confirm dialog, `StrengthRepository.deleteSession`, back to Workout.
- **Finish (user, 2026-11-24; replaces the 2026-10-11 "Save only done sets / Save all sets" choice):** the
  log always keeps only ticked sets. With unticked sets and a template, the dialog asks about the template:
  "Keep full template" (ticked sets update, unticked sets/exercises keep previous values) or "Trim template
  to ticked sets" (template = what was done - e.g. swapped barbell curls for DB curls mid-workout). No
  template -> no dialog. `TemplateUpdate` in `ActiveWorkout.kt`; `FinishMode` stays for Edit sets.
- **Duration (2026-11-24):** clock minutes (end minute - start minute), so it matches the header's
  "13:51–13:52"; 0 -> "<1 min". Replaces "rounded, at least 1" (header showed 13:51–13:51 next to "1 min").
- **Issue #48** body + title updated 2026-11-24 to the extended scope; Manual steps 4-15 added.
- **Colours:** new tokens `progressUp`, `prBadge`, `onPrBadge` (light/dark from the handoff); set-row
  divider = `WarmField` (within 2 hex steps in both themes); everything else maps to existing tokens.

## Checklist

- [x] Data: `startedAt`, `notes`; repo `updateSession` / `deleteSession` (+ Firestore, fake); StrengthViewModel notes,
  Finish times, `repeatSession` + tests
- [x] Pure: duration, row labels (month, meta), PR + comparison + load labels + tests
- [x] `SessionDetailViewModel` (detail state, edit draft, delete, save as template) + tests
- [x] UI: Recent rows (date block, meta, clickable), 1c notes card, detail screen + edit mode + menu,
  nav route, colour tokens
- [x] Checks (test, lint, assembleDebug), emulator light + dark, screenshots (`docs/screenshots/48-recent-list/`); Manual steps 1-15 by user (2026-11-24)

## Next step

PR to develop open - waiting for the user's review; merge only on their explicit go-ahead.
