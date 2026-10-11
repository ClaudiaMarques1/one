# Architecture Rules

Invariants this codebase already follows. New code extends these patterns rather than inventing
new ones — cite the reference, don't restate it.

## Layering

Each feature package (`auth/ calendar/ dashboard/ diary/ exercise/ meals/ navigation/
notifications/ workout/`) owns its own `Screen` composable(s), `ViewModel`, `Repository`, and
Firestore model classes. Features must not import each other's repositories. Cross-feature reuse
goes in `ui/`.

- **New screens**: three-step registration, always together — `Destination.kt` (add a `data
  object` + list entry) and a `composable(...) { SectionTheme(...) { XScreen() } }` entry in
  `OneNavHost.kt`. Full example: `.claude/skills/android-conventions/SKILL.md`.

## Data Access

Only repositories touch `data/FirebaseModule.kt` (the sole singleton — `object FirebaseModule`
with `auth`/`firestore` as `by lazy` vals). ViewModels and composables never call Firestore or
`FirebaseModule` directly — verified: no `*Screen.kt` file or anything under `ui/` imports a
Firebase class.

- **Reads**: `callbackFlow` + `.addSnapshotListener` + `awaitClose`.
  Reference: `workout/StrengthRepository.kt:18-39`.
- **Writes**: `suspend fun` + `.await()`. Reference: `workout/StrengthRepository.kt:41-43`.
- **Models**: all fields defaulted; the repository applies `.copy(id = doc.id)` after
  `toObject()` (model files themselves stay plain data classes). Reference: `StrengthRepository.kt:34`
  — same shape in `DiaryRepository.kt`, `GoalRepository.kt`, `WorkoutTemplateRepository.kt`.
- **uid lookup**: `FirebaseModule.auth.currentUser?.uid ?: return null`, currently duplicated at
  line 14 in the four repositories above. Don't add a fifth duplicate — ask before extracting a
  shared helper (tracked separately as a refactor).
- **No atomic multi-write pattern exists yet**: `runTransaction`/`WriteBatch` are unused
  everywhere (0 matches). A change needing atomic multi-write is new ground here, not something
  to reference from existing code.

## State Ownership

Composables render; ViewModels own state. No Firestore-shaped logic in `@Composable` functions.
ViewModels expose `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), <empty>)`.
Reference: `diary/DiaryViewModel.kt:13,17-18` (default-arg repository injection + `stateIn`).

## The Error Pattern

Reference: `auth/AuthViewModel.kt:33-34` (`_authError: MutableStateFlow<String?>` exposed as
`authError: StateFlow<String?>`) with mutations in `signInWithEmail`/`signUpWithEmail`
(`:46-49`, `:55-58`) wrapped in `runCatching { }.onFailure { }`. New ViewModels that need error
state follow this shape — field named `_<domain>Error`, `runCatching` around the failable call.

When the failable call already returns a `Result` (e.g. `GoogleAuthClient.signIn`), chain
`.onFailure { }` on it directly instead of re-wrapping - see `signInWithGoogle` (`:36-42`).

## Deliberate Non-Goals

No Hilt/Koin, no Room, no multi-module split, no `domain/` layer, no Retrofit. Propose changes
here in a PR description; don't add them unprompted.
