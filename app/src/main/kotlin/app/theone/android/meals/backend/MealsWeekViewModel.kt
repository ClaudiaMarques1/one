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
import java.time.LocalDate

/** The 1d Week plan: the week's 7 date keys and a row per day. */
data class MealsWeekPlan(val days: List<String> = emptyList(), val rows: List<WeekPlanDay> = emptyList())

// Like MealsViewModel, the settings repository needs a Context, so the screen builds this through a
// viewModelFactory (`mealsWeekViewModel()` in meals/frontend).
@OptIn(ExperimentalCoroutinesApi::class)
class MealsWeekViewModel(
    private val settingsRepository: SettingsRepository,
    private val repository: MealsRepository = FirestoreMealsRepository(),
    private val strengthRepository: StrengthRepository = FirestoreStrengthRepository(),
    private val today: LocalDate = LocalDate.now(),
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {
    private val _mealsError = MutableStateFlow<String?>(null)
    val mealsError: StateFlow<String?> = _mealsError

    /** Any date in the week on screen; the arrows move it by 7 days. */
    private val anchor = MutableStateFlow(today)

    val plan: StateFlow<MealsWeekPlan> = combine(
        anchor,
        settingsRepository.observeMealSettings(),
        settingsRepository.observeWeekStart()
    ) { date, settings, weekStart ->
        weekOf(date, weekStart).map { it.toString() } to settings
    }
        .distinctUntilChanged()
        .flatMapLatest { (days, settings) ->
            combine(
                repository.observeMealsBetween(days.first(), days.last()),
                repository.observeRestDayChoices(days.first(), days.last()),
                strengthRepository.observeSessions().map { sessions ->
                    // Newest session first, so the first per date wins - as on the weekly view.
                    sessions.filter { it.date in days }
                        .groupBy { it.date }
                        .mapValues { (_, list) -> list.first().templateName.ifBlank { "Workout" } }
                }
            ) { meals, choices, workouts ->
                MealsWeekPlan(
                    days = days,
                    rows = buildWeekPlan(days, today.toString(), meals, settings, workouts, choices)
                )
            }
        }
        .catch { e -> _mealsError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MealsWeekPlan())

    val todayKey: String = today.toString()

    fun showPreviousWeek() {
        anchor.value = anchor.value.minusWeeks(1)
    }

    fun showNextWeek() {
        anchor.value = anchor.value.plusWeeks(1)
    }

    fun clearMealsError() {
        _mealsError.value = null
    }

    /** Day menu -> Copy to tomorrow. */
    fun copyToNextDay(date: String) = copyDay(date, LocalDate.parse(date).plusDays(1).toString())

    /** Day menu -> Copy to... (a picked date). Copies are added; nothing on that date is replaced. */
    fun copyDay(date: String, toDate: String) {
        val meals = mealsOn(date)
        if (meals.isEmpty() || toDate == date) return
        launchCatching { repository.addMeals(copyMealsTo(meals, toDate, clock())) }
    }

    /** Day menu -> Clear day (after the screen's confirmation): the day's meals go; water and the rest day choice stay. */
    fun clearDay(date: String) {
        val ids = mealsOn(date).map { it.id }
        if (ids.isEmpty()) return
        launchCatching { repository.deleteMeals(ids) }
    }

    /** Copy week (after confirmation): each day's meals onto the same weekday next week, in one write. */
    fun copyWeek() {
        val now = clock()
        val copies = plan.value.rows.flatMap { row ->
            copyMealsTo(row.meals, LocalDate.parse(row.date).plusWeeks(1).toString(), now)
        }
        if (copies.isEmpty()) return
        launchCatching { repository.addMeals(copies) }
    }

    private fun mealsOn(date: String): List<PlannedMeal> = plan.value.rows.firstOrNull { it.date == date }?.meals.orEmpty()

    private fun launchCatching(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { _mealsError.value = it.message }
        }
    }
}
