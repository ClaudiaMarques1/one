# Plan: #37 - make every feature unit-testable and checked in CI

Branch: `feature/cmarques/37-testing-environment` (local only - not pushed, no PR yet).
Issue: https://github.com/ClaudiaMarques1/one/issues/37 (Phase 1 checkboxes not yet ticked there).

## Decisions

- **Test double = in-memory fake per repository** (option 1), not the Firebase Emulator Suite or a
  real test account. The fake behaves as an always-signed-in user; no Firebase login in unit tests.
- Fakes don't model the signed-out state (real repos emit an empty list). Add it only when a
  ViewModel starts treating signed-out differently.
- Pattern per repository: `XRepository` becomes an interface; the Firestore code moves unchanged
  into `FirestoreXRepository`; ViewModels keep a default arg of the Firestore impl (no DI framework).
- Fakes live next to their tests (`app/src/test/.../<feature>/FakeXRepository.kt`) and expose a
  `failNext: Throwable?` switch for error paths. Shared rule: `test/.../testing/MainDispatcherRule.kt`.

- **Diary package layout** (user's call, 2026-10-11): `diary/journal/` and `diary/goals/`, each
  split into `backend/` (models, repository interface, `Firestore*` impl) and `frontend/`
  (screens, composables, ViewModel, display helpers). Tests mirror it. Keep `rules.keep` and the
  ktlint baseline paths in sync when moving. Other features not yet asked about.

## Phase 1 - repository interfaces + test infra

- [x] Diary restructure into journal|goals / backend|frontend - committed together with the Goal split

- [x] Diary: `DiaryRepository` interface + `FirestoreDiaryRepository`, `FakeDiaryRepository`,
      `MainDispatcherRule`, `DiaryViewModelTest` (add / newest-first / update / delete+undo / error) - `dfbb453`
- [x] Goal: `GoalRepository` interface + `FirestoreGoalRepository` (moved unchanged),
      `FakeGoalRepository`, `GoalViewModelTest` + newest-first / add / blank-draft no-op /
      edit in place / delete / error path
- [ ] Workout templates: split `workout/WorkoutTemplateRepository.kt` (`observeTemplates`,
      `addTemplate`, `updateTemplate`, `updateTemplateExercises`, `deleteTemplate`); `FakeWorkoutTemplateRepository`
- [ ] Strength: split `workout/StrengthRepository.kt` (`observeSessions`, `addSession`,
      `addSessionAndUpdateTemplate`). The last one is a Firestore transaction touching
      `workoutTemplates` too - the fake just records both writes; atomicity stays untested.
      Then `StrengthViewModel.saveSession` test.
- [ ] Calendar: switch `CalendarViewModel` default `StrengthRepository()` to the Firestore impl;
      `CalendarViewModel.dayByDate` test using the Diary + Strength fakes

## Phase 2 - CI

- [ ] `lint` job (Android Lint + ktlint)
- [ ] AGP built-in JaCoCo coverage report as artifact + coverage % in the run summary (no threshold)
- [ ] Placeholder `google-services.json` step as a shared composite action

## Phase 3 - definition of done + docs

- [ ] Process rule: never merge unless `gh pr checks` is green; feature PRs include ViewModel tests with fakes
- [ ] `test-execution.md`: "no seam for a fake" is now false - point at `FakeDiaryRepository`
- [ ] `architecture.md`: interface + `Firestore*` pattern; stale uid-lookup bullet (now
      `FirebaseModule.userCollection`); stale "no `runTransaction` exists" bullet
      (`StrengthRepository.addSessionAndUpdateTemplate` uses one)
- [ ] README Status section: line ~454 "no seam for a fake"
- [ ] `android-conventions` skill, `pr-review` skill, `ci-cd-rules.md`

Out of scope: Compose UI tests, coverage gate, `AuthViewModel` seam.

## Next step

Workout templates split (first unchecked Phase 1 item). Ask whether `workout/` should get the
same backend/frontend layout as `diary/` before moving anything.
