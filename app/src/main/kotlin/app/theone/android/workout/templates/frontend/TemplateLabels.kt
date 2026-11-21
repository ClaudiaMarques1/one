package app.theone.android.workout.templates.frontend

import app.theone.android.workout.sessions.backend.formatWeightInput
import app.theone.android.workout.templates.backend.TemplateSet

// Display text for a template's exercises - shared by the start sheet (1b) and the builder (1d).

/**
 * Last time's sets in one line: "3 × 10 · 35 kg" when every set is the same, otherwise
 * "4 sets · 8, 8, 6, 6 reps · 60–62.5 kg". "bodyweight" replaces the kg part when the weight is 0.
 */
internal fun setSummary(sets: List<TemplateSet>): String {
    if (sets.isEmpty()) return "No sets yet"
    val first = sets.first()
    if (sets.all { it == first }) return "${sets.size} × ${first.reps} · ${weightLabel(first.weightKg, first.weightKg)}"
    val min = sets.minOf { it.weightKg }
    val max = sets.maxOf { it.weightKg }
    return "${sets.size} sets · ${sets.joinToString(", ") { it.reps.toString() }} reps · ${weightLabel(min, max)}"
}

private fun weightLabel(min: Double, max: Double): String = when {
    max == 0.0 -> "bodyweight"
    min == max -> "${formatWeightInput(min)} kg"
    else -> "${formatWeightInput(min)}–${formatWeightInput(max)} kg"
}
