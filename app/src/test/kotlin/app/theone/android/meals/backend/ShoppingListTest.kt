package app.theone.android.meals.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ShoppingListTest {
    private val chilli = Recipe(
        id = "chilli",
        name = "Beef chilli",
        portions = 4,
        ingredients = listOf(
            RecipeIngredient("Beef mince", 500.0, "g", "Meat & fish"),
            RecipeIngredient("Kidney beans", 2.0, "tins", "Cupboard"),
            RecipeIngredient("Onion", 2.0, "", "Fruit & veg")
        )
    )
    private val omelette = Recipe(
        id = "omelette",
        name = "Veggie omelette",
        portions = 1,
        ingredients = listOf(RecipeIngredient("eggs", 3.0, "", "Dairy & eggs"), RecipeIngredient("Onion", 0.5, "", "Fruit & veg"))
    )

    private fun planned(recipeId: String, date: String = "2026-11-20") = PlannedMeal(date = date, recipeId = recipeId)

    @Test
    fun `each planned portion adds the recipe's ingredients divided by its portions, summed`() {
        val meals = listOf(planned("chilli"), planned("chilli"), planned("omelette"))
        val groups = buildShoppingList(meals, listOf(chilli, omelette), ShoppingState())

        val items = groups.flatMap { it.items }.associateBy { it.name }
        assertEquals(250.0, items.getValue("Beef mince").qty, 0.001) // 2 portions of 4
        assertEquals(1.0, items.getValue("Kidney beans").qty, 0.001)
        assertEquals(1.5, items.getValue("Onion").qty, 0.001) // 2 x 0.5 + 0.5, same name across recipes
        assertEquals(3.0, items.getValue("eggs").qty, 0.001)
    }

    @Test
    fun `groups follow aisle order and own items sit under Other`() {
        val groups = buildShoppingList(
            listOf(planned("chilli")),
            listOf(chilli),
            ShoppingState(manual = listOf(ManualShoppingItem("Bin bags")))
        )

        assertEquals(listOf("Fruit & veg", "Meat & fish", "Cupboard", "Other"), groups.map { it.aisle })
        assertEquals(0, groups.last().items.single().manualIndex)
    }

    @Test
    fun `meals without a recipe, or with a deleted one, add nothing`() {
        val groups = buildShoppingList(
            listOf(PlannedMeal(name = "Protein bar"), planned("gone")),
            listOf(chilli),
            ShoppingState()
        )

        assertTrue(groups.isEmpty())
    }

    @Test
    fun `ticks toggle by key, own items in place, and the count drops`() {
        var state = ShoppingState(manual = listOf(ManualShoppingItem("Bin bags")))
        var groups = buildShoppingList(listOf(planned("omelette")), listOf(omelette), state)
        assertEquals(3, groups.toBuy())

        val eggs = groups.flatMap { it.items }.first { it.name == "eggs" }
        state = state.toggle(eggs)
        val binBags = groups.flatMap { it.items }.first { it.manualIndex == 0 }
        state = state.toggle(binBags)
        groups = buildShoppingList(listOf(planned("omelette")), listOf(omelette), state)

        assertEquals(1, groups.toBuy())
        assertTrue(state.manual.single().checked)
        assertFalse(state.toggle(eggs).checked.contains(eggs.key))
    }

    @Test
    fun `own items are trimmed and blanks ignored`() {
        assertEquals("Bin bags", ShoppingState().addManual("  Bin bags ").manual.single().name)
        assertEquals(ShoppingState(), ShoppingState().addManual("   "))
    }

    @Test
    fun `range is today to the week's end, or the whole week when it isn't this week`() {
        val week = (16..22).map { LocalDate.of(2026, 11, it) }

        assertEquals(LocalDate.of(2026, 11, 19) to LocalDate.of(2026, 11, 22), shoppingRange(week, LocalDate.of(2026, 11, 19)))
        assertEquals(week.first() to week.last(), shoppingRange(week, LocalDate.of(2026, 11, 10)))
    }

    @Test
    fun `a recipe draft needs a name, portions and calories`() {
        val ingredients = listOf(RecipeIngredient(" Beans ", 2.0, " tins "), RecipeIngredient(""))
        val ok = recipeFromDraft("", " Chilli ", "4", "", "520", "42", "", "20", ingredients, 7)

        assertEquals("Chilli", ok?.name)
        assertEquals(0, ok?.cookMinutes)
        assertEquals(0, ok?.carbsG)
        assertEquals(listOf(RecipeIngredient("Beans", 2.0, "tins")), ok?.ingredients)
        assertNull(recipeFromDraft("", "Chilli", "0", "", "520", "", "", "", emptyList(), 7))
        assertNull(recipeFromDraft("", "Chilli", "4", "", "", "", "", "", emptyList(), 7))
        assertNull(recipeFromDraft("", " ", "4", "", "520", "", "", "", emptyList(), 7))
        assertNull(recipeFromDraft("", "Chilli", "4", "x", "520", "", "", "", emptyList(), 7))
    }
}
