# CI/CD Rules

## Unit tests - `.github/workflows/unit-tests.yml`

| Trigger | Condition |
|---|---|
| `pull_request` | targeting `develop` or `master` |
| `push` | `develop`, `master`, `release/**` |
| `workflow_dispatch` | manual re-run from the Actions tab |

One job, `test`, on `ubuntu-latest`: checkout -> `gradle/actions/setup-gradle` (caches `~/.gradle`,
including the JDK 25 daemon toolchain) -> write placeholder `app/google-services.json` ->
`./gradlew test`. On failure the HTML report
(`app/build/reports/tests/testDebugUnitTest/`) is uploaded as the `unit-test-report` artifact.

- No `actions/setup-java` and no repository secrets: the runner's preinstalled JDK launches the
  wrapper, which provisions JDK 25 itself via `gradle/gradle-daemon-jvm.properties`. If that
  download proves flaky, add `setup-java` with JDK 25 - don't downgrade the toolchain.
- `app/google-services.json` is gitignored, so CI writes a placeholder (dummy project, package
  `app.theone.android`, one web OAuth client for `R.string.default_web_client_id`). Unit tests
  never reach Firebase. A build that needs real Firebase (e.g. `assembleRelease`) needs the real
  file as a repository secret - that's its own ticket.
- `gradlew` must stay executable in git (`100755`); fix with `git update-index --chmod=+x gradlew`.
- Required status check on `develop` and `master` is set in GitHub branch protection, not in code.

## Local equivalents

```
./gradlew test    # same command CI runs
./gradlew lint    # not in CI yet
```

## Not in CI (separate tickets)

`./gradlew lint`, `connectedAndroidTest` (needs an emulator), and `assembleRelease` (#14, R8).
