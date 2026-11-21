package app.theone.android.workout.sessions.frontend

import app.theone.android.workout.sessions.backend.StrengthSet
import app.theone.android.workout.sessions.backend.formatWeightInput

// Display text for the active workout screen.

/** Elapsed workout time: "04:05", or "1:02:03" from one hour. */
internal fun formatElapsed(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs) else "%02d:%02d".format(minutes, secs)
}

/** Rest time left on the rest bar: "1:24", "0:05". */
internal fun formatRestRemaining(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

/** Rest chip and rest sheet option: "Off" for 0, else "2:00" / "0:30". */
internal fun restLabel(seconds: Int): String = if (seconds <= 0) "Off" else formatRestRemaining(seconds)

/** The Previous column: "60 kg × 8", "BW × 12" for bodyweight, "—" when there's no previous set. */
internal fun previousLabel(set: StrengthSet?): String = when {
    set == null -> "—"
    set.weightKg == 0.0 -> "BW × ${set.reps}"
    else -> "${formatWeightInput(set.weightKg)} kg × ${set.reps}"
}

/** "2 of 13 sets". */
internal fun progressLabel(done: Int, total: Int): String = "$done of $total sets"
