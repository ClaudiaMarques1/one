package app.theone.android.meals.backend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.theone.android.settings.backend.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The 1e list for one week: the dates it covers, the aisle groups and the week key for its ticks. */
data class ShoppingListUiState(
    val weekStart: String = "",
    val from: String = "",
    val to: String = "",
    val groups: List<ShoppingGroup> = emptyList(),
    val state: ShoppingState = ShoppingState()
) {
    val toBuy: Int get() = groups.toBuy()
}

// The settings repository (week start) needs a Context, so the screen builds this through a factory.
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModel(
    private val settingsRepository: SettingsRepository,
    /** Any date in the week to shop for (the Week plan's or the Food Diary's shown week). */
    dayInWeek: LocalDate,
    private val mealsRepository: MealsRepository = FirestoreMealsRepository(),
    private val recipesRepository: RecipesRepository = FirestoreRecipesRepository(),
    private val today: LocalDate = LocalDate.now()
) : ViewModel() {
    private val _shoppingError = MutableStateFlow<String?>(null)
    val shoppingError: StateFlow<String?> = _shoppingError

    val list: StateFlow<ShoppingListUiState> = settingsRepository.observeMealSettings()
        .map { settings -> weekOf(dayInWeek, settings.weekStart) }
        .distinctUntilChanged()
        .flatMapLatest { days ->
            val (from, to) = shoppingRange(days, today)
            val weekStart = days.first().toString()
            combine(
                mealsRepository.observeMealsBetween(from.toString(), to.toString()),
                recipesRepository.observeRecipes(),
                recipesRepository.observeShoppingState(weekStart)
            ) { meals, recipes, state ->
                ShoppingListUiState(
                    weekStart = weekStart,
                    from = from.toString(),
                    to = to.toString(),
                    groups = buildShoppingList(meals, recipes, state),
                    state = state
                )
            }
        }
        .catch { e -> _shoppingError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShoppingListUiState())

    fun toggle(item: ShoppingItem) = update { it.toggle(item) }

    fun addItem(name: String) = update { it.addManual(name) }

    fun clearShoppingError() {
        _shoppingError.value = null
    }

    private fun update(change: (ShoppingState) -> ShoppingState) {
        val current = list.value
        if (current.weekStart.isEmpty()) return
        val next = change(current.state)
        if (next == current.state) return
        viewModelScope.launch {
            runCatching { recipesRepository.setShoppingState(current.weekStart, next) }
                .onFailure { _shoppingError.value = it.message }
        }
    }
}
