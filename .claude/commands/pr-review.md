---
description: Review the diff against develop, present findings, then apply fixes on approval.
---

Follow the `pr-review` skill (`.claude/skills/pr-review/SKILL.md`):

1. Check `git diff develop...HEAD` against the skill's checklist.
2. Present findings as Must fix / Should fix / Consider.
3. **Stop and wait for approval before changing anything.**
4. On approval, apply the agreed fixes and run `./gradlew lint test`.
