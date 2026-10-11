# Test Execution

Read on demand. Which checks a change needs before it's called done, and failures already hit.

## Required checks by change type

| Change | Must pass |
|---|---|
| ViewModel, repository, model, or other pure Kotlin logic | `./gradlew test` |
| Compose UI, resources, strings | `./gradlew assembleDebug` |
| Any `.kt` / `.kts` (style) | `./gradlew lint` (runs ktlint against the baseline) |
| Gradle files, `libs.versions.toml`, plugins | `./gradlew assembleDebug test lint` |
| `AndroidManifest.xml`, permissions, notifications, WorkManager | `./gradlew installDebug` + manual check on a device/emulator |
| Auth flow, Firestore reads/writes, `firestore.rules` | `installDebug` + manual sign-in and a real read/write |

CI (`.github/workflows/unit-tests.yml`) runs `./gradlew test` only - lint and device checks stay local.

## What the unit tests can cover

Each repository is an interface with a `Firestore*` implementation; ViewModel tests pass an in-memory
`Fake*Repository` (test source set, next to the test) plus `testing/MainDispatcherRule`. Reference:
`workout/sessions/backend/StrengthViewModelTest.kt` + `FakeStrengthRepository.kt`. The `Firestore*`
classes themselves (queries, the transaction) stay untested - don't add Robolectric or a mocking
library for them without asking (see `architecture.md` Deliberate Non-Goals).

## Known failures

| Symptom | Cause / fix |
|---|---|
| `JAVA_HOME is not set` from `./gradlew` | No JDK on PATH. Use Android Studio's: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`. Gradle then provisions JDK 25 itself. |
| ktlint `Skipping rule(s) which are depending on a rule which is not loaded` / "failed to parse" | A rule is enabled while its dependency is disabled in `.editorconfig`; ktlint 1.0.1 crashes instead of skipping. Disable the dependent rule too. |
| ktlint reports violations in code you didn't touch | The baseline matches by line number; added/removed lines above a baselined violation shift it. Fix the violation - don't regenerate the baseline. |
| `File google-services.json is missing` | It's gitignored and local-only. Locally: download from Firebase Console. CI writes a placeholder (`.claude/ci-cd/ci-cd-rules.md`). |
