package app.theone.android.workout.sessions.frontend

import app.theone.android.workout.sessions.backend.Comparison
import app.theone.android.workout.sessions.backend.SetLog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

class SessionDetailLabelsTest {
    private val locale = Locale.ENGLISH
    private val oct1 = LocalDate.of(2026, 10, 1)

    @Test
    fun `subtitle has the date and start-end times, or just the date without a start`() {
        val start = LocalDate.of(2026, 10, 8).atTime(18, 2).toInstant(ZoneOffset.UTC).toEpochMilli()
        val end = start + 62 * 60_000L
        val date = LocalDate.of(2026, 10, 8)
        assertEquals("Thursday, 8 Oct 2026 · 18:02–19:04", sessionSubtitle(date, start, end, ZoneOffset.UTC, locale))
        assertEquals("Thursday, 8 Oct 2026", sessionSubtitle(date, null, end, ZoneOffset.UTC, locale))
    }

    @Test
    fun `duration tile`() {
        assertEquals("62 min", durationLabel(62))
        assertEquals("<1 min", durationLabel(0))
        assertEquals("—", durationLabel(null))
    }

    @Test
    fun `comparison labels`() {
        assertEquals("+5 kg vs 1 Oct", comparisonLabel(Comparison.Weight(5.0, oct1), locale))
        assertEquals("−2.5 kg vs 1 Oct", comparisonLabel(Comparison.Weight(-2.5, oct1), locale))
        assertEquals("+1 rep vs 1 Oct", comparisonLabel(Comparison.Reps(1, oct1), locale))
        assertEquals("−2 reps vs 1 Oct", comparisonLabel(Comparison.Reps(-2, oct1), locale))
        assertEquals("Same as 1 Oct", comparisonLabel(Comparison.Same(oct1), locale))
    }

    @Test
    fun `load labels, with bodyweight from the catalog`() {
        assertEquals("80 kg × 6", loadLabel(SetLog(80.0, 6, isPr = false), bodyweight = false))
        assertEquals("62.5 kg × 8", loadLabel(SetLog(62.5, 8, isPr = false), bodyweight = false))
        assertEquals("Bodyweight × 12", loadLabel(SetLog(0.0, 12, isPr = false), bodyweight = true))
        assertEquals("Bodyweight + 10 kg × 10", loadLabel(SetLog(10.0, 10, isPr = false), bodyweight = true))
    }
}
