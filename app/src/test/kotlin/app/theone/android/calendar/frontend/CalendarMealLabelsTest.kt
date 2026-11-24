package app.theone.android.calendar.frontend

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CalendarMealLabelsTest {
    @Test
    fun `meals card title - past days count what was logged, today and later eaten of planned`() {
        assertEquals("3 meals logged", mealsCardTitle(count = 4, eaten = 3, isPast = true))
        assertEquals("1 meal logged", mealsCardTitle(count = 2, eaten = 1, isPast = true))
        assertEquals("0 of 2 meals eaten", mealsCardTitle(count = 2, eaten = 0, isPast = true))
        assertEquals("1 of 4 meals eaten", mealsCardTitle(count = 4, eaten = 1, isPast = false))
        assertEquals("0 of 1 meal eaten", mealsCardTitle(count = 1, eaten = 0, isPast = false))
    }

    @Test
    fun `week summary adds meal days only when there are some`() {
        assertEquals("3 workouts · 4 diary", weekSummaryLabel(3, 4, 0))
        assertEquals("3 workouts · 4 diary · 1 meal day", weekSummaryLabel(3, 4, 1))
        assertEquals("0 workouts · 0 diary · 5 meal days", weekSummaryLabel(0, 0, 5))
    }

    @Test
    fun `meal count and kcal`() {
        assertEquals("1 meal", mealCountLabel(1))
        assertEquals("3 meals", mealCountLabel(3))
        assertEquals("1,850 kcal", kcalLabel(1850, Locale.ENGLISH))
    }
}
