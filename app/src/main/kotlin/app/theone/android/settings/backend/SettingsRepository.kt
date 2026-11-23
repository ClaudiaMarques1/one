package app.theone.android.settings.backend

import app.theone.android.meals.backend.MealSettings
import kotlinx.coroutines.flow.Flow

/** The user's app settings, kept on the device - they work offline and before sign-in. */
interface SettingsRepository {
    /** Whether the workout rest bar vibrates when the rest ends. Defaults to true. */
    fun observeVibrateOnRestEnd(): Flow<Boolean>

    suspend fun setVibrateOnRestEnd(enabled: Boolean)

    /** Whether a workout turns on Do Not Disturb (Priority only) while it runs (#65). Defaults to false. */
    fun observeDndDuringWorkout(): Flow<Boolean>

    suspend fun setDndDuringWorkout(enabled: Boolean)

    /** Light, dark, or follow the phone. Defaults to [ThemeMode.SYSTEM]. */
    fun observeThemeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    /** Settings -> Week starts on: the first day of every week the app shows. Defaults to the locale's. */
    fun observeWeekStart(): Flow<WeekStart>

    suspend fun setWeekStart(weekStart: WeekStart)

    /** Settings -> Meals: slots, training / rest-day targets, water target. Defaults from the meals handoff. */
    fun observeMealSettings(): Flow<MealSettings>

    suspend fun setMealSettings(settings: MealSettings)
}
