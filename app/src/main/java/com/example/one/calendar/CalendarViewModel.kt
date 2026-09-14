package com.example.one.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.one.diary.DiaryRepository
import com.example.one.workout.StrengthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class ActivityType { WORKOUT, MEAL, DIARY }

class CalendarViewModel(
    private val strengthRepository: StrengthRepository = StrengthRepository(),
    private val diaryRepository: DiaryRepository = DiaryRepository()
) : ViewModel() {

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}
