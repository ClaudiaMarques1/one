---
name: pr-review
description: Reviews the diff between this branch and develop for correctness and architecture-rule violations, presents findings as Must fix / Should fix / Consider, waits for approval, then applies agreed fixes and runs lint+test. Use when the user asks for a PR review or to review changes before opening a PR.
---

# PR Review

1. `git diff develop...HEAD` — the diff under review.
2. Check it against `.claude/rules/architecture.md` and this checklist:
   - Dependency versions come only from `gradle/libs.versions.toml`, never a literal in `app/build.gradle.kts`.
   - Every `callbackFlow` closes with `awaitClose`.
   - Firestore model fields are all-defaulted; `.copy(id = doc.id)` applied in the repository, not the model.
   - A new repository is an interface + `Firestore*` implementation using `FirebaseModule.userCollection`;
     its ViewModel takes the interface (default arg) and has tests with a `Fake*Repository`.
   - No blocking work on the main thread (Firestore calls, large parses, network wrapped in `suspend`/`withContext(Dispatchers.IO)`).
   - ViewModels expose `StateFlow` publicly, never a mutable `MutableStateFlow`.
   - No Firestore/`FirebaseModule` calls from composables.
   - Any new runtime-dangerous permission (e.g. notifications) is actually requested at runtime, not just declared in the manifest.
   - Multi-write sequences that should be atomic use `runTransaction`/a batch, not sequential unguarded `.await()` calls.
   - No `app/google-services.json` content appears in the diff.
   - No em-dashes.
3. Present findings grouped as **Must fix** / **Should fix** / **Consider**. Stop and wait for approval.
4. Once approved, apply the agreed fixes.
5. Run `./gradlew lint test` and report the result.

## What NOT to do

- Don't silently fix things without listing them first.
- Don't skip `./gradlew lint test` after applying fixes.
