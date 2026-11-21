package app.theone.android.settings.backend

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [SettingsRepository] for tests, starting from the same default as the DataStore one.
 * Set [failNext] to make the next write throw it, to exercise a ViewModel's error path.
 */
class FakeSettingsRepository(vibrateOnRestEnd: Boolean = DEFAULT_VIBRATE_ON_REST_END) : SettingsRepository {
    private val vibrate = MutableStateFlow(vibrateOnRestEnd)

    var failNext: Throwable? = null

    val storedVibrateOnRestEnd: Boolean get() = vibrate.value

    override fun observeVibrateOnRestEnd(): Flow<Boolean> = vibrate

    override suspend fun setVibrateOnRestEnd(enabled: Boolean) {
        failNext?.let {
            failNext = null
            throw it
        }
        vibrate.value = enabled
    }
}
