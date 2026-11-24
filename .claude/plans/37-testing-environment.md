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

- [x] `lint` job (Android Lint + ktlint), plus a `build` job (`assembleDebug`) - Phase 4
- [ ] AGP built-in JaCoCo coverage report as artifact + coverage % in the run summary (no threshold)
- [x] Placeholder `google-services.json` step as a shared composite action (`.github/actions/placeholder-google-services`)

## Phase 3 - definition of done + docs

- [ ] Process rule: never merge unless `gh pr checks` is green; feature PRs include ViewModel tests with fakes
- [x] `test-execution.md`: "no seam for a fake" replaced with the interface + fake pattern
- [ ] `architecture.md`: interface + `Firestore*` pattern; stale uid-lookup bullet (now
      `FirebaseModule.userCollection`); stale "no `runTransaction` exists" bullet
      (`StrengthRepository.addSessionAndUpdateTemplate` uses one)
- [x] README Status section: "no seam for a fake" line updated (its "No CI pipeline" line is also stale)
- [ ] `android-conventions` skill, `pr-review` skill, `ci-cd-rules.md`

Out of scope: Compose UI tests, coverage gate, `AuthViewModel` seam.

## Phase 4 - change verification workflow (user, 2026-10-20)

Goal: every change gets checked the same way. Backend changes are verified by automation; front-facing
changes also get an emulator review by the user. Each ticket carries its own checklist, and the ticket
moves across project board "one" as it progresses.

### Ticket lifecycle

1. User moves an idea card to **Todo** -> when we pick it up, convert the draft to an issue
   (`convertProjectV2DraftIssueItemToIssue`), label it, and add the checklist below to its body.
2. Branch `feature/cmarques/<n>-<kebab>` from `develop`, plan `.claude/plans/<n>-<kebab>.md`,
   card -> **In Progress**.
3. Work + automated checks; Claude ticks automated items and comments the results on the issue.
4. Front-facing: app installed on the `Pixel_7` emulator for the user; user ticks the manual items.
5. PR to `develop` with `Closes #<n>`; card -> **In Review** once every checklist item is ticked.
6. **User review gate (user, 2026-10-20):** when a PR is ready to go to `develop`, Claude tells the
   user (PR link + what to look at) and stops. The user reviews it on GitHub. Claude merges only after
   the user explicitly says to merge - never on its own, even with CI green and the checklist done.
7. Merge only with CI green, the checklist complete, and the user's go-ahead; the board workflow
   moves the card to **Done**.

### Change type -> checklist (source of truth: `rules/test-execution.md`)

Type is decided from the files touched (one ticket can be both):

| Type | Files | Automated (Claude ticks) | Manual (user ticks) |
|---|---|---|---|
| Backend | ViewModel, repository, model, pure logic | unit tests added/updated (ViewModel + `Fake*Repository`); `test`; `lint` | - |
| Front-facing | composables, `res/`, strings, navigation | `test` (label/format helpers); `lint`; `assembleDebug`; installed + opened on the emulator | numbered test steps written for the ticket (from the design/idea) |
| Data/auth | Firestore reads/writes, `firestore.rules`, auth | as backend | signed-in real read/write on the emulator |
| Platform | manifest, permissions, notifications, WorkManager | `assembleDebug`; installed + opened on the emulator | permission prompt / notification fires on the emulator |
| Build | Gradle, `libs.versions.toml`, CI | `assembleDebug test lint` | - |

Checklist shape in the issue body:

```
## Checks
### Automated
- [ ] Unit tests added/updated for new logic
- [ ] `./gradlew test`
- [ ] `./gradlew lint`
- [ ] `./gradlew assembleDebug`
- [ ] Installed and opened on the emulator
### Manual (emulator)
- [ ] 1. <step> - expect <result>
- [ ] 2. <step> - expect <result>
```

### Automation to build

- [x] **`.github/ISSUE_TEMPLATE/change.md`** - the checklist above, so issues made in the web UI get it too.
- [x] **`.github/pull_request_template.md`** (user, 2026-10-20) - What changed / Why / How it was
      tested / Notes as the lasting record per ticket, plus a "Before merge" checklist split into
      "Verified by Claude" and "Your review".
