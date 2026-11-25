package app.theone.android.settings.backend

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.theone.android.meals.backend.MealSettings
import app.theone.android.meals.backend.MealSlot
import app.theone.android.meals.backend.MealTargets
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

    override fun observeDndDuringWorkout(): Flow<Boolean> =
        dataStore.data.map { it[KEY_DND_DURING_WORKOUT] ?: false }

    override suspend fun setDndDuringWorkout(enabled: Boolean) {
        dataStore.edit { it[KEY_DND_DURING_WORKOUT] = enabled }
    }

    override fun observeThemeMode(): Flow<ThemeMode> =
        dataStore.data.map { themeModeFromStored(it[KEY_THEME]) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    // Same "weekStart" key Meals used before it became an app-wide setting (#62), so the choice carries over.
    override fun observeWeekStart(): Flow<WeekStart> =
        dataStore.data.map { prefs -> WeekStart.entries.firstOrNull { it.name == prefs[KEY_WEEK_START] } ?: WeekStart.fromLocale() }

    override suspend fun setWeekStart(weekStart: WeekStart) {
        dataStore.edit { it[KEY_WEEK_START] = weekStart.name }
    }

    override fun observeTrainingWeekExpanded(): Flow<Boolean> =
        dataStore.data.map { it[KEY_TRAINING_WEEK_EXPANDED] ?: false }

    override suspend fun setTrainingWeekExpanded(expanded: Boolean) {
        dataStore.edit { it[KEY_TRAINING_WEEK_EXPANDED] = expanded }
    }

    override fun observeMealSettings(): Flow<MealSettings> = dataStore.data.map { prefs ->
        val defaults = MealSettings()
        MealSettings(
            enabledSlots = prefs[KEY_MEAL_SLOTS]?.mapNotNull(MealSlot::fromStored)?.toSet() ?: defaults.enabledSlots,
            trainingTargets = prefs.readTargets(TRAINING, defaults.trainingTargets),
            restTargets = prefs.readTargets(REST, defaults.restTargets),
            waterGlasses = prefs[KEY_WATER_GLASSES] ?: defaults.waterGlasses,
            slotTimes = MealSlot.entries.associateWith { slot -> prefs[slotTimeKey(slot)] ?: defaults.timeOf(slot) }
        )
    }

    override suspend fun setMealSettings(settings: MealSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_MEAL_SLOTS] = settings.enabledSlots.map { it.name }.toSet()
            prefs.writeTargets(TRAINING, settings.trainingTargets)
            prefs.writeTargets(REST, settings.restTargets)
            prefs[KEY_WATER_GLASSES] = settings.waterGlasses
            MealSlot.entries.forEach { slot -> prefs[slotTimeKey(slot)] = settings.timeOf(slot) }
        }
    }

    private fun slotTimeKey(slot: MealSlot) = intPreferencesKey("slotTime_${slot.name}")

    private fun Preferences.readTargets(prefix: String, defaults: MealTargets) = MealTargets(
        kcal = this[intPreferencesKey("${prefix}Kcal")] ?: defaults.kcal,
        proteinG = this[intPreferencesKey("${prefix}ProteinG")] ?: defaults.proteinG,
        carbsG = this[intPreferencesKey("${prefix}CarbsG")] ?: defaults.carbsG,
        fatG = this[intPreferencesKey("${prefix}FatG")] ?: defaults.fatG
    )

    private fun MutablePreferences.writeTargets(prefix: String, targets: MealTargets) {
        this[intPreferencesKey("${prefix}Kcal")] = targets.kcal
        this[intPreferencesKey("${prefix}ProteinG")] = targets.proteinG
        this[intPreferencesKey("${prefix}CarbsG")] = targets.carbsG
        this[intPreferencesKey("${prefix}FatG")] = targets.fatG
    }

    private companion object {
        val KEY_VIBRATE_ON_REST_END = booleanPreferencesKey("vibrateOnRestEnd")
        val KEY_DND_DURING_WORKOUT = booleanPreferencesKey("dndDuringWorkout")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_MEAL_SLOTS = stringSetPreferencesKey("mealSlots")
        val KEY_WATER_GLASSES = intPreferencesKey("waterGlasses")
        val KEY_WEEK_START = stringPreferencesKey("weekStart")
        val KEY_TRAINING_WEEK_EXPANDED = booleanPreferencesKey("trainingWeekExpanded")
        const val TRAINING = "trainingTarget"
        const val REST = "restTarget"
    }
}

/** Today's behaviour before the setting existed: the rest bar vibrates. */
const val DEFAULT_VIBRATE_ON_REST_END = true
