# Plan: Goals - habit reminder time picker (design option 1a)

Branch: `feat/goals-time-picker` (from `develop` at `9470b04`, after #38). No issue number yet.
Handover: `C:\Users\claud\Downloads\OneApp-designs\Diary\design_handoff_goals_time_picker\`
(`README.md` is the spec; `Goals Time Picker.dc.html` option 1a is the interactive reference). Outside
the repo - read it with `/add-dir`.

## Decisions

- **Reminder ids are stable, not random on read.** The handover's `id = UUID.randomUUID()` default
  would give old Firestore reminders (no `id` field) a new id every time they're read, so WorkManager
  tags would never match on cancel. Instead `id` defaults to `""`; migration assigns old reminders
  `legacy-<TIMEOFDAY>` and new times get a UUID when they're created.
- **Migration runs in `FirestoreGoalRepository` on read** (`Habit.withMigratedReminders()`, pure,
  in `goals/backend/`), so the ViewModel, the list card and scheduling all see one shape. Reminders
  are kept sorted by time. On save, `timesOfDay` is derived from the enabled and disabled times.
- **Old WorkManager jobs.** Old work was named `habit_reminder_<habitId>_<TIMEOFDAY>`. Saving or
  deleting a habit also cancels those four old names, so a re-saved habit never fires twice. Habits
  that are never re-saved keep their old job (same behaviour as before) until they are.
- **Notifications stay in `notifications/`** (WorkManager stores the worker class name). Only the
  scheduler's work-name/notification-id inputs change, and the worker's input keys stay the same.
- **No-suffix hours are 24-hour** (`8` = 08:00), as in the mock. 1-6 is not defaulted to PM.
- **Habit name is edited inline** in the expanded card header (16sp/600, placeholder "Habit name").
  The mock only shows a filled name, and "Add another habit" needs an empty one.
- **Save** is enabled when the goal name is set and at least one habit has a name. Habits with a
  blank name are dropped on save.
- **Builder is full-screen.** The Diary tab bar is hidden while building a goal, matching the mock's
  shell (close button + title).
- **Colours** go in `ui/theme/Color.kt` as named vals (architecture rule), not as file-local
  `Color(0x...)`. **Dashed border**: a shared `ui/dashedBorder` (Calendar keeps its own copy for now, see follow-ups).
- **Layout**: logic in `goals/backend/` (parse, part of day, 15-minute options, next time,
  migration, ViewModel). Display in `goals/frontend/` (time label formatting, time field + dropdown,
  builder, goal card).

- **New goal starts with one blank, expanded habit**, so the builder opens on a card to fill in.
- **Edit/Delete menus** on journal entries and goal cards share `ui/EditDeleteMenu` (Edit black,
  Delete red, both with icons) - asked for during this work.

## Open / follow-ups

- ~~No control to remove a whole habit~~ - user asked for a ⋮ menu with Delete on the habit card
  (expanded and collapsed), done via `EditDeleteMenu` with Edit hidden.
- `CalendarScreen.kt` keeps its private `dashedBorder`; switch it to `ui/dashedBorder` when its
  ktlint baseline is cleaned.
- No tests for the Compose time field itself (no UI test setup) - its parsing/steps logic is
  covered in `HabitTimesTest`.

## Checklist

- [x] Backend: `HabitReminder.id`, `partOf(hour)`, `parseTime`, 15-minute options, `nextReminderTime`,
      `withMigratedReminders`, derived `timesOfDay`. Unit tests for all of the handover's parse table rows.
- [x] `FirestoreGoalRepository` applies the migration on read.
- [x] `GoalViewModel`: `addDraftHabit(name, reminders)` returns the id, `renameDraftHabit`,
      `updateHabitReminders` sorts, and save drops blank habits and derives `timesOfDay`. Tests updated.
- [x] Scheduling: `HabitReminderScheduler` per-reminder work name `habitId:reminderId` + legacy
      cancel; `HabitReminderSync` per reminder; the notification label is `partOf(hour).label`.
- [x] Frontend: time field with 96-option dropdown (scroll to current time - 2 rows, select-all on
      focus, type + parse on Done or blur, Escape/Back cancels, tap outside commits), habit card
      (expanded and collapsed), Add time, Add another habit, top bar + bottom Save bar.
      `HabitReminderDialog`, `hourOptionsFor` and `formatHour` are removed.
- [x] `GoalCard` subtitle `Drink water · 7:30 AM, 1:15 PM` (enabled times only).
- [x] `DiaryScreen`: hide the tab row while building; wire the new callbacks.
- [x] `./gradlew assembleDebug test lint` green (109 tests), installed on the emulator (Pixel_7).
- [x] User reviewed on the emulator; committed (`e762a4f`) and opened PR #40 -> develop.
- [ ] Manual: old per-part goal opens with its times and doesn't double-notify after re-save; a
      reminder set a few minutes ahead fires.

## Next step

PR #40: wait for CI green + the two manual checks, then merge.
