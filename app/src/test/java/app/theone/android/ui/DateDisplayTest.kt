package app.theone.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DateDisplayTest {
    @Test
    fun `formats an ISO date key as dd-MM-yyyy`() {
        assertEquals("11-10-2026", formatDisplayDate("2026-10-11"))
    }

    @Test
    fun `pads single-digit day and month`() {
        assertEquals("01-02-2024", formatDisplayDate("2024-02-01"))
    }

    @Test
    fun `returns the input unchanged when it isn't an ISO date`() {
        assertEquals("", formatDisplayDate(""))
        assertEquals("not a date", formatDisplayDate("not a date"))
    }
}
