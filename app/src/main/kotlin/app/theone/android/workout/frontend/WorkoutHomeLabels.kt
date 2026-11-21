package app.theone.android.workout.frontend

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToLong

// Display text for the Workout home screen (1a) and the start sheet (1b).

/** "1 exercise" / "4 exercises". */
internal fun countLabel(count: Int, noun: String): String = if (count == 1) "1 $noun" else "$count ${noun}s"

/** Home subtitle: "3 workouts this week". */
internal fun weekCountLabel(count: Int): String = "${countLabel(count, "workout")} this week"

/** "4 exercises · 13 sets". */
internal fun templateMetaLabel(exerciseCount: Int, setCount: Int): String =
    "${countLabel(exerciseCount, "exercise")} · ${countLabel(setCount, "set")}"

/** Template card footer: "Last done Thursday, 8 Oct" or "Not done yet". */
internal fun lastDoneLabel(date: LocalDate?, locale: Locale = Locale.getDefault()): String =
    if (date == null) "Not done yet" else "Last done ${date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", locale))}"

/**
 * Start sheet meta: "last done today", "last done Thursday" within the past week, else
 * "last done 8 Oct" - a bare weekday would be ambiguous further back.
 */
internal fun lastDoneShortLabel(date: LocalDate?, today: LocalDate, locale: Locale = Locale.getDefault()): String {
    if (date == null) return "not done yet"
    val daysAgo = ChronoUnit.DAYS.between(date, today)
    val text = when {
        daysAgo == 0L -> "today"
        daysAgo in 1..6 -> date.format(DateTimeFormatter.ofPattern("EEEE", locale))
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", locale))
    }
    return "last done $text"
}

/** Recent row meta: "13 sets · 4,560 kg"; bodyweight-only sessions leave out the kg. */
internal fun sessionMetaLabel(setCount: Int, volumeKg: Double, locale: Locale = Locale.getDefault()): String {
    val sets = countLabel(setCount, "set")
    val volume = volumeKg.roundToLong()
    return if (volume <= 0) sets else "$sets · ${NumberFormat.getIntegerInstance(locale).format(volume)} kg"
}

/** Recent row date block: "Thu" above "8". */
internal fun shortWeekday(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofPattern("EEE", locale))
