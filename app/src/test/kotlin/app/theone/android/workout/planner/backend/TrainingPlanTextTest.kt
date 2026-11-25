package app.theone.android.workout.planner.backend

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class TrainingPlanTextTest {
    private val names = mapOf("push" to "Push", "pull" to "Pull", "leg" to "Leg")

    private fun d(day: Int): LocalDate = LocalDate.of(2026, 10, day)

    @Test
    fun `week note follows the handover copy`() {
        assertEquals("3 of 4 done · 1 more planned", TrainingPlanText.weekNote(TrainingWeek(done = 3, planned = 1, target = 4)))
        assertEquals("4 of 4 done · target reached", TrainingPlanText.weekNote(TrainingWeek(done = 4, planned = 0, target = 4)))
        assertEquals("0 of 3 done", TrainingPlanText.weekNote(TrainingWeek(done = 0, planned = 0, target = 3)))
    }

    @Test
    fun `pips fill done then planned then empty up to the larger of target and total`() {
        assertEquals(
            listOf(WeekPip.DONE, WeekPip.PLANNED, WeekPip.PLANNED, WeekPip.EMPTY),
            TrainingWeek(done = 1, planned = 2, target = 4).pips
        )
        assertEquals(5, TrainingWeek(done = 3, planned = 2, target = 4).pips.size)
    }

    @Test
    fun `week counts logged days as done and later sessions as planned`() {
        val sessions = listOf(
            PlannedSession(d(12), d(12), "push", LocalTime.NOON),
            PlannedSession(d(13), d(13), "pull", LocalTime.NOON),
            PlannedSession(d(15), d(15), "leg", LocalTime.NOON),
            PlannedSession(d(16), d(16), "push", LocalTime.NOON, skipped = true)
        )
        // Today Tue 13: Mon 12 logged, Tue 13 logged, Thu 15 planned, Fri 16 skipped; Sun 11 logged is last week.
        val week = TrainingPlanText.week(sessions, setOf(d(11), d(12), d(13)), d(13), DayOfWeek.MONDAY, target = 4)
        assertEquals(TrainingWeek(done = 2, planned = 1, target = 4), week)
        // With a Sunday start, Sun 11 is this week.
        assertEquals(3, TrainingPlanText.week(sessions, setOf(d(11), d(12), d(13)), d(13), DayOfWeek.SUNDAY, 4).done)
    }

    @Test
    fun `routine line for weekday mode lists training days in week order and skips deleted templates`() {
        val routine = TrainingRoutine(
            weekdayPlan = mapOf(
                "THURSDAY" to WeekdaySlot("leg"),
                "MONDAY" to WeekdaySlot("push"),
                "SUNDAY" to WeekdaySlot("gone")
            )
        )
        assertEquals("By weekday: Mon Push, Thu Leg", TrainingPlanText.routineLine(routine, names, DayOfWeek.MONDAY))
        assertEquals("By weekday: no training days", TrainingPlanText.routineLine(TrainingRoutine(), names, DayOfWeek.MONDAY))
    }

    @Test
    fun `routine line for a split shows the order, days from the week start and the time`() {
        val routine = TrainingRoutine(
            mode = PlanMode.SPLIT,
            splitDays = listOf("FRIDAY", "MONDAY", "SUNDAY"),
            splitOrder = listOf("push", "pull", "leg"),
            splitTime = "18:00"
        )
        assertEquals(
            "Rotating split: Push → Pull → Leg on Sun, Mon, Fri at 18:00",
            TrainingPlanText.routineLine(routine, names, DayOfWeek.SUNDAY)
        )
    }

    @Test
    fun `next up line names the workout, short date and time`() {
        val next = PlannedSession(d(12), d(12), "pull", LocalTime.of(18, 0))
        assertEquals("Next: Pull · Mon 12 Oct at 18:00", TrainingPlanText.nextUpLine(next, names))
        assertEquals("Nothing planned yet", TrainingPlanText.nextUpLine(null, names))
    }

    @Test
    fun `target note says matches, add more, or over`() {
        assertEquals("Your routine has 4 days a week, which matches.", TrainingPlanText.targetNote(4, 4))
        assertEquals("Your routine has 1 day a week, which matches.", TrainingPlanText.targetNote(1, 1))
        assertEquals("Your routine has 3 days. Add 1 more to reach it.", TrainingPlanText.targetNote(3, 4))
        assertEquals("Your routine has 5 days, 1 over.", TrainingPlanText.targetNote(5, 4))
        assertEquals("1 day", TrainingPlanText.targetLabel(1))
    }

    @Test
    fun `training days ignore deleted templates`() {
        val ids = setOf("push", "leg")
        val weekday = TrainingRoutine(weekdayPlan = mapOf("MONDAY" to WeekdaySlot("push"), "TUESDAY" to WeekdaySlot("gone")))
        assertEquals(1, TrainingPlanText.trainingDays(weekday, ids))
        val split = TrainingRoutine(mode = PlanMode.SPLIT, splitDays = listOf("MONDAY", "TUESDAY"), splitOrder = listOf("gone"))
        assertEquals(0, TrainingPlanText.trainingDays(split, ids))
        assertEquals(2, TrainingPlanText.trainingDays(split.copy(splitOrder = listOf("gone", "leg")), ids))
    }
}
