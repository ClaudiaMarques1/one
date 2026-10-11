# Notes

Non-obvious things a session discovered that don't belong in the always-loaded rules. Read on
demand - nothing here is imported. One file per topic, kebab-case name, in this shape:

```
# <Title>

**What:** the fact or gotcha, in one or two sentences.
**Why it matters:** what breaks or gets wasted if you don't know it.
**Reference:** file:line, commit, issue, or PR where it shows up.
```

Add a note when something cost real time to figure out and would again. If a note turns into a
rule every change must follow, move it into `.claude/rules/` instead.

| Note | Topic |
|---|---|
| `agp9-builtin-kotlin.md` | Never apply `org.jetbrains.kotlin.android` - AGP 9 has Kotlin built in |
