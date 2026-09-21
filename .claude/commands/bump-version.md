---
description: Bump the app version, create a release branch, commit, and push.
argument-hint: <X.Y.Z>
---

Follow the `bump-version` skill (`.claude/skills/bump-version/SKILL.md`) for version `$ARGUMENTS`:

1. Abort if on `master`.
2. Branch `release/<X.Y.Z>` from `develop`.
3. Bump `versionName`/`versionCode` in `app/build.gradle.kts`.
4. Commit `chore(release): bump version to <X.Y.Z>` (no trailer) and push.
