# One

**One** is an Android app for tracking gym and life in one place - workouts, calendar, meals, and diary.

Built with **Kotlin** and **Jetpack Compose** on the client and **Firebase** (Authentication + Cloud
Firestore) as the backend, in a single-module, feature-package architecture.

---

## CI/CD

GitHub Actions (`.github/workflows/unit-tests.yml`) runs three jobs on every PR and on pushes to
`develop`/`master`: `test` (unit tests), `lint` (Android Lint + ktlint) and `build` (`assembleDebug`).
The same checks run locally:

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on a connected device/emulator
./gradlew test                   # JVM unit tests
./gradlew lint                   # static analysis (Android Lint + ktlint)
./gradlew connectedAndroidTest   # instrumented tests (needs a device/emulator)
scripts/emulator.sh              # boot the Pixel_7 AVD if needed, install + open the app
```

Each change is tracked as an issue with a checklist of automated and manual checks; see
`.claude/rules/test-execution.md`.

---

## Project Structure

```bash
.
├── app/                                          # the only Gradle module
│   ├── src/main/kotlin/app/theone/android/
│   │   ├── auth/                                 # backend/ AuthViewModel, GoogleAuthClient; frontend/ sign-in screen
│   │   ├── calendar/                             # backend/ ViewModel + date keys; frontend/ screen + labels
│   │   ├── dashboard/                            # frontend/ Home/overview screen
│   │   ├── data/                                 # FirebaseModule - the app's only shared singleton
│   │   ├── diary/                                # frontend/ overview; journal/ + goals/, each backend/ (logic) + frontend/ (UI)
│   │   ├── exercise/                             # backend/ catalog model + loader; frontend/ picker dialog
│   │   ├── meals/                                # frontend/ Meals screen (placeholder)
│   │   ├── navigation/                           # Bottom nav destinations (per-section accents) + NavHost
│   │   ├── notifications/                        # Habit reminder channel, WorkManager worker + scheduler
│   │   ├── ui/                                   # Shared UI (ErrorBanner) and Compose theme (ui/theme/)
│   │   ├── workout/                              # frontend/ overview; templates/ + sessions/, each backend/ + frontend/
│   │   └── MainActivity.kt
│   ├── src/main/assets/exercises.json            # Bundled exercise catalog (~1,300 entries)
│   ├── src/main/res/                             # strings.xml, themes, launcher icons
│   ├── src/test/                                 # JVM unit tests (ViewModels)
│   ├── src/androidTest/                          # Instrumented tests
│   └── build.gradle.kts
│
├── gradle/
│   ├── libs.versions.toml                        # Version catalog - the only place versions live
│   └── gradle-daemon-jvm.properties              # Self-provisioning JDK 25 toolchain
│
├── .claude/                                      # Claude Code rules, skills and agent guidance
├── firebase.json                                 # Points the CLI at firestore.rules
├── firestore.rules                               # Per-user data scoping, enforced server-side
└── README.md
```

Layering is **feature-package**, not layer-based: there is no `domain/` and no `di/`. Each top-level
package owns its own screens, ViewModel(s), repository, and Firestore models. Cross-feature reuse
belongs in `ui/`; features must not import each other's repositories. See
`.claude/rules/architecture.md` for the invariants and reference implementations.

---

## Tech Stack

### Build

| Layer                | Technology                              | Version          |
| -------------------- | --------------------------------------- | ---------------- |
| Language             | Kotlin                                  | 2.1.0            |
| Build system         | Gradle (Android Gradle Plugin)          | AGP 9.4.0        |
| JDK toolchain        | Self-provisioning Foojay toolchain      | JDK 25           |
| Java source/target   | Java compatibility level                | 11               |
| SDK levels           | `minSdk` / `targetSdk` / `compileSdk`   | 24 / 37 / 37     |

### App

| Layer                | Technology                              | Version          |
| -------------------- | --------------------------------------- | ---------------- |
| UI toolkit           | Jetpack Compose (Material 3)            | BOM 2024.12.01   |
| Material components  | com.google.android.material             | 1.12.0           |
| Navigation           | Navigation Compose                      | 2.8.5            |
| Lifecycle / ViewModel| androidx.lifecycle (+ savedstate)       | 2.8.7            |
| Core KTX             | androidx.core:core-ktx                  | 1.13.1           |
| Auth                 | Firebase Authentication                 | BOM 33.7.0       |
| Google Sign-In       | Credential Manager + googleid           | 1.3.0 + 1.1.1    |
| Database             | Cloud Firestore                         | BOM 33.7.0       |
| Coroutines interop   | kotlinx-coroutines-play-services        | 1.9.0            |
| Image / GIF loading  | Coil 3                                  | 3.0.4            |
| Background work      | WorkManager                             | 2.10.0           |
| Google Services      | com.google.gms.google-services plugin   | 4.4.2            |

### Testing

| Layer                | Technology                              | Version          |
| -------------------- | --------------------------------------- | ---------------- |
| Unit tests           | JUnit 4                                 | 4.13.2           |
| Coroutine tests      | kotlinx-coroutines-test                 | 1.9.0            |
| Instrumented tests   | androidx.test.ext:junit                 | 1.1.5            |
| UI tests             | Compose UI test + Espresso              | BOM + 3.5.1      |

> All versions live in `gradle/libs.versions.toml`. Never put a literal version string in
> `app/build.gradle.kts`.

---

## Setup

### Prerequisites

1. **Android Studio** - latest stable, with the Android SDK for API 37 installed.
2. **JDK** - none needed locally. The Gradle daemon provisions its own JDK 25 toolchain
   (`gradle/gradle-daemon-jvm.properties`); it downloads on first build.
3. **A device or emulator** running Android 7.0 (API 24) or newer.
4. **Firebase CLI** (optional, only to deploy Firestore rules) - `npm install -g firebase-tools`.

### Clone & Open

```bash
git clone https://github.com/ClaudiaMarques1/one.git
cd one
```

Open the project in Android Studio, or build straight from the command line.

### Firebase Configuration

1. Create a Firebase project and add an Android app with package name `app.theone.android`.
2. In Firebase Console -> Authentication -> Sign-in method, enable **Email/Password** and **Google**.
3. Add your debug (and later release) keystore SHA-1 under Project settings -> your Android app ->
   SHA certificate fingerprints. Get it with:

   ```bash
   ./gradlew signingReport
   ```

4. Download `google-services.json` from Firebase and place it at `app/google-services.json`.
5. Deploy the Firestore security rules so per-user scoping is enforced server-side, not just by
   convention in the client:

   ```bash
   firebase login
   firebase use <your-firebase-project-id>
   firebase deploy --only firestore:rules
   ```

### Build & Run

```bash
./gradlew installDebug
```

### Run Tests

```bash
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests (device/emulator required)
./gradlew lint                   # static analysis
```

---

## Configuration

This app has no `.env` file. Environment-specific configuration lives in two places:

| File                                | Purpose                                                                 |
| ----------------------------------- | ----------------------------------------------------------------------- |
| `app/google-services.json`          | Firebase client config (project id, API key, OAuth client ids). Generated by Firebase Console - never hand-edit, never paste its contents anywhere. |
| `app/build.gradle.kts`              | `applicationId`, `versionName`, `versionCode`, SDK levels.              |
| `gradle/libs.versions.toml`         | Every dependency and plugin version.                                    |
| `gradle.properties`                 | Gradle JVM args and configuration cache.                                |

Runtime permissions declared in `app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

