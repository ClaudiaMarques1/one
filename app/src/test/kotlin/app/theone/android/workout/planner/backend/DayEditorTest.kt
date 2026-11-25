package app.theone.android.workout.planner.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// Today is Tue 13 Oct 2026. Routine: Mon Push 18:00, Thu Leg 07:30.
class DayEditorTest {
    private val today = LocalDate.of(2026, 10, 13)

    private fun d(day: Int): LocalDate = LocalDate.of(2026, 10, day)

    private val names = mapOf("push" to "Push day", "pull" to "Pull day", "leg" to "Leg day")
    private val routine = TrainingRoutine(
        effectiveFrom = "2026-10-05",
        weekdayPlan = mapOf("MONDAY" to WeekdaySlot("push"), "THURSDAY" to WeekdaySlot("leg", "07:30"))
    )

    private fun editor(
        day: Int,
        overrides: List<DayOverride> = emptyList(),
        logged: Set<LocalDate> = emptySet(),
        r: TrainingRoutine = routine
    ): DayEditorUi {
        val sessions = TrainingSchedule.build(listOf(r), overrides, d(1), d(31), "push", names.keys)
        return DayEditor.build(d(day), today, sessions, overrides, names, logged, DayOfWeek.MONDAY, r.mode == PlanMode.SPLIT)
    }

    @Test
    fun `a past training day with nothing logged was missed`() {
        assertEquals("Missed: Push day was planned", editor(12).missedLine)
        assertEquals(EditorKind.PAST, editor(12).kind)
        assertNull(editor(12, logged = setOf(d(12))).missedLine)
        assertNull(editor(11).missedLine)
    }

    @Test
    fun `a future day without a session is a rest day`() {
        val e = editor(14)
        assertEquals(EditorKind.REST, e.kind)
        assertEquals("Rest day", e.status)
    }

    @Test
    fun `a planned day from the routine can move to free days this week from today on`() {
        val e = editor(15)
        assertEquals(EditorKind.PLANNED, e.kind)
        assertEquals("From your routine", e.status)
        assertFalse(e.canReset)
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), e.moveDays.map { it.weekday })
        assertEquals(d(15), e.moveDays.single { it.current }.date)
        assertEquals(listOf(d(13), d(14), d(16), d(17), d(18)), e.moveDays.filter { it.free }.map { it.date })
    }

    @Test
    fun `status lines follow the kind of change`() {
        assertEquals("Changed for this day only", editor(15, listOf(DayOverride("2026-10-15", templateId = "pull"))).status)
        val moved = listOf(DayOverride("2026-10-15", moveTo = "2026-10-17"))
        assertEquals("Moved from Thu 15 Oct", editor(17, moved).status)
        assertEquals(EditorKind.REST, editor(15, moved).kind)
        val added = editor(14, listOf(DayOverride("2026-10-14", added = true)))
        assertEquals("Added for this day only", added.status)
        assertEquals("Remove", added.resetLabel)
        assertTrue(added.canReset)
        assertEquals("Reset", editor(15, listOf(DayOverride("2026-10-15", time = "08:00"))).resetLabel)
    }

    @Test
    fun `a skipped day names the next workout, and a split says the order carries on`() {
        val skipped = editor(15, listOf(DayOverride("2026-10-15", skipped = true)))
        assertEquals(EditorKind.SKIPPED, skipped.kind)
        assertEquals("Skipped", skipped.status)
        assertEquals("Skipped Leg day. Next up is Push day on Mon 19 Oct.", skipped.skipNote)

        val split = TrainingRoutine(
            effectiveFrom = "2026-10-05",
            mode = PlanMode.SPLIT,
            splitDays = listOf("MONDAY", "THURSDAY"),
            splitOrder = listOf("push", "pull", "leg")
        )
        // Split turns: Mon 5 push, Thu 8 pull, Mon 12 leg, Thu 15 push, Mon 19 pull.
        assertEquals(
            "Skipped Push day. The split order carries on. Next up is Pull day on Mon 19 Oct.",
            editor(15, listOf(DayOverride("2026-10-15", skipped = true)), r = split).skipNote
        )
    }

    @Test
    fun `the time steps by 30 minutes inside 05 00 - 23 00`() {
        assertEquals(LocalTime.of(23, 0), DayEditor.stepTime(LocalTime.of(22, 45), 30))
        assertEquals(LocalTime.of(5, 0), DayEditor.stepTime(LocalTime.of(5, 15), -30))
        assertEquals(LocalTime.of(8, 0), DayEditor.stepTime(LocalTime.of(7, 30), 30))
        val late = editor(15, listOf(DayOverride("2026-10-15", time = "23:00")))
        assertFalse(late.canTimeUp)
        assertTrue(late.canTimeDown)
    }

    @Test
    fun `upcoming rows list the next sessions with what changed`() {
        val overrides = listOf(
            DayOverride("2026-10-15", moveTo = "2026-10-17"),
            DayOverride("2026-10-19", skipped = true),
            DayOverride("2026-10-21", added = true),
            DayOverride("2026-10-22", time = "08:00")
        )
        val sessions = TrainingSchedule.build(listOf(routine), overrides, today, today.plusDays(13), "push", names.keys)
        val rows = DayEditor.upcoming(sessions, names, today, 14)
        assertEquals(
            listOf(
                "Sat 17 Leg day 07:30 · moved from Thu 15 Oct",
                "Mon 19 Push day Skipped",
                "Wed 21 Push day 18:00 · added",
                "Thu 22 Leg day 08:00 · changed",
                "Mon 26 Push day 18:00"
            ),
            rows.map { "${it.weekday} ${it.day} ${it.title} ${it.subtitle}" }
        )
        assertTrue(rows[1].skipped)
    }
}
