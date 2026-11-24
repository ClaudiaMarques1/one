# #69 Calendar: 5a month view with Meals

Branch `feature/cmarques/69-calendar-meals` from develop `e830f44` (after #48 / PR #68). Enhancement ticket #69
(card In Progress), converted from the "Adding meals to Calendar" idea card on 2026-11-24.

**Design:** `OneApp-designs/Calendar/design_handoff_training_planner/README.md` (received 2026-11-24), screen
**5a** / **5a dark** in `Calendar Redesign.dc.html`. 5a is the Calendar month view with the training plan built
in; this ticket builds 5a *without* the training parts. Older handoff `design_handoff_calendar_views` still
describes the Week and Day views.

## Decisions (user, 2026-11-24)

- **Always show meals** - no setting; `showMeals` goes.
- **Ticket:** an enhancement (`feat(calendar)`, label `enhancement`).
- **Split:** #69 = 5a month layout + meals. The training ticket ("Workout: schedule training days in Calendar")
  adds the "Training this week" card, Planned/Missed outlines + legend items, the Training plan card in the day
  sheet, the 4a routine sheet, reminders and Google Calendar.
- **Week and Day views stay** (switcher kept); 5a replaces only Month. Week/Day get meals per the older handoff
  (green dot, agenda item, Meals card).
- **Google Calendar (training ticket):** one-off = Calendar app intent when signed in with Google, else `.ics`;
  plus the 4a sync switch via `CalendarContract` (device calendar, READ/WRITE_CALENDAR permission prompt, no new
  library).

## 5a in this ticket

- **Header:** month (28/600) + year below; outlined **Today** only off the current month; chevrons. Switcher below.
- **Stat tiles:** Workouts (sessions done this month) / Diary entries / **Meals eaten / planned** (`62` + ` / 75`).
  Best streak tile goes.
- **Grid fill:** diagonal 135° bands, one per section with activity, order Workout -> Meals -> Diary.
  Workout = done (past; planned comes with training); Meals = eaten (past) or planned (today/future); Diary = past.
  No activity: past `#F4F1EE`, future transparent + muted text. Rings: selected on-surface, today accent.
- **Legend:** Workout, Meals, Diary, Today (Planned/Missed added by the training ticket) + hint
  "Tap a day to see what you logged or change what is planned" (training wording - use "see what you logged"
  until training lands? decide when building).
- **Day sheet** (`ModalBottomSheet`, tap any day): title + Today pill; cards Workout done (-> session detail 3a,
  route exists since #48), Meals (today/future `1 of 4 meals eaten`, past `3 meals logged`; tap -> Meals on that
  date), Diary, Empty (past, nothing). Replaces the inline day details in Month.
- **Colours:** handoff light/dark table - check which exist in `CalendarColors`; new ones in `ui/theme/`.

## Data (started before the handover, still valid)

- `CalendarViewModel` takes `MealsRepository`, reads `observeMealsBetween` for the selected month +-7 days
  (`CalendarDateKeys.mealsRange`); `DayActivity.meals` (slot order), `mealsEaten`, `mealsKcal`.
- Meal sort matches the Meals checklist: `MealSlot` enum order, unknown slot = Snacks.
- Month meal counts: eaten for past days, planned (all) for today/future.
- **Open Meals on a date:** navigate to Meals, put `openDate` in its back-stack entry's `SavedStateHandle`;
  `MealsScreen` reads it once and calls `selectDate`.

## Checklist

- [x] VM: meals flow, `DayActivity.meals`, month stats (workouts, diary, meals eaten/planned), drop `showMeals` + tests
- [x] Pure: cell bands (sections per day), meal labels, stats + tests
- [x] UI 5a: header, tiles, band cells, legend + hint, day sheet with cards; Week/Day meals bits
- [x] Nav: Calendar -> Meals on a date; Workout card -> session detail
- [x] Colours light + dark from the handoff
- [x] Checks (test, lint, assembleDebug), emulator light + dark, screenshots (`docs/screenshots/69-calendar-meals/`); Manual steps 1-10 by user (2026-11-24)

## Built (2026-11-24)

- Hint reads "Tap a day to see what you logged" - the handoff's "or change what is planned" comes with training.
- Day sheet: `ModalBottomSheet` capped at 86% height. It covers the bottom nav (M3 sheets sit in their own
  window); the handoff has it stop above the nav - accepted deviation.
- Calendar -> session detail goes through the Workout tab (`switchTab` in `OneNavHost`), because the detail
  screen's Repeat needs the Workout entry on the back stack - Back from the detail lands on Workout.
- Removed with the Best streak tile: `CalendarDateKeys.monthKeysUpToToday` / `longestActiveStreak` (+ tests).

## Next step

PR to develop open - waiting for the user's review; merge only on their explicit go-ahead. Then start the
"Workout: training planner in Calendar" card (convert to an enhancement ticket).

## Fixed during testing (2026-11-24, user video "calendar stuck")

- Calendar's **Log workout / Diary entry** pushed Workout/Diary onto the Calendar tab's own back stack
  (`navController.navigate`), so tapping Calendar restored that stack and showed Diary again. Now `switchTab`.
  Pre-existing, but the day sheet's empty state made it easy to hit.
- The day sheet reopened on returning to Calendar (its `rememberSaveable` date survived navigation) and, covering
  the bottom nav, swallowed tab taps. Leaving from the sheet now clears it.
- **Dark tints** (user, 2026-11-24 - "you cannot tell the difference between the blue and green"): tested 4 sets on
  the emulator, user picked **B**: workout `#1C3350`, meal `#1F3F27`, diary `#4A2A4C` (handoff `#19222A` /
  `#1A221A` / `#3D3041`). dE blue-green 11.6 -> 37.2, blue-purple 14.5 -> 20.7, green-purple 22.1 -> 47.2; day
  numbers 8.3-9.1:1. Light tints unchanged (blue-green dE 19.3).
- **Future day numbers** (user: "make sure the numbers can still be seen"): the muted grey was 2.6:1 on the page and
  1.6:1 on a tint. Now SecondaryText on the page (5.3 dark / 5.6 light) and BodySecondary on a tinted future cell;
  week strip and agenda future dates use SecondaryText too.
- **Training split** (user, 2026-11-24): two tickets after #69 - (1) "Workout: training planner in Calendar
  (routine, day changes, 5a + 4a)" = data + schedule builder, 5a training parts, 4a Routine/Upcoming, Meals hook;
  (2) "Workout: training reminders + Google Calendar" = reminders, nudge (needs a saved "workout started" flag),
  one-off event / .ics, CalendarContract sync (new permission -> security checklist). Both cards in Todo.