> **Note:** without `app/google-services.json`, the build fails at the `google-services` plugin step -
> authentication and Firestore cannot be configured at all.

---

## Key Features

### Authentication

- Firebase Authentication with **email/password** and **Google Sign-In** (via Credential Manager +
  `googleid`), handled by `auth/backend/GoogleAuthClient.kt`
- Sign-in renders on a fixed black-and-white screen, independent of the rest of the app's theme
- Errors surface through `AuthViewModel`'s `_authError: MutableStateFlow<String?>`, wrapped in
  `runCatching { }.onFailure { }`

### Home

- Dashboard overview with a live Diary summary (entry count + latest mood) and placeholder cards for
  the other sections, in neutral black/grey/white
- The profile picture (account photo, or initials) opens a **profile drawer** with **Account details**
  (name, email, sign-in method, and Sign out) and **Settings**
- **Settings** are stored locally on the device with DataStore, so they work offline and need no
  network: "Vibrate when rest ends" (on by default) controls the workout rest-timer buzz

### Workout - Strength Training

- **Home** - workouts this week, template cards (exercise and set counts, last done, Start) and the
  three most recent sessions with set count and volume
- Build reusable **templates** (name, optional description, exercise list) with drag-to-reorder
  (`sh.calvin.reorderable`), picking exercises from a body-part rail with selection counts, a search
  across all categories, thumbnails and a tray of picks
- **Start** opens a sheet with each exercise's last-time summary; the active workout shows a Previous
  column, kg/reps fields, a tick per set (empty fields fill from Previous), elapsed time and progress
- Ticking a set starts that exercise's rest timer (Off to 3:00, chosen per exercise and remembered on
  the template) in a floating bar with -30/+30/Skip and a vibration when it ends
- Exercise menu to duplicate or remove, long-press a set to remove it, add exercises mid-session;
  Finish with unticked sets asks whether to save only the done sets or all of them
- The active session is held in a `SavedStateHandle`, so it survives rotation and process death
- Finishing a session writes the session to history **and** carries the performed reps/weight back
  onto the template in a single Firestore **transaction**
  (`FirestoreStrengthRepository.addSessionAndUpdateTemplate`), so a partial failure applies neither write

