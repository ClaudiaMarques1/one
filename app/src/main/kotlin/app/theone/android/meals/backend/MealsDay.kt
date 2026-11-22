package app.theone.android.meals.backend

/** kcal and grams of protein, carbs and fat added up over some meals. */
data class MacroTotals(
    val kcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0
) {
    operator fun plus(meal: PlannedMeal) = MacroTotals(
        kcal = kcal + meal.kcal,
        proteinG = proteinG + meal.proteinG,
        carbsG = carbsG + meal.carbsG,
        fatG = fatG + meal.fatG
    )
}

fun List<PlannedMeal>.totals(): MacroTotals = fold(MacroTotals()) { sum, meal -> sum + meal }

/** One slot card on Today: the slot and the meals planned into it. */
data class SlotMeals(val slot: MealSlot, val meals: List<PlannedMeal>) {
    val kcal: Int get() = meals.sumOf { it.kcal }
}

/**
 * Everything the Today screen shows for one date. [workoutName] is non-null on a training day; it
 * switches the targets and makes the optional Pre-/Post-workout slots eligible.
 */
data class MealsDay(
    val date: String,
    val workoutName: String? = null,
    /** When the workout was logged (minutes after midnight) - places it on the timeline. */
    val workoutMinutes: Int? = null,
    val targets: MealTargets = DEFAULT_REST_TARGETS,
    val slots: List<SlotMeals> = emptyList(),
    val eaten: MacroTotals = MacroTotals(),
    val planned: MacroTotals = MacroTotals(),
    val waterGlasses: Int = 0,
    val waterTargetGlasses: Int = DEFAULT_WATER_GLASSES,
    /** Training-day targets and workout slots; from the Rest day switch, else from a logged workout. */
    val isTrainingDay: Boolean = false
)

/**
 * Builds the day from raw data. The Rest day switch's [restDayChoice] wins; without one, a logged
 * workout makes it a training day. A slot shows when it's switched on in Settings (optional slots
 * only on training days) - or whenever it already holds meals, so nothing logged is ever hidden.
 * The workout itself stays on the timeline even on a day marked as rest - it still happened.
 */
fun buildMealsDay(
    date: String,
    meals: List<PlannedMeal>,
    settings: MealSettings,
    workoutName: String?,
    waterGlasses: Int,
    workoutMinutes: Int? = null,
    restDayChoice: Boolean? = null
): MealsDay {
    val training = restDayChoice?.not() ?: (workoutName != null)
    val bySlot = meals.groupBy { MealSlot.fromStored(it.slot) ?: MealSlot.SNACKS }
    val slots = MealSlot.entries.mapNotNull { slot ->
        val slotMeals = bySlot[slot].orEmpty()
        val eligible = slot in settings.enabledSlots && (!slot.optional || training)
        if (eligible || slotMeals.isNotEmpty()) SlotMeals(slot, slotMeals) else null
    }
    return MealsDay(
        date = date,
        workoutName = workoutName,
        workoutMinutes = workoutMinutes.takeIf { workoutName != null },
        targets = if (training) settings.trainingTargets else settings.restTargets,
        slots = slots,
        eaten = meals.filter { it.eaten }.totals(),
        planned = meals.totals(),
        waterGlasses = waterGlasses,
        waterTargetGlasses = settings.waterGlasses,
        isTrainingDay = training
    )
}

/** Tapping glass [n] (1-based) sets the count to n, or to n - 1 when n is already the last full glass. */
fun tapWaterGlass(current: Int, n: Int): Int = if (current == n) n - 1 else n
