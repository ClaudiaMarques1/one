package app.theone.android.calendar.frontend

import java.text.NumberFormat
import java.util.Locale

// Display text for Meals in the Calendar (#69, 5a).

internal fun mealCountLabel(count: Int): String = if (count == 1) "1 meal" else "$count meals"

internal fun kcalLabel(kcal: Int, locale: Locale = Locale.getDefault()): String =
    "${NumberFormat.getIntegerInstance(locale).format(kcal)} kcal"

/**
 * Meals card title (5a): past days count what was eaten - "3 meals logged" (or "0 of 2 meals eaten" when
 * none was ticked); today and future days "1 of 4 meals eaten".
 */
internal fun mealsCardTitle(count: Int, eaten: Int, isPast: Boolean): String =
    if (isPast && eaten > 0) "${mealCountLabel(eaten)} logged" else "$eaten of ${mealCountLabel(count)} eaten"

/** Week agenda header: "3 workouts · 4 diary", plus "· 2 meal days" when the week has meals. */
internal fun weekSummaryLabel(workouts: Int, diaryDays: Int, mealDays: Int): String {
    val base = "$workouts workouts · $diaryDays diary"
    return when (mealDays) {
        0 -> base
        1 -> "$base · 1 meal day"
        else -> "$base · $mealDays meal days"
    }
}
