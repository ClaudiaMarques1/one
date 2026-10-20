# CI/CD Rules

## CI - `.github/workflows/unit-tests.yml`

| Trigger | Condition |
|---|---|
| `pull_request` | targeting `develop` or `master` |
| `push` | `develop`, `master`, `release/**` |
| `workflow_dispatch` | manual re-run from the Actions tab |

Three parallel jobs on `ubuntu-latest`, each: checkout -> `gradle/actions/setup-gradle` (caches
`~/.gradle`, including the JDK 25 daemon toolchain) -> placeholder `app/google-services.json` (shared
composite action `.github/actions/placeholder-google-services`) -> one Gradle task:

| Job | Runs | On failure |
|---|---|---|
| `test` | `./gradlew test` | uploads `app/build/reports/tests/testDebugUnitTest/` as `unit-test-report` |
| `lint` | `./gradlew lint` (Android Lint + ktlint against the baseline) | uploads `lint-results-debug.html` as `lint-report` |
| `build` | `./gradlew assembleDebug` | - |

They mirror the Automated items of a ticket's checklist (`.claude/rules/test-execution.md`). Job names
are the status-check names - renaming one breaks a branch-protection rule that requires it.

- No `actions/setup-java` and no repository secrets: the runner's preinstalled JDK launches the
  wrapper, which provisions JDK 25 itself via `gradle/gradle-daemon-jvm.properties`. If that
  download proves flaky, add `setup-java` with JDK 25 - don't downgrade the toolchain.
- `app/google-services.json` is gitignored, so CI writes a placeholder (dummy project, package
  `app.theone.android`, one web OAuth client for `R.string.default_web_client_id`). CI never
  reaches Firebase. A build that needs real Firebase (e.g. `assembleRelease`) needs the real
  file as a repository secret - that's its own ticket.
- `gradlew` and `scripts/*.sh` must stay executable in git (`100755`); fix with
  `git update-index --chmod=+x <file>`.
- Required status checks on `develop` and `master` are set in GitHub branch protection, not in code.

## Local equivalents

```
./gradlew test lint assembleDebug    # what the three CI jobs run
scripts/emulator.sh                  # boot Pixel_7 if needed, install + open the app for manual testing
```

## Not in CI (separate tickets)

`connectedAndroidTest` (needs an emulator), `assembleRelease` (#14, R8), and a coverage report (#37 Phase 2).
