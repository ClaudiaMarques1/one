package com.example.one.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val accentColor: Color? = null
) {
    data object Home : Destination("home", "Home", Icons.Filled.Home)
    data object Calendar : Destination("calendar", "Calendar", Icons.Filled.CalendarMonth, Color(0xFFFB8C00))
    data object Workout : Destination("workout", "Workout", Icons.Filled.FitnessCenter, Color(0xFF1E88E5))
    data object Meals : Destination("meals", "Meals", Icons.Filled.Restaurant, Color(0xFF43A047))
    data object Diary : Destination("diary", "Diary", Icons.Filled.Book, Color(0xFF8E24AA))
}

val bottomNavDestinations = listOf(
    Destination.Home,
    Destination.Calendar,
    Destination.Workout,
    Destination.Meals,
    Destination.Diary
)
