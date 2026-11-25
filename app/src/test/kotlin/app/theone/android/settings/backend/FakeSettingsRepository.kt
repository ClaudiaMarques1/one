package app.theone.android.settings.backend

import app.theone.android.meals.backend.MealSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [SettingsRepository] for tests, starting from the same defaults as the DataStore one.
 * Set [failNext] to make the next write throw it, to exercise a ViewModel's error path.
 */
class FakeSettingsRepository(
    vibrateOnRestEnd: Boolean = DEFAULT_VIBRATE_ON_REST_END,
    themeMode: ThemeMode = DEFAULT_THEME_MODE,
    mealSettings: MealSettings = MealSettings(),
    weekStart: WeekStart = WeekStart.MONDAY,
    dndDuringWorkout: Boolean = false
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

    private val dnd = MutableStateFlow(dndDuringWorkout)

    val storedDndDuringWorkout: Boolean get() = dnd.value

    override fun observeDndDuringWorkout(): Flow<Boolean> = dnd

    override suspend fun setDndDuringWorkout(enabled: Boolean) {
        throwIfFailing()
        dnd.value = enabled
    }

    override fun observeThemeMode(): Flow<ThemeMode> = theme

    override suspend fun setThemeMode(mode: ThemeMode) {
        throwIfFailing()
        theme.value = mode
    }

    private val meals = MutableStateFlow(mealSettings)

    val storedMealSettings: MealSettings get() = meals.value

    private val week = MutableStateFlow(weekStart)

    val storedWeekStart: WeekStart get() = week.value

    override fun observeWeekStart(): Flow<WeekStart> = week

    override suspend fun setWeekStart(weekStart: WeekStart) {
        throwIfFailing()
        week.value = weekStart
    }

    override fun observeMealSettings(): Flow<MealSettings> = meals

    override suspend fun setMealSettings(settings: MealSettings) {
        throwIfFailing()
        meals.value = settings
    }

    private val trainingWeekExpanded = MutableStateFlow(false)

    val storedTrainingWeekExpanded: Boolean get() = trainingWeekExpanded.value

    override fun observeTrainingWeekExpanded(): Flow<Boolean> = trainingWeekExpanded

    override suspend fun setTrainingWeekExpanded(expanded: Boolean) {
        throwIfFailing()
        trainingWeekExpanded.value = expanded
    }

    private fun throwIfFailing() {
        failNext?.let {
            failNext = null
            throw it
        }
    }
}
