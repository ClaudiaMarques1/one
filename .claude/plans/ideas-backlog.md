# Ideas backlog

Source: the user's running list at `~/Downloads/ideas.txt` (outside the repo, numbered
`N - idea`). Each idea is a draft card in the **Ideas** column of project board "one"
(https://github.com/users/ClaudiaMarques1/projects/1).

When the user says new ideas were added: read the file, add every number above the last one listed
here as a draft card (`gh project item-create`, then Status = Ideas), and append it below. When an idea
is picked up, convert the card to an issue, move it to Todo / In Progress, and give it its own plan.

| # | Card | Notes |
|---|---|---|
| 1 | Workout: play the exercise GIF when tapping its name during a workout | `exercises.json` already has `gifUrl` for all 1,324 exercises; needs a GIF-capable image loader (new dependency). The GIFs are © Gym visual (not MIT like the data) - get Gym visual's permission first, show the "© Gym visual" credit, keep 180x180 (THIRD_PARTY_NOTICES.md) |
| 2 | Journal: optional cycle tracking to factor into workouts | Inclusive gender option; main goal is menstrual-cycle tracking |
| 3 | Settings: dark / light mode toggle | Fits in the settings from #11 |
| 4 | Workout: turn on Do Not Disturb during a workout | Needs notification-policy access granted by the user |
| 5 | Journal: body and weight measurement check-ins | |
| 6 | Active workout: move completed exercises up with the other done ones | Touches 1c active workout (#43) |
| 7 | Spotify widget - worth adding? | Open question |
| 8 | Wear OS: connect the app to a Wear OS watch | Prerequisite for 9 and 10 |
| 9 | Wear OS: watch app with music controls | Depends on 8 |
| 10 | Wear OS: show heart rate and step count in the app | Depends on 8 |
| 11 | Profile drawer with settings (vibrate when rest ends) | Already the planned follow-up in `workout-redesign.md` |
| 12 | Meals: log meals with calories | Meals screen is a placeholder today |
| 13 | New Languages to be added | Added on the board, not in `ideas.txt`. The redesigned screens use literal strings (only sign-in and the dashboard use `strings.xml`), so translation means moving strings into resources first |
| 14 | Feature: have a guest option for those who want to use the workout feature | Added on the board, not in `ideas.txt`. Once-off use, data not kept. Firebase anonymous auth fits the existing per-uid rules; wiping the guest's data on exit needs a plan |
| 15 | Create Course templates | Added on the board, not in `ideas.txt`. Ready-made routines (treadmill/rowing/cycling intervals, stretching) - timed intervals, not sets x reps, so closer to the README's planned Cardio / Warmup session types |

Last added: **#15** (2026-11-21). #13-15 were created on the board directly - the next new number in
`ideas.txt` is 16.
