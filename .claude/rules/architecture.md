# Architecture Rules

Invariants this codebase already follows. New code extends these patterns rather than inventing
new ones — cite the reference, don't restate it.

## Layering

Each feature package (`auth/ calendar/ dashboard/ diary/ exercise/ meals/ navigation/
notifications/ workout/`) owns its own `Screen` composable(s), `ViewModel`, `Repository`, and
Firestore model classes. Features must not import each other's repositories. Cross-feature reuse
goes in `ui/`. Inside a feature: overview in `<feature>/frontend/` (+`backend/`), each part in
`<feature>/<part>/{backend,frontend}/` - displayed = frontend, functional/button-driven (VMs, models, repos) = backend.
Reference: `diary/` (`frontend/DiaryScreen.kt` tab bar; `journal/`, `goals/`); tests mirror it. `data/`,
`navigation/`, `notifications/` (WorkManager persists the worker class name), `ui/` stay flat.

- **New screens**: three-step registration, always together — `Destination.kt` (add a `data
  object` + list entry) and a `composable(...) { SectionTheme(...) { XScreen() } }` entry in
  `OneNavHost.kt`. Full example: `.claude/skills/android-conventions/SKILL.md`.

## Data Access

Only repositories touch `data/FirebaseModule.kt` (the sole singleton — `object FirebaseModule`
with `auth`/`firestore` as `by lazy` vals). ViewModels and composables never call Firestore or
`FirebaseModule` directly — verified: no `*Screen.kt` file or anything under `ui/` imports a
Firebase class.

- **Reads**: `callbackFlow` + `.addSnapshotListener` + `awaitClose`.
  Reference: `workout/sessions/backend/FirestoreStrengthRepository.kt:15-37`.
- **Writes**: `suspend fun` + `.await()`. Reference: `workout/sessions/backend/FirestoreStrengthRepository.kt:39-41`.
- **Models**: all fields defaulted; the repository applies `.copy(id = doc.id)` after
  `toObject()` (model files themselves stay plain data classes). Reference: `FirestoreStrengthRepository.kt:32`
  — same shape in `FirestoreDiaryRepository.kt`, `FirestoreGoalRepository.kt`, `FirestoreWorkoutTemplateRepository.kt`.
- **uid lookup**: `FirebaseModule.auth.currentUser?.uid ?: return null`, currently duplicated at
  line 14 in the four repositories above. Don't add a fifth duplicate — ask before extracting a
  shared helper (tracked separately as a refactor).
- **No atomic multi-write pattern exists yet**: `runTransaction`/`WriteBatch` are unused
  everywhere (0 matches). A change needing atomic multi-write is new ground here, not something
  to reference from existing code.

## State Ownership

Composables render; ViewModels own state. No Firestore-shaped logic in `@Composable` functions.
ViewModels expose `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), <empty>)`.
Reference: `diary/journal/backend/DiaryViewModel.kt:12,16-18` (default-arg repository injection + `stateIn`).
`Color(0x...)` literals live only in `ui/theme/`; screens use `MaterialTheme.colorScheme` roles
(in `SectionTheme`, `primary` is the section accent), else a named val from `ui/theme/Color.kt`.
Dates: `java.time` only (desugaring on for minSdk 24). Store ISO `yyyy-MM-dd` keys - they sort as
text (`FirestoreDiaryRepository` `orderBy("date")`); show numeric dates via `ui/formatDisplayDate` (dd-MM-yyyy).

## The Error Pattern

Reference: `auth/backend/AuthViewModel.kt:33-34` (`_authError: MutableStateFlow<String?>` exposed as
`authError: StateFlow<String?>`) with mutations in `signInWithEmail`/`signUpWithEmail`
(`:46-49`, `:55-58`) wrapped in `runCatching { }.onFailure { }`. New ViewModels that need error
state follow this shape — field named `_<domain>Error`, `runCatching` around the failable call.
If the call already returns a `Result`, chain `.onFailure { }` on it directly (`signInWithGoogle`).

## Deliberate Non-Goals

No Hilt/Koin, no Room, no multi-module split, no `domain/` layer, no Retrofit. Propose changes
here in a PR description; don't add them unprompted.
