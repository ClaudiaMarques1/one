package app.theone.android.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Covers the draft-management logic only, which is pure local state with no repository call -
// GoalRepository is a final, Firebase-coupled class with no seam for a fake, so saveDraftGoal()/
// deleteGoal() aren't covered here. See .claude/rules/architecture.md's non-goals: no DI
// framework was introduced to make that testable.
class GoalViewModelTest {

    private val viewModel = GoalViewModel(repository = GoalRepository())

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
}
