package app.theone.android.meals.backend

import app.theone.android.settings.backend.WeekStart
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** One row of the 1b timeline, at [minutes] after midnight. */
sealed interface TimelineRow {
    val minutes: Int

    data class Meal(val slot: MealSlot, val meal: PlannedMeal, override val minutes: Int) : TimelineRow

    data class EmptySlot(val slot: MealSlot, override val minutes: Int) : TimelineRow

    data class Workout(val name: String, override val minutes: Int) : TimelineRow

    /** Eaten meals turn their node and rail green; workouts and empty slots never do. */
    val eaten: Boolean get() = this is Meal && meal.eaten
}

/**
 * The day's slots in time order, each meal its own row (a slot with several meals repeats its time),
 * an empty slot as a placeholder, and the workout at the time it was logged. Ties keep checklist
 * order, with the workout after the slots that share its minute.
 */
fun buildTimeline(day: MealsDay, settings: MealSettings): List<TimelineRow> {
    val rows = day.slots.flatMap { slotMeals ->
        val minutes = settings.timeOf(slotMeals.slot)
        if (slotMeals.meals.isEmpty()) {
            listOf(TimelineRow.EmptySlot(slotMeals.slot, minutes))
        } else {
            slotMeals.meals.map { TimelineRow.Meal(slotMeals.slot, it, minutes) }
        }
    }
    val workout = day.workoutName?.let { name -> day.workoutMinutes?.let { TimelineRow.Workout(name, it) } }
    return (rows + listOfNotNull(workout)).sortedBy { it.minutes } // stable: ties keep insertion order
}

/** The rail line above row [index] is green when it and the row before are both eaten. */
fun List<TimelineRow>.lineAboveEaten(index: Int): Boolean = index > 0 && this[index].eaten && this[index - 1].eaten

/** kcal still to eat today: never below 0. */
fun MealsDay.kcalLeft(): Int = (targets.kcal - eaten.kcal).coerceAtLeast(0)

/** kcal planned but not eaten yet. */
fun MealsDay.kcalMorePlanned(): Int = (planned.kcal - eaten.kcal).coerceAtLeast(0)

/** The 7 dates of the week holding [date], starting on [weekStart]. */
fun weekOf(date: LocalDate, weekStart: WeekStart): List<LocalDate> {
    val first = date.with(TemporalAdjusters.previousOrSame(weekStart.dayOfWeek))
    return (0L..6L).map { first.plusDays(it) }
}

/** The week strip: its 7 date keys and the ones holding any meal (for the dots). */
data class MealsWeek(val days: List<String> = emptyList(), val mealDates: Set<String> = emptySet())
