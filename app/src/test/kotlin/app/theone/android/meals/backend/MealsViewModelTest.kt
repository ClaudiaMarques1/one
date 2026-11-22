package app.theone.android.meals.backend

import app.theone.android.settings.backend.FakeSettingsRepository
import app.theone.android.testing.MainDispatcherRule
import app.theone.android.workout.sessions.backend.FakeStrengthRepository
import app.theone.android.workout.sessions.backend.StrengthSession
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class MealsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 19)
    private val todayKey = today.toString()

    private fun viewModel(
        meals: FakeMealsRepository = FakeMealsRepository(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        strength: FakeStrengthRepository = FakeStrengthRepository()
    ) = MealsViewModel(settings, meals, strength, today)

    private fun TestScope.collect(viewModel: MealsViewModel) {
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.day.collect {} }
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.favourites.collect {} }
    }

    @Test
    fun `an empty day shows the four default slots and rest targets`() = runTest {
        val viewModel = viewModel()
        collect(viewModel)

        val day = viewModel.day.value
        assertEquals(todayKey, day.date)
        assertEquals(4, day.slots.size)
        assertEquals(DEFAULT_REST_TARGETS, day.targets)
        assertEquals(0, day.planned.kcal)
    }

    @Test
    fun `a workout logged today makes it a training day`() = runTest {
        val strength = FakeStrengthRepository(listOf(StrengthSession(date = todayKey, templateName = "Leg day")))
        val viewModel = viewModel(strength = strength)
        collect(viewModel)

        assertEquals("Leg day", viewModel.day.value.workoutName)
        assertEquals(DEFAULT_TRAINING_TARGETS, viewModel.day.value.targets)
    }

    @Test
    fun `a workout on another day doesn't count`() = runTest {
        val strength = FakeStrengthRepository(listOf(StrengthSession(date = "2026-11-18", templateName = "Push")))
        val viewModel = viewModel(strength = strength)
        collect(viewModel)

        assertNull(viewModel.day.value.workoutName)
    }

    @Test
    fun `adding favourites plans them into the slot, not eaten`() = runTest {
        val meals = FakeMealsRepository()
        val viewModel = viewModel(meals = meals)
        collect(viewModel)

        viewModel.addFavourites(
            MealSlot.LUNCH,
            listOf(
                FavouriteMeal(name = "Turkey wrap", kcal = 450, proteinG = 35, carbsG = 42, fatG = 14),
                FavouriteMeal(name = "Apple", kcal = 95)
            )
        )

        val lunch = viewModel.day.value.slots.first { it.slot == MealSlot.LUNCH }
        assertEquals(listOf("Turkey wrap", "Apple"), lunch.meals.map { it.name })
        assertTrue(meals.storedMeals.all { it.date == todayKey && !it.eaten })
        assertEquals(545, viewModel.day.value.planned.kcal)
        assertEquals(0, viewModel.day.value.eaten.kcal)
    }

    @Test
    fun `a new meal saved to favourites is stored in both places`() = runTest {
        val meals = FakeMealsRepository()
        val viewModel = viewModel(meals = meals)
        collect(viewModel)

        viewModel.addNewMeal(MealSlot.SNACKS, "  Cottage cheese  ", 220, 24, 20, 4, saveToFavourites = true)

        assertEquals("Cottage cheese", meals.storedMeals.single().name)
        assertEquals(MealSlot.SNACKS.name, meals.storedMeals.single().slot)
        assertEquals("Cottage cheese", meals.storedFavourites.single().name)
        assertEquals(220, viewModel.favourites.value.single().kcal)
    }

    @Test
    fun `a new meal without save-to-favourites isn't saved as a favourite, and a blank name is ignored`() = runTest {
        val meals = FakeMealsRepository()
        val viewModel = viewModel(meals = meals)
        collect(viewModel)

        viewModel.addNewMeal(MealSlot.SNACKS, "Banana", 105, 1, 27, 0, saveToFavourites = false)
        viewModel.addNewMeal(MealSlot.SNACKS, "   ", 100, 0, 0, 0, saveToFavourites = true)

        assertEquals(1, meals.storedMeals.size)
        assertTrue(meals.storedFavourites.isEmpty())
    }

    @Test
    fun `ticking a meal eaten moves it into the eaten totals, and back`() = runTest {
        val meals = FakeMealsRepository(
            listOf(PlannedMeal(id = "m1", date = todayKey, slot = MealSlot.LUNCH.name, name = "Bowl", kcal = 640, proteinG = 48))
        )
        val viewModel = viewModel(meals = meals)
        collect(viewModel)
        val meal = viewModel.day.value.slots.first { it.slot == MealSlot.LUNCH }.meals.single()

        viewModel.setEaten(meal, true)
        assertEquals(640, viewModel.day.value.eaten.kcal)
        assertEquals(48, viewModel.day.value.eaten.proteinG)

        viewModel.setEaten(meal, false)
        assertEquals(0, viewModel.day.value.eaten.kcal)
        assertEquals(640, viewModel.day.value.planned.kcal)
    }

    @Test
    fun `removing a meal deletes it`() = runTest {
        val meals = FakeMealsRepository(listOf(PlannedMeal(id = "m1", date = todayKey, slot = MealSlot.DINNER.name, kcal = 500)))
        val viewModel = viewModel(meals = meals)
        collect(viewModel)

        viewModel.removeMeal(meals.storedMeals.single())

        assertTrue(meals.storedMeals.isEmpty())
        assertEquals(0, viewModel.day.value.planned.kcal)
    }

    @Test
    fun `water taps follow the glass rule and are stored for today`() = runTest {
        val meals = FakeMealsRepository()
        val viewModel = viewModel(meals = meals)
        collect(viewModel)

        viewModel.tapWaterGlass(5)
        assertEquals(5, meals.storedWater(todayKey))
        viewModel.tapWaterGlass(5)
        assertEquals(4, viewModel.day.value.waterGlasses)
    }

    @Test
    fun `meal settings drive the targets and the water target`() = runTest {
        val custom = MealSettings(restTargets = MealTargets(2000, 120, 200, 70), waterGlasses = 12)
        val viewModel = viewModel(settings = FakeSettingsRepository(mealSettings = custom))
        collect(viewModel)

        assertEquals(2000, viewModel.day.value.targets.kcal)
        assertEquals(12, viewModel.day.value.waterTargetGlasses)
    }

    @Test
    fun `the arrows move a day at a time, and new meals land on the day shown`() = runTest {
        val meals = FakeMealsRepository(
            listOf(PlannedMeal(id = "y1", date = "2026-11-18", slot = MealSlot.DINNER.name, name = "Chilli", kcal = 520))
        )
        val viewModel = viewModel(meals = meals)
        collect(viewModel)

        viewModel.showNextDay()
        assertEquals("2026-11-20", viewModel.day.value.date)
        viewModel.addNewMeal(MealSlot.BREAKFAST, "Oats", 380, 18, 52, 11, saveToFavourites = false)
        assertEquals("2026-11-20", meals.storedMeals.last().date)

        viewModel.showPreviousDay()
        viewModel.showPreviousDay()
        assertEquals("2026-11-18", viewModel.day.value.date)
        assertEquals(520, viewModel.day.value.planned.kcal)

        viewModel.showToday()
        assertEquals(todayKey, viewModel.day.value.date)
        assertEquals(0, viewModel.day.value.planned.kcal)
    }

    @Test
    fun `a failed write surfaces mealsError`() = runTest {
        val meals = FakeMealsRepository().apply { failNext = IllegalStateException("offline") }
        val viewModel = viewModel(meals = meals)
        collect(viewModel)

        viewModel.addNewMeal(MealSlot.SNACKS, "Bar", 210, 20, 22, 7, saveToFavourites = false)

        assertEquals("offline", viewModel.mealsError.value)
        assertTrue(meals.storedMeals.isEmpty())
        viewModel.clearMealsError()
        assertNull(viewModel.mealsError.value)
    }
}
