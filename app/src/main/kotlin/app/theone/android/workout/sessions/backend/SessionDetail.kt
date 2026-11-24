package app.theone.android.workout.sessions.backend

import java.time.LocalDate
import java.time.format.DateTimeParseException

// Session detail (3a, #48): a finished session with its PRs and a comparison per exercise against the
// previous session of the same template. Built from the loaded sessions - nothing extra is stored.

/** How an exercise went compared with the previous session of the same template, on [since]. */
sealed interface Comparison {
    val since: LocalDate

    /** The heaviest set's weight changed by [deltaKg]. */
    data class Weight(val deltaKg: Double, override val since: LocalDate) : Comparison

    /** Same top weight; total reps changed by [deltaReps]. */
    data class Reps(val deltaReps: Int, override val since: LocalDate) : Comparison

    data class Same(override val since: LocalDate) : Comparison
}

/** Shown in the "up" colour; a drop stays in the secondary colour (never red). */
val Comparison.isUp: Boolean
    get() = when (this) {
        is Comparison.Weight -> deltaKg > 0
        is Comparison.Reps -> deltaReps > 0
        is Comparison.Same -> false
    }

data class SetLog(val weightKg: Double, val reps: Int, val isPr: Boolean)

data class ExerciseLog(
    val name: String,
    /** From the exercise catalog's equipment ("body weight"), for the `Bodyweight + 10 kg × 10` label. */
    val bodyweight: Boolean,
    val sets: List<SetLog>,
    val comparison: Comparison?
)

data class SessionDetail(
    val session: StrengthSession,
    val date: LocalDate?,
    val exercises: List<ExerciseLog>
) {
    val durationMinutes: Long? get() = session.durationMinutes()
    val setCount: Int get() = exercises.sumOf { it.sets.size }
    val prCount: Int get() = exercises.sumOf { exercise -> exercise.sets.count { it.isPr } }
}

/**
 * Clock minutes from start to end - the same minutes the header's "18:02–19:04" shows, so the two always
 * agree (0 when both fall in the same minute); null when the start wasn't recorded (older sessions).
 */
fun StrengthSession.durationMinutes(): Long? {
    val start = startedAt ?: return null
    if (createdAt < start) return null
    return createdAt / 60_000 - start / 60_000
}

fun StrengthSession.localDate(): LocalDate? =
    try {
        LocalDate.parse(date)
    } catch (e: DateTimeParseException) {
        null
    }

/** Oldest first: by date, then by when it was saved. */
private val chronological = compareBy<StrengthSession>({ it.date }, { it.createdAt })

/**
 * The detail of session [sessionId] among [sessions] (all loaded sessions, any order), or null if it's
 * not there. [bodyweightNames] are the catalog's bodyweight exercises, lower-cased.
 */
fun buildSessionDetail(sessionId: String, sessions: List<StrengthSession>, bodyweightNames: Set<String>): SessionDetail? {
    val session = sessions.firstOrNull { it.id == sessionId } ?: return null
    val earlier = sessions.filter { it.id != session.id && chronological.compare(it, session) < 0 }
    // Best weight x reps so far per exercise, from earlier sessions; grows through this session's sets.
    val best = mutableMapOf<String, Double>()
    val exercises = session.exercises.map { exercise ->
        val history = earlier.flatMap { s -> s.exercises.filter { it.name == exercise.name }.flatMap { it.sets } }
        val hasHistory = history.isNotEmpty() || exercise.name in best
        var top = best[exercise.name] ?: history.maxOfOrNull { it.score() } ?: 0.0
        val sets = exercise.sets.map { set ->
            val score = set.score()
            val isPr = hasHistory && score > 0 && score > top
            if (score > top) top = score
            SetLog(set.weightKg, set.reps, isPr)
        }
        best[exercise.name] = top
        ExerciseLog(
            name = exercise.name,
            bodyweight = exercise.name.lowercase() in bodyweightNames,
            sets = sets,
            comparison = compareWithPrevious(session, exercise, earlier)
        )
    }
    return SessionDetail(session, session.localDate(), exercises)
}

private fun StrengthSet.score(): Double = reps * weightKg

private fun compareWithPrevious(session: StrengthSession, exercise: StrengthExercise, earlier: List<StrengthSession>): Comparison? {
    val previousSession = earlier
        .filter { it.templateName == session.templateName && it.exercises.any { e -> e.name == exercise.name } }
        .maxWithOrNull(chronological) ?: return null
    val since = previousSession.localDate() ?: return null
    val previous = previousSession.exercises.first { it.name == exercise.name }
    if (exercise.sets.isEmpty() || previous.sets.isEmpty()) return null
    val weightDelta = exercise.sets.maxOf { it.weightKg } - previous.sets.maxOf { it.weightKg }
    if (weightDelta != 0.0) return Comparison.Weight(weightDelta, since)
    val repsDelta = exercise.sets.sumOf { it.reps } - previous.sets.sumOf { it.reps }
    return if (repsDelta != 0) Comparison.Reps(repsDelta, since) else Comparison.Same(since)
}
