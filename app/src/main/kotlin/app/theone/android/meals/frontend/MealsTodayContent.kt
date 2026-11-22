package app.theone.android.meals.frontend

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.meals.backend.MealSettings
import app.theone.android.meals.backend.MealSlot
import app.theone.android.meals.backend.MealsDay
import app.theone.android.meals.backend.PlannedMeal
import app.theone.android.meals.backend.SlotMeals
import app.theone.android.meals.backend.buildMealsDay
import app.theone.android.ui.dashedBorder
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.DeleteRed
import app.theone.android.ui.theme.MealsColors
import app.theone.android.ui.theme.OneTheme
import app.theone.android.ui.theme.SectionAccent
import app.theone.android.ui.theme.SectionTheme
import app.theone.android.ui.theme.WarmBodyText
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmField
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface
import java.time.LocalDate

// 1a in the meals handoff: Today as a checklist. Renders MealsViewModel's day; every change goes back
// through the callbacks.

private val CardShape = RoundedCornerShape(20.dp)
private val TabularNumbers = "tnum"

@Composable
fun MealsTodayContent(
    day: MealsDay,
    onAddToSlot: (MealSlot) -> Unit,
    onToggleEaten: (PlannedMeal, Boolean) -> Unit,
    onRemoveMeal: (PlannedMeal) -> Unit,
    onTapGlass: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSurface),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TodayHeader(day.date) }
        day.workoutName?.let { workout -> item { TrainingBanner(workout) } }
        item { SummaryCard(day) }
        items(day.slots, key = { it.slot.name }) { slot ->
            SlotCard(
                slot = slot,
                onAdd = { onAddToSlot(slot.slot) },
                onToggleEaten = onToggleEaten,
                onRemoveMeal = onRemoveMeal
            )
        }
        item { WaterCard(glasses = day.waterGlasses, targetGlasses = day.waterTargetGlasses, onTapGlass = onTapGlass) }
    }
}

@Composable
private fun TodayHeader(dateKey: String) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Text("Today", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, color = WarmOnSurface)
        Text(
            todayDateLabel(LocalDate.parse(dateKey)),
            fontSize = 14.sp,
            color = WarmSecondaryText,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun TrainingBanner(workoutName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarmField)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = WarmIcon, modifier = Modifier.size(18.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(workoutName) }
                append(" · targets raised for training")
            },
            fontSize = 13.sp,
            color = WarmBodyText
        )
    }
}

@Composable
private fun SummaryCard(day: MealsDay) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface)) {
                        append(formatKcal(day.eaten.kcal))
                    }
                    withStyle(SpanStyle(fontSize = 14.sp, color = WarmSecondaryText)) {
                        append(" / ${formatKcal(day.targets.kcal)} kcal")
                    }
                },
                style = TextStyle(fontFeatureSettings = TabularNumbers),
                modifier = Modifier.weight(1f)
            )
            Text("${formatKcal(day.planned.kcal)} planned", fontSize = 13.sp, color = WarmSecondaryText)
        }
        KcalBar(eaten = day.eaten.kcal, planned = day.planned.kcal, target = day.targets.kcal)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MacroColumn("Protein", day.eaten.proteinG, day.targets.proteinG, Modifier.weight(1f))
            MacroColumn("Carbs", day.eaten.carbsG, day.targets.carbsG, Modifier.weight(1f))
            MacroColumn("Fat", day.eaten.fatG, day.targets.fatG, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(MealsColors.Eaten, "Eaten")
            LegendItem(MealsColors.Planned, "Planned")
        }
    }
}

/** Eaten, then planned-but-not-eaten, each as a share of the target, clamped to the bar. */
@Composable
private fun KcalBar(eaten: Int, planned: Int, target: Int) {
    val eatenShare by animateFloatAsState(share(eaten, target), tween(200), label = "eatenShare")
    val plannedShare by animateFloatAsState(share(planned, target), tween(200), label = "plannedShare")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MealsColors.Soft)
    ) {
        Box(Modifier.fillMaxWidth(plannedShare).height(10.dp).background(MealsColors.Planned))
        Box(Modifier.fillMaxWidth(eatenShare).height(10.dp).background(MealsColors.Eaten))
    }
}

private fun share(value: Int, target: Int): Float = if (target <= 0) 0f else (value.toFloat() / target).coerceIn(0f, 1f)

@Composable
private fun MacroColumn(label: String, eaten: Int, target: Int, modifier: Modifier) {
    val fill by animateFloatAsState(share(eaten, target), tween(200), label = "macro")
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = WarmSecondaryText)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = WarmOnSurface)) { append("$eaten") }
                withStyle(SpanStyle(color = WarmSecondaryText)) { append(" / $target g") }
            },
            fontSize = 14.sp,
            style = TextStyle(fontFeatureSettings = TabularNumbers),
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MealsColors.Soft)
        ) {
            Box(Modifier.fillMaxWidth(fill).height(4.dp).background(MealsColors.Eaten))
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, fontSize = 12.sp, color = WarmSecondaryText)
    }
}

