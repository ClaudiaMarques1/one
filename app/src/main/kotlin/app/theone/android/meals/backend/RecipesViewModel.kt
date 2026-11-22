package app.theone.android.meals.backend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Meals -> Recipes -> Owned: the user's recipes, and saving / deleting them. Also feeds the Add sheet's Recipes tab. */
class RecipesViewModel(private val repository: RecipesRepository = FirestoreRecipesRepository()) : ViewModel() {
    private val _recipesError = MutableStateFlow<String?>(null)
    val recipesError: StateFlow<String?> = _recipesError

    val recipes: StateFlow<List<Recipe>> = repository.observeRecipes()
        .catch { e -> _recipesError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            runCatching { repository.saveRecipe(recipe) }.onFailure { _recipesError.value = it.message }
        }
    }

    fun deleteRecipe(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteRecipe(id) }.onFailure { _recipesError.value = it.message }
        }
    }

    fun clearRecipesError() {
        _recipesError.value = null
    }
}
