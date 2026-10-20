package app.theone.android.workout.sessions.backend

import app.theone.android.workout.templates.backend.DEFAULT_REST_SECONDS
import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.TemplateSet
import app.theone.android.workout.templates.backend.WorkoutTemplate
import java.io.Serializable
import java.util.UUID
import kotlin.math.ceil

// The in-progress workout. Kept apart from the Firestore models (StrengthSession/StrengthExercise/
// StrengthSet) because it needs state those don't store: what was typed (an empty field isn't 0 kg),
// whether a set is done, each exercise's rest time, and the template's previous sets. Serializable so
// it lives in StrengthViewModel's SavedStateHandle and survives process death.

/** Rest options offered in the rest sheet, in seconds; 0 = Off. */
val restOptionsSeconds: List<Int> = listOf(0, 30, 60, 90, 120, 180)

data class ActiveSet(
    val repsText: String = "",
    val weightText: String = "",
    val done: Boolean = false
) : Serializable

data class ActiveExercise(
    val name: String = "",
    val sets: List<ActiveSet> = emptyList(),
    /** The template's sets from last time, by set index - the Previous column and field placeholders. */
    val previousSets: List<StrengthSet> = emptyList(),
    val restSeconds: Int = DEFAULT_REST_SECONDS,
    /** Stable per instance, so rest times and list keys survive removing or duplicating exercises. */
    val instanceId: String = UUID.randomUUID().toString()
) : Serializable {
    fun previous(setIndex: Int): StrengthSet? = previousSets.getOrNull(setIndex)
}

data class RestState(
    val exerciseName: String,
    val totalSeconds: Int,
    val endsAtMillis: Long
) : Serializable

/** What Finish saves when some sets aren't ticked. */
enum class FinishMode { DONE_SETS_ONLY, ALL_SETS }

fun parseReps(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it >= 0 }

/** Accepts a comma as the decimal separator, like "62,5". */
fun parseWeightKg(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }

/** Text for a weight field: "60" for whole numbers, "62.5" otherwise. */
fun formatWeightInput(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) weightKg.toLong().toString() else weightKg.toString()

/** One set row per template set, empty and not done - the previous values show as placeholders. */
fun WorkoutTemplate.toActiveExercises(newId: () -> String = { UUID.randomUUID().toString() }): List<ActiveExercise> =
    exercises.map { exercise ->
        val previous = exercise.sets.map { StrengthSet(reps = it.reps, weightKg = it.weightKg) }
        ActiveExercise(
            name = exercise.name,
            sets = List(previous.size) { ActiveSet() },
            previousSets = previous,
            restSeconds = exercise.restSeconds,
            instanceId = newId()
        )
    }

/** Ticking a set: toggles done; when marking done, empty fields are filled from the previous set. */
fun ActiveExercise.toggleSetDone(setIndex: Int): ActiveExercise {
    val set = sets.getOrNull(setIndex) ?: return this
    val toggled = if (set.done) {
        set.copy(done = false)
    } else {
        val previous = previous(setIndex)
        set.copy(
            done = true,
            repsText = set.repsText.ifBlank { previous?.reps?.toString().orEmpty() },
            weightText = set.weightText.ifBlank { previous?.weightKg?.let(::formatWeightInput).orEmpty() }
        )
    }
    return copy(sets = sets.toMutableList().also { it[setIndex] = toggled })
}

/** "Add set": copies the last set's kg and reps, not done. */
fun ActiveExercise.withAddedSet(): ActiveExercise {
    val last = sets.lastOrNull()
    return copy(sets = sets + ActiveSet(repsText = last?.repsText.orEmpty(), weightText = last?.weightText.orEmpty()))
}

fun List<ActiveExercise>.doneSetCount(): Int = sumOf { exercise -> exercise.sets.count { it.done } }

fun List<ActiveExercise>.totalSetCount(): Int = sumOf { it.sets.size }

fun List<ActiveExercise>.hasUnfinishedSets(): Boolean = any { exercise -> exercise.sets.any { !it.done } }

/** The sets Finish keeps for one exercise, as saved values (an empty field falls back to the previous set, then 0). */
fun ActiveExercise.setsToSave(mode: FinishMode): List<StrengthSet> =
    sets.mapIndexedNotNull { index, set ->
        if (mode == FinishMode.DONE_SETS_ONLY && !set.done) return@mapIndexedNotNull null
        val previous = previous(index)
        StrengthSet(
            reps = parseReps(set.repsText) ?: previous?.reps ?: 0,
            weightKg = parseWeightKg(set.weightText) ?: previous?.weightKg ?: 0.0
        )
    }

/** The saved session's exercises: exercises left with no sets are dropped. */
fun List<ActiveExercise>.toSessionExercises(mode: FinishMode): List<StrengthExercise> =
    mapNotNull { exercise ->
        exercise.setsToSave(mode).takeIf { it.isNotEmpty() }?.let { StrengthExercise(name = exercise.name, sets = it) }
    }

/**
 * What finishing writes back to the template: every exercise in the workout, in order, with the sets
 * just logged - or its previous sets if nothing was saved for it - and its rest time.
 */
fun List<ActiveExercise>.toTemplateExercises(mode: FinishMode): List<TemplateExercise> =
    map { exercise ->
        val sets = exercise.setsToSave(mode).ifEmpty { exercise.previousSets }
        TemplateExercise(
            name = exercise.name,
            sets = sets.map { TemplateSet(it.reps, it.weightKg) },
            restSeconds = exercise.restSeconds
        )
    }

/** Whole seconds left, rounded up so the bar shows 0:01 rather than 0:00 in the last second. */
fun RestState.remainingSeconds(nowMillis: Long): Int =
    ceil((endsAtMillis - nowMillis).coerceAtLeast(0) / 1000.0).toInt()

/** Share of the rest already elapsed, 0..1, for the progress line. */
fun RestState.elapsedFraction(nowMillis: Long): Float =
    if (totalSeconds <= 0) 1f else ((totalSeconds * 1000L - (endsAtMillis - nowMillis)) / (totalSeconds * 1000f)).coerceIn(0f, 1f)

/**
 * The rest bar's -30 / +30. -30 with 30s or less left ends the rest (null). +30 extends the time
 * left and raises the total if the time left would exceed it.
 */
fun RestState.adjusted(deltaSeconds: Int, nowMillis: Long): RestState? {
    val left = remainingSeconds(nowMillis)
    if (deltaSeconds < 0 && left <= -deltaSeconds) return null
    val newLeft = left + deltaSeconds
    return copy(totalSeconds = maxOf(totalSeconds, newLeft), endsAtMillis = nowMillis + newLeft * 1000L)
}