### Calendar

- Full monthly grid with month navigation; days with a logged workout show a blue dot, days with a
  diary entry show a purple dot

### Diary

- **Journal** - "+ Add Entry" opens a composer: emoji mood, 1-10 energy and sleep sliders, a tappable
  body silhouette (plus chips for back/glutes/hamstrings) to mark sore areas, and a one-line
  reflection prompt
- **Goals** - goals made of habits, each tagged to one or more times of day
  (Morning/Afternoon/Evening/Night)

### Notifications

- A clock icon on a habit schedules a repeating local reminder inside that habit's time window
- `notifications/HabitReminderScheduler.kt` maps each `(habitId, timeOfDay)` pair to one unique
  `PeriodicWorkRequest` (24h period, `ExistingPeriodicWorkPolicy.REPLACE`), delivered by
  `HabitReminderWorker` on its own notification channel

### Meals

- Placeholder screen, coming soon

### Theming

- Material 3 with `SectionTheme` per-feature accent colors carried across the bottom nav and each
  section's screens: Calendar orange, Workout blue, Meals green, Diary purple
  (`navigation/Destination.kt`)
- `MonochromeTheme` drives the black-and-white sign-in screen

### Exercise Catalog

The exercise picker is backed by a trimmed, English-only copy of the
[hasaneyldrm/exercises-dataset](https://github.com/hasaneyldrm/exercises-dataset) (~1,300 exercises),
bundled as `app/src/main/assets/exercises.json` (name, body part, equipment, target muscle,
instructions). Thumbnails and animated GIFs are **not** bundled - Coil loads them on demand from that
dataset's jsdelivr CDN mirror and caches them to disk, so they work offline after first viewing
without copying ~1,300 copyrighted media files into this repo. The media is (c) Gym Visual, used with
attribution shown in the picker; commercial reuse needs separate permission from Gym Visual, so this
is a development-time stand-in until custom exercise content is prepared for a published release.

---

## Data Model

All app data is per-user and lives under `users/{uid}/`:

| Collection                          | Written by                          | Model                    |
| ----------------------------------- | ----------------------------------- | ------------------------ |
| `users/{uid}/diaryEntries`          | `diary/journal/backend/FirestoreDiaryRepository.kt` | `DiaryEntry`             |
| `users/{uid}/goals`                 | `diary/goals/backend/FirestoreGoalRepository.kt` | `Goal` (+ habits)        |
| `users/{uid}/strengthSessions`      | `workout/sessions/backend/FirestoreStrengthRepository.kt` | `StrengthSession`        |
| `users/{uid}/workoutTemplates`      | `workout/templates/backend/FirestoreWorkoutTemplateRepository.kt` | `WorkoutTemplate`     |

### Access Rules

Only repositories touch `data/FirebaseModule.kt` - the app's single singleton, exposing `auth` and
`firestore` as `by lazy` vals plus a `userCollection(name)` helper that resolves
`users/{uid}/<name>` and returns `null` when signed out. ViewModels and composables never call
Firestore directly.

- **Reads:** `callbackFlow` + `.addSnapshotListener` + `awaitClose`, bounded with `.limit()`
  (365 for the growing logs, 100 for curated sets)
- **Writes:** `suspend fun` + `.await()`
- **Models:** all fields defaulted; the repository applies `.copy(id = doc.id)` after `toObject()`
- **State:** ViewModels expose
  `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), <empty>)`

### Security Rules

There are no schema migrations (Firestore is schemaless), but `firestore.rules` is versioned in the
repo and must be deployed for scoping to be enforced:

```bash
firebase deploy --only firestore:rules
```

```js
match /users/{uid}/{collection}/{docId} {
  allow read, write: if request.auth != null && request.auth.uid == uid;
}
```

> Client-side uid filtering is a convention, not a guarantee. Deploy the rules before putting real
> data in a project.

---

## Troubleshooting

### Build fails on `google-services`

- Confirm `app/google-services.json` exists and its `package_name` is `app.theone.android`
- Re-download it from Firebase Console after changing the package name or adding a SHA-1

### Google Sign-In fails or shows no accounts

- Add the debug keystore SHA-1 to the Firebase Android app (`./gradlew signingReport`)
- Confirm Google is enabled under Authentication -> Sign-in method
- Credential Manager needs a Google account on the device/emulator (use a Play Services image)

### Firestore reads return nothing

- Confirm the user is signed in - `FirebaseModule.userCollection()` returns `null` when signed out
- Confirm the rules are deployed (`firebase deploy --only firestore:rules`); a rules rejection shows
  as a `PERMISSION_DENIED` snapshot listener error

### Habit reminders don't fire

- Grant the `POST_NOTIFICATIONS` runtime permission (Android 13+)
- WorkManager is inexact by design; timing drifts, and aggressive OEM battery managers (e.g. Huawei)
  may kill background work entirely

### Gradle / toolchain issues

```bash
./gradlew --stop           # stop the daemon
./gradlew clean build      # clean rebuild
```

The first build downloads the JDK 25 toolchain - allow it network access.

---

## Contributing

1. Create a feature branch from `develop`
2. Make your changes, following `.claude/rules/architecture.md`
3. Write/update tests
4. Ensure everything passes: `./gradlew test lint`
5. Submit a PR to `develop`

Commits follow **Conventional Commits**. This repo does **not** use `Co-Authored-By:` trailers.
Never commit directly on `develop` or `master`. Branches are not deleted after merge - branch fresh
for a new milestone or feature, not for every small fix.

### Branch Strategy

| Branch          | Purpose                            |
| --------------- | ---------------------------------- |
| `master`        | Production-ready releases          |
| `develop`       | Default branch; integration target |
| `feat/*`        | New features                       |
| `fix/*`         | Bug fixes                          |
| `chore/*`       | Tooling, config, maintenance       |
| `docs/*`        | Documentation                      |
| `test/*`        | Test-only changes                  |
| `release/X.Y.Z` | Release preparation                |

Branch names use a kebab-case description, e.g. `feat/rest-timer-presets`.

---

## Git Branching & Release Workflow

This project follows a **feature -> develop -> release -> master** workflow with semantic versioning.

```text
feat/* ─────┐
            ▼
        develop
            │
    (create release branch)
            │
            ▼
    release/X.Y.Z
            │
    (bump versionName + versionCode)
            │
            ▼
         master ── tag: vX.Y.Z
            │
    (merge back into develop)
            ▼
        develop
```

### Release Steps

1. **Create release branch from develop**

    ```bash
    git checkout develop
    git checkout -b release/0.2.0
    ```

2. **Bump the version in `app/build.gradle.kts`**

    Set `versionName = "0.2.0"` and increment `versionCode` by exactly 1, then:

    ```bash
    git add app/build.gradle.kts
    git commit -m "chore(release): bump version to 0.2.0"
    git push -u origin release/0.2.0
    ```

3. **Merge into master**

    ```bash
    git checkout master
    git merge --no-ff release/0.2.0 -m "release: 0.2.0"
    ```

4. **Tag the release**

    ```bash
    git tag -a v0.2.0 -m "Release version 0.2.0"
    git push origin master --tags
    ```

5. **Merge back into develop** *(only if changes were made on the release branch)*

    ```bash
    git checkout develop
    git merge release/0.2.0
    git push origin develop
    ```

Current version: **0.1.0** (`versionCode 1`).

---

## Status

Auth, the navigation shell, Home (with the profile drawer and Settings), Calendar, and Workout's Strength Training (templates ->
session -> history) are functional end-to-end with Firestore. Diary's Journal and Goals/Habits, with
reminder notifications, are also functional. Meals tracks the day - Today checklist,
Add meal (favourites or a new meal), water, and training / rest-day targets in Settings. The whole app has a
dark mode: Settings -> Appearance -> Theme (Light / Dark / System, default System).

