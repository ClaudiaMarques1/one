package app.theone.android.meals.backend

import kotlinx.coroutines.flow.Flow

/**
 * Meal, favourite and water storage. The app uses [FirestoreMealsRepository]; tests use an in-memory
 * fake, so ViewModel logic is unit-testable without Firebase.
 */
interface MealsRepository {
    /** Meals planned for [date] ("yyyy-MM-dd"), oldest first; empty when signed out. */
    fun observeMeals(date: String): Flow<List<PlannedMeal>>

    /** Saved favourites, by name; empty when signed out. */
    fun observeFavourites(): Flow<List<FavouriteMeal>>

    /** Glasses of water logged for [date]; 0 when none or signed out. */
    fun observeWaterGlasses(date: String): Flow<Int>

    /** Adds [meals] and, when given, saves [newFavourite] - atomically, as one user action. */
    suspend fun addMeals(meals: List<PlannedMeal>, newFavourite: FavouriteMeal? = null)

    suspend fun setEaten(id: String, eaten: Boolean)

    suspend fun deleteMeal(id: String)

    suspend fun setWaterGlasses(date: String, glasses: Int)
}
