# Plan: #37 - make every feature unit-testable and checked in CI

Branch: `feature/cmarques/37-testing-environment`, built on `chore/feature-package-structure`
(PR #38 -> develop: restructure + Diary/Goal groundwork). Remaining #37 work continues here.
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

- **Feature package layout** (user's call, 2026-10-11, applies to every feature): the overview
  lives in `<feature>/frontend/` (+ `backend/` if it gets logic); each part gets
  `<feature>/<part>/backend/` and `frontend/`. Displayed = frontend (composables, labels, date
  labels); functional or button-driven = backend (ViewModels, models, repositories, tap/edit
  logic, date keys, reminder sync). Mixed files get split. Tests mirror it. Keep `rules.keep` and
  the ktlint baseline paths in sync when moving.

## Phase 1 - repository interfaces + test infra

- [x] Diary restructure into journal|goals / backend|frontend - committed together with the Goal split
- [x] Diary layout corrected to the displayed/functional rule: overview to `diary/frontend/`,
      ViewModels to backend, `DiaryDateUtils` -> `DiaryDateKeys` + `DiaryDateLabels`, composer tap
      logic -> `DiaryEntryEdits`, entry labels -> `DiaryEntryLabels`, reminder sync -> `HabitReminderSync`
- [x] Restructure the rest (confirmed 2026-10-11): `workout/frontend/` overview (WorkoutScreen,
      WorkoutHomeContent) + `workout/templates/` and `workout/sessions/` {backend,frontend};
      `calendar/`, `auth/`, `exercise/` -> backend|frontend (CalendarDateUtils split into
      CalendarDateKeys + CalendarDateLabels); `dashboard/`, `meals/` -> frontend only.
      Unchanged: `data/`, `navigation/` (stays its own folder), `ui/`, `MainActivity`, and
      `notifications/` - WorkManager stores HabitReminderWorker's class name for scheduled
      reminders, so moving it breaks reminders already on devices.

- [x] Diary: `DiaryRepository` interface + `FirestoreDiaryRepository`, `FakeDiaryRepository`,
      `MainDispatcherRule`, `DiaryViewModelTest` (add / newest-first / update / delete+undo / error) - `dfbb453`
- [x] Goal: `GoalRepository` interface + `FirestoreGoalRepository` (moved unchanged),
      `FakeGoalRepository`, `GoalViewModelTest` + newest-first / add / blank-draft no-op /
      edit in place / delete / error path
- [x] Workout templates: `WorkoutTemplateRepository` interface + `FirestoreWorkoutTemplateRepository`,
      `FakeWorkoutTemplateRepository`, ViewModel tests for newest-first / add / blank-draft no-op /
      edit in place / delete / error path
- [x] Strength: `StrengthRepository` interface + `FirestoreStrengthRepository`, `FakeStrengthRepository`
      (records template carry-overs in `templateUpdates`; transaction atomicity not modelled),
      `saveSession` tests: from template / without template / empty no-op / error path
- [x] Calendar: `CalendarViewModelTest` on the Diary + Strength fakes; `dayByDate` tests for merging by
      date, several sessions on a date, newest diary entry wins, live updates. Phase 1 done.

## Phase 2 - CI

- [ ] `lint` job (Android Lint + ktlint)
- [ ] AGP built-in JaCoCo coverage report as artifact + coverage % in the run summary (no threshold)
- [ ] Placeholder `google-services.json` step as a shared composite action

## Phase 3 - definition of done + docs

- [ ] Process rule: never merge unless `gh pr checks` is green; feature PRs include ViewModel tests with fakes
- [x] `test-execution.md`: "no seam for a fake" replaced with the interface + fake pattern
- [ ] `architecture.md`: interface + `Firestore*` pattern; stale uid-lookup bullet (now
      `FirebaseModule.userCollection`); stale "no `runTransaction` exists" bullet
      (`StrengthRepository.addSessionAndUpdateTemplate` uses one)
- [x] README Status section: "no seam for a fake" line updated (its "No CI pipeline" line is also stale)
- [ ] `android-conventions` skill, `pr-review` skill, `ci-cd-rules.md`

Out of scope: Compose UI tests, coverage gate, `AuthViewModel` seam.

## Next step

Phase 2 (CI): add a lint job to `.github/workflows/unit-tests.yml` (Android Lint + ktlint), then the
JaCoCo coverage artifact + summary, then the shared placeholder `google-services.json` action.
