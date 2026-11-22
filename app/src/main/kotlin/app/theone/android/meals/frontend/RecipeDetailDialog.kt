package app.theone.android.meals.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.theone.android.meals.backend.AISLES
import app.theone.android.meals.backend.Recipe
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.DeleteRed
import app.theone.android.ui.theme.MealsColors
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface

/**
 * Meals -> Recipes -> Owned -> a recipe, read-only (#60, as asked in review): the details, with a
 * top-bar menu to Edit (opens the editor) or Delete (after a confirmation).
 */
@Composable
fun RecipeDetailDialog(recipe: Recipe, onEdit: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxSize().background(WarmSurface)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmOnSurface)
                }
                Text(
                    recipe.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmOnSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Recipe options", tint = WarmIcon)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = WarmIcon) },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = DeleteRed) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = DeleteRed) },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            }
                        )
                    }
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
                Text(recipeDetailLine(recipe.portions, recipe.cookMinutes), fontSize = 14.sp, color = WarmSecondaryText)
                DetailCard {
                    Text("Per portion", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WarmSecondaryText)
                    Text(
                        "${formatKcal(recipe.kcal)} kcal",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MealsColors.Deep,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MacroValue("Protein", recipe.proteinG, Modifier.weight(1f))
                        MacroValue("Carbs", recipe.carbsG, Modifier.weight(1f))
                        MacroValue("Fat", recipe.fatG, Modifier.weight(1f))
                    }
                }
                Text(
                    "Ingredients (for the whole recipe)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmSecondaryText,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
                if (recipe.ingredients.isEmpty()) {
                    Text(
                        "No ingredients yet - Edit to add them.",
                        fontSize = 14.sp,
                        color = WarmSecondaryText,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    DetailCard(padded = false) {
                        val ordered = recipe.ingredients.sortedBy { AISLES.indexOf(it.aisle).let { i -> if (i < 0) AISLES.size else i } }
                        ordered.forEachIndexed { index, ingredient ->
                            if (index > 0) HorizontalDivider(color = WarmDivider)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ingredient.name, fontSize = 15.sp, color = WarmOnSurface)
                                    Text(ingredient.aisle, fontSize = 12.sp, color = WarmSecondaryText)
                                }
                                Text(formatQty(ingredient.qty, ingredient.unit), fontSize = 14.sp, color = WarmSecondaryText)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this recipe?") },
            text = { Text("Meals already planned from it stay, but they won't add to the shopping list any more.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete", color = DeleteRed, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = WarmOnSurface) } },
            containerColor = CardSurface
        )
    }
}

@Composable
private fun DetailCard(padded: Boolean = true, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardSurface)
            .border(1.dp, WarmCardBorder, shape)
            .padding(if (padded) 16.dp else 0.dp)
    ) { content() }
}

@Composable
private fun MacroValue(label: String, grams: Int, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = WarmSecondaryText)
        Text("$grams g", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface)
    }
}
