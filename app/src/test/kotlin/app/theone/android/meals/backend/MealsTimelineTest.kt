package app.theone.android.meals.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MealsTimelineTest {
    private val date = "2026-11-19"

    private fun meal(slot: MealSlot, kcal: Int, eaten: Boolean = false) =
        PlannedMeal(date = date, slot = slot.name, name = slot.label, kcal = kcal, eaten = eaten)

    // The handoff mock: training day, workout slots on, post-workout empty, workout at 18:00.
    private val settings = MealSettings(enabledSlots = MealSlot.entries.toSet())
    private val mockDay = buildMealsDay(
        date = date,
        meals = listOf(
            meal(MealSlot.BREAKFAST, 420, eaten = true),
            meal(MealSlot.LUNCH, 640, eaten = true),
            meal(MealSlot.SNACKS, 210),
            meal(MealSlot.DINNER, 690),
            meal(MealSlot.PRE_WORKOUT, 180)
        ),
        settings = settings,
        workoutName = "Leg day",
        waterGlasses = 5,
        workoutMinutes = 18 * 60
    )

    @Test
    fun `timeline follows the handoff order, with the workout at its time`() {
        val rows = buildTimeline(mockDay, settings)

        val labels = rows.map {
            when (it) {
                is TimelineRow.Meal -> it.slot.label
                is TimelineRow.EmptySlot -> "${it.slot.label} (empty)"
                is TimelineRow.Workout -> it.name
            }
        }
        assertEquals(
            listOf("Breakfast", "Lunch", "Snacks", "Pre-workout", "Leg day", "Post-workout (empty)", "Dinner"),
            labels
        )
    }

    @Test
    fun `slot times from settings reorder the timeline`() {
        val custom = settings.copy(slotTimes = DEFAULT_SLOT_TIMES + (MealSlot.SNACKS to 10 * 60))
        val rows = buildTimeline(mockDay, custom)

        assertEquals(MealSlot.SNACKS, (rows[1] as TimelineRow.Meal).slot)
        assertEquals(600, rows[1].minutes)
    }

    @Test
    fun `a slot with two meals repeats its time, and no workout row on a rest day`() {
        val day = buildMealsDay(
            date,
            listOf(meal(MealSlot.LUNCH, 300), meal(MealSlot.LUNCH, 200)),
            MealSettings(),
            workoutName = null,
            waterGlasses = 0,
            workoutMinutes = 18 * 60
        )
        val rows = buildTimeline(day, MealSettings())

        assertEquals(2, rows.count { it is TimelineRow.Meal && it.slot == MealSlot.LUNCH })
        assertFalse(rows.any { it is TimelineRow.Workout })
    }

    @Test
    fun `the rail turns green only between two eaten meals`() {
        val rows = buildTimeline(mockDay, settings)

        assertFalse(rows.lineAboveEaten(0)) // first row has no line above
        assertTrue(rows.lineAboveEaten(1)) // Lunch eaten after an eaten Breakfast
        assertFalse(rows.lineAboveEaten(2)) // Snacks not eaten
        assertTrue(rows[0].eaten)
        assertFalse(rows.first { it is TimelineRow.Workout }.eaten)
    }

    @Test
    fun `kcal left and more planned match the handoff mock`() {
        assertEquals(1440, mockDay.kcalLeft()) // 2,500 - 1,060
        assertEquals(1080, mockDay.kcalMorePlanned()) // 2,140 planned - 1,060 eaten
        val over = mockDay.copy(eaten = MacroTotals(kcal = 2600))
        assertEquals(0, over.kcalLeft())
    }

    @Test
    fun `week starts on the chosen day`() {
        val thursday = LocalDate.of(2026, 11, 19)

        assertEquals(LocalDate.of(2026, 11, 16), weekOf(thursday, WeekStart.MONDAY).first())
        assertEquals(LocalDate.of(2026, 11, 15), weekOf(thursday, WeekStart.SUNDAY).first())
        assertEquals(7, weekOf(thursday, WeekStart.SUNDAY).size)
        assertEquals(LocalDate.of(2026, 11, 16), weekOf(LocalDate.of(2026, 11, 16), WeekStart.MONDAY).first())
    }

    @Test
    fun `a day marked as rest keeps its workout row but uses rest targets`() {
        val rest = buildMealsDay(
            date,
            emptyList(),
            settings,
            workoutName = "Leg day",
            waterGlasses = 0,
            workoutMinutes = 18 * 60,
            restDayChoice = true
        )

        assertFalse(rest.isTrainingDay)
        assertEquals(DEFAULT_REST_TARGETS, rest.targets)
        assertFalse(rest.slots.any { it.slot.optional })
        assertTrue(buildTimeline(rest, settings).any { it is TimelineRow.Workout })
    }
}
