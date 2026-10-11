package app.theone.android.calendar

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pure date-key ("yyyy-MM-dd", matching the Firestore-backed models' `date` field) arithmetic for
 * the calendar screen. Kept free of ViewModel/repository state so it's directly unit-testable.
 */
internal object CalendarDateUtils {
    fun todayKey(): String = formatDateKey(Calendar.getInstance())

    fun formatDateKey(calendar: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

    fun parseDateKey(dateKey: String): Calendar {
        val (year, month, day) = dateKey.split("-").map { it.toInt() }
        return Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day)
        }
    }

    /** Calendar.SUNDAY (1) .. Calendar.SATURDAY (7), derived from the device locale. */
    fun firstDayOfWeek(locale: Locale = Locale.getDefault()): Int =
        Calendar.getInstance(locale).firstDayOfWeek

    fun addMonths(dateKey: String, delta: Int): String {
        val calendar = parseDateKey(dateKey)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, 1) // avoid rollover (e.g. May 31 + 1 month skipping June)
        calendar.add(Calendar.MONTH, delta)
        calendar.set(Calendar.DAY_OF_MONTH, minOf(day, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)))
        return formatDateKey(calendar)
    }

    fun addDays(dateKey: String, delta: Int): String {
        val calendar = parseDateKey(dateKey)
        calendar.add(Calendar.DAY_OF_MONTH, delta)
        return formatDateKey(calendar)
    }

    fun step(dateKey: String, view: CalendarView, delta: Int): String = when (view) {
        CalendarView.MONTH -> addMonths(dateKey, delta)
        CalendarView.WEEK -> addDays(dateKey, delta * 7)
        CalendarView.DAY -> addDays(dateKey, delta)
    }

    /** The 7 date keys of the week containing [dateKey], starting on [firstDayOfWeek]. */
    fun weekDateKeys(dateKey: String, firstDayOfWeek: Int): List<String> {
        val calendar = parseDateKey(dateKey)
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val offsetFromStart = ((currentDayOfWeek - firstDayOfWeek) + 7) % 7
        calendar.add(Calendar.DAY_OF_MONTH, -offsetFromStart)
        return List(7) { index ->
            formatDateKey(calendar).also { if (index < 6) calendar.add(Calendar.DAY_OF_MONTH, 1) }
        }
    }

    /** Leading `null` blanks (not clickable) followed by one date key per day of the month. */
    fun monthGridCells(year: Int, month: Int, firstDayOfWeek: Int): List<String?> {
        val first = calendarOf(year, month, 1)
        val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val leadingBlanks = ((first.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek) + 7) % 7
        val days = (1..daysInMonth).map { day ->
            formatDateKey(calendarOf(year, month, day))
        }
        return List(leadingBlanks) { null } + days
    }

    /** Single-letter weekday labels in week-start order, e.g. S M T W T F S or M T W T F S S. */
    fun weekdayLetterLabels(firstDayOfWeek: Int): List<String> {
        val sundayFirst = listOf("S", "M", "T", "W", "T", "F", "S")
        return List(7) { index -> sundayFirst[(firstDayOfWeek - 1 + index) % 7] }
    }

    fun shortWeekdayLabel(dateKey: String): String =
        SimpleDateFormat("EEE", Locale.getDefault()).format(parseDateKey(dateKey).time)

    fun monthTitle(dateKey: String): String =
        SimpleDateFormat("MMMM", Locale.getDefault()).format(parseDateKey(dateKey).time)

    fun dayTitle(dateKey: String): String =
        SimpleDateFormat("d MMMM", Locale.getDefault()).format(parseDateKey(dateKey).time)

    fun yearOf(dateKey: String): Int = parseDateKey(dateKey).get(Calendar.YEAR)

    fun monthOf(dateKey: String): Int = parseDateKey(dateKey).get(Calendar.MONTH)

    fun dayOfWeekFullLabel(dateKey: String): String =
        SimpleDateFormat("EEEE", Locale.getDefault()).format(parseDateKey(dateKey).time)

    fun weekRangeLabel(weekKeys: List<String>): String {
        val format = SimpleDateFormat("d MMM", Locale.getDefault())
        val start = format.format(parseDateKey(weekKeys.first()).time)
        val end = format.format(parseDateKey(weekKeys.last()).time)
        return "$start – $end"
    }

    /** Date keys of every day in [year]/[month] up to and including [todayKey] (string-ordered). */
    fun monthKeysUpToToday(year: Int, month: Int, todayKey: String): List<String> {
        val first = calendarOf(year, month, 1)
        val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (1..daysInMonth)
            .map { day -> formatDateKey(calendarOf(year, month, day)) }
            .filter { it <= todayKey }
    }

    private fun calendarOf(year: Int, month: Int, day: Int): Calendar = Calendar.getInstance().apply {
        clear()
        set(year, month, day)
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
}
