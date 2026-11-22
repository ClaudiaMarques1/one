package app.theone.android.meals.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealsWeekPlanTest {
    private val today = "2026-11-19"
    private val days = (15..21).map { "2026-11-%02d".format(it) }
    private val settings = MealSettings()

    private fun meal(date: String, slot: MealSlot, kcal: Int, eaten: Boolean = false, id: String = "") =
        PlannedMeal(id = id, date = date, slot = slot.name, kcal = kcal, eaten = eaten)

    @Test
    fun `days are past, today or future`() {
        val rows = buildWeekPlan(days, today, emptyList(), settings, emptyMap(), emptyMap())

        assertEquals(DayState.PAST, rows[0].state)
        assertEquals(DayState.TODAY, rows[4].state)
        assertEquals(DayState.FUTURE, rows[6].state)
    }

    @Test
    fun `a past day is on target within 5 percent of its target`() {
        // Rest days, 2,300 target: Sunday is 20 under (on target), Tuesday 290 under.
        val meals = listOf(
            meal("2026-11-15", MealSlot.LUNCH, 2280, eaten = true),
            meal("2026-11-17", MealSlot.LUNCH, 2010, eaten = true)
        )
        val rows = buildWeekPlan(days, today, meals, settings, emptyMap(), emptyMap())

        assertTrue(rows[0].onTarget)
        assertEquals(2280, rows[0].eatenKcal)
        assertEquals(2300, rows[0].targetKcal)
        assertFalse(rows[2].onTarget)
    }

    @Test
    fun `chips show eaten, planned and empty per slot`() {
        val meals = listOf(
            meal(today, MealSlot.BREAKFAST, 420, eaten = true),
            meal(today, MealSlot.LUNCH, 640, eaten = true),
            meal(today, MealSlot.LUNCH, 100),
            meal(today, MealSlot.DINNER, 690)
        )
        val row = buildWeekPlan(days, today, meals, settings, emptyMap(), emptyMap())[4]

        assertEquals(
            listOf(ChipState.EATEN, ChipState.PLANNED, ChipState.PLANNED, ChipState.EMPTY),
            row.chips.map { it.second }
        )
        assertEquals(1060, row.eatenKcal)
        assertEquals(1850, row.plannedKcal)
    }

    @Test
    fun `training days use training targets and show the workout or Training`() {
        val rows = buildWeekPlan(
            days,
            today,
            emptyList(),
            settings,
            workouts = mapOf("2026-11-16" to "Push day"),
            restDayChoices = mapOf("2026-11-20" to false, "2026-11-16" to true)
        )

        assertEquals("Push day", rows[1].trainingLabel) // a workout wins over a stored rest choice
        assertEquals(2500, rows[1].targetKcal)
        assertEquals("Training", rows[5].trainingLabel) // planned with the switch off
        assertNull(rows[6].trainingLabel)
        assertEquals(2300, rows[6].targetKcal)
    }

    @Test
    fun `copies are planned, without ids, on the new date and in order`() {
        val meals = listOf(
            meal(today, MealSlot.DINNER, 690, eaten = true, id = "b").copy(createdAt = 20),
            meal(today, MealSlot.BREAKFAST, 420, id = "a").copy(createdAt = 10)
        )

        val copies = copyMealsTo(meals, "2026-11-20", now = 1000)

        assertEquals(listOf(MealSlot.BREAKFAST.name, MealSlot.DINNER.name), copies.map { it.slot })
        assertTrue(copies.all { it.id.isEmpty() && it.date == "2026-11-20" && !it.eaten })
        assertEquals(listOf(1000L, 1001L), copies.map { it.createdAt })
    }
}
