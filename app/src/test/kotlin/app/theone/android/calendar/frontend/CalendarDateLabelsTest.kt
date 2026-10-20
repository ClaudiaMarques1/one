package app.theone.android.calendar.frontend

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class CalendarDateLabelsTest {
    @Test
    fun `weekdayLetterLabels orders single letters from the week start`() {
        assertEquals(listOf("S", "M", "T", "W", "T", "F", "S"), CalendarDateLabels.weekdayLetterLabels(Calendar.SUNDAY))
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), CalendarDateLabels.weekdayLetterLabels(Calendar.MONDAY))
    }
}
