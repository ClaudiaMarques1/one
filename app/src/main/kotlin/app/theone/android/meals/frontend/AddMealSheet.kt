package app.theone.android.meals.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.meals.backend.FavouriteMeal
import app.theone.android.meals.backend.MealSlot
import app.theone.android.meals.backend.Recipe
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.MealsColors
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmDisabledFill
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmField
import app.theone.android.ui.theme.WarmHandle
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmMutedIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface

// 1c in the meals handoff: Favourites, Recipes (your owned recipes, #60) and New.

private enum class AddTab(val label: String) { FAVOURITES("Favourites"), RECIPES("Recipes"), NEW("New") }

/** What the New tab hands back: a meal to add, and whether to save it as a favourite too. */
data class NewMeal(
    val name: String,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val saveToFavourites: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealSheet(
    slot: MealSlot,
    favourites: List<FavouriteMeal>,
    recipes: List<Recipe>,
    onAddPicks: (List<FavouriteMeal>, List<Recipe>) -> Unit,
    onAddNew: (NewMeal) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(AddTab.FAVOURITES) }
    // Picks survive tab switches (handoff 1c); ids, so they're saveable.
    var picks by rememberSaveable { mutableStateOf(listOf<String>()) }
    var recipePicks by rememberSaveable { mutableStateOf(listOf<String>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var recipeQuery by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var saveToFavourites by rememberSaveable { mutableStateOf(true) }

    val newMeal: NewMeal? = run {
        val k = kcal.trim().takeIf { it.isNotEmpty() }?.let(::parseAmount)
        val p = parseAmount(protein)
        val c = parseAmount(carbs)
        val f = parseAmount(fat)
        if (name.isBlank() || k == null || p == null || c == null || f == null) null else NewMeal(name.trim(), k, p, c, f, saveToFavourites)
    }
    val newTab = tab == AddTab.NEW
    val pickCount = picks.size + recipePicks.size
    val enabled = if (newTab) newMeal != null else pickCount > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WarmSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 4.dp).size(32.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(WarmHandle))
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().imePadding()) {
            Text(
                "Add to ${slot.label}",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = WarmOnSurface,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 12.dp)
            )
            TabControl(selected = tab, onSelect = { tab = it })
            Box(modifier = Modifier.weight(1f, fill = false).heightIn(min = 320.dp)) {
                when (tab) {
                    AddTab.FAVOURITES -> FavouritesTab(
                        favourites = favourites,
                        query = query,
                        onQueryChange = { query = it },
                        picks = picks,
                        onTogglePick = { id -> picks = if (id in picks) picks - id else picks + id }
                    )
                    AddTab.RECIPES -> RecipesTab(
                        recipes = recipes,
                        query = recipeQuery,
                        onQueryChange = { recipeQuery = it },
                        picks = recipePicks,
                        onTogglePick = { id -> recipePicks = if (id in recipePicks) recipePicks - id else recipePicks + id }
                    )
                    AddTab.NEW -> NewTab(
                        name = name, onName = { name = it },
                        kcal = kcal, onKcal = { kcal = it },
                        protein = protein, onProtein = { protein = it },
                        carbs = carbs, onCarbs = { carbs = it },
                        fat = fat, onFat = { fat = it },
                        saveToFavourites = saveToFavourites, onSaveToFavourites = { saveToFavourites = it }
                    )
                }
            }
            HorizontalDivider(color = WarmDivider)
            Button(
                onClick = {
                    if (newTab) {
                        newMeal?.let(onAddNew)
                    } else {
                        onAddPicks(favourites.filter { it.id in picks }, recipes.filter { it.id in recipePicks })
                    }
                },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 28.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = WarmDisabledFill,
                    disabledContentColor = WarmMutedIcon
                )
            ) {
                Text(addButtonLabel(slot.label, pickCount, newTab), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TabControl(selected: AddTab, onSelect: (AddTab) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .border(1.dp, WarmButtonOutline, shape)
            .selectableGroup()
    ) {
        AddTab.entries.forEachIndexed { index, tab ->
            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(WarmButtonOutline))
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isSelected) MealsColors.Container else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tab.label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) MealsColors.Deep else WarmOnSurface
                )
            }
        }
    }
}

