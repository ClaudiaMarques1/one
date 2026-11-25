package app.theone.android.workout.planner.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// Oct 2026: Mon 5, Tue 6, Thu 8, Fri 9, Mon 12, Tue 13, Wed 14, Thu 15, Fri 16, Sat 17.
class TrainingScheduleTest {
    private fun d(day: Int): LocalDate = LocalDate.of(2026, 10, day)

    private val split = TrainingRoutine(
        effectiveFrom = "2026-10-05",
        mode = PlanMode.SPLIT,
        splitDays = listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY"),
        splitOrder = listOf("push", "pull", "leg"),
        splitTime = "18:00"
    )

    private fun build(
        routines: List<TrainingRoutine> = listOf(split),
        overrides: List<DayOverride> = emptyList(),
        from: Int = 5,
        to: Int = 18
    ) = TrainingSchedule.build(routines, overrides, d(from), d(to), defaultTemplateId = "push")

    private fun List<PlannedSession>.on(day: Int) = filter { it.date == d(day) }

    @Test
    fun `split mode takes the workouts in turn on the training days`() {
        val sessions = build(to = 16)
        assertEquals(listOf(5, 6, 8, 9, 12, 13, 15, 16), sessions.map { it.date.dayOfMonth })
        assertEquals(listOf("push", "pull", "leg", "push", "pull", "leg", "push", "pull"), sessions.map { it.templateId })
        assertTrue(sessions.all { it.time == LocalTime.of(18, 0) })
    }

    @Test
    fun `split mode counts from the anchor when one is set`() {
        val sessions = build(listOf(split.copy(splitAnchor = "2026-10-06")), to = 9)
        assertEquals(listOf(6, 8, 9), sessions.map { it.date.dayOfMonth })
        assertEquals(listOf("push", "pull", "leg"), sessions.map { it.templateId })
    }

    @Test
    fun `weekday mode gives each day its own workout and time`() {
        val weekday = TrainingRoutine(
            effectiveFrom = "2026-10-05",
            weekdayPlan = mapOf(
                "MONDAY" to WeekdaySlot("push", "18:00"),
                "WEDNESDAY" to WeekdaySlot("pull", "07:30")
            )
        )
        val sessions = build(listOf(weekday), to = 11)
        assertEquals(listOf(d(5) to "push", d(7) to "pull"), sessions.map { it.date to it.templateId })
        assertEquals(LocalTime.of(7, 30), sessions[1].time)
    }

    @Test
    fun `nothing is planned before the routine starts`() {
        assertTrue(build(from = 1, to = 4).isEmpty())
    }

    @Test
    fun `a newer routine version applies from its start date and the past keeps the old one`() {
        val v1 = TrainingRoutine(effectiveFrom = "2026-10-05", weekdayPlan = mapOf("MONDAY" to WeekdaySlot("push")))
        val v2 = TrainingRoutine(effectiveFrom = "2026-10-12", weekdayPlan = mapOf("TUESDAY" to WeekdaySlot("pull")))
        val sessions = build(listOf(v2, v1), to = 14)
        assertEquals(listOf(d(5) to "push", d(13) to "pull"), sessions.map { it.date to it.templateId })
    }

    @Test
    fun `a skipped session still uses its turn so the order carries on`() {
        val sessions = build(overrides = listOf(DayOverride(sourceDate = "2026-10-12", skipped = true)))
        assertTrue(sessions.on(12).single().skipped)
        assertEquals("pull", sessions.on(12).single().templateId)
        assertEquals("leg", sessions.on(13).single().templateId)
    }

    @Test
    fun `a moved session shows on the new day and leaves the original day empty`() {
        val sessions = build(overrides = listOf(DayOverride(sourceDate = "2026-10-16", moveTo = "2026-10-17")))
        assertTrue(sessions.on(16).isEmpty())
        val moved = sessions.on(17).single()
        assertEquals(d(16), moved.sourceDate)
        assertEquals(d(16), moved.movedFrom)
        assertEquals("pull", moved.templateId)
    }

    @Test
    fun `a session moved into the range from just outside it is included`() {
        val sessions = build(overrides = listOf(DayOverride(sourceDate = "2026-10-16", moveTo = "2026-10-17")), from = 17, to = 17)
        assertEquals(d(16), sessions.single().sourceDate)
    }

    @Test
    fun `changing the workout or time marks the day as edited`() {
        val sessions = build(
            overrides = listOf(
                DayOverride(sourceDate = "2026-10-12", templateId = "leg", time = "07:00"),
                // Same workout as the routine already has.
                DayOverride(sourceDate = "2026-10-13", templateId = "leg")
            )
        )
        val changed = sessions.on(12).single()
        assertEquals("leg", changed.templateId)
        assertEquals(LocalTime.of(7, 0), changed.time)
        assertTrue(changed.edited)
        assertFalse(sessions.on(13).single().edited)
    }

