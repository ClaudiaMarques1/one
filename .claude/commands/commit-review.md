---
description: Show the proposed commit grouping and message(s) and wait for approval before committing.
---

Follow the `commit-review` skill (`.claude/skills/commit-review/SKILL.md`):

1. Abort if on `develop` or `master`.
2. Gather status/diff/log, group changes, draft commit message(s).
3. **Stop and present the plan — do not commit or push yet.**
4. On approval, commit (no trailer) and `git push -u origin <branch>`.