@Composable
private fun FavouritesTab(
    favourites: List<FavouriteMeal>,
    query: String,
    onQueryChange: (String) -> Unit,
    picks: List<String>,
    onTogglePick: (String) -> Unit
) {
    PickList(
        rows = favourites.map { PickRow(it.id, it.name, macroLine(it.kcal, it.proteinG, it.carbsG, it.fatG)) },
        query = query,
        onQueryChange = onQueryChange,
        placeholder = "Search favourites",
        emptyNote = "No favourites yet. Add a meal on the New tab and keep \"Save to favourites\" on.",
        picks = picks,
        onTogglePick = onTogglePick
    )
}

/** Recipes tab (1c): the owned recipes, macros per portion plus cook time. Picking adds one portion. */
@Composable
private fun RecipesTab(
    recipes: List<Recipe>,
    query: String,
    onQueryChange: (String) -> Unit,
    picks: List<String>,
    onTogglePick: (String) -> Unit
) {
    PickList(
        rows = recipes.map { PickRow(it.id, it.name, recipeLine(it.kcal, it.proteinG, it.carbsG, it.fatG, it.cookMinutes)) },
        query = query,
        onQueryChange = onQueryChange,
        placeholder = "Search recipes",
        emptyNote = "No recipes yet. Write one in Meals -> Recipes -> Owned.",
        picks = picks,
        onTogglePick = onTogglePick
    )
}

private data class PickRow(val id: String, val name: String, val line: String)

/** Search + a multi-pick list, shared by the Favourites and Recipes tabs. */
@Composable
private fun PickList(
    rows: List<PickRow>,
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    emptyNote: String,
    picks: List<String>,
    onTogglePick: (String) -> Unit
) {
    val shown = rows.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    Column {
        MealsSearchField(query = query, onQueryChange = onQueryChange, placeholder = placeholder)
        when {
            rows.isEmpty() -> EmptyNote(emptyNote)
            shown.isEmpty() -> EmptyNote("Nothing matches \"${query.trim()}\".")
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                items(shown, key = { it.id }) { row ->
                    val picked = row.id in picks
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = picked, role = Role.Checkbox, onValueChange = { onTogglePick(row.id) })
                            .padding(start = 24.dp, top = 10.dp, end = 16.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.name, fontSize = 15.sp, color = WarmOnSurface)
                            Text(row.line, fontSize = 12.sp, color = WarmSecondaryText)
                        }
                        PickToggle(picked)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickToggle(picked: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (picked) MealsColors.Accent else CardSurface)
            .border(1.dp, if (picked) MealsColors.Accent else WarmButtonOutline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (picked) Icons.Filled.Check else Icons.Filled.Add,
            contentDescription = null,
            tint = if (picked) MaterialTheme.colorScheme.onPrimary else MealsColors.Accent,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
internal fun MealsSearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(WarmField)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = WarmIcon, modifier = Modifier.size(22.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) Text(placeholder, fontSize = 15.sp, color = WarmSecondaryText)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = WarmOnSurface),
                cursorBrush = SolidColor(MealsColors.Accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        color = WarmSecondaryText,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
    )
}

@Composable
private fun NewTab(
    name: String,
    onName: (String) -> Unit,
    kcal: String,
    onKcal: (String) -> Unit,
    protein: String,
    onProtein: (String) -> Unit,
    carbs: String,
    onCarbs: (String) -> Unit,
    fat: String,
    onFat: (String) -> Unit,
    saveToFavourites: Boolean,
    onSaveToFavourites: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MealField("Meal name", name, onName, Modifier.fillMaxWidth(), numeric = false)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MealField("Calories", kcal, onKcal, Modifier.weight(1f), suffix = "kcal")
            MealField("Protein", protein, onProtein, Modifier.weight(1f), suffix = "g")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MealField("Carbs", carbs, onCarbs, Modifier.weight(1f), suffix = "g")
            MealField("Fat", fat, onFat, Modifier.weight(1f), suffix = "g")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = saveToFavourites, role = Role.Switch, onValueChange = onSaveToFavourites)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Save to favourites", fontSize = 15.sp, color = WarmOnSurface, modifier = Modifier.weight(1f))
            Switch(
                checked = saveToFavourites,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = MealsColors.Accent, checkedBorderColor = MealsColors.Accent)
            )
        }
    }
}

@Composable
private fun MealField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    numeric: Boolean = true,
    suffix: String? = null
) {
    val invalid = numeric && value.isNotBlank() && parseAmount(value) == null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        singleLine = true,
        isError = invalid,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
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
