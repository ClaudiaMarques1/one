package app.theone.android.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.theone.android.auth.AuthViewModel
import app.theone.android.calendar.CalendarScreen
import app.theone.android.dashboard.DashboardScreen
import app.theone.android.diary.DiaryScreen
import app.theone.android.meals.MealsScreen
import app.theone.android.ui.theme.SectionTheme
import app.theone.android.workout.WorkoutScreen

@Composable
fun OneNavHost(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination

            NavigationBar {
                bottomNavDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    val accent = destination.accentColor
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                        colors = if (accent != null) {
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = accent,
                                selectedTextColor = accent,
                                indicatorColor = accent.copy(alpha = 0.18f)
                            )
                        } else {
                            NavigationBarItemDefaults.colors()
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Home.route) { DashboardScreen(onSignOut = authViewModel::signOut) }
            composable(Destination.Calendar.route) {
                SectionTheme(Destination.Calendar.accentColor!!) {
                    CalendarScreen(
                        onNavigateToWorkout = { navController.navigate(Destination.Workout.route) },
                        onNavigateToDiary = { navController.navigate(Destination.Diary.route) }
                    )
                }
            }
            composable(Destination.Workout.route) {
                SectionTheme(Destination.Workout.accentColor!!) { WorkoutScreen() }
            }
            composable(Destination.Meals.route) {
                SectionTheme(Destination.Meals.accentColor!!) { MealsScreen() }
            }
            composable(Destination.Diary.route) {
                SectionTheme(Destination.Diary.accentColor!!) { DiaryScreen() }
            }
        }
    }
}
