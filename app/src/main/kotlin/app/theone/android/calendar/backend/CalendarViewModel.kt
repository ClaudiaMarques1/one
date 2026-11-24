package app.theone.android.calendar.backend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.theone.android.diary.journal.backend.DiaryEntry
import app.theone.android.diary.journal.backend.DiaryRepository
import app.theone.android.diary.journal.backend.FirestoreDiaryRepository
import app.theone.android.meals.backend.FirestoreMealsRepository
import app.theone.android.meals.backend.MealSlot
import app.theone.android.meals.backend.MealsRepository
import app.theone.android.meals.backend.PlannedMeal
import app.theone.android.workout.sessions.backend.FirestoreStrengthRepository
import app.theone.android.workout.sessions.backend.StrengthRepository
import app.theone.android.workout.sessions.backend.StrengthSession
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

enum class ActivityType { WORKOUT, MEAL, DIARY }

enum class CalendarView { MONTH, WEEK, DAY }

data class DayActivity(
    val sessions: List<StrengthSession> = emptyList(),
    val diary: DiaryEntry? = null,
    /** The day's planned meals in slot order (#69). */
    val meals: List<PlannedMeal> = emptyList()
) {
    /** Active days (month totals, best streak) are workout or diary days - meals don't count, as in the handoff. */
    val isActive: Boolean get() = sessions.isNotEmpty() || diary != null
    val hasAnything: Boolean get() = isActive || meals.isNotEmpty()
    val mealsEaten: Int get() = meals.count { it.eaten }
    val mealsKcal: Int get() = meals.sumOf { it.kcal }
}

/** Meals in the Meals checklist order: by slot (an unknown slot counts as Snacks, as there), then when added. */
internal fun List<PlannedMeal>.inSlotOrder(): List<PlannedMeal> =
    sortedWith(compareBy({ (MealSlot.fromStored(it.slot) ?: MealSlot.SNACKS).ordinal }, { it.createdAt }))

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val strengthRepository: StrengthRepository = FirestoreStrengthRepository(),
    private val diaryRepository: DiaryRepository = FirestoreDiaryRepository(),
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    // Calendar is the screen that brings the sections together, so it reads their repositories (#69).
    private val mealsRepository: MealsRepository = FirestoreMealsRepository()
) : ViewModel() {
    private val _calendarError = MutableStateFlow<String?>(null)
    val calendarError: StateFlow<String?> = _calendarError

    val view: StateFlow<CalendarView> = savedStateHandle.getStateFlow(KEY_VIEW, CalendarView.MONTH)
    val selectedDate: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SELECTED_DATE, CalendarDateKeys.todayKey())

    // Meals are queried by date range (sessions and diary load whole); the range only changes with the month.
    private val meals = selectedDate
        .map { CalendarDateKeys.mealsRange(it) }
        .distinctUntilChanged()
        .flatMapLatest { (from, to) -> mealsRepository.observeMealsBetween(from, to) }

    val dayByDate: StateFlow<Map<String, DayActivity>> = combine(
        strengthRepository.observeSessions(),
        diaryRepository.observeEntries(),
        meals
    ) { sessions, diaryEntries, plannedMeals ->
        val result = mutableMapOf<String, DayActivity>()
        sessions.groupBy { it.date }.forEach { (date, sessionsForDate) ->
            result[date] = (result[date] ?: DayActivity()).copy(sessions = sessionsForDate)
        }
        diaryEntries.groupBy { it.date }.forEach { (date, entriesForDate) ->
            result[date] = (result[date] ?: DayActivity()).copy(diary = entriesForDate.maxByOrNull { it.createdAt })
        }
        plannedMeals.groupBy { it.date }.forEach { (date, mealsForDate) ->
            result[date] = (result[date] ?: DayActivity()).copy(meals = mealsForDate.inSlotOrder())
        }
        result
    }.catch { e -> _calendarError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun clearCalendarError() {
        _calendarError.value = null
    }

    fun setView(newView: CalendarView) {
        savedStateHandle[KEY_VIEW] = newView
    }

    fun selectDate(dateKey: String) {
        savedStateHandle[KEY_SELECTED_DATE] = dateKey
    }

    fun goToToday() {
        savedStateHandle[KEY_SELECTED_DATE] = CalendarDateKeys.todayKey()
    }

    fun stepPrevious() {
        savedStateHandle[KEY_SELECTED_DATE] = CalendarDateKeys.step(selectedDate.value, view.value, delta = -1)
    }

    fun stepNext() {
        savedStateHandle[KEY_SELECTED_DATE] = CalendarDateKeys.step(selectedDate.value, view.value, delta = 1)
    }

    private companion object {
        const val KEY_VIEW = "calendarView"
        const val KEY_SELECTED_DATE = "calendarSelectedDate"
    }
}
