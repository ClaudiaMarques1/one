package app.theone.android.meals.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealsDayTest {
    private val date = "2026-11-19"

    private fun meal(slot: MealSlot, kcal: Int, p: Int, c: Int, f: Int, eaten: Boolean = false) =
        PlannedMeal(date = date, slot = slot.name, name = slot.label, kcal = kcal, proteinG = p, carbsG = c, fatG = f, eaten = eaten)

    // The handoff's mock day: two meals eaten, three planned.
    private val mockMeals = listOf(
        meal(MealSlot.BREAKFAST, 420, 32, 48, 10, eaten = true),
        meal(MealSlot.LUNCH, 640, 48, 70, 16, eaten = true),
        meal(MealSlot.SNACKS, 210, 20, 22, 7),
        meal(MealSlot.DINNER, 690, 45, 55, 28)
    )

    @Test
    fun `totals match the handoff mock - eaten 1,060 and planned 1,960`() {
        val day = buildMealsDay(date, mockMeals, MealSettings(), workoutName = null, waterGlasses = 5)

        assertEquals(MacroTotals(1060, 80, 118, 26), day.eaten)
        assertEquals(1960, day.planned.kcal)
        assertEquals(5, day.waterGlasses)
    }

    @Test
    fun `rest day uses rest targets and hides workout slots`() {
        val settings = MealSettings(enabledSlots = MealSlot.entries.toSet())
        val day = buildMealsDay(date, emptyList(), settings, workoutName = null, waterGlasses = 0)

        assertFalse(day.isTrainingDay)
        assertEquals(DEFAULT_REST_TARGETS, day.targets)
        assertEquals(listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACKS), day.slots.map { it.slot })
    }

    @Test
    fun `training day uses training targets and shows enabled workout slots`() {
        val settings = MealSettings(enabledSlots = MealSlot.entries.toSet() - MealSlot.POST_WORKOUT)
        val day = buildMealsDay(date, emptyList(), settings, workoutName = "Leg day", waterGlasses = 0)

        assertTrue(day.isTrainingDay)
        assertEquals(DEFAULT_TRAINING_TARGETS, day.targets)
        assertEquals(MealSlot.PRE_WORKOUT, day.slots.last().slot)
        assertFalse(day.slots.any { it.slot == MealSlot.POST_WORKOUT })
    }

    @Test
    fun `workout slots stay off by default even on training days`() {
        val day = buildMealsDay(date, emptyList(), MealSettings(), workoutName = "Leg day", waterGlasses = 0)

        assertEquals(4, day.slots.size)
    }

    @Test
    fun `a disabled slot still shows when it holds meals`() {
        val settings = MealSettings(enabledSlots = setOf(MealSlot.BREAKFAST))
        val day = buildMealsDay(date, listOf(meal(MealSlot.DINNER, 500, 30, 50, 10)), settings, workoutName = null, waterGlasses = 0)

        assertEquals(listOf(MealSlot.BREAKFAST, MealSlot.DINNER), day.slots.map { it.slot })
        assertEquals(500, day.slots.last().kcal)
    }

    @Test
    fun `tapping a glass fills up to it, tapping the last full glass empties it`() {
        assertEquals(5, tapWaterGlass(current = 0, n = 5))
        assertEquals(4, tapWaterGlass(current = 5, n = 5))
        assertEquals(2, tapWaterGlass(current = 5, n = 2))
        assertEquals(0, tapWaterGlass(current = 1, n = 1))
    }
}
