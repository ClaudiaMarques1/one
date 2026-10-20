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
    fun `addDraftHabit appends a blank habit by default and returns its id`() {
        val id = viewModel.addDraftHabit()

        val habit = viewModel.draftHabits.value.single()
        assertEquals(id, habit.id)
        assertEquals("", habit.name)
        assertTrue(habit.reminders.isEmpty())
    }

    @Test
    fun `addDraftHabit keeps the given name and sorts its reminders by time`() {
        viewModel.addDraftHabit("Stretch", listOf(HabitReminder(id = "b", hour = 18), HabitReminder(id = "a", hour = 7, minute = 30)))

        val habit = viewModel.draftHabits.value.single()
        assertEquals("Stretch", habit.name)
        assertEquals(listOf("a", "b"), habit.reminders.map { it.id })
    }

    @Test
    fun `renameDraftHabit renames only the matching habit`() {
        val first = viewModel.addDraftHabit()
        viewModel.addDraftHabit("Meditate")

        viewModel.renameDraftHabit(first, "Stretch")

        assertEquals(listOf("Stretch", "Meditate"), viewModel.draftHabits.value.map { it.name })
    }

    @Test
    fun `removeDraftHabit removes the matching habit`() {
        val stretchId = viewModel.addDraftHabit("Stretch")
        viewModel.addDraftHabit("Meditate")
        viewModel.removeDraftHabit(stretchId)
        val habits = viewModel.draftHabits.value
        assertEquals(1, habits.size)
        assertEquals("Meditate", habits[0].name)
    }

    @Test
    fun `updateHabitReminders updates only the matching habit, sorted by time`() {
        val stretchId = viewModel.addDraftHabit("Stretch")
        viewModel.addDraftHabit("Meditate")
        val late = HabitReminder(id = "late", hour = 21)
        val early = HabitReminder(id = "early", hour = 7, minute = 30)

        viewModel.updateHabitReminders(stretchId, listOf(late, early))

        val habits = viewModel.draftHabits.value
        assertEquals(listOf(early, late), habits.first { it.id == stretchId }.reminders)
        assertTrue(habits.first { it.name == "Meditate" }.reminders.isEmpty())
    }

    @Test
    fun `addReminderTime adds 9 AM first, then 2 hours after the latest time`() {
        val id = viewModel.addDraftHabit("Drink water")

        viewModel.addReminderTime(id)
        viewModel.addReminderTime(id)

        val reminders = viewModel.draftHabits.value.single().reminders
        assertEquals(listOf(9 to 0, 11 to 0), reminders.map { it.hour to it.minute })
        assertTrue(reminders.all { it.enabled && it.id.isNotEmpty() })
        assertEquals(2, reminders.map { it.id }.distinct().size)
    }

    @Test
    fun `changeReminderTime re-sorts, toggleReminder flips, removeReminderTime drops`() {
        val id = viewModel.addDraftHabit("Drink water", listOf(HabitReminder(id = "a", hour = 7), HabitReminder(id = "b", hour = 12)))

        viewModel.changeReminderTime(id, "a", 15, 45)
        assertEquals(listOf("b", "a"), viewModel.draftHabits.value.single().reminders.map { it.id })

        viewModel.toggleReminder(id, "b")
        assertEquals(false, viewModel.draftHabits.value.single().reminders.first { it.id == "b" }.enabled)

        viewModel.removeReminderTime(id, "b")
        assertEquals(listOf("a"), viewModel.draftHabits.value.single().reminders.map { it.id })
    }

    @Test
    fun `canSave needs a goal name and at least one named habit`() = runTest {
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.canSave.collect {} }
        val id = viewModel.addDraftHabit()
        viewModel.updateDraftName("Stay hydrated")
        assertEquals(false, viewModel.canSave.value)

        viewModel.renameDraftHabit(id, "Drink water")
        assertEquals(true, viewModel.canSave.value)
    }

    @Test
    fun `habitsToSave drops unnamed habits and derives timesOfDay`() {
        viewModel.addDraftHabit("", listOf(HabitReminder(id = "x", hour = 8)))
        viewModel.addDraftHabit(" Drink water ", listOf(HabitReminder(id = "a", hour = 19), HabitReminder(id = "b", hour = 7, minute = 30)))

        val habit = viewModel.habitsToSave().single()
        assertEquals("Drink water", habit.name)
        assertEquals(listOf("MORNING", "EVENING"), habit.timesOfDay)
        assertEquals(listOf("MORNING", "EVENING"), habit.reminders.map { it.timeOfDay })
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
        viewModel.addDraftHabit("Stretch")

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
        viewModel.addDraftHabit("Stretch")

        viewModel.saveDraftGoal()

        assertEquals("offline", viewModel.goalError.value)
        assertTrue(repository.stored.isEmpty())
        assertEquals("Morning routine", viewModel.draftName.value)
        viewModel.clearGoalError()
        assertNull(viewModel.goalError.value)
    }
}
