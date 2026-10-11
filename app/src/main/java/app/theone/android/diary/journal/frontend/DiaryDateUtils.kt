package app.theone.android.diary.journal.frontend

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/** Pure date-key ("yyyy-MM-dd") helpers for the Journal list, kept free of ViewModel state. */
internal object DiaryDateUtils {
    fun todayKey(): String = formatDateKey(LocalDate.now())

    /** ISO-8601 local date, i.e. "yyyy-MM-dd". */
    fun formatDateKey(date: LocalDate): String = date.toString()

    fun parseDateKey(dateKey: String): LocalDate = LocalDate.parse(dateKey)

    fun fullDateLabel(dateKey: String): String = fullDateLabel(parseDateKey(dateKey))

    fun fullDateLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    /** "This week" / "Last week" / the month name, bucketed relative to [todayKey]. */
    fun groupLabel(dateKey: String, todayKey: String): String {
        val entry = parseDateKey(dateKey)
        val entryWeekStart = weekStart(entry)
        val todayWeekStart = weekStart(parseDateKey(todayKey))
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
