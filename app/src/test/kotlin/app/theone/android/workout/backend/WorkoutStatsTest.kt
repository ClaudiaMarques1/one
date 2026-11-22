package app.theone.android.workout.backend

import app.theone.android.workout.sessions.backend.StrengthExercise
import app.theone.android.workout.sessions.backend.StrengthSession
import app.theone.android.workout.sessions.backend.StrengthSet
import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.TemplateSet
import app.theone.android.workout.templates.backend.WorkoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WorkoutStatsTest {
    private fun session(date: String, name: String = "Push Day", createdAt: Long = 0L) =
        StrengthSession(date = date, templateName = name, createdAt = createdAt)

    @Test
    fun `sessionsThisWeek counts Monday up to today`() {
        // 2026-10-15 is a Thursday; Monday is the 12th.
        val sessions = listOf(
            session("2026-10-11"),
            session("2026-10-12"),
            session("2026-10-15"),
            session("2026-10-16"),
            session("not a date")
        )

        assertEquals(2, sessionsThisWeek(sessions, LocalDate.of(2026, 10, 15)))
    }

    @Test
    fun `sessionsThisWeek starts the week on Sunday when set`() {
        // Thursday 2026-10-15: the Sunday week starts on the 11th, so the 11th counts too.
        val sessions = listOf(session("2026-10-10"), session("2026-10-11"), session("2026-10-12"), session("2026-10-15"))

        assertEquals(3, sessionsThisWeek(sessions, LocalDate.of(2026, 10, 15), DayOfWeek.SUNDAY))
    }

    @Test
    fun `lastDoneByTemplate keeps the latest date per template`() {
        val sessions = listOf(session("2026-10-01"), session("2026-10-08"), session("2026-10-05", name = "Legs"))

        assertEquals(
            mapOf("Push Day" to LocalDate.of(2026, 10, 8), "Legs" to LocalDate.of(2026, 10, 5)),
            lastDoneByTemplate(sessions)
        )
    }

    @Test
    fun `recentSessions is the latest three, newest first`() {
        val sessions = listOf(
            session("2026-10-01", "a"),
            session("2026-10-08", "b", createdAt = 1),
            session("2026-10-08", "c", createdAt = 2),
            session("2026-10-05", "d")
        )

        assertEquals(listOf("c", "b", "d"), recentSessions(sessions).map { it.templateName })
    }

    @Test
    fun `set count and volume`() {
        val session = StrengthSession(
            exercises = listOf(
                StrengthExercise("Bench", listOf(StrengthSet(8, 60.0), StrengthSet(6, 62.5))),
                StrengthExercise("Pull-up", listOf(StrengthSet(10, 0.0)))
            )
        )

        assertEquals(3, session.setCount())
        assertEquals(8 * 60.0 + 6 * 62.5, session.volumeKg(), 0.0)
        assertEquals(2, WorkoutTemplate(exercises = listOf(TemplateExercise("Bench", listOf(TemplateSet(), TemplateSet())))).setCount())
    }
}
