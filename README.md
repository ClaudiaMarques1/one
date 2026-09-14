# One

A gym/life companion app for Android — workouts, calendar, meals, and diary in one place.

Built with Jetpack Compose and Firebase.

## Features

- **Sign in** with email/password or Google (Firebase Authentication)
- **Home** — dashboard overview with a live Diary summary and placeholder cards for the other sections
- **Workout — Strength Training** — pick exercises from a categorized catalog (browse by body part: Back, Chest, Shoulders, Legs, etc., with search, thumbnails, and animated GIF demos), log sets (reps × weight) per exercise, save the session, and view session history — synced per-user via Cloud Firestore
- **Calendar** — placeholder, coming soon
- **Meals** — placeholder, coming soon
- **Diary** — add and delete dated entries, synced per-user via Cloud Firestore

Workout is planned to eventually split into four independent session types — Warmup/Stretch, Strength Training, Cardio, and Warmdown — each logged separately since not every day covers all four. Strength Training is built first; the others follow the same Firestore-backed pattern.

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Firebase Authentication (Google Sign-In via Credential Manager, and email/password)
- Cloud Firestore
- Navigation Compose
- Coil (image + GIF loading for the exercise catalog)

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
├── auth/        Sign-in screen, AuthViewModel, GoogleAuthClient
├── calendar/    Calendar screen
├── dashboard/   Home/overview screen
├── exercise/    Exercise catalog model, asset loader, category picker dialog
├── workout/     Strength Training screen, ViewModel, repository, Firestore model
├── meals/       Meals screen
├── diary/       Diary screen, ViewModel, repository, Firestore model
├── navigation/  Bottom nav destinations and NavHost
└── ui/theme/    Compose theme (colors, typography)
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
6. Build and run:
   ```
   ./gradlew installDebug
   ```

## Status

Early-stage foundation: auth, navigation shell, and Home dashboard are working. Workout's Strength Training flow is functional end-to-end with Firestore, as is Diary. Calendar and Meals are still placeholder screens, and Workout's Cardio/Warmup/Warmdown session types are not built yet.
