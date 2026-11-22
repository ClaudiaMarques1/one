package app.theone.android.meals.backend

/** Shopping list aisles, in list order. Stored by label on ingredients. */
val AISLES = listOf("Fruit & veg", "Meat & fish", "Dairy & eggs", "Cupboard", "Frozen", "Bakery", "Other")

const val OTHER_AISLE = "Other"

/** One ingredient line of a recipe: the quantity is for the whole recipe ([Recipe.portions] portions). */
data class RecipeIngredient(
    val name: String = "",
    val qty: Double = 0.0,
    val unit: String = "",
    val aisle: String = OTHER_AISLE
)

/**
 * A recipe the user wrote (`users/{uid}/recipes`) - Meals -> Recipes -> Owned. Calories and macros
 * are per portion; the ingredient quantities are for the whole recipe.
 */
data class Recipe(
    val id: String = "",
    val name: String = "",
    val portions: Int = 1,
    val cookMinutes: Int = 0,
    val kcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

/** An item the user added to the shopping list by hand (always in Other). */
data class ManualShoppingItem(
    val name: String = "",
    val checked: Boolean = false
)

/** Ticks and own items for one week's shopping list (`users/{uid}/shopping/{week start}`). */
data class ShoppingState(
    /** Keys of ticked recipe items ([ShoppingItem.key]). */
    val checked: List<String> = emptyList(),
    val manual: List<ManualShoppingItem> = emptyList()
)
