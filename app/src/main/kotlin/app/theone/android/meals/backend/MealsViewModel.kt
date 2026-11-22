package app.theone.android.meals.backend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.theone.android.settings.backend.SettingsRepository
import app.theone.android.workout.sessions.backend.FirestoreStrengthRepository
import app.theone.android.workout.sessions.backend.StrengthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// The settings repository needs a Context, so there's no default for it - the screen builds this
// through a viewModelFactory (`mealsViewModel()` in meals/frontend), like settingsViewModel().
@OptIn(ExperimentalCoroutinesApi::class)
class MealsViewModel(
    private val settingsRepository: SettingsRepository,
    private val repository: MealsRepository = FirestoreMealsRepository(),
    // Training day = a workout logged for the date, read the same way Calendar does, until workout
    // scheduling exists.
    private val strengthRepository: StrengthRepository = FirestoreStrengthRepository(),
    private val today: LocalDate = LocalDate.now(),
    // Turns a session's createdAt into a time of day for the timeline's workout row.
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {
    private val _mealsError = MutableStateFlow<String?>(null)
    val mealsError: StateFlow<String?> = _mealsError

    // The day on screen, shared by both Today views: the checklist's day arrows and the timeline's
    // week strip and week arrows all move it.
    private val date = MutableStateFlow(today.toString())

    val mealSettings: StateFlow<MealSettings> = settingsRepository.observeMealSettings()
        .catch { e -> _mealsError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MealSettings())

    /** The selected day's week (7 dates from the week-start setting) and which of them hold meals. */
    val week: StateFlow<MealsWeek> = combine(date, mealSettings) { dateKey, settings ->
        weekOf(LocalDate.parse(dateKey), settings.weekStart).map { it.toString() }
    }
        .distinctUntilChanged()
        .flatMapLatest { days ->
            repository.observeMealDates(days.first(), days.last()).map { dates -> MealsWeek(days, dates) }
        }
        .catch { e -> _mealsError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MealsWeek())

    fun selectDate(dateKey: String) {
        date.value = dateKey
    }

    fun showPreviousWeek() = shiftDay(-7)

    fun showNextWeek() = shiftDay(7)

    /** Today's key, so the screen can label the day ("Today", "Tomorrow", ...). */
    val todayKey: String = today.toString()

    fun showPreviousDay() = shiftDay(-1)

    fun showNextDay() = shiftDay(1)

    fun showToday() {
        date.value = todayKey
    }

    private fun shiftDay(delta: Long) {
        date.value = LocalDate.parse(date.value).plusDays(delta).toString()
    }

    val day: StateFlow<MealsDay> = date.flatMapLatest { dateKey ->
        combine(
            repository.observeMeals(dateKey),
            repository.observeWaterGlasses(dateKey),
            settingsRepository.observeMealSettings(),
            strengthRepository.observeSessions()
        ) { meals, water, settings, sessions ->
            val workout = sessions.firstOrNull { it.date == dateKey }
            buildMealsDay(
                date = dateKey,
                meals = meals,
                settings = settings,
                workoutName = workout?.templateName?.ifBlank { "Workout" },
                waterGlasses = water,
                workoutMinutes = workout?.let {
                    val time = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalTime()
                    time.hour * 60 + time.minute
                }
            )
        }
    }
        .catch { e -> _mealsError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MealsDay(date = today.toString()))

    val favourites: StateFlow<List<FavouriteMeal>> = repository.observeFavourites()
        .catch { e -> _mealsError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearMealsError() {
        _mealsError.value = null
    }

    /** Adds the picked favourites to [slot] as planned (not eaten). */
    fun addFavourites(slot: MealSlot, picks: List<FavouriteMeal>) {
        if (picks.isEmpty()) return
        val dateKey = date.value
        launchCatching {
            repository.addMeals(
                picks.map {
                    PlannedMeal(
                        date = dateKey,
                        slot = slot.name,
                        name = it.name,
                        kcal = it.kcal,
                        proteinG = it.proteinG,
                        carbsG = it.carbsG,
                        fatG = it.fatG
                    )
                }
            )
        }
    }

    /** The Add sheet's New tab: adds the meal to [slot] and, when asked, saves it as a favourite too. */
    fun addNewMeal(
        slot: MealSlot,
        name: String,
        kcal: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int,
        saveToFavourites: Boolean
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val meal = PlannedMeal(
            date = date.value,
            slot = slot.name,
            name = trimmed,
            kcal = kcal,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG
        )
        val favourite = if (saveToFavourites) {
            FavouriteMeal(name = trimmed, kcal = kcal, proteinG = proteinG, carbsG = carbsG, fatG = fatG)
        } else {
            null
        }
        launchCatching { repository.addMeals(listOf(meal), favourite) }
    }

    fun setEaten(meal: PlannedMeal, eaten: Boolean) {
        launchCatching { repository.setEaten(meal.id, eaten) }
    }

    fun removeMeal(meal: PlannedMeal) {
        launchCatching { repository.deleteMeal(meal.id) }
    }

    /** Water card: tapping glass [n] fills up to it, or empties it when it's the last full glass. */
    fun tapWaterGlass(n: Int) {
        val current = day.value.waterGlasses
        val next = tapWaterGlass(current, n).coerceIn(0, MAX_WATER_GLASSES)
        val dateKey = date.value
        launchCatching { repository.setWaterGlasses(dateKey, next) }
    }

    /** Timeline water card: -1 / +1 glass, clamped to 0..[MAX_WATER_GLASSES]. */
    fun stepWaterGlasses(delta: Int) {
        val next = stepWater(day.value.waterGlasses, delta)
        if (next == day.value.waterGlasses) return
        val dateKey = date.value
        launchCatching { repository.setWaterGlasses(dateKey, next) }
    }

    private fun launchCatching(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { _mealsError.value = it.message }
        }
    }
}
