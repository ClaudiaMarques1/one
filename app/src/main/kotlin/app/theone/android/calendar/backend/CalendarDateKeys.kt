package app.theone.android.calendar.backend

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure date-key ("yyyy-MM-dd", matching the Firestore-backed models' `date` field) arithmetic for
 * the calendar. Kept free of ViewModel/repository state so it's directly unit-testable.
 *
 * Public weekday ints use `java.util.Calendar` numbering (SUNDAY = 1 .. SATURDAY = 7) and months are
 * 0-based, as before the java.time migration, so callers are unchanged.
 */
internal object CalendarDateKeys {
    fun todayKey(): String = formatDateKey(LocalDate.now())

    /** ISO-8601 local date, i.e. "yyyy-MM-dd". */
    fun formatDateKey(date: LocalDate): String = date.toString()

    fun parseDateKey(dateKey: String): LocalDate = LocalDate.parse(dateKey)

    // LocalDate.plusMonths clamps to the target month's last day (May 31 + 1 month = June 30).
    fun addMonths(dateKey: String, delta: Int): String =
        formatDateKey(parseDateKey(dateKey).plusMonths(delta.toLong()))

    fun addDays(dateKey: String, delta: Int): String =
        formatDateKey(parseDateKey(dateKey).plusDays(delta.toLong()))

    fun step(dateKey: String, view: CalendarView, delta: Int): String = when (view) {
        CalendarView.MONTH -> addMonths(dateKey, delta)
        CalendarView.WEEK -> addDays(dateKey, delta * 7)
        CalendarView.DAY -> addDays(dateKey, delta)
    }

    /** The 7 date keys of the week containing [dateKey], starting on [firstDayOfWeek]. */
    fun weekDateKeys(dateKey: String, firstDayOfWeek: Int): List<String> {
        val date = parseDateKey(dateKey)
        val offsetFromStart = ((date.dayOfWeek.toCalendarDay() - firstDayOfWeek) + 7) % 7
        val start = date.minusDays(offsetFromStart.toLong())
        return List(7) { index -> formatDateKey(start.plusDays(index.toLong())) }
    }

    /** Leading `null` blanks (not clickable) followed by one date key per day of the month. */
    fun monthGridCells(year: Int, month: Int, firstDayOfWeek: Int): List<String?> {
        val yearMonth = YearMonth.of(year, month + 1)
        val leadingBlanks = ((yearMonth.atDay(1).dayOfWeek.toCalendarDay() - firstDayOfWeek) + 7) % 7
        val days = (1..yearMonth.lengthOfMonth()).map { day -> formatDateKey(yearMonth.atDay(day)) }
        return List(leadingBlanks) { null } + days
    }

    fun yearOf(dateKey: String): Int = parseDateKey(dateKey).year

    /** 0-based (January = 0), matching `java.util.Calendar.MONTH`. */
    fun monthOf(dateKey: String): Int = parseDateKey(dateKey).monthValue - 1

    /** Date keys of every day in [year]/[month] up to and including [todayKey] (string-ordered). */
    fun monthKeysUpToToday(year: Int, month: Int, todayKey: String): List<String> {
        val yearMonth = YearMonth.of(year, month + 1)
        return (1..yearMonth.lengthOfMonth())
            .map { day -> formatDateKey(yearMonth.atDay(day)) }
            .filter { it <= todayKey }
    }

    fun longestActiveStreak(orderedDateKeys: List<String>, isActive: (String) -> Boolean): Int {
        var longest = 0
        var current = 0
        orderedDateKeys.forEach { key ->
            current = if (isActive(key)) current + 1 else 0
            longest = maxOf(longest, current)
        }
        return longest
    }

    // DayOfWeek is MONDAY = 1 .. SUNDAY = 7; Calendar is SUNDAY = 1 .. SATURDAY = 7.
    private fun DayOfWeek.toCalendarDay(): Int = value % 7 + 1
}
