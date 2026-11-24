package app.theone.android.calendar.backend

import java.util.Locale

// Month view 5a (training planner handoff, #69): what colours a day cell and the stat tiles.

/**
 * The sections that colour a month cell, always in bottom-nav order Workout -> Meals -> Diary (5a):
 * a logged workout and the diary up to today; meals eaten on past days, planned from today on.
 * Planned workouts join with the training planner.
 */
fun DayActivity.cellSections(dateKey: String, todayKey: String): List<ActivityType> = buildList {
    val past = dateKey < todayKey
    val upToToday = dateKey <= todayKey
    if (upToToday && sessions.isNotEmpty()) add(ActivityType.WORKOUT)
    if (if (past) mealsEaten > 0 else meals.isNotEmpty()) add(ActivityType.MEAL)
    if (upToToday && diary != null) add(ActivityType.DIARY)
}

/** Stat tiles: workouts done, diary entries, and meals eaten of those planned, over the whole month. */
data class MonthStats(val workouts: Int = 0, val diaryEntries: Int = 0, val mealsEaten: Int = 0, val mealsPlanned: Int = 0)

/** [month] is 0-based, like [CalendarDateKeys.monthOf]. */
fun monthStats(year: Int, month: Int, dayByDate: Map<String, DayActivity>): MonthStats {
    val prefix = String.format(Locale.ROOT, "%04d-%02d-", year, month + 1)
    val days = dayByDate.filterKeys { it.startsWith(prefix) }.values
    return MonthStats(
        workouts = days.sumOf { it.sessions.size },
        diaryEntries = days.count { it.diary != null },
        mealsEaten = days.sumOf { it.mealsEaten },
        mealsPlanned = days.sumOf { it.meals.size }
    )
}
