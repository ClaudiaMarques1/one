package app.theone.android.meals.backend

import kotlin.math.abs

/** Where a Week plan row sits relative to today: past days show what was eaten, future days what's planned. */
enum class DayState { PAST, TODAY, FUTURE }

/** A B / L / D / S chip: every meal eaten, some planned, or nothing in the slot. */
enum class ChipState { EATEN, PLANNED, EMPTY }

/** The four main slots the Week plan chips show, in order. */
val WEEK_CHIP_SLOTS = listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACKS)

/** One row of the 1d Week plan. */
data class WeekPlanDay(
    val date: String,
    val state: DayState,
    val eatenKcal: Int,
    val plannedKcal: Int,
    val targetKcal: Int,
    /** Past days only: within 5% of the target. */
    val onTarget: Boolean,
    val chips: List<Pair<MealSlot, ChipState>>,
    /** The logged workout's name, `Training` for a planned training day, null on a rest day. */
    val trainingLabel: String?,
    val meals: List<PlannedMeal>
)

/**
 * Builds the week's rows. Training uses the same rule as the weekly view: a logged workout, or the
 * Rest day switch turned off (a stored `false` choice).
 */
fun buildWeekPlan(
    days: List<String>,
    today: String,
    meals: List<PlannedMeal>,
    settings: MealSettings,
    workouts: Map<String, String>,
    restDayChoices: Map<String, Boolean>
): List<WeekPlanDay> {
    val byDate = meals.groupBy { it.date }
    return days.map { date ->
        val dayMeals = byDate[date].orEmpty()
        val workout = workouts[date]
        val training = workout != null || restDayChoices[date] == false
        val target = (if (training) settings.trainingTargets else settings.restTargets).kcal
        val eaten = dayMeals.filter { it.eaten }.sumOf { it.kcal }
        val state = when {
            date < today -> DayState.PAST
            date == today -> DayState.TODAY
            else -> DayState.FUTURE
        }
        WeekPlanDay(
            date = date,
            state = state,
            eatenKcal = eaten,
            plannedKcal = dayMeals.sumOf { it.kcal },
            targetKcal = target,
            onTarget = state == DayState.PAST && target > 0 && abs(eaten - target) <= target * 0.05,
            chips = WEEK_CHIP_SLOTS.map { slot -> slot to chipState(dayMeals.filter { it.slot == slot.name }) },
            trainingLabel = workout ?: if (training) "Training" else null,
            meals = dayMeals
        )
    }
}

private fun chipState(slotMeals: List<PlannedMeal>): ChipState = when {
    slotMeals.isEmpty() -> ChipState.EMPTY
    slotMeals.all { it.eaten } -> ChipState.EATEN
    else -> ChipState.PLANNED
}

/**
 * Copies of [meals] for [toDate], as planned (not eaten) and without ids, for `addMeals`. [now] keeps
 * the copies in their original order on the new day.
 */
fun copyMealsTo(meals: List<PlannedMeal>, toDate: String, now: Long): List<PlannedMeal> =
    meals.sortedBy { it.createdAt }.mapIndexed { index, meal ->
        meal.copy(id = "", date = toDate, eaten = false, createdAt = now + index)
    }
