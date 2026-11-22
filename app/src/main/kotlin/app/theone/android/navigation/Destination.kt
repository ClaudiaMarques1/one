package app.theone.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import app.theone.android.ui.theme.SectionAccent

/** [icon] is the outlined nav icon, [selectedIcon] the filled one shown when the tab is selected. */
sealed class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val accent: SectionAccent? = null
) {
    data object Home : Destination("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)

    data object Calendar : Destination(
        "calendar",
        "Calendar",
        Icons.Outlined.CalendarMonth,
        Icons.Filled.CalendarMonth,
        SectionAccent.Calendar
    )

    data object Workout : Destination("workout", "Workout", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter, SectionAccent.Workout)

    data object Meals : Destination("meals", "Meals", Icons.Outlined.Restaurant, Icons.Filled.Restaurant, SectionAccent.Meals)

    data object Diary : Destination("diary", "Diary", Icons.Outlined.Book, Icons.Filled.Book, SectionAccent.Diary)

    // Reached from the profile drawer on Home; not in the bottom nav, which is hidden on them.
    data object Account : Destination("account", "Account details", Icons.Filled.Person)

    data object Settings : Destination("settings", "Settings", Icons.Filled.Settings)

    // Meals' Week plan (1d), opened from the weekly view's header; outside the bottom nav.
    data object MealsWeek : Destination("meals/week", "Week plan", Icons.Filled.CalendarViewWeek, accent = SectionAccent.Meals)

    // Meals' Shopping list (1e) for the week holding {date}; from the Week plan and the Food Diary header.
    data object MealsShopping : Destination(
        "meals/shopping/{date}",
        "Shopping list",
        Icons.Filled.ShoppingCart,
        accent = SectionAccent.Meals
    ) {
        const val ARG_DATE = "date"

        fun routeFor(dayInWeek: String) = "meals/shopping/$dayInWeek"
    }
}

val bottomNavDestinations = listOf(
    Destination.Home,
    Destination.Calendar,
    Destination.Workout,
    Destination.Meals,
    Destination.Diary
)
