package app.theone.android.diary.goals.frontend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.theone.android.diary.goals.backend.FirestoreGoalRepository
import app.theone.android.diary.goals.backend.Goal
import app.theone.android.diary.goals.backend.GoalRepository
import app.theone.android.diary.goals.backend.Habit
import app.theone.android.diary.goals.backend.HabitReminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class GoalViewModel(
    private val repository: GoalRepository = FirestoreGoalRepository()
) : ViewModel() {
    private val _goalError = MutableStateFlow<String?>(null)
    val goalError: StateFlow<String?> = _goalError

    val goals: StateFlow<List<Goal>> = repository.observeGoals()
        .catch { e -> _goalError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearGoalError() {
        _goalError.value = null
    }

    private val _draftName = MutableStateFlow("")
    val draftName: StateFlow<String> = _draftName

    private val _draftHabits = MutableStateFlow<List<Habit>>(emptyList())
    val draftHabits: StateFlow<List<Habit>> = _draftHabits

    private val _editingGoal = MutableStateFlow<Goal?>(null)
    val editingGoal: StateFlow<Goal?> = _editingGoal

    fun updateDraftName(name: String) {
        _draftName.value = name
    }

    fun addDraftHabit(name: String, timesOfDay: List<String>) {
        if (name.isBlank() || timesOfDay.isEmpty()) return
        _draftHabits.value = _draftHabits.value + Habit(id = UUID.randomUUID().toString(), name = name, timesOfDay = timesOfDay)
    }

    fun removeDraftHabit(index: Int) {
        _draftHabits.value = _draftHabits.value.toMutableList().apply { removeAt(index) }
    }

    fun updateHabitReminders(habitId: String, reminders: List<HabitReminder>) {
        _draftHabits.value = _draftHabits.value.map { habit ->
            if (habit.id == habitId) habit.copy(reminders = reminders) else habit
        }
    }

    fun startEditingGoal(goal: Goal) {
        _editingGoal.value = goal
        _draftName.value = goal.name
        _draftHabits.value = goal.habits
    }

    fun clearDraft() {
        _draftName.value = ""
        _draftHabits.value = emptyList()
        _editingGoal.value = null
    }

    fun saveDraftGoal() {
        val name = _draftName.value
        val habits = _draftHabits.value
        if (name.isBlank() || habits.isEmpty()) return
        val editing = _editingGoal.value
        viewModelScope.launch {
            runCatching {
                if (editing != null) {
                    repository.updateGoal(editing.copy(name = name, habits = habits))
                } else {
                    repository.addGoal(Goal(name = name, habits = habits))
                }
            }.onFailure { _goalError.value = it.message }
                .onSuccess { clearDraft() }
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteGoal(id) }
                .onFailure { _goalError.value = it.message }
        }
    }
}
