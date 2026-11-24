package app.theone.android.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.theone.android.auth.backend.AuthViewModel
import app.theone.android.auth.frontend.AccountDetailsScreen
import app.theone.android.calendar.frontend.CalendarScreen
import app.theone.android.dashboard.frontend.DashboardScreen
import app.theone.android.dashboard.frontend.ProfileDrawerSheet
import app.theone.android.diary.frontend.DiaryScreen
import app.theone.android.meals.frontend.MealsScreen
import app.theone.android.meals.frontend.MealsWeekScreen
import app.theone.android.meals.frontend.ShoppingListScreen
import app.theone.android.settings.frontend.SettingsScreen
import app.theone.android.settings.frontend.SettingsSection
import app.theone.android.settings.frontend.SettingsSectionScreen
import app.theone.android.ui.theme.LocalOneColors
import app.theone.android.ui.theme.NeutralNavBar
import app.theone.android.ui.theme.NeutralSelected
import app.theone.android.ui.theme.NeutralTheme
import app.theone.android.ui.theme.SectionAccent
import app.theone.android.ui.theme.SectionTheme
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.workout.frontend.WorkoutScreen
import app.theone.android.workout.frontend.strengthViewModel
import app.theone.android.workout.sessions.frontend.SessionDetailScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun OneNavHost(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val profile by authViewModel.profile.collectAsState()

    // Switches bottom-nav tab the way the bar does: one entry per tab, each tab's stack saved and restored.
    val switchTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

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
                                onClick = { switchTab(destination.route) },
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
                // Consumed so a screen's imePadding() only adds what the keyboard covers beyond the
                // bottom bar - otherwise the bar's height shows as an empty gap above the keyboard.
                modifier = Modifier.padding(padding).consumeWindowInsets(padding)
            ) {
                composable(Destination.Home.route) {
                    NeutralTheme {
                        DashboardScreen(profile = profile, onAvatarClick = { scope.launch { drawerState.open() } })
                    }
                }
                composable(Destination.Calendar.route) {
                    SectionTheme(SectionAccent.Calendar) {
                        CalendarScreen(
                            // Tab switches, not pushes: a pushed Workout/Diary sat on the Calendar tab's own stack,
                            // so tapping Calendar restored that stack and showed Diary again - Calendar looked stuck.
                            onNavigateToWorkout = { switchTab(Destination.Workout.route) },
                            onNavigateToDiary = { switchTab(Destination.Diary.route) },
                            // Session detail's Repeat needs the Workout tab under it, so open it from that tab.
                            onOpenWorkoutSession = { sessionId ->
                                switchTab(Destination.Workout.route)
                                navController.navigate(Destination.WorkoutSession.routeFor(sessionId))
                            },
                            // Meals on that date (#69): the tab reads the date from its entry once, then clears it.
                            onOpenMeals = { dateKey ->
                                switchTab(Destination.Meals.route)
                                navController.currentBackStackEntry?.savedStateHandle?.set(Destination.Meals.KEY_OPEN_DATE, dateKey)
                            }
                        )
                    }
                }
                composable(Destination.Workout.route) {
                    SectionTheme(SectionAccent.Workout) {
                        WorkoutScreen(onOpenSession = { navController.navigate(Destination.WorkoutSession.routeFor(it.id)) })
                    }
                }
                composable(
                    Destination.WorkoutSession.route,
                    arguments = listOf(navArgument(Destination.WorkoutSession.ARG_SESSION_ID) { type = NavType.StringType })
                ) { entry ->
                    // Repeat starts the workout in the Workout tab's own StrengthViewModel, so it opens there.
                    val workoutEntry = remember(entry) { navController.getBackStackEntry(Destination.Workout.route) }
                    val strengthViewModel = strengthViewModel(owner = workoutEntry)
                    SectionTheme(SectionAccent.Workout) {
                        SessionDetailScreen(
                            onBack = { navController.popBackStack() },
                            onRepeat = { session, template ->
                                strengthViewModel.repeatSession(session, template)
                                navController.popBackStack(Destination.Workout.route, inclusive = false)
                            }
                        )
                    }
                }
                composable(Destination.Meals.route) { entry ->
                    val openDate by entry.savedStateHandle.getStateFlow<String?>(Destination.Meals.KEY_OPEN_DATE, null).collectAsState()
                    SectionTheme(SectionAccent.Meals) {
                        MealsScreen(
                            onOpenWeekPlan = { navController.navigate(Destination.MealsWeek.route) },
                            onOpenShoppingList = { navController.navigate(Destination.MealsShopping.routeFor(it)) },
                            openDate = openDate,
                            onOpenDateHandled = { entry.savedStateHandle[Destination.Meals.KEY_OPEN_DATE] = null }
                        )
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
                    NeutralTheme {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenSection = { navController.navigate(Destination.SettingsSectionPage.routeFor(it.key)) }
                        )
                    }
                }
                composable(
                    Destination.SettingsSectionPage.route,
                    arguments = listOf(navArgument(Destination.SettingsSectionPage.ARG_SECTION) { type = NavType.StringType })
                ) { entry ->
                    val section = SettingsSection.fromKey(entry.arguments?.getString(Destination.SettingsSectionPage.ARG_SECTION))
                    NeutralTheme {
                        if (section == null) {
                            // An unknown section key can't come from the app itself; just go back.
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            SettingsSectionScreen(section = section, onBack = { navController.popBackStack() })
                        }
                    }
                }
                composable(Destination.MealsWeek.route) {
                    SectionTheme(SectionAccent.Meals) {
                        MealsWeekScreen(
                            onBack = { navController.popBackStack() },
                            onOpenShoppingList = { navController.navigate(Destination.MealsShopping.routeFor(it)) }
                        )
                    }
                }
                composable(
                    Destination.MealsShopping.route,
                    arguments = listOf(navArgument(Destination.MealsShopping.ARG_DATE) { type = NavType.StringType })
                ) { entry ->
                    val dayInWeek = entry.arguments?.getString(Destination.MealsShopping.ARG_DATE) ?: LocalDate.now().toString()
                    SectionTheme(SectionAccent.Meals) {
                        ShoppingListScreen(dayInWeek = dayInWeek, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
