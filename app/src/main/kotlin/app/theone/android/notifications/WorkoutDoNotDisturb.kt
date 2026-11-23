package app.theone.android.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** The filter a workout switches to, or null to leave Do Not Disturb alone because it's already on (#65). */
fun dndFilterOnStart(current: Int): Int? =
    if (current == NotificationManager.INTERRUPTION_FILTER_ALL) NotificationManager.INTERRUPTION_FILTER_PRIORITY else null

/**
 * The filter to put back after a workout, or null: only when the workout turned DND on ([saved] is
 * what was set before) and it's still Priority - if the user changed it by hand meanwhile, theirs wins.
 */
fun dndFilterOnRestore(saved: Int?, current: Int): Int? =
    saved?.takeIf { current == NotificationManager.INTERRUPTION_FILTER_PRIORITY }

/** The system screen where the user grants this app Do Not Disturb access - there's no in-app prompt. */
fun dndAccessSettingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

// The filter from before the workout is kept on disk, so a workout cut short by the app being killed
// can still be undone on the next launch (MainActivity).
private val Context.workoutDndDataStore: DataStore<Preferences> by preferencesDataStore(name = "workout_dnd")

/**
 * Do Not Disturb during a workout (#65): Priority only while it runs, then back to what was set before.
 * Needs notification-policy access (ACCESS_NOTIFICATION_POLICY), granted by the user in system settings.
 */
class WorkoutDoNotDisturb(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager: NotificationManager? = appContext.getSystemService(NotificationManager::class.java)
    private val dataStore = appContext.workoutDndDataStore

    fun hasAccess(): Boolean = notificationManager?.isNotificationPolicyAccessGranted == true

    /** Turns DND on if it's off; does nothing without access, or when this workout already did. */
    suspend fun turnOn() {
        val manager = notificationManager ?: return
        if (!hasAccess() || dataStore.data.first()[KEY_FILTER_BEFORE] != null) return
        val current = manager.currentInterruptionFilter
        val target = dndFilterOnStart(current) ?: return
        dataStore.edit { it[KEY_FILTER_BEFORE] = current }
        runCatching { manager.setInterruptionFilter(target) }
            .onFailure { dataStore.edit { it.remove(KEY_FILTER_BEFORE) } }
    }

    /** Puts back the filter from before the workout, if [turnOn] changed it. Safe to call any time. */
    suspend fun restore() {
        val saved = dataStore.data.first()[KEY_FILTER_BEFORE] ?: return
        dataStore.edit { it.remove(KEY_FILTER_BEFORE) }
        val manager = notificationManager?.takeIf { hasAccess() } ?: return
        dndFilterOnRestore(saved, manager.currentInterruptionFilter)?.let { filter ->
            runCatching { manager.setInterruptionFilter(filter) }
        }
    }

    private companion object {
        val KEY_FILTER_BEFORE = intPreferencesKey("filterBeforeWorkout")
    }
}
