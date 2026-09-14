package com.example.one.navigation

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
import com.example.one.auth.AuthViewModel
import com.example.one.calendar.CalendarScreen
import com.example.one.dashboard.DashboardScreen
import com.example.one.diary.DiaryScreen
import com.example.one.meals.MealsScreen
import com.example.one.ui.theme.SectionTheme
import com.example.one.workout.WorkoutScreen

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
                SectionTheme(Destination.Calendar.accentColor!!) { CalendarScreen() }
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
