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
   emulator", then give the user the issue's numbered Manual steps and stop - they test and tick them.
8. **All items ticked** (re-read the issue - the user ticks Manual ones on GitHub): tick the PR's
   "Verified by Claude" items that are true (`gh pr edit <pr> --body-file`; never the "Your review"
   ones), mark the PR ready (`gh pr ready`), move the board card to **In Review** (`gh project item-edit`, ids in
   `test-execution.md` "Ticket lifecycle"), then tell the user the PR is ready for their review with its link and what to
   look at - and stop. Merging waits for the user's explicit "merge".

## What NOT to do

- Don't tick Manual items, and don't tick an Automated item whose check didn't run or failed.
- Don't merge, or run `gh pr merge` - the user reviews on GitHub first.
- Don't edit any part of the issue body other than the checkbox characters.
