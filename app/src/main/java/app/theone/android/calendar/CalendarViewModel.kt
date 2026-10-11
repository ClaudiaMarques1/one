package app.theone.android.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.theone.android.diary.DiaryEntry
import app.theone.android.diary.DiaryRepository
import app.theone.android.workout.StrengthRepository
import app.theone.android.workout.StrengthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class ActivityType { WORKOUT, MEAL, DIARY }

enum class CalendarView { MONTH, WEEK, DAY }

data class DayActivity(
    val sessions: List<StrengthSession> = emptyList(),
    val diary: DiaryEntry? = null,
    val mealCount: Int = 0
) {
    val isActive: Boolean get() = sessions.isNotEmpty() || diary != null
}

class CalendarViewModel(
    private val strengthRepository: StrengthRepository = StrengthRepository(),
    private val diaryRepository: DiaryRepository = DiaryRepository(),
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {
    private val _calendarError = MutableStateFlow<String?>(null)
    val calendarError: StateFlow<String?> = _calendarError

    val view: StateFlow<CalendarView> = savedStateHandle.getStateFlow(KEY_VIEW, CalendarView.MONTH)
    val selectedDate: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SELECTED_DATE, CalendarDateUtils.todayKey())

    // Meals has no repository yet (meals/MealsScreen.kt is a placeholder) - this stays false, and
    // mealCount below stays 0, until that lands. Left as a real field so wiring a future meals
    // source only needs to flip this, not touch the screen.
    val showMeals: Boolean = false

    val dayByDate: StateFlow<Map<String, DayActivity>> = combine(
        strengthRepository.observeSessions(),
        diaryRepository.observeEntries()
    ) { sessions, diaryEntries ->
        val result = mutableMapOf<String, DayActivity>()
        sessions.groupBy { it.date }.forEach { (date, sessionsForDate) ->
            result[date] = (result[date] ?: DayActivity()).copy(sessions = sessionsForDate)
        }
        diaryEntries.groupBy { it.date }.forEach { (date, entriesForDate) ->
            result[date] = (result[date] ?: DayActivity()).copy(diary = entriesForDate.maxByOrNull { it.createdAt })
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
        savedStateHandle[KEY_SELECTED_DATE] = CalendarDateUtils.todayKey()
    }

    fun stepPrevious() {
        savedStateHandle[KEY_SELECTED_DATE] = CalendarDateUtils.step(selectedDate.value, view.value, delta = -1)
    }

    fun stepNext() {
        savedStateHandle[KEY_SELECTED_DATE] = CalendarDateUtils.step(selectedDate.value, view.value, delta = 1)
    }

    private companion object {
        const val KEY_VIEW = "calendarView"
        const val KEY_SELECTED_DATE = "calendarSelectedDate"
    }
}
