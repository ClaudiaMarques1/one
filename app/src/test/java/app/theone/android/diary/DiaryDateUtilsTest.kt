package app.theone.android.diary

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DiaryDateUtilsTest {
    private val today = "2026-06-15" // a date safely away from any year boundary

    private fun shiftedKey(days: Int): String {
        val calendar = DiaryDateUtils.parseDateKey(today)
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return DiaryDateUtils.formatDateKey(calendar)
    }

    @Test
    fun `fullDateLabel formats as weekday, day month`() {
        assertEquals("Monday, 15 June", DiaryDateUtils.fullDateLabel("2026-06-15"))
    }

    @Test
    fun `groupLabel buckets the same date as This week`() {
        assertEquals("This week", DiaryDateUtils.groupLabel(today, today))
    }

    @Test
    fun `groupLabel buckets exactly one week earlier as Last week`() {
        assertEquals("Last week", DiaryDateUtils.groupLabel(shiftedKey(-7), today))
    }

    @Test
    fun `groupLabel buckets exactly one week later as Last week relative to that later today`() {
        // Symmetric check: from the later date's point of view, the earlier date is one week back.
        assertEquals("Last week", DiaryDateUtils.groupLabel(today, shiftedKey(7)))
    }

    @Test
    fun `groupLabel buckets an older date by month name`() {
        val tenWeeksAgo = shiftedKey(-70)
        val expectedMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(DiaryDateUtils.parseDateKey(tenWeeksAgo).time)
        assertEquals(expectedMonth, DiaryDateUtils.groupLabel(tenWeeksAgo, today))
    }
}
