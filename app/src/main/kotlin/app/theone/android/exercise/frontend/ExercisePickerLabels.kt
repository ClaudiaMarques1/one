package app.theone.android.exercise.frontend

import app.theone.android.exercise.backend.Exercise
import java.util.Locale

// Display text for the exercise picker.

/** Catalog values are lower case: "upper arms" -> "Upper Arms". */
internal fun String.toDisplayLabel(): String =
    split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.getDefault()) } }

/** Row meta: "Pectorals · Barbell". */
internal fun Exercise.metaLabel(): String = "${target.toDisplayLabel()} · ${equipment.toDisplayLabel()}"

/** Confirm button: "Select exercises" with nothing picked, else "Add 1 exercise" / "Add 3 exercises". */
internal fun confirmLabel(count: Int): String = when (count) {
    0 -> "Select exercises"
    1 -> "Add 1 exercise"
    else -> "Add $count exercises"
}
