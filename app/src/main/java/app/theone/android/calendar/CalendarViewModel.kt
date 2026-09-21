package app.theone.android.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.theone.android.diary.DiaryRepository
import app.theone.android.workout.StrengthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class ActivityType { WORKOUT, MEAL, DIARY }

class CalendarViewModel(
    private val strengthRepository: StrengthRepository = StrengthRepository(),
    private val diaryRepository: DiaryRepository = DiaryRepository()
) : ViewModel() {

    private val _calendarError = MutableStateFlow<String?>(null)
    val calendarError: StateFlow<String?> = _calendarError

    val activityByDate: StateFlow<Map<String, Set<ActivityType>>> = combine(
        strengthRepository.observeSessions(),
        diaryRepository.observeEntries()
    ) { sessions, diaryEntries ->
        val result = mutableMapOf<String, MutableSet<ActivityType>>()
        sessions.forEach { session ->
            result.getOrPut(session.date) { mutableSetOf() }.add(ActivityType.WORKOUT)
        }
        diaryEntries.forEach { entry ->
            result.getOrPut(entry.date) { mutableSetOf() }.add(ActivityType.DIARY)
        }
        result
    }.catch { e -> _calendarError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun clearCalendarError() {
        _calendarError.value = null
    }
}
