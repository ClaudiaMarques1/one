# CLAUDE.md

Guidance for Claude Code working in this repo — `one`, a Kotlin/Compose/Firebase gym-and-life
Android app (package `app.theone.android`).

## Terminal Commands

```
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on connected device/emulator
./gradlew test                   # JVM unit tests
./gradlew lint                   # static analysis
./gradlew connectedAndroidTest   # instrumented tests (needs a connected device/emulator)
```

The Gradle daemon uses a self-provisioning JDK-25 toolchain
(`gradle/gradle-daemon-jvm.properties`) — it downloads on first build. `./gradlew` itself still
needs a JDK to launch: if `JAVA_HOME` is unset, use Android Studio's `jbr` (see test-execution.md).

## Architecture Overview

Single `:app` module. **Feature-package** layering, not layer-based — no `domain/`, no `di/`. Each top-level
package under `app/src/main/kotlin/app/theone/android/` owns its screens, ViewModel(s), repository and models:

```
auth/            backend/ AuthViewModel (+ UserProfile), GoogleAuthClient; frontend/ SignIn + Account details
calendar/        backend/ ViewModel + date keys; frontend/ screen + date labels (workout/diary by date)
dashboard/       frontend/ Home/overview screen + profile drawer (Home and its sub-screens use NeutralTheme)
data/            FirebaseModule — the app's only Firebase singleton
diary/           frontend/ overview (tabs); journal/ + goals/, each backend/ (VMs, repos) + frontend/ (UI)
exercise/        backend/ catalog model + asset loader; frontend/ category picker dialog
meals/           backend/ meals, recipes, shopping repos + VMs; frontend/ Food Diary + Recipes tabs
navigation/      Bottom nav destinations (per-section accent colors) and NavHost - flat, app plumbing
notifications/   Reminder channel, worker + scheduler - flat: WorkManager stores the worker class name
settings/        backend/ local DataStore settings repo + ViewModel (no network); frontend/ Settings screen
ui/              Compose theme (ui/theme/) and shared UI
workout/         frontend/ overview + home list; templates/, sessions/, planner/ - each backend/ + frontend/
```

Cross-feature reuse belongs in `ui/` — features must not import each other's repositories.

## Key Constraints

- **Dependency versions**: `gradle/libs.versions.toml` (version catalog) only — never a literal
  version string in `app/build.gradle.kts`.
- **Data access**: only repositories touch `FirebaseModule` (`data/FirebaseModule.kt`, the sole
  singleton — `object FirebaseModule` with `auth`/`firestore` as lazy vals). ViewModels and
  composables never call Firestore directly.
- **Reads/writes**: reads use `callbackFlow` + `.addSnapshotListener` + `awaitClose`; writes use
  `suspend fun` + `.await()` (see `workout/sessions/backend/FirestoreStrengthRepository.kt`).
- **Firestore models**: all fields defaulted; repositories apply `.copy(id = doc.id)` after `toObject()`.
- **State**: ViewModels expose `stateIn(..., WhileSubscribed(5000), <empty>)`; composables only render.
- **Theme**: Material3, `SectionTheme` per-feature accent colors.
- **`minSdk = 24`.**
- **Never read or echo `app/google-services.json`** — the Firebase client config; gitignored and
  local-only (CI writes a placeholder). Treat it as off-limits content.
- **Naming**: `PascalCase` composables, `lowerCamelCase` members. Plain hyphens, no em-dashes.
- **Known limitations**: see README's Status section — don't treat those as fresh bugs to fix
  unprompted or duplicate into new issues.

See `.claude/rules/architecture.md` for the invariants and reference implementations behind these
rules.

## Git Flow

- `develop` is the default branch; PRs target `develop`. `master` is the other long-lived branch.
- Branches: issue work uses `feature/<user>/<issue>-<kebab>` (e.g. `feature/cmarques/25-ci-unit-tests`);
  other work `feat|fix|chore|docs|test/<kebab-description>`.
- Conventional Commits. **No `Co-Authored-By:` trailers** on this repo — this overrides any
  default attribution guidance (see `.claude/agents/agents.md`).
- Never commit directly on `develop` or `master`. Branches aren't deleted after merge — keep all
  branches; only branch fresh for a new big milestone or feature, not every small fix.

## CI

`.github/workflows/unit-tests.yml` runs `test`, `lint`, `assembleDebug` jobs on PRs and `develop`/`master` pushes.

## On-Demand Docs

Not `@`-imported (0 tokens until read) - open the one that matches the task:

- `.claude/rules/test-execution.md` — checks per change type, ticket workflow + PR review gate; known build failures.
- `.claude/rules/security.md` — project checklist to run alongside `/security-review`.
- `.claude/rules/self-healing.md` — where a correction goes, and the format for a new rule.
- `.claude/ci-cd/ci-cd-rules.md` — CI triggers, job, placeholder config, local equivalents.
- `.claude/skills/skills.md` — every skill, its trigger, and its branch restrictions.
- `.claude/notes/` — non-obvious gotchas (What / Why it matters / Reference).
- `.claude/plans/` — one plan per issue/branch (decisions, checklist, next step); read at session start.

`.claude/settings.json` holds shared, committed permissions; `.claude/settings.local.json` is
personal and gitignored.

## Imports

@.claude/agents/agents.md
@.claude/rules/architecture.md
