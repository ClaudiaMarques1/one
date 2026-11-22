package app.theone.android.meals.backend

import app.theone.android.settings.backend.FakeSettingsRepository
import app.theone.android.settings.backend.WeekStart
import app.theone.android.testing.MainDispatcherRule
import app.theone.android.workout.sessions.backend.FakeStrengthRepository
import app.theone.android.workout.sessions.backend.StrengthSession
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class MealsWeekViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 19) // a Thursday
    private val mondayStart = FakeSettingsRepository(weekStart = WeekStart.MONDAY)

    private fun meal(id: String, date: String, slot: MealSlot, kcal: Int, eaten: Boolean = false) =
        PlannedMeal(id = id, date = date, slot = slot.name, name = id, kcal = kcal, eaten = eaten)

    private fun viewModel(
        meals: FakeMealsRepository = FakeMealsRepository(),
        strength: FakeStrengthRepository = FakeStrengthRepository()
    ) = MealsWeekViewModel(mondayStart, meals, strength, today, clock = { 5000L })

    private fun TestScope.collect(viewModel: MealsWeekViewModel) {
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.plan.collect {} }
    }

    @Test
    fun `the plan covers the week from the week-start setting, with workouts`() = runTest {
        val meals = FakeMealsRepository(listOf(meal("a", "2026-11-17", MealSlot.LUNCH, 600, eaten = true)))
        val strength = FakeStrengthRepository(listOf(StrengthSession(date = "2026-11-18", templateName = "Pull day")))
        val viewModel = viewModel(meals, strength)
        collect(viewModel)

        val plan = viewModel.plan.value
        assertEquals("2026-11-16", plan.days.first())
        assertEquals(7, plan.rows.size)
        assertEquals(600, plan.rows[1].eatenKcal)
        assertEquals("Pull day", plan.rows[2].trainingLabel)
    }

    @Test
    fun `the arrows move a week at a time`() = runTest {
        val viewModel = viewModel()
        collect(viewModel)

        viewModel.showNextWeek()
        assertEquals("2026-11-23", viewModel.plan.value.days.first())
        viewModel.showPreviousWeek()
        viewModel.showPreviousWeek()
        assertEquals("2026-11-09", viewModel.plan.value.days.first())
    }

    @Test
    fun `copy to tomorrow adds planned copies and keeps what's there`() = runTest {
        val meals = FakeMealsRepository(
            listOf(
                meal("a", "2026-11-19", MealSlot.BREAKFAST, 420, eaten = true),
                meal("b", "2026-11-19", MealSlot.DINNER, 690),
                meal("c", "2026-11-20", MealSlot.LUNCH, 500)
            )
        )
        val viewModel = viewModel(meals)
        collect(viewModel)

        viewModel.copyToNextDay("2026-11-19")

        val friday = meals.storedMeals.filter { it.date == "2026-11-20" }
        assertEquals(3, friday.size)
        assertTrue(friday.none { it.eaten })
        assertEquals(1610, viewModel.plan.value.rows[4].plannedKcal)
    }

    @Test
    fun `copy to a picked date, including another week`() = runTest {
        val meals = FakeMealsRepository(listOf(meal("a", "2026-11-19", MealSlot.SNACKS, 210)))
        val viewModel = viewModel(meals)
        collect(viewModel)

        viewModel.copyDay("2026-11-19", "2026-12-01")
        viewModel.copyDay("2026-11-19", "2026-11-19") // same day: ignored

        assertEquals(listOf("2026-11-19", "2026-12-01"), meals.storedMeals.map { it.date })
    }

    @Test
    fun `clear day removes only that day's meals and leaves water`() = runTest {
        val meals = FakeMealsRepository(
            listOf(meal("a", "2026-11-19", MealSlot.LUNCH, 640), meal("b", "2026-11-18", MealSlot.LUNCH, 500)),
            water = mapOf("2026-11-19" to 4)
        )
        val viewModel = viewModel(meals)
        collect(viewModel)

        viewModel.clearDay("2026-11-19")

        assertEquals(listOf("b"), meals.storedMeals.map { it.id })
        assertEquals(4, meals.storedWater("2026-11-19"))
    }

    @Test
    fun `copy week puts each day's meals on the same weekday next week, not eaten`() = runTest {
        val meals = FakeMealsRepository(
            listOf(
                meal("a", "2026-11-16", MealSlot.BREAKFAST, 400, eaten = true),
                meal("b", "2026-11-22", MealSlot.DINNER, 700)
            )
        )
        val viewModel = viewModel(meals)
        collect(viewModel)

        viewModel.copyWeek()

        val copies = meals.storedMeals.filter { it.id !in setOf("a", "b") }
        assertEquals(setOf("2026-11-23", "2026-11-29"), copies.map { it.date }.toSet())
        assertTrue(copies.none { it.eaten })
    }

    @Test
    fun `a failed write surfaces mealsError`() = runTest {
        val meals = FakeMealsRepository(listOf(meal("a", "2026-11-19", MealSlot.LUNCH, 640)))
        val viewModel = viewModel(meals)
        collect(viewModel)
        meals.failNext = IllegalStateException("offline")

        viewModel.clearDay("2026-11-19")

        assertEquals("offline", viewModel.mealsError.value)
        viewModel.clearMealsError()
        assertNull(viewModel.mealsError.value)
    }
}
