package app.theone.android.meals.backend

import kotlinx.coroutines.flow.Flow

/**
 * Owned recipes and the per-week shopping list state. The app uses [FirestoreRecipesRepository];
 * tests use an in-memory fake.
 */
interface RecipesRepository {
    /** The user's recipes, by name; empty when signed out. */
    fun observeRecipes(): Flow<List<Recipe>>

    /** Adds [recipe] when its id is blank, otherwise replaces the stored one. */
    suspend fun saveRecipe(recipe: Recipe)

    suspend fun deleteRecipe(id: String)

    /** Ticks and own items for the week starting [weekStart] ("yyyy-MM-dd"). */
    fun observeShoppingState(weekStart: String): Flow<ShoppingState>

    suspend fun setShoppingState(weekStart: String, state: ShoppingState)
}
