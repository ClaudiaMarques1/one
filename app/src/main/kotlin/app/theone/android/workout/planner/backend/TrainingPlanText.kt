package app.theone.android.workout.planner.backend

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** One bar in the "Training this week" card, left to right: done, then planned, then empty up to the target. */
enum class WeekPip { DONE, PLANNED, EMPTY }

/** This week's training against the routine's weekly target (5a "Training this week"). */
data class TrainingWeek(val done: Int = 0, val planned: Int = 0, val target: Int = 0) {
    val pips: List<WeekPip>
        get() = List(maxOf(target, done + planned)) { i ->
            when {
                i < done -> WeekPip.DONE
                i < done + planned -> WeekPip.PLANNED
                else -> WeekPip.EMPTY
            }
        }
}

/** The planner's copy (handover 4a / 5a section 3), kept pure so it can be unit-tested. */
object TrainingPlanText {
    private val shortDateFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

    /** `Fri 16 Oct`. */
    fun shortDate(date: LocalDate): String = date.format(shortDateFormat)

    /** The days of a week in display order, starting on [firstDayOfWeek]. */
    fun weekDays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> = (0L..6L).map { firstDayOfWeek.plus(it) }

    /**
     * Done = days this week with a logged workout; planned = today or later, not done, with a session not skipped.
     * The week containing [today] starts on [firstDayOfWeek].
     */
    fun week(
        sessions: List<PlannedSession>,
        loggedDates: Set<LocalDate>,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek,
        target: Int
    ): TrainingWeek {
        val start = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val days = (0L..6L).map { start.plusDays(it) }
        val done = days.count { it in loggedDates }
        val planned = days.count { day ->
            day >= today && day !in loggedDates && sessions.any { it.date == day && !it.skipped }
        }
        return TrainingWeek(done = done, planned = planned, target = target)
    }

    /** `3 of 4 done · 1 more planned · target reached`. */
    fun weekNote(week: TrainingWeek): String = buildString {
        append("${week.done} of ${week.target} done")
        if (week.planned > 0) append(" · ${week.planned} more planned")
        if (week.target > 0 && week.done >= week.target) append(" · target reached")
    }

    /**
     * `By weekday: Mon Push, Thu Leg` or `Rotating split: Push → Pull → Leg on Mon, Tue, Thu, Fri at 18:00`.
     * Templates missing from [names] (deleted) are left out, as the schedule treats them as rest.
     */
    fun routineLine(routine: TrainingRoutine, names: Map<String, String>, firstDayOfWeek: DayOfWeek): String {
        val days = weekDays(firstDayOfWeek)
        return when (routine.mode) {
            PlanMode.WEEKDAY -> {
                val parts = days.mapNotNull { day ->
                    routine.weekdayPlan[day.name]?.templateId?.let(names::get)?.let { "${day.short()} $it" }
                }
                "By weekday: " + parts.ifEmpty { listOf("no training days") }.joinToString(", ")
            }
            PlanMode.SPLIT -> {
                val order = routine.splitOrder.mapNotNull(names::get).joinToString(" → ")
                val on = days.filter { it.name in routine.splitDays }.map { it.short() }.ifEmpty { listOf("no days") }
                "Rotating split: $order on ${on.joinToString(", ")} at ${routine.splitTime}"
            }
        }
    }

    /** 4a header: `Next: Pull day · Mon 12 Oct at 18:00`, or `Nothing planned yet`. */
    fun nextUpLine(next: PlannedSession?, names: Map<String, String>): String =
        if (next == null) {
            "Nothing planned yet"
        } else {
            "Next: ${names[next.templateId] ?: "Workout"} · ${shortDate(next.date)} at ${next.time}"
        }

    /** Training days a week the routine plans (a weekday day with a deleted template doesn't count). */
    fun trainingDays(routine: TrainingRoutine, templateIds: Set<String>): Int = when (routine.mode) {
        PlanMode.WEEKDAY -> routine.weekdayPlan.count { (day, slot) -> day.isDayName() && slot.templateId in templateIds }
        PlanMode.SPLIT -> if (routine.splitOrder.none { it in templateIds }) 0 else routine.splitDays.count { it.isDayName() }
    }

    /** Weekly target note: matches / add N more / N over. */
    fun targetNote(days: Int, target: Int): String = when {
        days == target -> "Your routine has $days ${dayWord(days)} a week, which matches."
        days < target -> "Your routine has $days ${dayWord(days)}. Add ${target - days} more to reach it."
        else -> "Your routine has $days days, ${days - target} over."
    }

    fun targetLabel(target: Int): String = "$target ${dayWord(target)}"

    private fun dayWord(n: Int) = if (n == 1) "day" else "days"

    private fun DayOfWeek.short(): String = name.take(1) + name.substring(1, 3).lowercase()

    private fun String.isDayName(): Boolean = DayOfWeek.values().any { it.name == this }
}
