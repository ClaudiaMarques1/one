# Self-Healing

Read on demand. How to fix things in the same change instead of leaving them for the next session.

## Rules

- If a build/lint/test command fails after a change, fix it before calling the task done.
- Don't silently work around a failing check by disabling it, baselining it, or skipping it.
- If a `.claude/` file turns out wrong (stale line number, renamed file, false claim), fix that
  file in the same change.

## Where a correction goes

| Trigger | Update |
|---|---|
| A cited `file:line` or file path is stale | The `.claude/` file that cites it |
| A rule turns out false, or the code changed the rule | `architecture.md` / `CLAUDE.md` (watch the always-loaded budget in agents.md) |
| A new required check, or a check failure worth remembering | `rules/test-execution.md` |
| A gotcha that cost real time but isn't a rule | A new file in `notes/` |
| A skill misbehaved or its restrictions changed | That skill's `SKILL.md`, its `commands/` wrapper, and `skills/skills.md` |
| A CI change | `ci-cd/ci-cd-rules.md` |

## Format for a new rule

Lead with the rule in one line, then:

**Why:** the incident or constraint behind it - so a future session can judge edge cases.
**How to apply:** when it kicks in and what to do, concretely.

Before adding: check an existing file doesn't already cover it (update that instead), cite a real
`file:line` rather than restating code, and re-count the always-loaded docs if you touched them.
