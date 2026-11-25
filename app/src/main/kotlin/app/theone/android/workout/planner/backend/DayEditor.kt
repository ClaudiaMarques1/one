package app.theone.android.workout.planner.backend

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** What the day editor shows for a date (handover 5a section 6 / 4a Upcoming). */
enum class EditorKind { PAST, REST, PLANNED, SKIPPED }

/** One day in "Move to another day this week". */
data class MoveDay(val date: LocalDate, val weekday: String, val current: Boolean, val free: Boolean)

data class DayEditorUi(
    val date: LocalDate,
    val kind: EditorKind,
    /** `From your routine`, `Moved from Fri 16 Oct`, `Skipped`, `Rest day`... */
    val status: String,
    val session: PlannedSession? = null,
    /** Past days only: `Missed: Pull day was planned`, or null when nothing was missed. */
    val missedLine: String? = null,
    val moveDays: List<MoveDay> = emptyList(),
    val canReset: Boolean = false,
    /** `Remove` for a workout added on a rest day, `Reset` for any other change. */
    val resetLabel: String = "Reset",
    /** Skipped days: `Skipped Pull day. The split order carries on. Next up is Leg day on Thu 15 Oct.` */
    val skipNote: String = "",
    val canTimeDown: Boolean = false,
    val canTimeUp: Boolean = false
)

/** A row in 4a's Upcoming tab. */
data class UpcomingRow(
    val date: LocalDate,
    val weekday: String,
    val day: Int,
    val title: String,
    /** `18:00`, `18:00 · moved from Fri 16 Oct`, `18:00 · changed`, `18:00 · added`, or `Skipped`. */
    val subtitle: String,
    val skipped: Boolean
)

/** Builds the day editor and the Upcoming rows from the schedule. Pure, like [TrainingSchedule]. */
object DayEditor {
    val EARLIEST: LocalTime = LocalTime.of(5, 0)
    val LATEST: LocalTime = LocalTime.of(23, 0)

    /**
     * [sessions] must cover [date]'s week and the days after it that the skip note looks at;
     * [overrides] are the stored day changes; [isSplit] adds "The split order carries on."
     */
    fun build(
        date: LocalDate,
        today: LocalDate,
        sessions: List<PlannedSession>,
        overrides: List<DayOverride>,
        names: Map<String, String>,
        loggedDates: Set<LocalDate>,
        firstDayOfWeek: DayOfWeek,
        isSplit: Boolean
    ): DayEditorUi {
        val session = sessions.firstOrNull { it.date == date }
        if (date < today) {
            val missed = session?.takeIf { !it.skipped && date !in loggedDates }
            return DayEditorUi(
                date = date,
                kind = EditorKind.PAST,
                status = "",
                session = session,
                missedLine = missed?.let { "Missed: ${name(it, names)} was planned" }
            )
        }
        if (session == null) return DayEditorUi(date = date, kind = EditorKind.REST, status = "Rest day")

        val hasOverride = overrides.any { it.sourceDate == session.sourceDate.toString() }
        val common = DayEditorUi(
            date = date,
            kind = if (session.skipped) EditorKind.SKIPPED else EditorKind.PLANNED,
            status = status(session),
            session = session,
            canReset = hasOverride,
            resetLabel = if (session.added) "Remove" else "Reset"
        )
        if (session.skipped) {
            val next = sessions.firstOrNull { it.date > date && !it.skipped }
            val note = buildString {
                append("Skipped ${name(session, names)}.")
                if (isSplit) append(" The split order carries on.")
                if (next != null) append(" Next up is ${name(next, names)} on ${TrainingPlanText.shortDate(next.date)}.")
            }
            return common.copy(skipNote = note)
        }
        val targets = TrainingSchedule.moveTargets(session, sessions, today, firstDayOfWeek).toSet()
        val weekStart = session.date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        return common.copy(
            moveDays = (0L..6L).map { weekStart.plusDays(it) }.map { day ->
                MoveDay(
                    day,
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    current = day == session.date,
                    free = day in targets
                )
            },
            canTimeDown = session.time > EARLIEST,
            canTimeUp = session.time < LATEST
        )
    }

    /** Status line under "Training plan" for a session (handover order: skipped, moved, added, changed, routine). */
    fun status(session: PlannedSession): String = when {
        session.skipped -> "Skipped"
        session.movedFrom != null -> "Moved from ${TrainingPlanText.shortDate(session.movedFrom)}"
        session.added -> "Added for this day only"
        session.edited -> "Changed for this day only"
        else -> "From your routine"
    }

    /** Sessions from [today] for [days] days, rest days left out (4a Upcoming). */
    fun upcoming(sessions: List<PlannedSession>, names: Map<String, String>, today: LocalDate, days: Long): List<UpcomingRow> =
        sessions.filter { it.date >= today && it.date < today.plusDays(days) }.map { s ->
            UpcomingRow(
                date = s.date,
                weekday = s.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                day = s.date.dayOfMonth,
                title = name(s, names),
                subtitle = if (s.skipped) {
                    "Skipped"
                } else {
                    s.time.toString() + when {
                        s.movedFrom != null -> " · moved from ${TrainingPlanText.shortDate(s.movedFrom)}"
                        s.added -> " · added"
                        s.edited -> " · changed"
                        else -> ""
                    }
                },
                skipped = s.skipped
            )
        }

    /** [current] moved by [minutes], kept inside 05:00-23:00 (no wrap past midnight). */
    fun stepTime(current: LocalTime, minutes: Int): LocalTime {
        val total = (current.hour * 60 + current.minute + minutes).coerceIn(EARLIEST.hour * 60, LATEST.hour * 60)
        return LocalTime.of(total / 60, total % 60)
    }

    private fun name(session: PlannedSession, names: Map<String, String>) = names[session.templateId] ?: "Workout"
}
