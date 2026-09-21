# One

A gym/life companion app for Android — workouts, calendar, meals, and diary in one place.

Built with Jetpack Compose and Firebase.

## Features

- **Sign in** with email/password or Google (Firebase Authentication), on a fixed black-and-white screen independent of the rest of the app's theme
- **Home** — dashboard overview with a live Diary summary and placeholder cards for the other sections
- **Workout — Strength Training**:
  - Build reusable **templates** (name, optional description, exercise list) via a categorized exercise picker (browse by body part, search, thumbnails, animated GIF demos)
  - Tap a template to start a session (confirms with a numbered exercise list first); log sets per exercise with freely add/edit/removable rows, a live session-duration timer, a per-exercise rest timer (pick 60s/90s/2:00 via a clock icon, auto-starts on "Done", adjustable ±30s), swipe an exercise card left to delete / right to copy, and add extra exercises mid-session
  - Finishing a session saves it to history (last 3 shown) **and** writes the performed reps/weight back onto the template, so next time it opens pre-filled
- **Calendar** — full monthly grid with month navigation; days with a logged workout show a blue dot, days with a diary entry show a purple dot
- **Meals** — placeholder, coming soon
- **Diary**:
  - **Journal** — "+ Add Entry" opens a composer: emoji mood, 1–10 energy and sleep sliders, a tappable body silhouette (plus chips for back/glutes/hamstrings) to mark sore areas, and a one-line reflection prompt
  - **Goals** — define goals made of habits, each tagged to one or more times of day (Morning/Afternoon/Evening/Night); a clock icon per habit schedules a repeating local notification within that habit's time window (via WorkManager)
- Each main section (Workout/Meals/Diary/Calendar) carries its own accent color across the bottom nav and its screens

Workout is planned to eventually split into four independent session types — Warmup/Stretch, Strength Training, Cardio, and Warmdown — each logged separately since not every day covers all four. Strength Training is built first; the others follow the same Firestore-backed pattern.

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Firebase Authentication (Google Sign-In via Credential Manager, and email/password)
- Cloud Firestore
- Navigation Compose
- Coil (image + GIF loading for the exercise catalog)
- WorkManager (habit reminder notifications)

### Exercise catalog

The Strength Training exercise picker is backed by a trimmed, English-only copy of the
[hasaneyldrm/exercises-dataset](https://github.com/hasaneyldrm/exercises-dataset) (~1,300 exercises),
bundled as `app/src/main/assets/exercises.json` (name, body part, equipment, target muscle, instructions).
Thumbnails and animated GIFs are **not** bundled — they're loaded on demand from that dataset's GitHub repo via
a jsdelivr CDN URL and cached to disk by Coil, so they work offline after first viewing without copying
~1,300 copyrighted media files into this repo. The media is © Gym Visual, used with attribution (shown in the
picker); it requires separate permission from Gym Visual before commercial reuse, so this is a
development-time stand-in until custom exercise content is prepared for a published release.

## Project structure

```
app/src/main/java/com/example/one/
├── auth/            Sign-in screen, AuthViewModel, GoogleAuthClient
├── calendar/        Calendar screen + ViewModel (aggregates workout/diary activity by date)
├── dashboard/       Home/overview screen
├── exercise/        Exercise catalog model, asset loader, category picker dialog
├── workout/         Templates, active session, StrengthSession history — screens, ViewModels, repositories, Firestore models
├── meals/           Meals screen
├── diary/           Journal + Goals/Habits tabs, ViewModels, repositories, Firestore models
├── notifications/   Habit reminder notification channel, WorkManager worker + scheduler
├── navigation/      Bottom nav destinations (with per-section accent colors) and NavHost
└── ui/theme/        Compose theme (colors, typography, per-section and monochrome theme overrides)
```

## Setup

1. Clone the repo and open it in Android Studio.
2. Create a Firebase project and add an Android app with package name `com.example.one`.
3. In Firebase Console → Authentication → Sign-in method, enable **Email/Password** and **Google**.
4. Add your debug (and later, release) keystore's SHA-1 fingerprint under Project settings → your Android app → SHA certificate fingerprints. Get it with:
   ```
   ./gradlew signingReport
   ```
5. Download `google-services.json` from Firebase and place it at `app/google-services.json`.
6. Deploy the Firestore security rules (`firestore.rules`) so per-user data scoping is actually
   enforced server-side, not just by convention in the client code:
   ```
   npm install -g firebase-tools   # one-time
   firebase login
   firebase use <your-firebase-project-id>
   firebase deploy --only firestore:rules
   ```
7. Build and run:
   ```
   ./gradlew installDebug
   ```

## Status

Auth, navigation shell, Home dashboard, Calendar, and Workout's Strength Training (templates → session → history) are functional end-to-end with Firestore. Diary's Journal and Goals/Habits (with reminder notifications) are also functional. Meals is still a placeholder, and Workout's Cardio/Warmup/Warmdown session types are not built yet.

Known gaps: pre-existing diary entries created before the journal redesign show blank mood/energy/sleep fields (schema changed, no migration). Habit reminders use WorkManager rather than exact alarms, so timing can drift — especially on OEMs (e.g. Huawei) that aggressively kill background work.
