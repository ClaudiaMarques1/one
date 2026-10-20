package app.theone.android.diary.journal.backend

// Edits the diary entry composer applies when the user taps a body part or the sleep stepper.

/** 0 (none) -> 1 (Mild) -> 2 (Sore) -> 3 (Very sore) -> 0. */
internal fun cycleSorenessLevel(soreness: Map<String, Int>, partId: String): Map<String, Int> {
    val next = ((soreness[partId] ?: 0) + 1) % 4
    return if (next == 0) soreness - partId else soreness + (partId to next)
}

internal fun clampSleepHours(value: Int): Int = value.coerceIn(0, 14)

internal fun sorenessCount(soreness: Map<String, Int>, ids: List<String>): Int = ids.count { soreness.containsKey(it) }
