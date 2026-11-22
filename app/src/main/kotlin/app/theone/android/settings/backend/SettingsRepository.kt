package app.theone.android.settings.backend

import kotlinx.coroutines.flow.Flow

/** The user's app settings, kept on the device - they work offline and before sign-in. */
interface SettingsRepository {
    /** Whether the workout rest bar vibrates when the rest ends. Defaults to true. */
    fun observeVibrateOnRestEnd(): Flow<Boolean>

    suspend fun setVibrateOnRestEnd(enabled: Boolean)

    /** Light, dark, or follow the phone. Defaults to [ThemeMode.SYSTEM]. */
    fun observeThemeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
