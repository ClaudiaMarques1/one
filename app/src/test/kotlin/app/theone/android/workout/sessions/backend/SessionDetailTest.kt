package app.theone.android.workout.sessions.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SessionDetailTest {
    private fun session(
        id: String,
        date: String,
        vararg exercises: StrengthExercise,
        template: String = "Push day",
        createdAt: Long = 0L
    ) = StrengthSession(id = id, date = date, templateName = template, exercises = exercises.toList(), createdAt = createdAt)

    private fun detail(id: String, vararg sessions: StrengthSession) = buildSessionDetail(id, sessions.toList(), emptySet())!!

    private fun bench(vararg sets: Pair<Double, Int>) = StrengthExercise("Bench press", sets.map { (kg, reps) -> StrengthSet(reps, kg) })

    @Test
    fun `duration counts clock minutes like the header's start-end times, null without a start`() {
        val minute = 60_000L
        assertEquals(62L, StrengthSession(startedAt = 0L, createdAt = 62 * minute + 20_000L).durationMinutes())
        // 13:51:10 -> 13:51:50 shows "13:51–13:51": 0 min, not rounded up to 1.
        assertEquals(0L, StrengthSession(startedAt = 10_000L, createdAt = 50_000L).durationMinutes())
        // 13:51:50 -> 13:52:10 shows "13:51–13:52": 1 min, though only 20 s passed.
        assertEquals(1L, StrengthSession(startedAt = 50_000L, createdAt = minute + 10_000L).durationMinutes())
        assertNull(StrengthSession(startedAt = null, createdAt = 10_000L).durationMinutes())
        assertNull(StrengthSession(startedAt = 20_000L, createdAt = 10_000L).durationMinutes())
    }

    @Test
    fun `a set is a PR when weight x reps beats every earlier set of that exercise`() {
        val earlier = session("a", "2026-10-01", bench(70.0 to 8)) // 560
        // 600 beats 560 (PR); then 480, 560 and 350 don't beat 600.
        val today = session("b", "2026-10-08", bench(60.0 to 10, 80.0 to 6, 80.0 to 7, 70.0 to 5))
        val detail = detail("b", today, earlier)

        assertEquals(listOf(true, false, false, false), detail.exercises.single().sets.map { it.isPr })
        assertEquals(1, detail.prCount)
        assertEquals(4, detail.setCount)
    }

    @Test
    fun `later sets in the same session must beat the earlier ones too`() {
        val earlier = session("a", "2026-10-01", bench(50.0 to 10)) // 500
        val today = session("b", "2026-10-08", bench(60.0 to 10, 62.5 to 10, 60.0 to 10))
        val sets = buildSessionDetail("b", listOf(earlier, today), emptySet())!!.exercises.single().sets
        assertEquals(listOf(true, true, false), sets.map { it.isPr })
    }

    @Test
    fun `no PRs without an earlier session of that exercise, and never for 0 kg`() {
        val first = session("a", "2026-10-01", bench(60.0 to 10, 80.0 to 6))
        assertTrue(buildSessionDetail("a", listOf(first), emptySet())!!.exercises.single().sets.none { it.isPr })

        val dips = { reps: Int -> StrengthExercise("Dips", listOf(StrengthSet(reps, 0.0))) }
        val later = session("c", "2026-10-08", dips(12))
        val bodyweightDetail = buildSessionDetail("c", listOf(session("b", "2026-10-01", dips(8)), later), emptySet())!!
        assertFalse(bodyweightDetail.exercises.single().sets.single().isPr)
    }

    @Test
    fun `comparison uses the top weight, then total reps, against the previous session of the same template`() {
        val oct1 = LocalDate.of(2026, 10, 1)
        val previous = session("a", "2026-10-01", bench(75.0 to 6, 70.0 to 8))
        val otherTemplate = session("x", "2026-10-05", bench(100.0 to 1), template = "Legs")

        val heavier = session("b", "2026-10-08", bench(80.0 to 6))
        assertEquals(Comparison.Weight(5.0, oct1), detail("b", previous, otherTemplate, heavier).exercises.single().comparison)

        val moreReps = session("c", "2026-10-08", bench(75.0 to 7, 70.0 to 8))
        val reps = buildSessionDetail("c", listOf(previous, moreReps), emptySet())!!.exercises.single().comparison
        assertEquals(Comparison.Reps(1, oct1), reps)
        assertTrue(reps!!.isUp)

        val same = session("d", "2026-10-08", bench(75.0 to 6, 70.0 to 8))
        assertEquals(Comparison.Same(oct1), buildSessionDetail("d", listOf(previous, same), emptySet())!!.exercises.single().comparison)

        val lighter = session("e", "2026-10-08", bench(72.5 to 6))
        val down = buildSessionDetail("e", listOf(previous, lighter), emptySet())!!.exercises.single().comparison
        assertEquals(Comparison.Weight(-2.5, oct1), down)
        assertFalse(down!!.isUp)
    }

    @Test
    fun `no comparison without an earlier session of the same template with that exercise`() {
        val only = session("a", "2026-10-08", bench(80.0 to 6))
        val otherTemplate = session("b", "2026-10-01", bench(70.0 to 6), template = "Legs")
        assertNull(buildSessionDetail("a", listOf(only, otherTemplate), emptySet())!!.exercises.single().comparison)
    }

    @Test
    fun `same-day sessions are ordered by when they were saved`() {
        val morning = session("a", "2026-10-08", bench(70.0 to 8), createdAt = 1L)
        val evening = session("b", "2026-10-08", bench(75.0 to 8), createdAt = 2L)
        assertEquals(Comparison.Weight(5.0, LocalDate.of(2026, 10, 8)), detail("b", evening, morning).exercises.single().comparison)
        assertNull(buildSessionDetail("a", listOf(evening, morning), emptySet())!!.exercises.single().comparison)
    }

    @Test
    fun `bodyweight comes from the catalog names, case-insensitively`() {
        val dips = session("a", "2026-10-08", StrengthExercise("Triceps Dips", listOf(StrengthSet(10, 10.0))), bench(80.0 to 6))
        val detail = buildSessionDetail("a", listOf(dips), setOf("triceps dips"))!!
        assertEquals(listOf(true, false), detail.exercises.map { it.bodyweight })
    }

    @Test
    fun `an unknown session id gives null`() {
        assertNull(buildSessionDetail("missing", listOf(session("a", "2026-10-08")), emptySet()))
    }
}
