package app.theone.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.theone.android.ui.theme.CalendarAccent
import app.theone.android.ui.theme.DiaryAccent
import app.theone.android.ui.theme.MealsAccent
import app.theone.android.ui.theme.WorkoutAccent

sealed class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val accentColor: Color? = null
) {
    data object Home : Destination("home", "Home", Icons.Filled.Home)
    data object Calendar : Destination("calendar", "Calendar", Icons.Filled.CalendarMonth, CalendarAccent)
    data object Workout : Destination("workout", "Workout", Icons.Filled.FitnessCenter, WorkoutAccent)
    data object Meals : Destination("meals", "Meals", Icons.Filled.Restaurant, MealsAccent)
    data object Diary : Destination("diary", "Diary", Icons.Filled.Book, DiaryAccent)
}

val bottomNavDestinations = listOf(
    Destination.Home,
    Destination.Calendar,
    Destination.Workout,
    Destination.Meals,
    Destination.Diary
)
