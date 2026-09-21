---
name: commit
description: Stage and commit changes on this Android repo using Conventional Commits — groups files by what changed (deps, build, feature package, tests, docs), writes the message, and pushes. Use when the user asks to commit, or when work is ready to commit.
---

# Commit

1. Abort if the current branch is `develop` or `master` — name the branch and stop.
2. Run in parallel: `git status`, `git diff HEAD`, `git log develop..HEAD`.
3. Group changed files:

   | Path pattern | Type |
   |---|---|
   | `gradle/libs.versions.toml` | `chore(deps)` |
   | `app/build.gradle.kts`, `build.gradle.kts`, `gradle/` | `chore(build)` |
   | `app/src/.../<package>/**` | `feat(<package>)` (or `fix(<package>)` for a bugfix) |
   | `app/src/main/res/**` | `feat` or `chore`, depending on content |
   | `*Test.kt` | `test` |
   | `*.md` | `docs` |
   | `.claude/**` | `chore(claude)` |

   Scopes: `auth workout diary calendar dashboard meals exercise notifications nav theme build deps claude`.

   If a change only makes sense as a single commit despite touching multiple groups above (e.g.
   a feature and its test added together), don't force an artificial split — make one commit and
   pick the most relevant type/scope. (`commit-review` inherits this since it references the
   same rules.)
4. Stage explicitly — `git add <file>` per file. Never `git add -A` or `git add .`.
5. Write the commit message via heredoc: `<type>(<scope>): <summary>`. **No `Co-Authored-By:` trailer** (see `.claude/agents/agents.md`).
6. `git push -u origin <branch>`.

If the changes span multiple groups from the table, make one commit per group rather than one commit for everything.

## What NOT to do

- Don't commit on `develop` or `master`.
- Don't stage files you didn't intend to — check `git status` after `git add`.
- Don't add any commit trailer.
- Don't bundle unrelated changes into one commit.