@Composable
private fun SlotCard(
    slot: SlotMeals,
    onAdd: () -> Unit,
    onToggleEaten: (PlannedMeal, Boolean) -> Unit,
    onRemoveMeal: (PlannedMeal) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().card()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(slot.slot.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface)
            if (slot.meals.isNotEmpty()) {
                Text("${formatKcal(slot.kcal)} kcal", fontSize = 13.sp, color = WarmSecondaryText)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAdd, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add to ${slot.slot.label}",
                    tint = MealsColors.Accent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (slot.meals.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 14.dp)
                    .fillMaxWidth()
                    .dashedBorder(MealsColors.Planned, 12.dp, width = 1.5.dp)
                    .clickable(onClick = onAdd)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Add from favourites or recipes", fontSize = 14.sp, color = MealsColors.Accent)
            }
        } else {
            slot.meals.forEach { meal ->
                MealRow(meal = meal, onToggleEaten = { onToggleEaten(meal, !meal.eaten) }, onRemove = { onRemoveMeal(meal) })
            }
        }
    }
}

/** Tap (the circle or the row) toggles eaten; long-press offers Remove - not in the mock, see the #53 plan. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealRow(meal: PlannedMeal, onToggleEaten: () -> Unit, onRemove: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val ring by animateColorAsState(if (meal.eaten) MealsColors.Eaten else WarmButtonOutline, tween(150), label = "ring")
    val fill by animateColorAsState(if (meal.eaten) MealsColors.Eaten else CardSurface, tween(150), label = "fill")
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    role = Role.Checkbox,
                    onClickLabel = if (meal.eaten) "Mark not eaten" else "Mark eaten",
                    onLongClickLabel = "More options",
                    onClick = onToggleEaten,
                    onLongClick = { menuOpen = true }
                )
                .semantics { contentDescription = "${meal.name}, ${if (meal.eaten) "eaten" else "not eaten"}" }
                .padding(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .border(2.dp, ring, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (meal.eaten) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = CardSurface, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.name, fontSize = 15.sp, color = if (meal.eaten) WarmSecondaryText else WarmOnSurface)
                Text(
                    macroLine(meal.kcal, meal.proteinG, meal.carbsG, meal.fatG),
                    fontSize = 12.sp,
                    color = WarmSecondaryText,
                    style = TextStyle(fontFeatureSettings = TabularNumbers),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Remove", color = DeleteRed) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = DeleteRed) },
                onClick = {
                    menuOpen = false
                    onRemove()
                }
            )
        }
    }
}

@Composable
private fun WaterCard(glasses: Int, targetGlasses: Int, onTapGlass: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Water", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface, modifier = Modifier.weight(1f))
            Text(waterLabel(glasses, targetGlasses), fontSize = 13.sp, color = WarmSecondaryText)
        }
        // One button per glass of the target; extra glasses past the target still show.
        val count = maxOf(targetGlasses, glasses)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            for (n in 1..count) {
                val full = n <= glasses
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "Set water to ${litres(n)} L") { onTapGlass(n) }
                        .semantics { contentDescription = "Glass $n of $count, ${if (full) "full" else "empty"}" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (full) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        tint = if (full) MealsColors.Eaten else WarmButtonOutline,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.card(): Modifier =
    clip(CardShape)
        .background(CardSurface)
        .border(1.dp, WarmCardBorder, CardShape)

private fun previewMeal(slot: MealSlot, name: String, kcal: Int, p: Int, c: Int, f: Int, eaten: Boolean = false) =
    PlannedMeal(date = "2026-11-19", slot = slot.name, name = name, kcal = kcal, proteinG = p, carbsG = c, fatG = f, eaten = eaten)

private val previewDay = buildMealsDay(
    date = "2026-11-19",
    meals = listOf(
        previewMeal(MealSlot.BREAKFAST, "Greek yoghurt, oats & berries", 420, 32, 48, 10, eaten = true),
        previewMeal(MealSlot.LUNCH, "Chicken rice bowl", 640, 48, 70, 16, eaten = true),
        previewMeal(MealSlot.SNACKS, "Protein bar", 210, 20, 22, 7)
    ),
    settings = MealSettings(),
    workoutName = "Leg day",
    waterGlasses = 5
)

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun MealsTodayPreview() {
    OneTheme(darkTheme = false) {
        SectionTheme(SectionAccent.Meals) {
            MealsTodayContent(previewDay, onAddToSlot = {}, onToggleEaten = { _, _ -> }, onRemoveMeal = {}, onTapGlass = {})
        }
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun MealsTodayDarkPreview() {
    OneTheme(darkTheme = true) {
        SectionTheme(SectionAccent.Meals) {
            MealsTodayContent(previewDay, onAddToSlot = {}, onToggleEaten = { _, _ -> }, onRemoveMeal = {}, onTapGlass = {})
        }
    }
}
