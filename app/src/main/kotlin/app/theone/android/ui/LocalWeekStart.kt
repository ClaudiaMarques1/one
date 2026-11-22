package app.theone.android.ui

import androidx.compose.runtime.staticCompositionLocalOf
import app.theone.android.settings.backend.WeekStart

/**
 * The Week starts on setting for every screen that draws a week. MainActivity provides it from
 * Settings (like the theme); the default only applies to previews.
 */
val LocalWeekStart = staticCompositionLocalOf { WeekStart.fromLocale() }
