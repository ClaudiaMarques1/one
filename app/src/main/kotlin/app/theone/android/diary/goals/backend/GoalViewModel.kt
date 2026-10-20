package app.theone.android.diary.goals.backend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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

    /** Save is allowed once the goal has a name and at least one named habit. */
    val canSave: StateFlow<Boolean> = combine(_draftName, _draftHabits) { name, habits ->
        name.isNotBlank() && habits.any { it.name.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Appends a habit (blank by default, for "Add another habit") and returns its id. */
    fun addDraftHabit(name: String = "", reminders: List<HabitReminder> = emptyList()): String {
        val id = UUID.randomUUID().toString()
        _draftHabits.value = _draftHabits.value + Habit(id = id, name = name, reminders = reminders.sortedByTime())
        return id
    }

    fun renameDraftHabit(habitId: String, name: String) {
        _draftHabits.value = _draftHabits.value.map { habit ->
            if (habit.id == habitId) habit.copy(name = name) else habit
        }
    }

    fun removeDraftHabit(habitId: String) {
        _draftHabits.value = _draftHabits.value.filterNot { it.id == habitId }
    }

    fun updateHabitReminders(habitId: String, reminders: List<HabitReminder>) {
        _draftHabits.value = _draftHabits.value.map { habit ->
            if (habit.id == habitId) habit.copy(reminders = reminders.sortedByTime()) else habit
        }
    }

    /** "Add time": 2 hours after the latest time (or 9:00), reminder on. */
    fun addReminderTime(habitId: String) {
        updateReminders(habitId) { it + nextReminderTime(it) }
    }

    fun changeReminderTime(habitId: String, reminderId: String, hour: Int, minute: Int) {
        updateReminders(habitId) { reminders ->
            reminders.map { if (it.id == reminderId) it.copy(hour = hour, minute = minute) else it }
        }
    }

    fun toggleReminder(habitId: String, reminderId: String) {
        updateReminders(habitId) { reminders ->
            reminders.map { if (it.id == reminderId) it.copy(enabled = !it.enabled) else it }
        }
    }

    fun removeReminderTime(habitId: String, reminderId: String) {
        updateReminders(habitId) { reminders -> reminders.filterNot { it.id == reminderId } }
    }

    private fun updateReminders(habitId: String, change: (List<HabitReminder>) -> List<HabitReminder>) {
        val habit = _draftHabits.value.find { it.id == habitId } ?: return
        updateHabitReminders(habitId, change(habit.reminders))
    }

    fun startEditingGoal(goal: Goal) {
        _editingGoal.value = goal
        _draftName.value = goal.name
        _draftHabits.value = goal.habits.map { it.withMigratedReminders() }
    }

    fun clearDraft() {
        _draftName.value = ""
        _draftHabits.value = emptyList()
        _editingGoal.value = null
    }

    /**
     * The draft habits as they will be saved: unnamed habits dropped, names trimmed, reminders
     * sorted and `timesOfDay` derived. Reminder scheduling uses the same list.
     */
    fun habitsToSave(): List<Habit> =
        _draftHabits.value.filter { it.name.isNotBlank() }.map { it.copy(name = it.name.trim()).forSave() }

    fun saveDraftGoal() {
        val name = _draftName.value.trim()
        val habits = habitsToSave()
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
