package app.theone.android.meals.frontend

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import app.theone.android.meals.backend.RecipesViewModel
import app.theone.android.settings.backend.DataStoreSettingsRepository
import app.theone.android.ui.ErrorBanner

/** A [MealsViewModel] reading meal settings from the device's DataStore. */
@Composable
fun mealsViewModel(): MealsViewModel = viewModel(
    factory = viewModelFactory {
        initializer { MealsViewModel(DataStoreSettingsRepository(checkNotNull(this[APPLICATION_KEY]))) }
    }
)

/** The recipe editor's target: a new recipe, or the id of the one being edited. */
private const val NEW_RECIPE = "new"

/**
 * Meals: tabs **Food Diary** (the weekly view, 1b) | **Recipes** (Owned / Search Recipe) - #60.
 * The 1a checklist (MealsTodayContent) is parked since #56.
 */
@Composable
fun MealsScreen(
    onOpenWeekPlan: () -> Unit = {},
    onOpenShoppingList: (dayInWeek: String) -> Unit = {},
    viewModel: MealsViewModel = mealsViewModel(),
    recipesViewModel: RecipesViewModel = viewModel()
) {
    val day by viewModel.day.collectAsState()
    val favourites by viewModel.favourites.collectAsState()
    val mealsError by viewModel.mealsError.collectAsState()
    val settings by viewModel.mealSettings.collectAsState()
    val week by viewModel.week.collectAsState()
    val recipes by recipesViewModel.recipes.collectAsState()
    val recipesError by recipesViewModel.recipesError.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // The slot the Add sheet is open for, or null when closed.
    var addingTo by rememberSaveable { mutableStateOf<MealSlot?>(null) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            MealsTabRow(labels = listOf("Food Diary", "Recipes"), selected = tab, onSelect = { tab = it })
            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    0 -> MealsTimelineContent(
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
                        onOpenShoppingList = { onOpenShoppingList(day.date) },
                        todayKey = viewModel.todayKey
                    )
                    else -> RecipesContent(
                        recipes = recipes,
                        onNewRecipe = { editing = NEW_RECIPE },
                        onOpenRecipe = { editing = it.id }
                    )
                }
            }
        }
        ErrorBanner(
            message = mealsError ?: recipesError,
            onDismiss = {
                viewModel.clearMealsError()
                recipesViewModel.clearRecipesError()
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    addingTo?.let { slot ->
        AddMealSheet(
            slot = slot,
            favourites = favourites,
            recipes = recipes,
            onAddPicks = { pickedFavourites, pickedRecipes ->
                viewModel.addPicks(slot, pickedFavourites, pickedRecipes)
                addingTo = null
            },
            onAddNew = { meal ->
                viewModel.addNewMeal(slot, meal.name, meal.kcal, meal.proteinG, meal.carbsG, meal.fatG, meal.saveToFavourites)
                addingTo = null
            },
            onDismiss = { addingTo = null }
        )
    }

    editing?.let { target ->
        val recipe = recipes.firstOrNull { it.id == target }
        // An id that no longer exists (deleted elsewhere) just closes the editor.
        if (target != NEW_RECIPE && recipe == null) {
            editing = null
        } else {
            RecipeEditorDialog(
                recipe = recipe,
                onSave = {
                    recipesViewModel.saveRecipe(it)
                    editing = null
                },
                onDelete = {
                    recipesViewModel.deleteRecipe(it.id)
                    editing = null
                },
                onDismiss = { editing = null }
            )
        }
    }
}
