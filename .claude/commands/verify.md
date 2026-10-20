---
description: Run the change's checks, tick them on its issue, and open the emulator for manual steps.
---

Follow the `verify` skill (`.claude/skills/verify/SKILL.md`):

1. Find the issue from the branch name; stop if it has no `## Checks` section.
2. Classify the diff against `develop` (backend / front-facing / data / platform / build).
3. Run the matching `./gradlew` tasks; fix and re-run on failure.
4. Tick only the passed **Automated** items on the issue and comment the results.
5. Front-facing: run `scripts/emulator.sh` and hand the user the **Manual** steps.
6. When everything is ticked: PR ready, card to In Review, tell the user - **never merge**.
