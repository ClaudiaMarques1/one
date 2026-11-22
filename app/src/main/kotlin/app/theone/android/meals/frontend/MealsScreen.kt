package app.theone.android.meals.frontend

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.theone.android.meals.backend.MealSlot
import app.theone.android.meals.backend.MealsViewModel
import app.theone.android.settings.backend.DataStoreSettingsRepository
import app.theone.android.ui.ErrorBanner

/** A [MealsViewModel] reading meal settings from the device's DataStore. */
@Composable
fun mealsViewModel(): MealsViewModel = viewModel(
    factory = viewModelFactory {
        initializer { MealsViewModel(DataStoreSettingsRepository(checkNotNull(this[APPLICATION_KEY]))) }
    }
)

@Composable
fun MealsScreen(onOpenWeekPlan: () -> Unit = {}, viewModel: MealsViewModel = mealsViewModel()) {
    val day by viewModel.day.collectAsState()
    val favourites by viewModel.favourites.collectAsState()
    val mealsError by viewModel.mealsError.collectAsState()
    val settings by viewModel.mealSettings.collectAsState()
    val week by viewModel.week.collectAsState()
    // The slot the Add sheet is open for, or null when closed.
    var addingTo by rememberSaveable { mutableStateOf<MealSlot?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // The weekly view (1b) is Meals' only view. The 1a checklist (MealsTodayContent) is parked:
        // the Today view setting was removed on #56 and the screen is no longer shown.
        MealsTimelineContent(
            day = day,
            week = week,
            settings = settings,
            onSelectDate = viewModel::selectDate,
            onPreviousWeek = viewModel::showPreviousWeek,
            onNextWeek = viewModel::showNextWeek,
            onAddToSlot = { addingTo = it },
            onToggleEaten = viewModel::setEaten,
            onRemoveMeal = viewModel::removeMeal,
            onTapGlass = viewModel::tapWaterGlass,
            onRestDayChange = viewModel::setRestDay,
            onOpenWeekPlan = onOpenWeekPlan,
            todayKey = viewModel.todayKey
        )
        ErrorBanner(
            message = mealsError,
            onDismiss = viewModel::clearMealsError,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    addingTo?.let { slot ->
        AddMealSheet(
            slot = slot,
            favourites = favourites,
            onAddFavourites = { picks ->
                viewModel.addFavourites(slot, picks)
                addingTo = null
            },
            onAddNew = { meal ->
                viewModel.addNewMeal(slot, meal.name, meal.kcal, meal.proteinG, meal.carbsG, meal.fatG, meal.saveToFavourites)
                addingTo = null
            },
            onDismiss = { addingTo = null }
        )
    }
}
