package app.theone.android.meals.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.meals.backend.Recipe
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.MealsColors
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmMutedIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface

// Meals -> Recipes (#60): Owned (your recipes) | Search Recipe (placeholder; the lookup has its own card).

@Composable
fun RecipesContent(recipes: List<Recipe>, onNewRecipe: () -> Unit, onOpenRecipe: (Recipe) -> Unit) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize().background(WarmSurface)) {
        MealsSegmentedControl(
            labels = listOf("Owned", "Search Recipe"),
            selected = section,
            onSelect = { section = it },
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)
        )
        when (section) {
            0 -> OwnedRecipes(recipes, onNewRecipe, onOpenRecipe, Modifier.weight(1f))
            else -> SearchRecipesComingSoon(Modifier.weight(1f))
        }
    }
}

@Composable
private fun OwnedRecipes(recipes: List<Recipe>, onNewRecipe: () -> Unit, onOpenRecipe: (Recipe) -> Unit, modifier: Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = recipes.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    Column(modifier = modifier) {
        if (recipes.isNotEmpty()) MealsSearchField(query = query, onQueryChange = { query = it }, placeholder = "Search your recipes")
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
        ) {
            when {
                recipes.isEmpty() -> item {
                    Note(
                        "No recipes yet. Write your first one - name, portions, calories per portion and " +
                            "the ingredients for the shopping list."
                    )
                }
                shown.isEmpty() -> item { Note("Nothing matches \"${query.trim()}\".") }
                else -> item {
                    val shape = RoundedCornerShape(20.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(CardSurface)
                            .border(1.dp, WarmCardBorder, shape)
                    ) {
                        shown.forEachIndexed { index, recipe ->
                            if (index > 0) HorizontalDivider(color = WarmDivider)
                            RecipeRow(recipe) { onOpenRecipe(recipe) }
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = WarmDivider)
        Button(
            onClick = onNewRecipe,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)
                .height(48.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  New recipe", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RecipeRow(recipe: Recipe, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open recipe", onClick = onClick)
            .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(recipe.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface)
            Text(
                recipeLine(recipe.kcal, recipe.proteinG, recipe.carbsG, recipe.fatG, recipe.cookMinutes),
                fontSize = 12.sp,
                color = WarmSecondaryText,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(recipeServingLine(recipe.portions, recipe.ingredients.size), fontSize = 12.sp, color = WarmSecondaryText)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = WarmMutedIcon)
    }
}

@Composable
private fun SearchRecipesComingSoon(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Icon(Icons.Outlined.TravelExplore, contentDescription = null, tint = MealsColors.Accent, modifier = Modifier.size(40.dp))
        Text("Search recipes is coming soon", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface)
        Text(
            "Look up recipes for ideas and save the ones you like to Owned.",
            fontSize = 14.sp,
            color = WarmSecondaryText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Note(text: String) {
    Text(text, fontSize = 14.sp, color = WarmSecondaryText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp))
}
