# Test Execution

Read on demand. Which checks a change needs before it's called done, how a ticket moves from idea
to merge, and failures already hit. Run the checks with `/verify` (`.claude/skills/verify/SKILL.md`).

## Required checks by change type

A change can have several types; it needs the union of their checks. Automated checks are ticked on
the issue by `/verify`; Manual steps are written per ticket as numbered "step - expect result" lines
and ticked by the user after testing on the emulator.

| Type | Change | Automated | Manual (user, emulator) |
|---|---|---|---|
| Backend | ViewModel, repository, model, or other pure Kotlin logic | unit tests added/updated (ViewModel + `Fake*Repository`); `./gradlew test lint` | - |
| Front-facing | Compose UI, resources, strings, navigation | `./gradlew test lint assembleDebug`; installed + opened via `scripts/emulator.sh` | the ticket's numbered steps |
| Data / auth | Auth flow, Firestore reads/writes, `firestore.rules` | as Backend | signed-in real read/write |
| Platform | `AndroidManifest.xml`, permissions, notifications, WorkManager | `./gradlew assembleDebug`; installed + opened | permission prompt / notification fires |
| Build | Gradle files, `libs.versions.toml`, plugins, CI | `./gradlew assembleDebug test lint` | - |

Every `.kt` / `.kts` change also needs `./gradlew lint` (runs ktlint against the baseline).
CI (`.github/workflows/unit-tests.yml`) runs `test`, `lint` and `build` (`assembleDebug`) as separate
jobs on every PR; device checks stay local.

## Screenshots

Every change that can be screenshotted gets screenshots, kept in two places:

1. **Git:** `docs/screenshots/<n>-<kebab>/` (descriptive file names + a short `README.md` index),
   committed with the ticket's work.
2. **The ticket:** once pushed, a comment on the issue showing them inline - `<img width="200">`
   in 4-column tables, `src` = `https://raw.githubusercontent.com/ClaudiaMarques1/one/<commit-sha>/docs/screenshots/...`
   (pin the SHA, not the branch, so links survive merge/branch changes; `gh` can't upload attachments).
   Repeat in the PR body's "How it was tested".

**Why:** the user wants the visual record with the change and on the ticket, not in a scratch folder.
**How to apply:** before committing, check each image for personal data - emails, passwords, full
names, tokens - and paint it over (PowerShell `System.Drawing` `FillRectangle` in the surface colour;
no ImageMagick here) or leave that image out. Skip outdated shots that no longer match the code.

## Ticket lifecycle

Project board "one" (https://github.com/users/ClaudiaMarques1/projects/1), columns
Ideas -> Todo -> In Progress -> In Review -> Done.

1. The user moves an idea card to **Todo**. When work starts on it - one card at a time - convert it
   to an issue and give it the `## Checks` checklist (`.github/ISSUE_TEMPLATE/change.md`).
2. Branch `feature/<user>/<n>-<kebab>` from `develop`, plan `.claude/plans/<n>-<kebab>.md`, card ->
   **In Progress**.
3. Build, then `/verify`: Automated items ticked, results commented. Front-facing: the app is opened
   on the emulator and the user works through the Manual steps.
4. PR to `develop` with `Closes #<n>`, body filled from `.github/pull_request_template.md`: What
   changed / Why / How it was tested / Notes - the lasting record for the ticket - plus the "Before
   merge" checklist. Claude ticks the "Verified by Claude" items once true; "Your review" items are
   the user's. When every issue item is ticked, card -> **In Review**.
5. **Review gate:** tell the user the PR is ready (link + what to look at) and stop. The user reviews
   it on GitHub; merge only on their explicit go-ahead. The card moves to **Done** on merge.

Board ids for `gh project item-edit`: project `PVT_kwHOBJtqVs4Bn5YT`, Status field
`PVTSSF_lAHOBJtqVs4Bn5YTzhmb5LI`; options Ideas `453c5649`, Todo `9431829e`, In Progress `a5220a49`,
In Review `fdb1b977`, Done `e4964fb5`. Item id: `gh project item-list 1 --owner ClaudiaMarques1`.

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
| App crashes on launch with `ClassNotFoundException: ...MainActivity`, or a local build passes while CI fails to compile | Stale Kotlin incremental-compile output after source files move (seen after the `java/` -> `kotlin/` move, #41): classes were dropped from `built_in_kotlinc/debug`, and the stale output also hid real compile errors (PR #42 was red in CI while every local build passed). Fix: `./gradlew :app:compileDebugKotlin --rerun assembleDebug` (works while Android Studio holds `app/build`; `clean` doesn't). After pushing, check `gh pr checks <pr>` - CI builds from scratch and is the source of truth. `scripts/emulator.sh` catches the launch crash via `app-status.sh`. |
| `scripts/app-status.sh` says "Foreground: unknown" on a physical phone | Android 10 (e.g. Huawei EMUI) has no `topResumedActivity`; the script also reads `mResumedActivity` now. With the phone and an emulator both attached, pick one: `ANDROID_SERIAL=<serial> scripts/emulator.sh` (`adb devices`). |
| `File google-services.json is missing` | It's gitignored and local-only. Locally: download from Firebase Console. CI writes a placeholder (`.claude/ci-cd/ci-cd-rules.md`). |
