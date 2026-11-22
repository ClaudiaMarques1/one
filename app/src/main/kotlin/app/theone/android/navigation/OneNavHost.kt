package app.theone.android.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.theone.android.auth.backend.AuthViewModel
import app.theone.android.auth.frontend.AccountDetailsScreen
import app.theone.android.calendar.frontend.CalendarScreen
import app.theone.android.dashboard.frontend.DashboardScreen
import app.theone.android.dashboard.frontend.ProfileDrawerSheet
import app.theone.android.diary.frontend.DiaryScreen
import app.theone.android.meals.frontend.MealsScreen
import app.theone.android.meals.frontend.MealsWeekScreen
import app.theone.android.settings.frontend.SettingsScreen
import app.theone.android.ui.theme.LocalOneColors
import app.theone.android.ui.theme.NeutralNavBar
import app.theone.android.ui.theme.NeutralSelected
import app.theone.android.ui.theme.NeutralTheme
import app.theone.android.ui.theme.SectionAccent
import app.theone.android.ui.theme.SectionTheme
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.workout.frontend.WorkoutScreen
import kotlinx.coroutines.launch

@Composable
fun OneNavHost(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val profile by authViewModel.profile.collectAsState()

    // The profile drawer covers the whole screen, bottom nav included (handoff 1b), so it wraps the
    // Scaffold. Opened only from Home's avatar; gestures are on while open so a swipe left closes it.
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        // Black at 32% in light, 60% in dark (home handoff).
        scrimColor = if (LocalOneColors.current.isDark) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.32f),
        drawerContent = {
            NeutralTheme {
                ProfileDrawerSheet(
                    profile = profile,
                    onAccountDetails = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Destination.Account.route)
                    },
                    onSettings = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Destination.Settings.route)
                    }
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                if (bottomNavDestinations.any { it.route == currentDestination?.route }) {
                    NavigationBar(containerColor = NeutralNavBar) {
                        bottomNavDestinations.forEach { destination ->
                            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                            // Home has no accent: its selected item is neutral, never dynamic colour.
                            val accent = destination.accent?.current
                            val selectedInk = accent ?: WarmOnSurface
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
                                icon = {
                                    Icon(
                                        if (selected) destination.selectedIcon else destination.icon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = {
                                    Text(destination.label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = selectedInk,
                                    selectedTextColor = selectedInk,
                                    indicatorColor = accent?.copy(alpha = 0.18f) ?: NeutralSelected,
                                    unselectedIconColor = WarmIcon,
                                    unselectedTextColor = WarmIcon
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Destination.Home.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Destination.Home.route) {
                    NeutralTheme {
                        DashboardScreen(profile = profile, onAvatarClick = { scope.launch { drawerState.open() } })
                    }
                }
                composable(Destination.Calendar.route) {
                    SectionTheme(SectionAccent.Calendar) {
                        CalendarScreen(
                            onNavigateToWorkout = { navController.navigate(Destination.Workout.route) },
                            onNavigateToDiary = { navController.navigate(Destination.Diary.route) }
                        )
                    }
                }
                composable(Destination.Workout.route) {
                    SectionTheme(SectionAccent.Workout) { WorkoutScreen() }
                }
                composable(Destination.Meals.route) {
                    SectionTheme(SectionAccent.Meals) {
                        MealsScreen(onOpenWeekPlan = { navController.navigate(Destination.MealsWeek.route) })
                    }
                }
                composable(Destination.Diary.route) {
                    SectionTheme(SectionAccent.Diary) { DiaryScreen() }
                }
                composable(Destination.Account.route) {
                    NeutralTheme {
                        AccountDetailsScreen(
                            profile = profile,
                            onBack = { navController.popBackStack() },
                            onSignOut = authViewModel::signOut
                        )
                    }
                }
                composable(Destination.Settings.route) {
                    NeutralTheme { SettingsScreen(onBack = { navController.popBackStack() }) }
                }
                composable(Destination.MealsWeek.route) {
                    SectionTheme(SectionAccent.Meals) { MealsWeekScreen(onBack = { navController.popBackStack() }) }
                }
            }
        }
    }
}
