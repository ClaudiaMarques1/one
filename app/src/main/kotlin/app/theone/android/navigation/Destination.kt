package app.theone.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.theone.android.ui.theme.CalendarAccent
import app.theone.android.ui.theme.DiaryAccent
import app.theone.android.ui.theme.MealsAccent
import app.theone.android.ui.theme.WorkoutAccent

/** [icon] is the outlined nav icon, [selectedIcon] the filled one shown when the tab is selected. */
sealed class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val accentColor: Color? = null
) {
    data object Home : Destination("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)

    data object Calendar : Destination("calendar", "Calendar", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth, CalendarAccent)

    data object Workout : Destination("workout", "Workout", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter, WorkoutAccent)

    data object Meals : Destination("meals", "Meals", Icons.Outlined.Restaurant, Icons.Filled.Restaurant, MealsAccent)

    data object Diary : Destination("diary", "Diary", Icons.Outlined.Book, Icons.Filled.Book, DiaryAccent)

    // Reached from the profile drawer on Home; not in the bottom nav, which is hidden on them.
    data object Account : Destination("account", "Account details", Icons.Filled.Person)

    data object Settings : Destination("settings", "Settings", Icons.Filled.Settings)
}

val bottomNavDestinations = listOf(
    Destination.Home,
    Destination.Calendar,
    Destination.Workout,
    Destination.Meals,
    Destination.Diary
)
