package app.theone.android.workout.sessions.frontend

import app.theone.android.workout.sessions.backend.Comparison
import app.theone.android.workout.sessions.backend.SetLog
import app.theone.android.workout.sessions.backend.formatWeightInput
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

// Display text for the session detail screen (3a, #48).

private const val MINUS = "−"

/**
 * Header subtitle: "Thursday, 8 Oct 2026 · 18:02–19:04" (start and end); just the date for older
 * sessions that didn't record their start.
 */
internal fun sessionSubtitle(
    date: LocalDate?,
    startedAt: Long?,
    endedAt: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String {
    val day = date?.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", locale)).orEmpty()
    if (startedAt == null) return day
    val time = DateTimeFormatter.ofPattern("HH:mm", locale)
    val start = Instant.ofEpochMilli(startedAt).atZone(zone).format(time)
    val end = Instant.ofEpochMilli(endedAt).atZone(zone).format(time)
    return if (day.isEmpty()) "$start–$end" else "$day · $start–$end"
}

/** Duration tile: "62 min", or "—" when the start wasn't recorded. */
internal fun durationLabel(minutes: Long?): String = if (minutes == null) "—" else minutesLabel(minutes)

/** "62 min"; "<1 min" when start and end fall in the same clock minute. Also the Recent row's meta. */
internal fun minutesLabel(minutes: Long): String = if (minutes < 1) "<1 min" else "$minutes min"

/** "+5 kg vs 1 Oct", "−2.5 kg vs 1 Oct", "+1 rep vs 1 Oct", "−2 reps vs 1 Oct", "Same as 1 Oct". */
internal fun comparisonLabel(comparison: Comparison, locale: Locale = Locale.getDefault()): String {
    val since = comparison.since.format(DateTimeFormatter.ofPattern("d MMM", locale))
    return when (comparison) {
        is Comparison.Weight -> "${sign(comparison.deltaKg > 0)}${formatWeightInput(comparison.deltaKg.absoluteValue)} kg vs $since"
        is Comparison.Reps -> {
            val reps = comparison.deltaReps.absoluteValue
            "${sign(comparison.deltaReps > 0)}$reps ${if (reps == 1) "rep" else "reps"} vs $since"
        }
        is Comparison.Same -> "Same as $since"
    }
}

private fun sign(up: Boolean) = if (up) "+" else MINUS

/** A set row's load: "80 kg × 6"; bodyweight exercises "Bodyweight × 10" / "Bodyweight + 10 kg × 10". */
internal fun loadLabel(set: SetLog, bodyweight: Boolean): String {
    val kg = formatWeightInput(set.weightKg)
    return when {
        !bodyweight -> "$kg kg × ${set.reps}"
        set.weightKg == 0.0 -> "Bodyweight × ${set.reps}"
        else -> "Bodyweight + $kg kg × ${set.reps}"
    }
}
