# One

A gym/life companion app for Android — workouts, calendar, meals, and diary in one place.

Built with Jetpack Compose and Firebase.

## Features

- **Sign in** with email/password or Google (Firebase Authentication)
- **Calendar** — placeholder, coming soon
- **Workout** — placeholder, coming soon
- **Meals** — placeholder, coming soon
- **Diary** — add and delete dated entries, synced per-user via Cloud Firestore

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Firebase Authentication (Google Sign-In via Credential Manager, and email/password)
- Cloud Firestore
- Navigation Compose

## Project structure

```
app/src/main/java/com/example/one/
├── auth/        Sign-in screen, AuthViewModel, GoogleAuthClient
├── calendar/    Calendar screen
├── workout/     Workout screen
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

Early-stage foundation: auth and navigation shell are working; Calendar, Workout, and Meals are placeholder screens pending their real implementations. Diary is functional end-to-end with Firestore.
