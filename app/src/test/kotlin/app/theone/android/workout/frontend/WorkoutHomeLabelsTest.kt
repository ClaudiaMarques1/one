package app.theone.android.workout.frontend

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class WorkoutHomeLabelsTest {
    private val locale = Locale.ENGLISH
    private val thursday = LocalDate.of(2026, 10, 8)

    @Test
    fun `countLabel handles singular and plural`() {
        assertEquals("1 set", countLabel(1, "set"))
        assertEquals("0 sets", countLabel(0, "set"))
        assertEquals("13 sets", countLabel(13, "set"))
    }

    @Test
    fun `weekCountLabel and templateMetaLabel`() {
        assertEquals("3 workouts this week", weekCountLabel(3))
        assertEquals("1 workout this week", weekCountLabel(1))
        assertEquals("4 exercises · 13 sets", templateMetaLabel(4, 13))
        assertEquals("1 exercise · 1 set", templateMetaLabel(1, 1))
    }

    @Test
    fun `lastDoneLabel shows weekday and date, or not done`() {
        assertEquals("Last done Thursday, 8 Oct", lastDoneLabel(thursday, locale))
        assertEquals("Not done yet", lastDoneLabel(null, locale))
    }

    @Test
    fun `lastDoneShortLabel uses today, a weekday within the week, then a date`() {
        assertEquals("last done today", lastDoneShortLabel(thursday, today = thursday, locale = locale))
        assertEquals("last done Thursday", lastDoneShortLabel(thursday, today = thursday.plusDays(6), locale = locale))
        assertEquals("last done 8 Oct", lastDoneShortLabel(thursday, today = thursday.plusDays(7), locale = locale))
        assertEquals("not done yet", lastDoneShortLabel(null, today = thursday, locale = locale))
    }

    @Test
    fun `sessionMetaLabel shows the duration, or just the sets when the start wasn't recorded`() {
        assertEquals("13 sets · 62 min", sessionMetaLabel(13, 62))
        assertEquals("1 set · 1 min", sessionMetaLabel(1, 1))
        assertEquals("1 set · <1 min", sessionMetaLabel(1, 0))
        assertEquals("3 sets", sessionMetaLabel(3, null))
    }

    @Test
    fun `shortWeekday is the abbreviated day name`() {
        assertEquals("Thu", shortWeekday(thursday, locale))
    }

    @Test
    fun `shortMonth is the abbreviated month in capitals`() {
        assertEquals("OCT", shortMonth(thursday, locale))
    }
}
