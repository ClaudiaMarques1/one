package app.theone.android.workout.planner.backend

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** A session on the schedule after the day's override is applied. */
data class PlannedSession(
    val date: LocalDate,
    /** The date the routine put it on - the override key. Differs from [date] after a move. */
    val sourceDate: LocalDate,
    val templateId: String,
    val time: LocalTime,
    val skipped: Boolean = false,
    val movedFrom: LocalDate? = null,
    val added: Boolean = false,
    /** Workout or time changed for this day only. */
    val edited: Boolean = false
)

enum class DayState { DONE, MISSED, PLANNED, SKIPPED, REST }

/**
 * Builds the planned sessions from the routine versions and one-off overrides (handover "Building the
 * schedule"). Pure - no Firestore, no clock: callers pass today and the week start.
 */
object TrainingSchedule {
    private val DEFAULT_TIME: LocalTime = LocalTime.of(18, 0)

    /**
     * Sessions dated [from]..[to] (inclusive), sorted by date and time.
     * [defaultTemplateId] is used for a workout added on a rest day without a chosen template.
     */
    fun build(
        routines: List<TrainingRoutine>,
        overrides: List<DayOverride>,
        from: LocalDate,
        to: LocalDate,
        defaultTemplateId: String = ""
    ): List<PlannedSession> {
        if (to < from) return emptyList()
        val versions = routines.mapNotNull { r -> r.effectiveFrom.toDateOrNull()?.let { it to r } }.sortedBy { it.first }
        // Moves stay inside one week, so a session moved into the range can come from up to 6 days outside it.
        val scanFrom = from.minusDays(6)
        val scanTo = to.plusDays(6)
        val base = routineSessions(versions, scanFrom, scanTo).associateBy { it.sourceDate }
        val bySource = overrides.mapNotNull { o -> o.sourceDate.toDateOrNull()?.let { it to o } }.toMap()

        val sessions = mutableListOf<PlannedSession>()
        var day = scanFrom
        while (day <= scanTo) {
            val routine = base[day]
            val override = bySource[day]
            val session = when {
                routine != null && override != null -> routine.withOverride(override)
                routine != null -> routine
                // A workout added on a rest day; any other override without a routine session is stale - dropped.
                override?.added == true -> PlannedSession(
                    date = day,
                    sourceDate = day,
                    templateId = override.templateId ?: defaultTemplateId,
                    time = override.time.toTimeOrNull() ?: DEFAULT_TIME,
                    added = true
                ).withOverride(override.copy(templateId = null, time = null))
                else -> null
            }
            if (session != null) sessions += session
            day = day.plusDays(1)
        }
        return sessions.filter { it.date in from..to }.sortedWith(compareBy({ it.date }, { it.time }))
    }

    /**
     * The day's state for the Calendar (handover "Day states"). A logged workout makes it Done (today too);
     * a past planned session without one is Missed.
     */
    fun dayState(date: LocalDate, today: LocalDate, sessionsOnDate: List<PlannedSession>, hasLoggedWorkout: Boolean): DayState {
        val active = sessionsOnDate.filter { it.date == date }
        return when {
            hasLoggedWorkout && date <= today -> DayState.DONE
            active.any { !it.skipped } -> if (date < today) DayState.MISSED else DayState.PLANNED
            active.isNotEmpty() -> DayState.SKIPPED
            else -> DayState.REST
        }
    }

    /**
     * Days [session] can move to: same week (starting on [firstDayOfWeek]), today or later, and with no
     * other session - skipped ones included, since they still show on their day.
     */
    fun moveTargets(session: PlannedSession, sessions: List<PlannedSession>, today: LocalDate, firstDayOfWeek: DayOfWeek): List<LocalDate> {
        val weekStart = session.date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val busy = sessions.filter { it.sourceDate != session.sourceDate }.map { it.date }.toSet()
        return (0L..6L).map { weekStart.plusDays(it) }.filter { it >= today && it != session.date && it !in busy }
    }

    /** The routine's own sessions on [from]..[to], before overrides; each day uses the latest version started by then. */
    private fun routineSessions(versions: List<Pair<LocalDate, TrainingRoutine>>, from: LocalDate, to: LocalDate): List<PlannedSession> {
        val sessions = mutableListOf<PlannedSession>()
        var day = from
        while (day <= to) {
            val routine = versions.lastOrNull { it.first <= day }?.second
            if (routine != null) routine.sessionOn(day)?.let { sessions += it }
            day = day.plusDays(1)
        }
        return sessions
    }

    private fun TrainingRoutine.sessionOn(day: LocalDate): PlannedSession? = when (mode) {
        PlanMode.WEEKDAY -> weekdayPlan[day.dayOfWeek.name]?.takeIf { it.templateId.isNotBlank() }?.let {
            PlannedSession(day, day, it.templateId, it.time.toTimeOrNull() ?: DEFAULT_TIME)
        }
        PlanMode.SPLIT -> {
            val days = splitDays.mapNotNull { name -> runCatching { DayOfWeek.valueOf(name) }.getOrNull() }.toSet()
            val anchor = splitAnchor.toDateOrNull() ?: effectiveFrom.toDateOrNull()
            if (day.dayOfWeek !in days || splitOrder.isEmpty() || anchor == null || day < anchor) {
                null
            } else {
                // Every training day since the anchor uses up a turn - skipped ones too, so the order carries on.
                val turn = countDays(anchor, day, days)
                PlannedSession(day, day, splitOrder[turn % splitOrder.size], splitTime.toTimeOrNull() ?: DEFAULT_TIME)
            }
        }
    }

    /** Training days in [from] (inclusive) .. [until] (exclusive). */
    private fun countDays(from: LocalDate, until: LocalDate, days: Set<DayOfWeek>): Int {
        val totalDays = until.toEpochDay() - from.toEpochDay()
        val fullWeeks = (totalDays / 7).toInt()
        var count = fullWeeks * days.size
        var day = from.plusWeeks(fullWeeks.toLong())
        while (day < until) {
            if (day.dayOfWeek in days) count++
            day = day.plusDays(1)
        }
        return count
    }

    private fun PlannedSession.withOverride(o: DayOverride): PlannedSession {
        val template = o.templateId?.takeIf { it.isNotBlank() }
        val time = o.time.toTimeOrNull()
        val moveTo = o.moveTo.toDateOrNull()?.takeIf { it != sourceDate }
        return copy(
            date = moveTo ?: date,
            templateId = template ?: templateId,
            time = time ?: this.time,
            skipped = o.skipped,
            movedFrom = if (moveTo != null) sourceDate else null,
            edited = (template != null && template != templateId) || (time != null && time != this.time)
        )
    }

    private fun String?.toDateOrNull(): LocalDate? = this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun String?.toTimeOrNull(): LocalTime? = this?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
}
