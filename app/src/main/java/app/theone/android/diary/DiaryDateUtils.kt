package app.theone.android.diary

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Pure date-key ("yyyy-MM-dd") helpers for the Journal list, kept free of ViewModel state. */
internal object DiaryDateUtils {
    fun todayKey(): String = formatDateKey(Calendar.getInstance())

    fun formatDateKey(calendar: Calendar): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

    fun parseDateKey(dateKey: String): Calendar {
        val (year, month, day) = dateKey.split("-").map { it.toInt() }
        return Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day)
        }
    }

    fun fullDateLabel(dateKey: String): String =
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(parseDateKey(dateKey).time)

    /** "This week" / "Last week" / the month name, bucketed relative to [todayKey]. */
    fun groupLabel(dateKey: String, todayKey: String): String {
        val entry = parseDateKey(dateKey)
        val today = parseDateKey(todayKey)
        val entryWeek = entry.get(Calendar.WEEK_OF_YEAR)
        val entryYear = entry.get(Calendar.YEAR)
        val todayWeek = today.get(Calendar.WEEK_OF_YEAR)
        val todayYear = today.get(Calendar.YEAR)
        return when {
            entryYear == todayYear && entryWeek == todayWeek -> "This week"
            entryYear == todayYear && entryWeek == todayWeek - 1 -> "Last week"
            entryYear == todayYear - 1 && todayWeek == 1 && entryWeek >= 52 -> "Last week"
            else -> SimpleDateFormat("MMMM", Locale.getDefault()).format(entry.time)
        }
    }
}
