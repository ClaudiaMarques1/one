---
description: Commit and push current changes using the repo's commit conventions.
---

Follow the `commit` skill (`.claude/skills/commit/SKILL.md`) exactly:

1. Abort if on `develop` or `master`.
2. Gather `git status`, `git diff HEAD`, `git log develop..HEAD`.
3. Group changed files per the skill's table; commit each group separately with a Conventional
   Commits message (no trailer).
4. Push with `git push -u origin <branch>`.
