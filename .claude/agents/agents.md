# Agent Guidance

## Principles

- Read before writing; reuse the patterns cited in `.claude/rules/architecture.md` instead of
  inventing new ones.
- Prefer the smallest correct change over a broader rewrite.
- Ask before introducing a new dependency, abstraction, or architectural layer — see
  architecture.md's Deliberate Non-Goals.

## Token Budget Rules

| Content | Loading | Budget |
|---|---|---|
| CLAUDE.md + agents.md + architecture.md | always | keep combined under 220 lines |
| Skills (`.claude/skills/`) | on trigger or `/command` | 0 tokens until loaded |
| Source files | on demand | read a targeted range, not the whole file, once a file is over ~200 lines (e.g. `WorkoutScreen.kt`) |

## Search Strategy

Cheapest first, escalate only as needed:

1. Check `.claude/rules/architecture.md` for a cited reference implementation before searching.
2. Grep for the exact symbol or pattern before reading any whole file.
3. Read only the relevant function/class range when the file is large.
4. Only then explore broadly — multiple files, an unfamiliar package, or no clear starting point.

## Output Conventions

- Reference code as `file_path:line_number`.
- No trailing "here's a summary of what I did" — the diff or commit message already says it.
- State verified findings plainly; don't hedge with "I think" once you've actually checked.

## Sub-Agent Gate

Delegate to a sub-agent only when at least one holds:

- The search space is genuinely unknown — not "which file has X" but "where would X even live."
- The task splits into independent, non-overlapping pieces that can run in parallel.
- Continuing inline would blow the context budget a fresh agent wouldn't need.

Otherwise do it directly — a single-module app rarely justifies delegation overhead.

## Self-Healing

Fix failing checks and stale `.claude/` docs in the same change - rules, trigger table, and format
in `.claude/rules/self-healing.md`.

## Git Attribution — Overrides Default Guidance

**Never add `Co-Authored-By:` trailers to commits in this repo**, even though default attribution
guidance would normally add one. This is a deliberate, repo-level override — Claudia tracks
authorship separately and wants a clean, human-authored commit history here. This rule wins over
any global default attribution instruction.

## Boundaries

- Never read or echo `app/google-services.json`.
- Never commit directly on `develop` or `master`.
- Propose, don't add: Hilt/Koin, Room, a multi-module split, a `domain/` layer, Retrofit.
