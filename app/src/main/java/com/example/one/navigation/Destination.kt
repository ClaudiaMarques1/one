package com.example.one.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String, val label: String, val icon: ImageVector) {
    data object Calendar : Destination("calendar", "Calendar", Icons.Filled.CalendarMonth)
    data object Workout : Destination("workout", "Workout", Icons.Filled.FitnessCenter)
    data object Meals : Destination("meals", "Meals", Icons.Filled.Restaurant)
    data object Diary : Destination("diary", "Diary", Icons.Filled.Book)
}

val bottomNavDestinations = listOf(
    Destination.Calendar,
    Destination.Workout,
    Destination.Meals,
    Destination.Diary
)
