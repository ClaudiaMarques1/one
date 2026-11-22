package app.theone.android.meals.backend

import java.time.LocalDate

/** One shopping list row. [key] identifies a recipe item's tick; own items are keyed by position. */
data class ShoppingItem(
    val key: String,
    val name: String,
    val qty: Double,
    val unit: String,
    val aisle: String,
    val checked: Boolean,
    val manualIndex: Int? = null
)

data class ShoppingGroup(val aisle: String, val items: List<ShoppingItem>)

/** The dates a week's shopping list covers: today to the week's end, or the whole week when it isn't this week. */
fun shoppingRange(weekDays: List<LocalDate>, today: LocalDate): Pair<LocalDate, LocalDate> {
    val first = weekDays.first()
    val last = weekDays.last()
    return (if (today in first..last) today else first) to last
}

/**
 * The 1e list: every planned meal that came from a recipe adds that recipe's ingredients for one
 * portion (quantity / portions). Same name (any case) + unit + aisle are summed. Groups follow
 * [AISLES]; own items go under Other. Meals whose recipe was deleted add nothing.
 */
fun buildShoppingList(meals: List<PlannedMeal>, recipes: List<Recipe>, state: ShoppingState): List<ShoppingGroup> {
    val byId = recipes.associateBy { it.id }
    val totals = linkedMapOf<String, ShoppingItem>()
    meals.mapNotNull { meal -> byId[meal.recipeId].takeIf { meal.recipeId.isNotEmpty() } }.forEach { recipe ->
        val portions = recipe.portions.coerceAtLeast(1)
        recipe.ingredients.filter { it.name.isNotBlank() }.forEach { ingredient ->
            val aisle = ingredient.aisle.takeIf { it in AISLES } ?: OTHER_AISLE
            val key = shoppingKey(ingredient.name, ingredient.unit, aisle)
            val perPortion = ingredient.qty / portions
            val current = totals[key]
            totals[key] = current?.copy(qty = current.qty + perPortion)
                ?: ShoppingItem(key, ingredient.name.trim(), perPortion, ingredient.unit.trim(), aisle, checked = key in state.checked)
        }
    }
    val manual = state.manual.mapIndexed { index, item ->
        ShoppingItem("manual-$index", item.name, 0.0, "", OTHER_AISLE, item.checked, manualIndex = index)
    }
    val all = totals.values + manual
    return AISLES.mapNotNull { aisle ->
        all.filter { it.aisle == aisle }.takeIf { it.isNotEmpty() }?.let { ShoppingGroup(aisle, it) }
    }
}

fun shoppingKey(name: String, unit: String, aisle: String): String =
    "${name.trim().lowercase()}|${unit.trim().lowercase()}|$aisle"

/** Unticked items to buy. */
fun List<ShoppingGroup>.toBuy(): Int = sumOf { group -> group.items.count { !it.checked } }

/** Toggles [item]'s tick in [state]: recipe items by key, own items in place. */
fun ShoppingState.toggle(item: ShoppingItem): ShoppingState = when (val index = item.manualIndex) {
    null -> copy(checked = if (item.key in checked) checked - item.key else checked + item.key)
    else -> copy(manual = manual.mapIndexed { i, m -> if (i == index) m.copy(checked = !m.checked) else m })
}

/** Adds an own item (trimmed); blank names are ignored. */
fun ShoppingState.addManual(name: String): ShoppingState =
    name.trim().takeIf { it.isNotEmpty() }?.let { copy(manual = manual + ManualShoppingItem(it)) } ?: this

/**
 * A recipe from the editor's text fields, or null while it isn't valid: a name, portions >= 1,
 * calories given; cook time and macros default to 0. Ingredient rows without a name are dropped.
 */
fun recipeFromDraft(
    id: String,
    name: String,
    portions: String,
    cookMinutes: String,
    kcal: String,
    protein: String,
    carbs: String,
    fat: String,
    ingredients: List<RecipeIngredient>,
    createdAt: Long
): Recipe? {
    val trimmed = name.trim()
    val portionCount = portions.trim().toIntOrNull()?.takeIf { it >= 1 } ?: return null
    val calories = kcal.trim().toIntOrNull()?.takeIf { it >= 0 } ?: return null

    fun optional(text: String): Int? = if (text.isBlank()) 0 else text.trim().toIntOrNull()?.takeIf { it >= 0 }
    val minutes = optional(cookMinutes) ?: return null
    val p = optional(protein) ?: return null
    val c = optional(carbs) ?: return null
    val f = optional(fat) ?: return null
    if (trimmed.isEmpty()) return null
    return Recipe(
        id = id,
        name = trimmed,
        portions = portionCount,
        cookMinutes = minutes,
        kcal = calories,
        proteinG = p,
        carbsG = c,
        fatG = f,
        ingredients = ingredients.filter { it.name.isNotBlank() }.map { it.copy(name = it.name.trim(), unit = it.unit.trim()) },
        createdAt = createdAt
    )
}
