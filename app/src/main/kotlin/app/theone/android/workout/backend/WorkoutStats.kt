package app.theone.android.workout.backend

import app.theone.android.workout.sessions.backend.StrengthSession
import app.theone.android.workout.templates.backend.WorkoutTemplate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters

// Numbers the Workout home screen derives from templates and session history - no schema change.

private fun StrengthSession.localDate(): LocalDate? =
    try {
        LocalDate.parse(date)
    } catch (e: DateTimeParseException) {
        null
    }

/** Sessions dated from the start of [today]'s week (on [firstDay], Settings -> Week starts on) up to and including [today]. */
fun sessionsThisWeek(sessions: List<StrengthSession>, today: LocalDate, firstDay: DayOfWeek = DayOfWeek.MONDAY): Int {
    val start = today.with(TemporalAdjusters.previousOrSame(firstDay))
    return sessions.count { session -> session.localDate()?.let { it in start..today } == true }
}

/** The most recent session date per template name. */
fun lastDoneByTemplate(sessions: List<StrengthSession>): Map<String, LocalDate> =
    sessions.mapNotNull { session -> session.localDate()?.let { session.templateName to it } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, dates) -> dates.max() }

/** The latest [count] sessions, newest first (date, then creation time for the same day). */
fun recentSessions(sessions: List<StrengthSession>, count: Int = 3): List<StrengthSession> =
    sessions.sortedWith(compareByDescending<StrengthSession> { it.date }.thenByDescending { it.createdAt }).take(count)

fun StrengthSession.setCount(): Int = exercises.sumOf { it.sets.size }

/** Total volume: sum of reps x weightKg over every set. 0 for bodyweight-only sessions. */
fun StrengthSession.volumeKg(): Double = exercises.sumOf { exercise -> exercise.sets.sumOf { it.reps * it.weightKg } }

fun WorkoutTemplate.setCount(): Int = exercises.sumOf { it.sets.size }
