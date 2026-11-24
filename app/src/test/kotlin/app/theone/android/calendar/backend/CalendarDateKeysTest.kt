package app.theone.android.calendar.backend

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class CalendarDateKeysTest {
    @Test
    fun `addMonths keeps day-of-month when the target month is long enough`() {
        assertEquals("2026-02-15", CalendarDateKeys.addMonths("2026-01-15", delta = 1))
    }

    @Test
    fun `addMonths clamps day-of-month to a shorter month`() {
        assertEquals("2023-02-28", CalendarDateKeys.addMonths("2023-01-31", delta = 1))
    }

    @Test
    fun `addMonths clamps to the 29th in a leap-year February`() {
        assertEquals("2024-02-29", CalendarDateKeys.addMonths("2024-01-31", delta = 1))
    }

    @Test
    fun `addMonths supports stepping backwards`() {
        assertEquals("2026-02-15", CalendarDateKeys.addMonths("2026-03-15", delta = -1))
    }

    @Test
    fun `addDays steps by the given number of days`() {
        assertEquals("2026-10-17", CalendarDateKeys.addDays("2026-10-10", delta = 7))
        assertEquals("2026-10-03", CalendarDateKeys.addDays("2026-10-10", delta = -7))
    }

    @Test
    fun `addDays rolls over a month boundary`() {
        assertEquals("2026-11-01", CalendarDateKeys.addDays("2026-10-31", delta = 1))
    }

    @Test
    fun `step dispatches by the active view`() {
        assertEquals("2026-11-10", CalendarDateKeys.step("2026-10-10", CalendarView.MONTH, delta = 1))
        assertEquals("2026-10-17", CalendarDateKeys.step("2026-10-10", CalendarView.WEEK, delta = 1))
        assertEquals("2026-10-11", CalendarDateKeys.step("2026-10-10", CalendarView.DAY, delta = 1))
    }

    @Test
    fun `weekDateKeys starting on Sunday covers the week containing a Monday`() {
        // 2024-01-01 is a Monday.
        val week = CalendarDateKeys.weekDateKeys("2024-01-01", firstDayOfWeek = Calendar.SUNDAY)
        assertEquals(
            listOf("2023-12-31", "2024-01-01", "2024-01-02", "2024-01-03", "2024-01-04", "2024-01-05", "2024-01-06"),
            week
        )
    }

    @Test
    fun `weekDateKeys starting on Monday covers the week containing a Monday`() {
        val week = CalendarDateKeys.weekDateKeys("2024-01-01", firstDayOfWeek = Calendar.MONDAY)
        assertEquals(
            listOf("2024-01-01", "2024-01-02", "2024-01-03", "2024-01-04", "2024-01-05", "2024-01-06", "2024-01-07"),
            week
        )
    }

    @Test
    fun `monthGridCells leads with blanks up to the first weekday, Sunday-first`() {
        // 2024-02-01 is a Thursday.
        val cells = CalendarDateKeys.monthGridCells(year = 2024, month = 1, firstDayOfWeek = Calendar.SUNDAY)
        assertEquals(33, cells.size) // 4 leading blanks + 29 days (2024 is a leap year)
        assertEquals(listOf(null, null, null, null), cells.take(4))
        assertEquals("2024-02-01", cells[4])
        assertEquals("2024-02-29", cells.last())
    }

    @Test
    fun `monthGridCells leads with fewer blanks, Monday-first`() {
        val cells = CalendarDateKeys.monthGridCells(year = 2024, month = 1, firstDayOfWeek = Calendar.MONDAY)
        assertEquals(32, cells.size) // 3 leading blanks + 29 days
        assertEquals(listOf(null, null, null), cells.take(3))
        assertEquals("2024-02-01", cells[3])
    }

    @Test
    fun `mealsRange is the whole month padded by 7 days each side`() {
        assertEquals("2026-09-24" to "2026-11-07", CalendarDateKeys.mealsRange("2026-10-10"))
        assertEquals("2026-09-24" to "2026-11-07", CalendarDateKeys.mealsRange("2026-10-31"))
        assertEquals("2024-01-25" to "2024-03-07", CalendarDateKeys.mealsRange("2024-02-29"))
    }
}