    @Test
    fun `a workout added on a rest day uses the default template at 18 00 unless one is chosen`() {
        val sessions = build(
            overrides = listOf(
                DayOverride(sourceDate = "2026-10-14", added = true),
                DayOverride(sourceDate = "2026-10-17", added = true, templateId = "leg", time = "10:00")
            )
        )
        val wed = sessions.on(14).single()
        assertEquals("push", wed.templateId)
        assertEquals(LocalTime.of(18, 0), wed.time)
        assertTrue(wed.added)
        assertFalse(wed.edited)
        assertEquals("leg" to LocalTime.of(10, 0), sessions.on(17).single().let { it.templateId to it.time })
    }

    @Test
    fun `an override on a day the routine no longer trains is dropped`() {
        val sessions = build(overrides = listOf(DayOverride(sourceDate = "2026-10-14", skipped = true)))
        assertTrue(sessions.on(14).isEmpty())
    }

    @Test
    fun `unreadable stored values fall back instead of failing`() {
        val routine = TrainingRoutine(
            effectiveFrom = "2026-10-05",
            weekdayPlan = mapOf("MONDAY" to WeekdaySlot("push", "bad"), "NOTADAY" to WeekdaySlot("pull"))
        )
        val sessions = build(listOf(routine, TrainingRoutine(effectiveFrom = "")), to = 11)
        assertEquals(LocalTime.of(18, 0), sessions.single().time)
    }

    @Test
    fun `day states follow the handover table`() {
        val today = d(13)
        val planned = { day: Int, skipped: Boolean -> listOf(PlannedSession(d(day), d(day), "push", LocalTime.NOON, skipped = skipped)) }
        assertEquals(DayState.DONE, TrainingSchedule.dayState(d(12), today, planned(12, false), hasLoggedWorkout = true))
        assertEquals(DayState.DONE, TrainingSchedule.dayState(d(11), today, emptyList(), hasLoggedWorkout = true))
        assertEquals(DayState.DONE, TrainingSchedule.dayState(today, today, planned(13, false), hasLoggedWorkout = true))
        assertEquals(DayState.MISSED, TrainingSchedule.dayState(d(12), today, planned(12, false), hasLoggedWorkout = false))
        assertEquals(DayState.SKIPPED, TrainingSchedule.dayState(d(12), today, planned(12, true), hasLoggedWorkout = false))
        assertEquals(DayState.PLANNED, TrainingSchedule.dayState(today, today, planned(13, false), hasLoggedWorkout = false))
        assertEquals(DayState.PLANNED, TrainingSchedule.dayState(d(15), today, planned(15, false), hasLoggedWorkout = false))
        assertEquals(DayState.SKIPPED, TrainingSchedule.dayState(d(15), today, planned(15, true), hasLoggedWorkout = false))
        assertEquals(DayState.REST, TrainingSchedule.dayState(d(14), today, emptyList(), hasLoggedWorkout = false))
        assertEquals(DayState.REST, TrainingSchedule.dayState(d(11), today, emptyList(), hasLoggedWorkout = false))
    }

    @Test
    fun `a session can move to a free day this week from today on`() {
        val sessions = build()
        val thu = sessions.on(15).single()
        // Week Mon 12 - Sun 18; today Tue 13 (busy), Fri 16 busy.
        assertEquals(listOf(d(14), d(17), d(18)), TrainingSchedule.moveTargets(thu, sessions, d(13), DayOfWeek.MONDAY))
        // Week Sun 11 - Sat 17.
        assertEquals(listOf(d(14), d(17)), TrainingSchedule.moveTargets(thu, sessions, d(13), DayOfWeek.SUNDAY))
    }

    @Test
    fun `a skipped session still blocks its day as a move target`() {
        val sessions = build(overrides = listOf(DayOverride(sourceDate = "2026-10-16", skipped = true)))
        val thu = sessions.on(15).single()
        assertFalse(d(16) in TrainingSchedule.moveTargets(thu, sessions, d(13), DayOfWeek.MONDAY))
    }

    @Test
    fun `an empty or reversed range plans nothing`() {
        assertTrue(build(from = 10, to = 9).isEmpty())
        assertTrue(TrainingSchedule.build(emptyList(), emptyList(), d(1), d(31)).isEmpty())
        assertNull(build(from = 7, to = 7).firstOrNull())
    }
}
