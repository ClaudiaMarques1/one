package app.theone.android.meals.backend

import app.theone.android.settings.backend.FakeSettingsRepository
import app.theone.android.testing.MainDispatcherRule
import app.theone.android.workout.sessions.backend.FakeStrengthRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class RecipesAndShoppingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 19)
    private val chilli = Recipe(
        id = "chilli",
        name = "Beef chilli",
        portions = 4,
        kcal = 520,
        proteinG = 42,
        ingredients = listOf(RecipeIngredient("Beef mince", 500.0, "g", "Meat & fish"))
    )

    @Test
    fun `recipes are listed by name, saved, edited and deleted`() = runTest {
        val repository = FakeRecipesRepository()
        val viewModel = RecipesViewModel(repository)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.recipes.collect {} }

        viewModel.saveRecipe(Recipe(name = "Pasta bake", portions = 6))
        viewModel.saveRecipe(chilli.copy(id = ""))
        assertEquals(listOf("Beef chilli", "Pasta bake"), viewModel.recipes.value.map { it.name })

        val saved = viewModel.recipes.value.first()
        viewModel.saveRecipe(saved.copy(cookMinutes = 50))
        assertEquals(50, repository.storedRecipes.first { it.id == saved.id }.cookMinutes)
        assertEquals(2, repository.storedRecipes.size)

        viewModel.deleteRecipe(saved.id)
        assertEquals(listOf("Pasta bake"), viewModel.recipes.value.map { it.name })
    }

    @Test
    fun `a failed recipe save surfaces recipesError`() = runTest {
        val repository = FakeRecipesRepository().apply { failNext = IllegalStateException("offline") }
        val viewModel = RecipesViewModel(repository)

        viewModel.saveRecipe(chilli)

        assertEquals("offline", viewModel.recipesError.value)
        viewModel.clearRecipesError()
        assertNull(viewModel.recipesError.value)
    }

    @Test
    fun `adding picks plans favourites and one portion of each recipe, keeping the recipe id`() = runTest {
        val meals = FakeMealsRepository()
        val viewModel = MealsViewModel(FakeSettingsRepository(), meals, FakeStrengthRepository(), today)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.day.collect {} }

        viewModel.addPicks(MealSlot.DINNER, listOf(FavouriteMeal(name = "Salad", kcal = 200)), listOf(chilli))

        assertEquals(listOf("", "chilli"), meals.storedMeals.map { it.recipeId })
        assertEquals(720, viewModel.day.value.planned.kcal)
    }

    @Test
    fun `the shopping list covers today to the week's end and keeps ticks and own items per week`() = runTest {
        val meals = FakeMealsRepository(
            listOf(
                PlannedMeal(id = "past", date = "2026-11-17", recipeId = "chilli"),
                PlannedMeal(id = "a", date = "2026-11-20", recipeId = "chilli"),
                PlannedMeal(id = "b", date = "2026-11-21", recipeId = "chilli")
            )
        )
        val recipes = FakeRecipesRepository(listOf(chilli))
        val settings = FakeSettingsRepository(mealSettings = MealSettings(weekStart = WeekStart.MONDAY))
        val viewModel = ShoppingListViewModel(settings, today, meals, recipes, today)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.list.collect {} }

        val list = viewModel.list.value
        assertEquals("2026-11-16", list.weekStart)
        assertEquals("2026-11-19", list.from)
        assertEquals("2026-11-22", list.to)
        val mince = list.groups.single().items.single()
        assertEquals(250.0, mince.qty, 0.001) // two portions in range; Tuesday's is before today

        viewModel.toggle(mince)
        viewModel.addItem("Bin bags")

        assertTrue(mince.key in recipes.storedShopping("2026-11-16").checked)
        assertEquals("Bin bags", recipes.storedShopping("2026-11-16").manual.single().name)
        assertEquals(1, viewModel.list.value.toBuy)
    }
}
