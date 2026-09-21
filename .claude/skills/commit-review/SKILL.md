---
name: commit-review
description: Like the commit skill, but shows the proposed file grouping and commit message(s) and waits for explicit approval before staging, committing, or pushing anything. Use when the user wants to review the commit plan first, or asks for "commit review".
---

# Commit Review

Same grouping and message rules as the `commit` skill (abort on `develop`/`master`; parallel `git status`/`git diff HEAD`/`git log develop..HEAD`; group by path pattern; scopes `auth workout diary calendar dashboard meals exercise notifications nav theme build deps claude`; explicit `git add <file>`; no trailer) — with one difference:

1. Do the abort check, status gathering, and grouping as normal.
2. **Stop here.** Present the proposed groups and draft commit message(s). Do not stage, commit, or push anything yet.
3. Wait for explicit approval or requested changes.
4. Once approved: stage, commit (heredoc, no trailer), and `git push -u origin <branch>`.

## What NOT to do

Same as `commit`, plus: never skip the confirmation step, even if the grouping looks obvious.
