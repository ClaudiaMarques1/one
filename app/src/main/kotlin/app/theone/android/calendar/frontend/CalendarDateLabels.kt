package app.theone.android.calendar.frontend

import app.theone.android.calendar.backend.CalendarDateKeys
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Display text for calendar date keys ("yyyy-MM-dd"): month, day and weekday labels. */
internal object CalendarDateLabels {
    /** Single-letter weekday labels in week-start order, e.g. S M T W T F S or M T W T F S S. */
    fun weekdayLetterLabels(firstDayOfWeek: Int): List<String> {
        val sundayFirst = listOf("S", "M", "T", "W", "T", "F", "S")
        return List(7) { index -> sundayFirst[(firstDayOfWeek - 1 + index) % 7] }
    }

    fun shortWeekdayLabel(dateKey: String): String = format(dateKey, "EEE")

    fun monthTitle(dateKey: String): String = format(dateKey, "MMMM")

    fun dayTitle(dateKey: String): String = format(dateKey, "d MMMM")

    fun dayOfWeekFullLabel(dateKey: String): String = format(dateKey, "EEEE")

    fun weekRangeLabel(weekKeys: List<String>): String =
        "${format(weekKeys.first(), "d MMM")} – ${format(weekKeys.last(), "d MMM")}"

    private fun format(dateKey: String, pattern: String): String =
        CalendarDateKeys.parseDateKey(dateKey).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}
