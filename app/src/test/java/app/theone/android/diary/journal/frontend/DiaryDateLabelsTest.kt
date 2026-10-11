package app.theone.android.diary.journal.frontend

import app.theone.android.diary.journal.backend.DiaryDateKeys
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.format.DateTimeFormatter
import java.util.Locale

class DiaryDateLabelsTest {
    private val today = "2026-06-15" // a date safely away from any year boundary

    private fun shiftedKey(days: Int): String {
        return DiaryDateKeys.formatDateKey(DiaryDateKeys.parseDateKey(today).plusDays(days.toLong()))
    }

    @Test
    fun `fullDateLabel formats as weekday, day month`() {
        assertEquals("Monday, 15 June", DiaryDateLabels.fullDateLabel("2026-06-15"))
    }

    @Test
    fun `groupLabel buckets the same date as This week`() {
        assertEquals("This week", DiaryDateLabels.groupLabel(today, today))
    }

    @Test
    fun `groupLabel buckets exactly one week earlier as Last week`() {
        assertEquals("Last week", DiaryDateLabels.groupLabel(shiftedKey(-7), today))
    }

    @Test
    fun `groupLabel buckets exactly one week later as Last week relative to that later today`() {
        // Symmetric check: from the later date's point of view, the earlier date is one week back.
        assertEquals("Last week", DiaryDateLabels.groupLabel(today, shiftedKey(7)))
    }

    @Test
    fun `groupLabel treats the same week across New Year as This week`() {
        // 2027-01-01 is a Friday; 2026-12-29 (Tuesday) is in its week whether weeks start Sunday or Monday.
        assertEquals("This week", DiaryDateLabels.groupLabel("2026-12-29", "2027-01-01"))
    }

    @Test
    fun `groupLabel treats the previous week across New Year as Last week`() {
        assertEquals("Last week", DiaryDateLabels.groupLabel("2026-12-22", "2027-01-01"))
    }

    @Test
    fun `groupLabel buckets an older date by month name`() {
        val tenWeeksAgo = shiftedKey(-70)
        val expectedMonth = DiaryDateKeys.parseDateKey(tenWeeksAgo).format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
        assertEquals(expectedMonth, DiaryDateLabels.groupLabel(tenWeeksAgo, today))
    }
}
