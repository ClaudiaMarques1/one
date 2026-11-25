# #72 Automatic QA pass in /verify

Branch `feature/cmarques/72-qa-pass` from develop `52958eb` (after #69 / PR #70). Enhancement ticket #72 (card
In Progress), converted from the "Automatic QA pass" idea card on 2026-11-24. No design - tooling only.

## Why

During #48 and #69 Claude drove the emulator by hand (adb `uiautomator dump`, `input tap/swipe`, `screencap`)
for screenshots and to reproduce the "calendar stuck" bug. The user then walked the same Manual steps again.
Running them first turns the user's pass into "check the screenshots + the needs-you steps".

## Decisions (user, 2026-11-24)

- Option 1 of 3 (Claude drives the emulator via adb); Compose UI tests / screenshot tests stay later options.
- The user still does the Manual pass and ticks the items - Claude never ticks Manual.
- Steps are confirmed in batches (memory `confirm-steps-in-batches`); the user reports CI done (no polling).
- Settings: the 2026-11-24 read-only rules (`adb devices`, `adb exec-out screencap/cat`, `adb shell cat/dumpsys/
  sleep`, `cygpath`). Adding `gh project item-create` was blocked by the auto-mode safety check (self-granted
  permission) - the user can add it themselves. `gh api graphql` stays under the existing `gh api` ask rule
  (it can write anything). `scripts/ui.sh` is already covered by `Bash(scripts/*:*)`.

## Approach (to agree with the user before building)

- `scripts/ui.sh` - subcommands, not a sourced file: `dump` (text + bounds), `tap "<text>"`, `tap-xy x y`,
  `type "<text>"`, `swipe up|down`, `wait "<text>" [secs]`, `shot <name>` (into a folder passed in / scratch),
  `back`, `home-tab <label>`. Uses `scripts/lib/android-sdk.sh` for `adb`; fails loudly when text isn't found.
- `/verify` step 7: before the hand-over, a QA pass per Manual step - act, check, screenshot, result
  pass / fail / needs-you; restore state; a fail stops the hand-over.
- Report: table in the `/verify` issue comment (results only - `gh` can't upload images) and in chat with the
  screenshot paths.
- `test-execution.md`: the lifecycle step + the QA pass's limits.

## Checklist

- [x] Approach agreed with the user (2026-11-24)
- [x] `scripts/ui.sh` (smoke-tested twice: launch, wait, tap-last nav, has, swipe, shot, back; usage on bad command)
- [x] `/verify` SKILL.md QA pass + `commands/verify.md` + `test-execution.md` + `skills.md`
- [x] Trial run on #69's Manual steps; report (step 8 rerun 2026-11-25 with the 23rd selected: pass)
- [x] Checks; user's Manual steps (2026-11-25)

## Trial run on #69 (2026-11-24)

Steps 1-7 and 10 pass via `scripts/ui.sh` (screenshots in scratch `qa/69/`); 2, 3, 9 are visual checks.
Lessons, to fold into the SKILL.md before the PR:
- Same text in two places: `=2` hit the Diary entries tile's "2" before day 2 -> use `tap-last` for grid
  days (below the tiles). Added exact match (`=text`) to `ui.sh` for this.
- Steps share state: step 7 selected day 2, so step 8's Week/Day opened on 2 Nov. Re-select the date a step
  needs (e.g. `tap-last "=23"` before Week) - don't rely on the previous step.
- Step 5 couldn't check the Meals card (no meals planned today) -> report "needs you / not reachable", don't pass.
- (2026-11-25) `has Meals` matched the bottom-nav label -> check text only the target shows ("meals logged").
- All four folded into the verify SKILL.md QA pass, step 2.

## Next step

PR #73 ready for review (CI green, every #72 check ticked, card In Review). Merge only on the user's "merge".
