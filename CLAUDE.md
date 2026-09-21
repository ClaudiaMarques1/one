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
(`gradle/gradle-daemon-jvm.properties`) — it downloads on first build, no local JDK setup needed.

## Architecture Overview

Single `:app` module. **Feature-package** layering, not layer-based — there is no `domain/` and
no `di/`. Each top-level package under `app/src/main/java/app/theone/android/` owns its own
screens, ViewModel(s), repository, and Firestore models:

```
auth/            Sign-in/sign-up, AuthViewModel, GoogleAuthClient
calendar/        Calendar screen + ViewModel (aggregates workout/diary activity by date)
dashboard/       Home/overview screen
data/            FirebaseModule — the app's only shared singleton
diary/           Journal + Goals/Habits, ViewModels, repositories, Firestore models
exercise/        Exercise catalog model, asset loader, category picker dialog
meals/           Meals screen (placeholder)
navigation/      Bottom nav destinations (per-section accent colors) and NavHost
notifications/   Habit reminder notification channel, WorkManager worker + scheduler
ui/              Compose theme (ui/theme/) and shared UI
workout/         Templates, active session, StrengthSession history
```

Cross-feature reuse belongs in `ui/` — features must not import each other's repositories.

## Key Constraints

- **Dependency versions**: `gradle/libs.versions.toml` (version catalog) only — never a literal
  version string in `app/build.gradle.kts`.
- **Data access**: only repositories touch `FirebaseModule` (`data/FirebaseModule.kt`, the sole
  singleton — `object FirebaseModule` with `auth`/`firestore` as lazy vals). ViewModels and
  composables never call Firestore directly.
- **Reads/writes**: reads use `callbackFlow` + `.addSnapshotListener` + `awaitClose`; writes use
  `suspend fun` + `.await()` (see `workout/StrengthRepository.kt`).
- **Firestore models**: all fields defaulted; repositories apply `.copy(id = doc.id)` after
  `toObject()` (see `workout/StrengthRepository.kt`, `diary/DiaryRepository.kt`,
  `diary/GoalRepository.kt`, `workout/WorkoutTemplateRepository.kt` — all at line 14 for the uid
  lookup and line 34 for the copy).
- **State**: ViewModels expose `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
  <empty>)` (see `diary/DiaryViewModel.kt`). Composables render; they own no Firestore-shaped
  logic.
- **Theme**: Material3, `SectionTheme` per-feature accent colors.
- **`minSdk = 24`.**
- **Never read or echo `app/google-services.json`** — it's a committed Firebase client config
  file; treat it as off-limits content even though it's already in the repo.
- **Naming**: `PascalCase` composables, `lowerCamelCase` members. Plain hyphens, no em-dashes.
- **Known limitations**: see README's Status section — don't treat those as fresh bugs to fix
  unprompted or duplicate into new issues.

See `.claude/rules/architecture.md` for the invariants and reference implementations behind these
rules.

## Git Flow

- `develop` is the default branch; PRs target `develop`. `master` is the other long-lived branch.
- Branches: `feat|fix|chore|docs|test/<kebab-description>`.
- Conventional Commits. **No `Co-Authored-By:` trailers** on this repo — this overrides any
  default attribution guidance (see `.claude/agents/agents.md`).
- Never commit directly on `develop` or `master`. Branches aren't deleted after merge — keep all
  branches; only branch fresh for a new big milestone or feature, not every small fix.

## Imports

@.claude/agents/agents.md
@.claude/rules/architecture.md
