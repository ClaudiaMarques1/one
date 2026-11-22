package app.theone.android.settings.backend

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [SettingsRepository] for tests, starting from the same defaults as the DataStore one.
 * Set [failNext] to make the next write throw it, to exercise a ViewModel's error path.
 */
class FakeSettingsRepository(
    vibrateOnRestEnd: Boolean = DEFAULT_VIBRATE_ON_REST_END,
    themeMode: ThemeMode = DEFAULT_THEME_MODE
) : SettingsRepository {
    private val vibrate = MutableStateFlow(vibrateOnRestEnd)
    private val theme = MutableStateFlow(themeMode)

    var failNext: Throwable? = null

    val storedVibrateOnRestEnd: Boolean get() = vibrate.value
    val storedThemeMode: ThemeMode get() = theme.value

    override fun observeVibrateOnRestEnd(): Flow<Boolean> = vibrate

    override suspend fun setVibrateOnRestEnd(enabled: Boolean) {
        throwIfFailing()
        vibrate.value = enabled
    }

    override fun observeThemeMode(): Flow<ThemeMode> = theme

    override suspend fun setThemeMode(mode: ThemeMode) {
        throwIfFailing()
        theme.value = mode
    }

    private fun throwIfFailing() {
        failNext?.let {
            failNext = null
            throw it
        }
    }
}
