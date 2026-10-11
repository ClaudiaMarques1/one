# AGP 9 has Kotlin built in

**What:** AGP 9 (`agp = "9.4.0"`) compiles `app/src/**/*.kt` with its own built-in Kotlin support and
registers the `kotlin` extension itself. Applying `org.jetbrains.kotlin.android` on top fails sync
with `Cannot add extension with name 'kotlin', as there is an extension already registered with that
name.` Only the Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`) is applied.

**Why it matters:** older guides and templates all add `kotlin-android`, so it looks like a missing
plugin. It isn't. A side effect: Gradle plugins that discover Kotlin source sets through the classic
Kotlin plugin don't see `app/src` - which is why ktlint needs the custom `ktlintCheckSource` task.

**Reference:** `app/build.gradle.kts:82-86` (ktlint workaround comment); the unused
`kotlin-android` catalog alias was removed in #31 so it can't be applied by accident.
