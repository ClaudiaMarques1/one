# Skills Index

Read on demand. Each skill lives in `.claude/skills/<name>/SKILL.md`; the matching
`.claude/commands/<name>.md` is a thin `/command` wrapper that points back to the skill, not a
second copy - change behaviour in the `SKILL.md`, keep the command's step summary in sync.

| Skill | Trigger | May touch | Branch / approval rules |
|---|---|---|---|
| `android-conventions` | Auto-loads when editing `.kt` under `app/src/main` (ViewModel, Repository, Screen) | Nothing - reference only | - |
| `commit` | `/commit`, or "commit this" | Staging, commits, `git push -u origin <branch>` | Aborts on `develop`/`master`. Explicit `git add <file>` only, no trailer |
| `commit-review` | `/commit-review`, or asked to review the commit plan | Same as `commit` | Same as `commit`, and stops for explicit approval before staging anything |
| `pr-review` | `/pr-review`, or asked to review before a PR | Source files for agreed fixes; runs `./gradlew lint test` | Diffs against `develop`; stops for approval before applying any fix |
| `verify` | `/verify`, or asked to verify/check a change on a ticket | Issue checkboxes (Automated only) + an issue comment; runs `./gradlew` checks, `scripts/emulator.sh` and the QA pass (`scripts/ui.sh` drives the app through each Manual step, restores what it changed); marks the PR ready, moves the board card | Needs an issue from the branch name; never ticks Manual items; never merges - tells the user the PR is ready and stops |
| `bump-version` | `/bump-version <X.Y.Z>` | `versionName`/`versionCode` in `app/build.gradle.kts`; creates and pushes `release/<X.Y.Z>` | Aborts on `master`; branches from `develop`; `versionCode` = previous + 1 |

None of the skills merge PRs, delete branches, or commit to `develop`/`master` - those stay manual
(see `CLAUDE.md` Git Flow).
