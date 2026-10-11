package app.theone.android.diary.goals.backend

import app.theone.android.testing.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GoalViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeGoalRepository()

    // Lazy: the ViewModel must be built after MainDispatcherRule has replaced Dispatchers.Main.
    private val viewModel by lazy { GoalViewModel(repository) }

    @Test
    fun `updateDraftName updates draftName`() {
        viewModel.updateDraftName("Morning routine")
        assertEquals("Morning routine", viewModel.draftName.value)
    }

    @Test
    fun `addDraftHabit ignores blank name`() {
        viewModel.addDraftHabit("", listOf("MORNING"))
        assertTrue(viewModel.draftHabits.value.isEmpty())
    }

    @Test
    fun `addDraftHabit ignores empty timesOfDay`() {
        viewModel.addDraftHabit("Stretch", emptyList())
        assertTrue(viewModel.draftHabits.value.isEmpty())
    }

    @Test
    fun `addDraftHabit appends a habit with the given name and times`() {
        viewModel.addDraftHabit("Stretch", listOf("MORNING", "EVENING"))
        val habits = viewModel.draftHabits.value
        assertEquals(1, habits.size)
        assertEquals("Stretch", habits[0].name)
        assertEquals(listOf("MORNING", "EVENING"), habits[0].timesOfDay)
    }

    @Test
    fun `removeDraftHabit removes at index`() {
        viewModel.addDraftHabit("Stretch", listOf("MORNING"))
        viewModel.addDraftHabit("Meditate", listOf("EVENING"))
        viewModel.removeDraftHabit(0)
        val habits = viewModel.draftHabits.value
        assertEquals(1, habits.size)
        assertEquals("Meditate", habits[0].name)
    }

    @Test
    fun `updateHabitReminders updates only the matching habit`() {
        viewModel.addDraftHabit("Stretch", listOf("MORNING"))
        viewModel.addDraftHabit("Meditate", listOf("EVENING"))
        val stretchId = viewModel.draftHabits.value[0].id
        val newReminders = listOf(HabitReminder(timeOfDay = "MORNING", hour = 7, minute = 30))

        viewModel.updateHabitReminders(stretchId, newReminders)

        val habits = viewModel.draftHabits.value
        assertEquals(newReminders, habits.first { it.id == stretchId }.reminders)
        assertTrue(habits.first { it.name == "Meditate" }.reminders.isEmpty())
    }

    @Test
    fun `startEditingGoal populates editingGoal draftName and draftHabits`() {
        val goal = Goal(id = "g1", name = "Sleep better", habits = listOf(Habit(name = "Wind down")))
        viewModel.startEditingGoal(goal)

        assertEquals(goal, viewModel.editingGoal.value)
        assertEquals("Sleep better", viewModel.draftName.value)
        assertEquals(goal.habits, viewModel.draftHabits.value)
    }

    @Test
    fun `clearDraft resets name habits and editingGoal`() {
        viewModel.startEditingGoal(Goal(id = "g1", name = "Sleep better", habits = listOf(Habit(name = "Wind down"))))

        viewModel.clearDraft()

        assertEquals("", viewModel.draftName.value)
        assertTrue(viewModel.draftHabits.value.isEmpty())
        assertNull(viewModel.editingGoal.value)
    }

    @Test
    fun `goals exposes the repository's goals newest first`() = runTest {
        repository.updateGoal(Goal(id = "old", createdAt = 1L))
        repository.updateGoal(Goal(id = "new", createdAt = 2L))
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.goals.collect {} }

        assertEquals(listOf("new", "old"), viewModel.goals.value.map { it.id })
    }

    @Test
    fun `saveDraftGoal adds a new goal and clears the draft`() {
        viewModel.updateDraftName("Morning routine")
        viewModel.addDraftHabit("Stretch", listOf("MORNING"))

        viewModel.saveDraftGoal()

        val saved = repository.stored.single()
        assertEquals("Morning routine", saved.name)
        assertEquals(listOf("Stretch"), saved.habits.map { it.name })
        assertEquals("", viewModel.draftName.value)
        assertTrue(viewModel.draftHabits.value.isEmpty())
    }

    @Test
    fun `saveDraftGoal does nothing without a name or habits`() {
        viewModel.updateDraftName("No habits")
        viewModel.saveDraftGoal()

        assertTrue(repository.stored.isEmpty())
        assertEquals("No habits", viewModel.draftName.value)
    }

    @Test
    fun `saveDraftGoal while editing updates the goal in place`() = runTest {
        val original = Goal(id = "g1", name = "Sleep better", habits = listOf(Habit(name = "Wind down")), createdAt = 5L)
        repository.updateGoal(original)
        viewModel.startEditingGoal(original)
        viewModel.updateDraftName("Sleep 8 hours")

        viewModel.saveDraftGoal()

        val updated = repository.stored.single()
        assertEquals("g1", updated.id)
        assertEquals("Sleep 8 hours", updated.name)
        assertEquals(5L, updated.createdAt)
        assertNull(viewModel.editingGoal.value)
    }

    @Test
    fun `deleteGoal removes the goal`() = runTest {
        repository.updateGoal(Goal(id = "g1"))
        repository.updateGoal(Goal(id = "g2"))

        viewModel.deleteGoal("g1")

        assertEquals(listOf("g2"), repository.stored.map { it.id })
    }

    @Test
    fun `a failed save surfaces in goalError and keeps the draft`() {
        repository.failNext = IllegalStateException("offline")
        viewModel.updateDraftName("Morning routine")
        viewModel.addDraftHabit("Stretch", listOf("MORNING"))

        viewModel.saveDraftGoal()

        assertEquals("offline", viewModel.goalError.value)
        assertTrue(repository.stored.isEmpty())
        assertEquals("Morning routine", viewModel.draftName.value)
        viewModel.clearGoalError()
        assertNull(viewModel.goalError.value)
    }
}
