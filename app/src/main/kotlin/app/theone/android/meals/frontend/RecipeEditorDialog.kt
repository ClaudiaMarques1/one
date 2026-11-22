package app.theone.android.meals.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.theone.android.meals.backend.AISLES
import app.theone.android.meals.backend.OTHER_AISLE
import app.theone.android.meals.backend.Recipe
import app.theone.android.meals.backend.RecipeIngredient
import app.theone.android.meals.backend.recipeFromDraft
import app.theone.android.ui.theme.MealsColors
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface
import java.math.BigDecimal

/** One editable ingredient row; quantity stays text until saving. */
private data class IngredientDraft(val name: String = "", val qty: String = "", val unit: String = "", val aisle: String = OTHER_AISLE)

private fun RecipeIngredient.toDraft() = IngredientDraft(
    name = name,
    qty = if (qty == 0.0) "" else BigDecimal(qty).stripTrailingZeros().toPlainString(),
    unit = unit,
    aisle = aisle
)

/**
 * Meals -> Recipes -> Owned: create or edit a recipe (not in the handoff - #60; Delete is in the
 * read-only view's menu). Calories and macros
 * are per portion; ingredient quantities are for the whole recipe, and feed the shopping list.
 */
@Composable
fun RecipeEditorDialog(recipe: Recipe?, onSave: (Recipe) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(recipe?.name.orEmpty()) }
    var portions by rememberSaveable { mutableStateOf(recipe?.portions?.toString() ?: "1") }
    var cookMinutes by rememberSaveable { mutableStateOf(recipe?.cookMinutes?.takeIf { it > 0 }?.toString().orEmpty()) }
    var kcal by rememberSaveable { mutableStateOf(recipe?.kcal?.toString().orEmpty()) }
    var protein by rememberSaveable { mutableStateOf(recipe?.proteinG?.toString().orEmpty()) }
    var carbs by rememberSaveable { mutableStateOf(recipe?.carbsG?.toString().orEmpty()) }
    var fat by rememberSaveable { mutableStateOf(recipe?.fatG?.toString().orEmpty()) }
    val ingredients = remember {
        mutableStateListOf<IngredientDraft>().apply {
            addAll(recipe?.ingredients?.map { it.toDraft() }.orEmpty())
            if (isEmpty()) add(IngredientDraft())
        }
    }

    val qtyOk = ingredients.all { it.qty.isBlank() || it.qty.trim().toDoubleOrNull()?.let { q -> q >= 0 } == true }
    val draft = if (!qtyOk) {
        null
    } else {
        recipeFromDraft(
            id = recipe?.id.orEmpty(),
            name = name,
            portions = portions,
            cookMinutes = cookMinutes,
            kcal = kcal,
            protein = protein,
            carbs = carbs,
            fat = fat,
            ingredients = ingredients.map { RecipeIngredient(it.name, it.qty.trim().toDoubleOrNull() ?: 0.0, it.unit, it.aisle) },
            createdAt = recipe?.createdAt ?: System.currentTimeMillis()
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxSize().background(WarmSurface).imePadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = WarmOnSurface)
                }
                Text(
                    if (recipe == null) "New recipe" else "Edit recipe",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmOnSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { draft?.let(onSave) }, enabled = draft != null) {
                    Text(
                        "Save",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (draft != null) MealsColors.Accent else WarmSecondaryText
                    )
                }
            }
            HorizontalDivider(color = WarmDivider)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EditorField("Recipe name", name, { name = it }, Modifier.fillMaxWidth(), numeric = false)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorField(
                        "Portions",
                        portions,
                        { portions = it },
                        Modifier.weight(1f),
                        invalid = portions.trim().toIntOrNull()?.let { it < 1 } ?: true
                    )
                    EditorField("Cook time", cookMinutes, { cookMinutes = it }, Modifier.weight(1f), suffix = "min")
                }
                Text(
                    "Per portion",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmSecondaryText,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorField("Calories", kcal, { kcal = it }, Modifier.weight(1f), suffix = "kcal")
                    EditorField("Protein", protein, { protein = it }, Modifier.weight(1f), suffix = "g")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorField("Carbs", carbs, { carbs = it }, Modifier.weight(1f), suffix = "g")
                    EditorField("Fat", fat, { fat = it }, Modifier.weight(1f), suffix = "g")
                }
                Text(
                    "Ingredients (for the whole recipe)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmSecondaryText,
                    modifier = Modifier.padding(top = 4.dp)
                )
                ingredients.forEachIndexed { index, ingredient ->
                    IngredientRow(
                        ingredient = ingredient,
                        onChange = { ingredients[index] = it },
                        onRemove = { ingredients.removeAt(index) }
                    )
                }
                TextButton(onClick = { ingredients.add(IngredientDraft()) }) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MealsColors.Accent, modifier = Modifier.size(20.dp))
                    Text("  Add ingredient", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MealsColors.Accent)
                }
            }
        }
    }
}

@Composable
private fun IngredientRow(ingredient: IngredientDraft, onChange: (IngredientDraft) -> Unit, onRemove: () -> Unit) {
    var aisleMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            EditorField("Ingredient", ingredient.name, { onChange(ingredient.copy(name = it)) }, Modifier.weight(1f), numeric = false)
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Remove ingredient", tint = WarmIcon)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            EditorField(
                "Qty",
                ingredient.qty,
                { onChange(ingredient.copy(qty = it)) },
                Modifier.width(88.dp),
                decimal = true,
                invalid = ingredient.qty.isNotBlank() && ingredient.qty.trim().toDoubleOrNull()?.let { it < 0 } != false
            )
            EditorField("Unit", ingredient.unit, { onChange(ingredient.copy(unit = it)) }, Modifier.width(88.dp), numeric = false)
            Box(modifier = Modifier.weight(1f)) {
                TextButton(onClick = { aisleMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(ingredient.aisle, fontSize = 14.sp, color = WarmOnSurface, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Aisle", tint = WarmIcon)
                }
                DropdownMenu(expanded = aisleMenu, onDismissRequest = { aisleMenu = false }) {
                    AISLES.forEach { aisle ->
                        DropdownMenuItem(
                            text = { Text(aisle) },
                            onClick = {
                                aisleMenu = false
                                onChange(ingredient.copy(aisle = aisle))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    numeric: Boolean = true,
    decimal: Boolean = false,
    suffix: String? = null,
    invalid: Boolean = numeric && !decimal && value.isNotBlank() && value.trim().toIntOrNull()?.let { it < 0 } != false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        singleLine = true,
        isError = invalid,
        keyboardOptions = when {
            decimal -> KeyboardOptions(keyboardType = KeyboardType.Decimal)
            numeric -> KeyboardOptions(keyboardType = KeyboardType.Number)
            else -> KeyboardOptions.Default
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = WarmButtonOutline,
            focusedBorderColor = MealsColors.Accent,
            focusedLabelColor = MealsColors.Accent,
            cursorColor = MealsColors.Accent
        ),
        modifier = modifier
    )
}
