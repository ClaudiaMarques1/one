package app.theone.android.meals.backend

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [RecipesRepository] for tests: recipes by name, ids assigned on add, `saveRecipe` upserts
 * by id; shopping state per week start. Set [failNext] to make the next write throw it.
 */
class FakeRecipesRepository(recipes: List<Recipe> = emptyList()) : RecipesRepository {
    private val recipesFlow = MutableStateFlow(recipes)
    private val shoppingFlow = MutableStateFlow<Map<String, ShoppingState>>(emptyMap())
    private var nextId = 1

    var failNext: Throwable? = null

    val storedRecipes: List<Recipe> get() = recipesFlow.value

    fun storedShopping(weekStart: String): ShoppingState = shoppingFlow.value[weekStart] ?: ShoppingState()

    override fun observeRecipes(): Flow<List<Recipe>> = recipesFlow.map { list -> list.sortedBy { it.name } }

    override suspend fun saveRecipe(recipe: Recipe) {
        throwIfFailing()
        recipesFlow.value = if (recipe.id.isEmpty()) {
            recipesFlow.value + recipe.copy(id = "recipe-${nextId++}")
        } else {
            recipesFlow.value.filterNot { it.id == recipe.id } + recipe
        }
    }

    override suspend fun deleteRecipe(id: String) {
        throwIfFailing()
        recipesFlow.value = recipesFlow.value.filterNot { it.id == id }
    }

    override fun observeShoppingState(weekStart: String): Flow<ShoppingState> =
        shoppingFlow.map { it[weekStart] ?: ShoppingState() }

    override suspend fun setShoppingState(weekStart: String, state: ShoppingState) {
        throwIfFailing()
        shoppingFlow.value = shoppingFlow.value + (weekStart to state)
    }

    private fun throwIfFailing() {
        failNext?.let {
            failNext = null
            throw it
        }
    }
}
