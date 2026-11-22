package app.theone.android.meals.frontend

import app.theone.android.meals.backend.DayState
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Display text for the Meals screens (meals handoff "Shared patterns"). Numbers use en-GB grouping
// ("2,500") whatever the device locale, as the handoff specifies.

private val Grouping: NumberFormat = NumberFormat.getIntegerInstance(Locale.UK)

/** The header title for [date]: "Today", "Tomorrow", "Yesterday", or the weekday ("Friday"). */
internal fun dayTitle(date: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    today.minusDays(1) -> "Yesterday"
    else -> date.format(DateTimeFormatter.ofPattern("EEEE", locale))
}

/**
 * The line under [dayTitle]: "Thursday, 19 Nov" under Today / Tomorrow / Yesterday, but just "24 Nov"
 * further out, where the title is already the weekday.
 */
internal fun dayDateLabel(date: LocalDate, today: LocalDate, locale: Locale = Locale.getDefault()): String {
    val nearby = date in today.minusDays(1)..today.plusDays(1)
    return date.format(DateTimeFormatter.ofPattern(if (nearby) "EEEE, d MMM" else "d MMM", locale))
}

/** "2,500". */
internal fun formatKcal(kcal: Int): String = Grouping.format(kcal)

/** "640 kcal · P 48 · C 70 · F 16". */
internal fun macroLine(kcal: Int, proteinG: Int, carbsG: Int, fatG: Int): String =
    "${formatKcal(kcal)} kcal · P $proteinG · C $carbsG · F $fatG"

/** Litres for a glass count (0.25 L each), without trailing zeros: 5 -> "1.25", 4 -> "1", 10 -> "2.5". */
internal fun litres(glasses: Int): String =
    BigDecimal(glasses).multiply(BigDecimal("0.25")).stripTrailingZeros().toPlainString()

/** "1.25 of 2.5 L". */
internal fun waterLabel(glasses: Int, targetGlasses: Int): String = "${litres(glasses)} of ${litres(targetGlasses)} L"

/** Footer of the Add sheet: "Pick a meal", "Add 2 to Snacks", or "Add to Snacks" on the New tab. */
internal fun addButtonLabel(slotLabel: String, picks: Int, newTab: Boolean): String = when {
    newTab -> "Add to $slotLabel"
    picks == 0 -> "Pick a meal"
    else -> "Add $picks to $slotLabel"
}

/** A kcal/gram field from the New tab: blank counts as 0, anything else must be a whole number >= 0. */
internal fun parseAmount(text: String): Int? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0
    return trimmed.toIntOrNull()?.takeIf { it >= 0 }
}

/** A slot or workout time: "08:00" in 24-hour locales, "8:00 AM" in 12-hour ones (pass DateFormat.is24HourFormat). */
internal fun formatMinutes(minutes: Int, is24Hour: Boolean, locale: Locale = Locale.getDefault()): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", locale))

/** Under the big "kcal left" number: "1,060 eaten of 2,500 · 900 more planned". */
internal fun timelineSummary(eatenKcal: Int, targetKcal: Int, morePlannedKcal: Int): String =
    "${formatKcal(eatenKcal)} eaten of ${formatKcal(targetKcal)} · ${formatKcal(morePlannedKcal)} more planned"

/** A macro tile: "108 g left" (never negative). */
internal fun gramsLeft(eaten: Int, target: Int): String = "${(target - eaten).coerceAtLeast(0)} g left"

/** The timeline header: "November". */
internal fun monthLabel(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofPattern("LLLL", locale))

/** Week strip weekday: "Sun". */
internal fun shortWeekday(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofPattern("EEE", locale))

/** An empty slot on the timeline: "Plan post-workout". */
internal fun planSlotLabel(slotLabel: String): String = "Plan ${slotLabel.lowercase()}"

/** Under the Rest day switch: "Workout logged · training-day targets · 2,500 kcal" when locked. */
internal fun restDaySubtitle(rest: Boolean, workoutLogged: Boolean, kcal: String): String = when {
    workoutLogged -> "Workout logged · training-day targets · $kcal kcal"
    rest -> "Rest-day targets · $kcal kcal"
    else -> "Training-day targets · $kcal kcal"
}

/** Week plan range: "15 - 21 Nov", or "29 Nov - 5 Dec" across months (plain hyphen, repo style). */
internal fun weekRangeLabel(first: LocalDate, last: LocalDate, locale: Locale = Locale.getDefault()): String {
    val month = DateTimeFormatter.ofPattern("MMM", locale)
    return if (first.month == last.month) {
        "${first.dayOfMonth} - ${last.dayOfMonth} ${last.format(month)}"
    } else {
        "${first.dayOfMonth} ${first.format(month)} - ${last.dayOfMonth} ${last.format(month)}"
    }
}

/** Week plan row summary: past = what was eaten, today = both, future = what's planned. */
internal fun weekDaySummary(state: DayState, eatenKcal: Int, plannedKcal: Int, targetKcal: Int): String = when (state) {
    DayState.PAST -> "${formatKcal(eatenKcal)} of ${formatKcal(targetKcal)} kcal"
    DayState.TODAY -> "${formatKcal(eatenKcal)} eaten · ${formatKcal(plannedKcal)} planned"
    DayState.FUTURE -> if (plannedKcal > 0) "${formatKcal(plannedKcal)} kcal planned" else "Nothing planned"
}
