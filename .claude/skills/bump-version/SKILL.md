---
name: bump-version
description: Bumps the app version — creates a release branch, updates versionName/versionCode in app/build.gradle.kts, commits, and pushes. Use when the user asks to bump or release a version, e.g. "/bump-version 0.2.0".
---

# Bump Version

Usage: `/bump-version <X.Y.Z>`

1. Abort if currently on `master` — releases branch from `develop`.
2. From `develop`, create `release/<X.Y.Z>`.
3. In `app/build.gradle.kts`: set `versionName = "<X.Y.Z>"` and increment `versionCode` by exactly 1.
4. Commit: `chore(release): bump version to <X.Y.Z>` (no trailer — see `.claude/agents/agents.md`).
5. `git push -u origin release/<X.Y.Z>`.
6. Verify: `grep -n 'versionName\|versionCode' app/build.gradle.kts` shows the new values.

## What NOT to do

- Don't bump the version directly on `master`.
- Don't set `versionCode` to anything other than the previous value + 1.
