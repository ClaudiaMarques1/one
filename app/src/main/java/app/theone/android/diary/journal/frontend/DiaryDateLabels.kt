package app.theone.android.diary.journal.frontend

import app.theone.android.diary.journal.backend.DiaryDateKeys
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/** Display labels for diary date keys ("yyyy-MM-dd") in the Journal list and composer header. */
internal object DiaryDateLabels {
    fun fullDateLabel(dateKey: String): String = fullDateLabel(DiaryDateKeys.parseDateKey(dateKey))

    fun fullDateLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    /** "This week" / "Last week" / the month name, bucketed relative to [todayKey]. */
    fun groupLabel(dateKey: String, todayKey: String): String {
        val entry = DiaryDateKeys.parseDateKey(dateKey)
        val entryWeekStart = weekStart(entry)
        val todayWeekStart = weekStart(DiaryDateKeys.parseDateKey(todayKey))
        return when (entryWeekStart) {
            todayWeekStart -> "This week"
            todayWeekStart.minusWeeks(1) -> "Last week"
            else -> entry.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
        }
    }

    // Comparing week-start dates (locale-aware first day) handles year boundaries without a special case.
    private fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek))
}