- [x] **`scripts/emulator.sh`** - boot `Pixel_7` if no device is attached (wait for
      `sys.boot_completed`), `./gradlew installDebug`, `am start` MainActivity, then confirm it came
      up via `app-status.sh`. The user tests by hand from the ticket's numbered Manual steps
      (decision 2026-10-20 - no screenshots or scripted UI checks; the launch check was added after
      a launch crash went unnoticed).
- [x] **`/verify` project skill** - reads the diff against `develop`, picks the change types, runs the
      matching Gradle tasks, ticks the Automated items in the issue (`gh issue edit --body-file`), and
      comments a pass/fail summary. Front-facing: then runs `scripts/emulator.sh` and points the user
      at the Manual steps. Never ticks Manual items.
- [x] **CI** (folds in Phase 2): add `lint` and `assembleDebug` jobs to `unit-tests.yml`, so the PR
      shows the same automated checks.
- [x] **Board moves** via `gh project item-edit` at branch start (In Progress) and PR ready (In Review).
- [x] Docs: `test-execution.md` (workflow + checklist), `skills/skills.md` (`/verify`), `ci-cd-rules.md`.

- [x] **`scripts/app-status.sh`** - installed build, running, foreground activity, crashes since the
      last launch; exits 1 on a crash or if the app isn't in front. `emulator.sh` clears the crash
      buffer before launching and fails if the app doesn't come up. Exists so device checks never
      need raw `adb` (no permission prompts).

Out of scope for now: instrumented Compose UI tests and screenshot diffing - the emulator smoke test
only proves the app starts; screen behaviour is the user's manual check.

## Permission rules added (for review)

Every allow-rule this work adds, so they can be reviewed in one place. Shared rules are in
`.claude/settings.json` (committed, apply to everyone); personal ones are in the gitignored
`.claude/settings.local.json` and are listed here only as a record.

| Rule | Where | Why | Added |
|---|---|---|---|
| `Bash(gh issue edit:*)` | shared | `/verify` ticks Automated checkboxes in the issue body | 2026-10-20 |
| `Bash(gh issue comment:*)` | shared | `/verify` posts the check results on the issue | 2026-10-20 |
| `Bash(gh pr ready:*)` | shared | `/verify` marks the PR ready once every item is ticked (merge stays with the user) | 2026-10-20 |
| `Bash(gh pr edit:*)` | shared | `/verify` ticks the PR's "Verified by Claude" items (never "Your review") | 2026-10-20 |
| `Bash(gh project item-list:*)` | shared | find a ticket's card on board "one" | 2026-10-20 |
| `Bash(gh project item-edit:*)` | shared | move the card (In Progress / In Review) | 2026-10-20 |
| `Bash(scripts/*:*)` | shared | repo scripts (`emulator.sh`, `app-status.sh`) run without prompts; they're committed and reviewed in PRs, so device access goes through them instead of raw `adb` | 2026-10-20 |
| `Bash(adb:*)` | personal | raw `adb` for one-off debugging; needs `adb` on PATH (wrapper at `~/bin/adb`) | 2026-10-20 |

Still prompts, on purpose: `gh pr merge`, `gh api`, `rm`/`mv`/`cp`, and ad-hoc commands not covered
above. `git stash`/`git merge` stay denied - the user runs those.

## Close-out (2026-11-24, user chose "close it now")

The work landed in PRs #38, #39 and #45 (merged by 2026-10-20), which said "(#37)" instead of "Closes #37",
so the issue stayed open and the card sat in In Progress for a month.

- Process rule ticked: in practice since #45 - the PR template's "CI green" item, `test-execution.md`
  (fakes + `MainDispatcherRule`, CI as source of truth) and the review gate.
- Docs: `architecture.md` had two stale bullets - the uid lookup "duplicated in four repositories, don't
  extract" (it's `FirebaseModule.userCollection` now) and "no transaction/batch exists" (there's one of
  each). Rewritten in place (always-loaded budget unchanged at 219), plus the interface + `Firestore*` +
  fake pattern. `android-conventions` repository/ViewModel examples and a `pr-review` check updated.
  `ci-cd-rules.md` already describes the three jobs - nothing stale.
- JaCoCo coverage moved to its own idea card ("CI: coverage report (JaCoCo) in the run summary").

## Next step

None - #37 closes with the close-out PR.
