Closes #<!-- issue number; use "Part of #n" if the ticket stays open -->

## What changed
<!-- The change in a few bullets: screens, logic, data, build. -->

## Why
<!-- The problem or idea behind it, and any decisions taken along the way (link the plan). -->

## How it was tested
<!-- Automated checks that ran, and the manual emulator steps from the issue. -->

## Notes for the reviewer
<!-- Anything to look at closely, follow-ups, known limitations. -->

## Before merge

### Verified by Claude
- [ ] CI green: `test`, `lint`, `build`
- [ ] Every item in the issue's `## Checks` is ticked (Automated by `/verify`, Manual by you on the emulator)
- [ ] Unit tests added/updated for new logic
- [ ] Plan `.claude/plans/<n>-<kebab>.md` up to date (decisions, ticked checklist, next step)
- [ ] `.claude/` docs and README updated where behaviour, rules or commands changed
- [ ] Security checklist (`.claude/rules/security.md`) run if this touches `firestore.rules`, `auth/`, a collection path, the manifest, workflows or logging - or not applicable

### Your review (on GitHub)
- [ ] Files changed read through - nothing unrelated (`.idea/`, local config, `google-services.json`)
- [ ] No new dependency, permission or architectural layer you didn't agree to
- [ ] "What changed / Why" above matches the diff and reads as the record for this ticket
- [ ] Approved - tell Claude to merge
