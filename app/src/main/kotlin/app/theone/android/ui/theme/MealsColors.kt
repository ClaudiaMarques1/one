package app.theone.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Meals greens (meals handoff "Colours: Meals"). The accent fill itself is SectionAccent.Meals. */
data class MealsPalette(
    val eaten: Color,
    val deep: Color,
    val container: Color,
    val soft: Color,
    val todayRow: Color,
    val planned: Color
)

val LightMealsPalette = MealsPalette(
    eaten = Color(0xFF43A047),
    deep = Color(0xFF1B5E20),
    container = Color(0xFFD9ECDA),
    soft = Color(0xFFEEF6EE),
    todayRow = Color(0xFFF4FAF4),
    planned = Color(0xFFA5D6A7)
)

// Meals has no dark mock. Values come from the same light -> dark conversion the other sections'
// design files use, so Meals matches them (the README's #81C784 / #A5D6A7 were "confirm with design").
val DarkMealsPalette = MealsPalette(
    eaten = Color(0xFF82D583),
    deep = Color(0xFFA5E9A4),
    container = Color(0xFF1A221A),
    soft = Color(0xFF141814),
    todayRow = Color(0xFF121412),
    planned = Color(0xFF305333)
)

/** `MealsColors.Eaten` etc., resolving light or dark from the current theme. */
object MealsColors {
    private val p: MealsPalette
        @Composable @ReadOnlyComposable
        get() = if (LocalOneColors.current.isDark) DarkMealsPalette else LightMealsPalette

    val Accent: Color
        @Composable @ReadOnlyComposable
        get() = MealsAccent

    val Eaten: Color
        @Composable @ReadOnlyComposable
        get() = p.eaten

    val Deep: Color
        @Composable @ReadOnlyComposable
        get() = p.deep

    val Container: Color
        @Composable @ReadOnlyComposable
        get() = p.container

    val Soft: Color
        @Composable @ReadOnlyComposable
        get() = p.soft

    val TodayRow: Color
        @Composable @ReadOnlyComposable
        get() = p.todayRow

    val Planned: Color
        @Composable @ReadOnlyComposable
        get() = p.planned
}
