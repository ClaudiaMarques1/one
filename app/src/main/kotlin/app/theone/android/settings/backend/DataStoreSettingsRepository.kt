package app.theone.android.settings.backend

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// One DataStore per file per process - a second instance on "settings" throws - so the delegate is
// top-level, like FirebaseModule is the one Firebase entry point.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** [SettingsRepository] on DataStore Preferences: local, no network. */
class DataStoreSettingsRepository(context: Context) : SettingsRepository {
    private val dataStore = context.applicationContext.settingsDataStore

    override fun observeVibrateOnRestEnd(): Flow<Boolean> =
        dataStore.data.map { it[KEY_VIBRATE_ON_REST_END] ?: DEFAULT_VIBRATE_ON_REST_END }

    override suspend fun setVibrateOnRestEnd(enabled: Boolean) {
        dataStore.edit { it[KEY_VIBRATE_ON_REST_END] = enabled }
    }

    private companion object {
        val KEY_VIBRATE_ON_REST_END = booleanPreferencesKey("vibrateOnRestEnd")
    }
}

/** Today's behaviour before the setting existed: the rest bar vibrates. */
const val DEFAULT_VIBRATE_ON_REST_END = true
