# Plan: #76 - Training planner phase 2 (4a Routine sheet + Training this week card)

Issue: https://github.com/ClaudiaMarques1/one/issues/76 (phase 2 of 4; card In Progress)
Branch: `feature/cmarques/76-training-planner-phase-2` from `develop` (`8f47510`, after #74 / PR #75).
Type: backend + data + front-facing.

## Handover

`OneApp-designs/Calendar/design_handoff_training_planner/README.md` - "4a: Training plan sheet" (Routine tab,
header, footer) and "5a" section 3 ("Training this week" card). Colours: README table, light + dark.

## Phases (order changed 2026-11-25, user)

1. #74 (merged) - data + schedule builder.
2. **#76** - 4a Routine tab + Save plan, and the 5a "Training this week" card (Edit routine opens 4a).
3. Todo card - rest of 5a (outlines, legend, cell bands, day sheet Training plan + Missed) + the shared day
   editor, which also powers 4a's Upcoming tab.
4. Todo card - Meals reads training days from the plan.
Reason: 5a can't show anything until a routine can be saved, and only 4a saves one.

## Decisions

- **Deleted template = rest day** (user, 2026-11-25): a routine day whose template no longer exists isn't
  planned; the user adds a workout that day if they want one. Implement in `TrainingSchedule.build` via the
  set of existing template ids, with a test.
- Upcoming tab -> phase 3 (it is the day editor). Reminders + Google Calendar cards -> ticket 2.
- Save plan writes a new routine version with `effectiveFrom = today` (#74 versions); split anchor = today
  when the split changes, else kept.
- Card expanded state: local DataStore (`SettingsRepository`), like week start.
- No routine yet: the week card shows a set-up prompt that opens 4a.

## Checklist

- [x] Deleted-template rule in `TrainingSchedule` (`templateIds`) + test
- [x] `TrainingPlanViewModel` (draft, validation, save, `_planError`) + 13 tests
- [x] `TrainingPlanText` (week, note, pips, routine line, next-up, target note) + 8 tests
- [x] 4a sheet UI (`workout/planner/frontend/TrainingPlanSheet.kt`), light + dark tokens in `CalendarColors`
- [x] Week card (`TrainingWeekCard.kt`) in Calendar month view (slot between tiles and grid); expanded
      state in DataStore (`trainingWeekExpanded`)
- [x] `test lint assembleDebug` pass (2026-11-25); first emulator look: card, 4a weekday + split, close
      without saving
- [x] `/verify` + QA pass (7 pass, 2 needs you), commit, PR #77 (merged 2026-11-25 `6dd961c`)

## Implementation notes

- One `TrainingPlanViewModel` serves the card and the sheet (factory `trainingPlanViewModel()`); it reads
  settings (week start, expanded), so `CalendarViewModel` is unchanged. Calendar passes the card into
  `MonthGridSection` as a slot.
- No routine: the card reads "No routine yet. Tap to plan your training days." and opens 4a.
- New ktlint-clean files were formatted with the ktlint CLI (`-F`) through a scratch Gradle init script on
  just those files - not the whole tree, which would disturb baselined files.
- Template order in menus: by name. Default weekly target 3.

## Next step

Done. Phase 3 (#78) continues in `78-training-planner-phase-3.md` (incl. the dark planned pip from QA step 8).