Workout is planned to split into four independent session types - Warmup/Stretch, Strength Training,
Cardio, and Warmdown - each logged separately, since not every day covers all four. Strength Training
is built first; the others follow the same Firestore-backed pattern.

### Known limitations

- Diary entries created before the journal redesign show blank mood/energy/sleep fields - the schema
  changed and there is no migration.
- Habit reminders use WorkManager rather than exact alarms, so timing can drift, especially on OEMs
  that aggressively kill background work.
- ViewModels are tested against in-memory fake repositories; the Firestore implementations
  themselves (queries, the session+template transaction) aren't unit-tested.
- The sign-in screen stays black-on-white in dark mode - neither design handover covers it.
- CI has no device: instrumented tests (`connectedAndroidTest`) and on-screen checks stay manual,
  on the emulator.

---

## License

Copyright (c) 2026 Claudia Marques. All rights reserved. The code is public to view, but it may not
be copied, modified, redistributed or published without written permission - see [LICENSE](LICENSE).

## Third-party content

- **Exercise data** - `app/src/main/assets/exercises.json` comes from
  [hasaneyldrm/exercises-dataset](https://github.com/hasaneyldrm/exercises-dataset), MIT License
  (c) Hasan Emir Yildirim. The full notice is in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
- **Exercise GIFs** - not included in this repository (`exercises.json` only holds their paths). The
  media is (c) Gym visual - https://gymvisual.com/ - and is not covered by the MIT License. This app
  is personal and non-commercial; showing the GIFs in the app, and any commercial use, will only
  happen with permission from Gym visual and with the "(c) Gym visual" attribution.
