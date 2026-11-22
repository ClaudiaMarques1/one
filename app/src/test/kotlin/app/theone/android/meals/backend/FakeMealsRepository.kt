package app.theone.android.meals.backend

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [MealsRepository] for tests. Mirrors the Firestore contract: meals per date oldest first,
 * favourites by name, ids assigned on add, and `addMeals` stores the meals and the favourite together.
 *
 * Set [failNext] to make the next write throw it, to exercise a ViewModel's error path.
 */
class FakeMealsRepository(
    meals: List<PlannedMeal> = emptyList(),
    favourites: List<FavouriteMeal> = emptyList(),
    water: Map<String, Int> = emptyMap()
) : MealsRepository {
    private val mealsFlow = MutableStateFlow(meals)
    private val favouritesFlow = MutableStateFlow(favourites)
    private val waterFlow = MutableStateFlow(water)
    private var nextId = 1

    var failNext: Throwable? = null

    val storedMeals: List<PlannedMeal> get() = mealsFlow.value
    val storedFavourites: List<FavouriteMeal> get() = favouritesFlow.value

    fun storedWater(date: String): Int = waterFlow.value[date] ?: 0

    override fun observeMeals(date: String): Flow<List<PlannedMeal>> =
        mealsFlow.map { list -> list.filter { it.date == date }.sortedBy { it.createdAt } }

    override fun observeMealDates(from: String, to: String): Flow<Set<String>> =
        mealsFlow.map { list -> list.map { it.date }.filter { it in from..to }.toSet() }

    override fun observeFavourites(): Flow<List<FavouriteMeal>> = favouritesFlow.map { list -> list.sortedBy { it.name } }

    override fun observeWaterGlasses(date: String): Flow<Int> = waterFlow.map { it[date] ?: 0 }

    override suspend fun addMeals(meals: List<PlannedMeal>, newFavourite: FavouriteMeal?) {
        throwIfFailing()
        mealsFlow.value = mealsFlow.value + meals.map { it.copy(id = "meal-${nextId++}") }
        if (newFavourite != null) {
            favouritesFlow.value = favouritesFlow.value + newFavourite.copy(id = "fav-${nextId++}")
        }
    }

    override suspend fun setEaten(id: String, eaten: Boolean) {
        throwIfFailing()
        mealsFlow.value = mealsFlow.value.map { if (it.id == id) it.copy(eaten = eaten) else it }
    }

    override suspend fun deleteMeal(id: String) {
        throwIfFailing()
        mealsFlow.value = mealsFlow.value.filterNot { it.id == id }
    }

    override suspend fun setWaterGlasses(date: String, glasses: Int) {
        throwIfFailing()
        waterFlow.value = waterFlow.value + (date to glasses)
    }

    private val restDays = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    fun storedRestDayChoice(date: String): Boolean? = restDays.value[date]

    override fun observeRestDayChoice(date: String): Flow<Boolean?> = restDays.map { it[date] }

    override suspend fun setRestDayChoice(date: String, restDay: Boolean?) {
        throwIfFailing()
        restDays.value = if (restDay == null) restDays.value - date else restDays.value + (date to restDay)
    }

    private fun throwIfFailing() {
        failNext?.let {
            failNext = null
            throw it
        }
    }
}
