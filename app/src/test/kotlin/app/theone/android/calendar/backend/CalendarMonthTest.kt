package app.theone.android.calendar.backend

import app.theone.android.diary.journal.backend.DiaryEntry
import app.theone.android.meals.backend.PlannedMeal
import app.theone.android.workout.sessions.backend.StrengthSession
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarMonthTest {
    private val today = "2026-10-10"
    private val workout = listOf(StrengthSession(templateName = "Push Day"))
    private val diary = DiaryEntry(mood = "🙂")
    private val planned = listOf(PlannedMeal(name = "Oats"), PlannedMeal(name = "Rice"))
    private val oneEaten = listOf(PlannedMeal(name = "Oats", eaten = true), PlannedMeal(name = "Rice"))

    @Test
    fun `a past day shows workout, eaten meals and diary in bottom-nav order`() {
        val day = DayActivity(sessions = workout, diary = diary, meals = oneEaten)
        assertEquals(
            listOf(ActivityType.WORKOUT, ActivityType.MEAL, ActivityType.DIARY),
            day.cellSections("2026-10-05", today)
        )
    }

    @Test
    fun `past meals only count when eaten, from today on planned meals count`() {
        val day = DayActivity(meals = planned)
        assertEquals(emptyList<ActivityType>(), day.cellSections("2026-10-05", today))
        assertEquals(listOf(ActivityType.MEAL), day.cellSections(today, today))
        assertEquals(listOf(ActivityType.MEAL), day.cellSections("2026-10-20", today))
    }

    @Test
    fun `today counts the workout and diary, future days don't`() {
        val day = DayActivity(sessions = workout, diary = diary)
        assertEquals(listOf(ActivityType.WORKOUT, ActivityType.DIARY), day.cellSections(today, today))
        assertEquals(emptyList<ActivityType>(), day.cellSections("2026-10-20", today))
    }

    @Test
    fun `month stats count sessions, diary days and meals eaten of planned in that month only`() {
        val days = mapOf(
            "2026-10-01" to DayActivity(sessions = workout + workout, meals = oneEaten),
            "2026-10-02" to DayActivity(diary = diary, meals = planned),
            "2026-10-31" to DayActivity(sessions = workout, diary = diary),
            "2026-11-01" to DayActivity(sessions = workout, diary = diary, meals = oneEaten)
        )

        assertEquals(MonthStats(workouts = 3, diaryEntries = 2, mealsEaten = 1, mealsPlanned = 4), monthStats(2026, 9, days))
        assertEquals(MonthStats(), monthStats(2026, 0, days))
    }
}
