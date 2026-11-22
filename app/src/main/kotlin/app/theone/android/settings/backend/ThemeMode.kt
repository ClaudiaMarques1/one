package app.theone.android.settings.backend

/** Settings -> Appearance -> Theme. Stored by [name], so don't rename the entries. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** [ThemeMode.SYSTEM] matches the app before the setting existed: it follows the phone. */
val DEFAULT_THEME_MODE = ThemeMode.SYSTEM

/** Whether the app draws dark, given the setting and whether the phone is in dark mode. */
fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> systemDark
}

/** Reads a stored name back, falling back to the default for a missing or unknown value. */
fun themeModeFromStored(stored: String?): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == stored } ?: DEFAULT_THEME_MODE
