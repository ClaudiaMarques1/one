package app.theone.android.settings.backend

import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Settings -> Week starts on (#62): the first day of every week the app shows - Calendar's month grid
 * and week view, Meals' week strip, Week plan and shopping list, Diary's "This week", Workout's
 * weekly count. Stored by name, so don't rename the entries.
 */
enum class WeekStart(val dayOfWeek: DayOfWeek) {
    MONDAY(DayOfWeek.MONDAY),
    SUNDAY(DayOfWeek.SUNDAY);

    /** `java.util.Calendar` numbering (SUNDAY = 1, MONDAY = 2), as Calendar's date keys use. */
    fun toCalendarDay(): Int = if (this == SUNDAY) 1 else 2

    companion object {
        /** Sunday where the locale starts weeks on Sunday (e.g. en-US), Monday otherwise. */
        fun fromLocale(locale: Locale = Locale.getDefault()): WeekStart =
            if (WeekFields.of(locale).firstDayOfWeek == DayOfWeek.SUNDAY) SUNDAY else MONDAY
    }
}
