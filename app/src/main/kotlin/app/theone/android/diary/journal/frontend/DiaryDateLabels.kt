package app.theone.android.diary.journal.frontend

import app.theone.android.diary.journal.backend.DiaryDateKeys
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Display labels for diary date keys ("yyyy-MM-dd") in the Journal list and composer header. */
internal object DiaryDateLabels {
    fun fullDateLabel(dateKey: String): String = fullDateLabel(DiaryDateKeys.parseDateKey(dateKey))

    fun fullDateLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    /**
     * "This week" / "Last week" / the month name, bucketed relative to [todayKey], with weeks starting
     * on [firstDay] (Settings -> General -> Week starts on).
     */
    fun groupLabel(dateKey: String, todayKey: String, firstDay: DayOfWeek = DayOfWeek.MONDAY): String {
        val entry = DiaryDateKeys.parseDateKey(dateKey)
        val entryWeekStart = weekStart(entry, firstDay)
        val todayWeekStart = weekStart(DiaryDateKeys.parseDateKey(todayKey), firstDay)
        return when (entryWeekStart) {
            todayWeekStart -> "This week"
            todayWeekStart.minusWeeks(1) -> "Last week"
            else -> entry.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
        }
    }

    // Comparing week-start dates handles year boundaries without a special case.
    private fun weekStart(date: LocalDate, firstDay: DayOfWeek): LocalDate = date.with(TemporalAdjusters.previousOrSame(firstDay))
}
