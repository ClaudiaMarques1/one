---
name: Change
about: A feature, fix, or idea being built - with its verification checklist
title: "feat(<scope>): "
labels: enhancement
assignees: ClaudiaMarques1
---

<!-- What and why, in a few lines. Link the design handoff or the idea if there is one. -->

**Type:** <!-- backend / front-facing / data / platform / build - one or more, see .claude/rules/test-execution.md -->

## Checks
### Automated
<!-- Ticked by /verify. Delete the lines that don't apply to this change type. -->
- [ ] Unit tests added/updated for new logic
- [ ] `./gradlew test`
- [ ] `./gradlew lint`
- [ ] `./gradlew assembleDebug`
- [ ] Installed and opened on the emulator
### Manual (emulator)
<!-- Front-facing / data / platform only. Ticked by you after testing. One numbered step each. -->
- [ ] 1. <step> - expect <result>
- [ ] 2. <step> - expect <result>
