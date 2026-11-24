# Architecture Rules

Invariants this codebase already follows. New code extends these patterns rather than inventing
new ones — cite the reference, don't restate it.

## Layering

Each feature package (`auth/ calendar/ dashboard/ diary/ exercise/ meals/ navigation/
notifications/ settings/ workout/`) owns its own `Screen` composable(s), `ViewModel`, `Repository`, and
Firestore model classes. Features must not import each other's repositories. Cross-feature reuse
goes in `ui/`. Inside a feature: overview in `<feature>/frontend/` (+`backend/`), each part in
`<feature>/<part>/{backend,frontend}/` - displayed = frontend, functional/button-driven (VMs, models, repos) = backend.
Reference: `diary/` (`frontend/DiaryScreen.kt` tab bar; `journal/`, `goals/`); tests mirror it. `data/`,
`navigation/`, `notifications/` (WorkManager persists the worker class name), `ui/` stay flat.

- **New screens**: three-step registration, always together — `Destination.kt` (add a `data
  object` + list entry) and a `composable(...) { SectionTheme(...) { XScreen() } }` entry in
  `OneNavHost.kt`. Full example: `.claude/skills/android-conventions/SKILL.md`.

## Data Access

Only repositories touch `data/FirebaseModule.kt` (the sole Firebase singleton — `object FirebaseModule`
with `auth`/`firestore` as `by lazy` vals; local settings: `settings/backend/DataStoreSettingsRepository.kt`).
ViewModels and composables never call Firestore or `FirebaseModule` directly — no `*Screen.kt` or `ui/`
file imports a Firebase class (screens get the signed-in user as `auth/backend/UserProfile`).

- **Reads**: `callbackFlow` + `.addSnapshotListener` + `awaitClose`.
  Reference: `workout/sessions/backend/FirestoreStrengthRepository.kt:15-37`.
- **Writes**: `suspend fun` + `.await()`. Reference: `workout/sessions/backend/FirestoreStrengthRepository.kt:39-41`.
- **Models**: all fields defaulted; the repository applies `.copy(id = doc.id)` after
  `toObject()` (model files themselves stay plain data classes). Reference: `FirestoreStrengthRepository.kt:32`
  — same shape in `FirestoreDiaryRepository.kt`, `FirestoreGoalRepository.kt`, `FirestoreWorkoutTemplateRepository.kt`.
- **Repositories**: an interface + `Firestore*` implementation (`StrengthRepository.kt`); ViewModels take the
  interface as a default arg (`= FirestoreStrengthRepository()`), tests pass a `Fake*Repository` (#37).
- **uid lookup**: `FirebaseModule.userCollection(name)` gives `users/{uid}/<name>` (null when signed out) -
  use it, never re-derive the uid in a repository.
- **Atomic multi-write**: `runTransaction` in `FirestoreStrengthRepository.kt:62` (session + template),
  `batch()` in `FirestoreMealsRepository.kt:119`.

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
