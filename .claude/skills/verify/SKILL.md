---
name: verify
description: Verifies the current branch's change against its ticket - works out the change type from the diff, runs the matching Gradle checks, ticks the Automated items in the GitHub issue and comments the results, then for front-facing changes opens the app on the emulator and hands the Manual steps to the user. Use when the user asks to verify/check a change, or when work on a ticket is ready to be tested.
---

# Verify

Workflow and checklist source of truth: `.claude/rules/test-execution.md`.

1. **Branch + ticket.** Abort on `develop`/`master`. Take the issue number from the branch name
   (`feature/<user>/<n>-...`); if there isn't one, ask which issue. `gh issue view <n> --json body,title`.
   If the issue has no `## Checks` section, stop and propose one from the template
   (`.github/ISSUE_TEMPLATE/change.md`) before going further.
2. **Change type.** List changed files: `git diff --name-only origin/develop...HEAD` plus uncommitted
   (`git status --porcelain`). Map them with the "Change type" table in `test-execution.md`; a change
   can have several types.
3. **Unit tests present?** New or changed logic (ViewModel, repository, model, pure helper) needs a
   matching test change under `app/src/test/`. If missing, write it before running checks.
4. **Run checks** - one Gradle call with the union of tasks the types need (`test`, `lint`,
   `assembleDebug`). On failure: fix it (self-healing rules), re-run. Never tick a failing check.
5. **Tick Automated items** that passed: write the issue body to a scratch file, change only lines
   under `### Automated` from `- [ ]` to `- [x]`, then `gh issue edit <n> --body-file <file>`.
   **Never tick anything under `### Manual`.**
6. **Comment** on the issue: commit SHA, change types, each check with pass/fail
   (`gh issue comment <n> --body-file <file>`).
7. **Front-facing / data / platform:** run `scripts/emulator.sh`, tick "Installed and opened on the
   emulator", then do the **QA pass** (below) and give the user its report with the issue's numbered
   Manual steps - and stop. They do their own pass and tick the Manual items.
8. **All items ticked** (re-read the issue - the user ticks Manual ones on GitHub): tick the PR's
   "Verified by Claude" items that are true (`gh pr edit <pr> --body-file`; never the "Your review"
   ones), mark the PR ready (`gh pr ready`), move the board card to **In Review** (`gh project item-edit`, ids in
   `test-execution.md` "Ticket lifecycle"), then tell the user the PR is ready for their review with its link and what to
   look at - and stop. Merging waits for the user's explicit "merge".

## QA pass (#72)

Run every Manual step on the emulator first, so the user starts from a pre-checked list. Drive the app
only through `scripts/ui.sh` (`dump`, `has`, `tap`, `tap-last` for bottom-nav labels, `tap-xy`, `type`,
`swipe`, `back`, `wait`, `shot`, `launch`) - no raw `adb` taps.

1. `scripts/ui.sh launch` first (fresh state). Note anything you'll change (theme, a setting) to put back.
2. Per step: act it out, check the "expect" part on screen (`has` / `dump`, or look at the `shot`), and
   save `shot <scratchpad>/qa/<n>/step-<k>.png`. Look at every screenshot - text being present isn't
   the same as it looking right.
3. Result per step:
   - **pass** - the expected text/state is there and the screenshot looks right.
   - **fail** - it isn't, or something else is wrong (crash: `scripts/app-status.sh`). Stop the hand-over:
     fix it, re-run the checks, re-run the QA pass.
   - **needs you** - a judgement only the user can make (does it match the design, is it what they
     meant), or a step you can't reach (permission prompt, real data you shouldn't create). Say why.
4. Don't invent data a step doesn't need. If a step needs some (a workout, a meal), create the minimum,
   say so in the report, and delete it after through the app (session Delete, meal remove) - or ask first
   when it can't be undone from the app.
5. Put back what you changed (theme, settings, a running workout - discard it), then `launch` again.
6. Report: one row per step - `| # | Step | Result | Note |` - in the `/verify` issue comment (results
   only; `gh` can't upload images) and in chat with the screenshot paths.

## What NOT to do

- Don't tick Manual items because the QA pass passed - the user's own pass is what ticks them.

- Don't tick Manual items, and don't tick an Automated item whose check didn't run or failed.
- Don't merge, or run `gh pr merge` - the user reviews on GitHub first.
- Don't edit any part of the issue body other than the checkbox characters.
