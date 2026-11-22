package app.theone.android.meals.frontend

import android.text.format.DateFormat
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.theone.android.meals.backend.MealSettings
import app.theone.android.meals.backend.MealSlot
import app.theone.android.meals.backend.MealsDay
import app.theone.android.meals.backend.MealsWeek
import app.theone.android.meals.backend.PlannedMeal
import app.theone.android.meals.backend.TimelineRow
import app.theone.android.meals.backend.buildMealsDay
import app.theone.android.meals.backend.buildTimeline
import app.theone.android.meals.backend.kcalLeft
import app.theone.android.meals.backend.kcalMorePlanned
import app.theone.android.meals.backend.lineAboveEaten
import app.theone.android.meals.backend.weekOf
import app.theone.android.settings.backend.WeekStart
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
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface
import java.time.LocalDate

// 1b in the meals handoff: the day as a timeline, with a week strip to move between days. Renders
// MealsViewModel's day + week; every change goes back through the callbacks.

private val TabularNumbers = "tnum"
private val BigNumberStyle = SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)

@Composable
fun MealsTimelineContent(
    day: MealsDay,
    week: MealsWeek,
    settings: MealSettings,
    onSelectDate: (String) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onAddToSlot: (MealSlot) -> Unit,
    onToggleEaten: (PlannedMeal, Boolean) -> Unit,
    onRemoveMeal: (PlannedMeal) -> Unit,
    onTapGlass: (Int) -> Unit,
    onRestDayChange: (Boolean) -> Unit = {},
    onOpenWeekPlan: () -> Unit = {},
    onOpenShoppingList: () -> Unit = {},
    todayKey: String = day.date
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val rows = buildTimeline(day, settings)
    Column(modifier = Modifier.fillMaxSize().background(WarmSurface)) {
        // Fixed header: month, week arrows, week strip.
        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)) {
            Row(modifier = Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    monthLabel(LocalDate.parse(day.date)),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmOnSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenShoppingList, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Shopping list", tint = WarmIcon)
                }
                IconButton(onClick = onOpenWeekPlan, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.CalendarViewWeek, contentDescription = "Week plan", tint = WarmIcon)
                }
                IconButton(onClick = onPreviousWeek, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week", tint = WarmIcon)
                }
                IconButton(onClick = onNextWeek, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week", tint = WarmIcon)
                }
            }
            WeekStrip(week = week, selected = day.date, onSelect = onSelectDate)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { RestDayRow(day, onRestDayChange) }
            item { TimelineSummary(day, isToday = day.date == todayKey) }
            // One item, not one per row: the list's 12dp item spacing would break the rail between rows.
            item {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    rows.forEachIndexed { index, row ->
                        TimelineRowItem(
                            row = row,
                            isFirst = index == 0,
                            isLast = index == rows.lastIndex,
                            lineAboveEaten = rows.lineAboveEaten(index),
                            is24Hour = is24Hour,
                            onAddToSlot = onAddToSlot,
                            onToggleEaten = onToggleEaten,
                            onRemoveMeal = onRemoveMeal
                        )
                    }
                }
            }
            item { WaterCard(day.waterGlasses, day.waterTargetGlasses, onTapGlass) }
        }
    }
}

@Composable
private fun WeekStrip(week: MealsWeek, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        week.days.forEach { key ->
            val date = LocalDate.parse(key)
            val isSelected = key == selected
            val ink = if (isSelected) MaterialTheme.colorScheme.onPrimary else WarmOnSurface
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MealsColors.Accent else Color.Transparent)
                    .clickable(role = Role.Tab, onClickLabel = "Show this day") { onSelect(key) }
                    .semantics { this.selected = isSelected }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(shortWeekday(date), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = ink, modifier = Modifier.alpha(0.8f))
                Text("${date.dayOfMonth}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = ink)
                val dot = when {
                    key !in week.mealDates -> Color.Transparent
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    else -> MealsColors.Eaten
                }
                Box(Modifier.size(5.dp).clip(CircleShape).background(dot))
            }
        }
    }
}

@Composable
private fun TimelineSummary(day: MealsDay, isToday: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MealsColors.Soft)
            .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                buildAnnotatedString {
                    withStyle(BigNumberStyle.copy(color = MealsColors.Deep)) {
                        append(formatKcal(day.kcalLeft()))
                    }
                    withStyle(SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MealsColors.Accent)) {
                        append(if (isToday) "  kcal left today" else "  kcal left")
                    }
                },
                style = TextStyle(fontFeatureSettings = TabularNumbers)
            )
            Text(
                timelineSummary(day.eaten.kcal, day.targets.kcal, day.kcalMorePlanned()),
                fontSize = 13.sp,
                color = WarmBodyText
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroTile("Protein", day.eaten.proteinG, day.targets.proteinG, Modifier.weight(1f))
            MacroTile("Carbs", day.eaten.carbsG, day.targets.carbsG, Modifier.weight(1f))
            MacroTile("Fat", day.eaten.fatG, day.targets.fatG, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MacroTile(label: String, eaten: Int, target: Int, modifier: Modifier) {
    val fill by animateFloatAsState(if (target <= 0) 0f else (eaten.toFloat() / target).coerceIn(0f, 1f), tween(200), label = "tile")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 12.sp, color = WarmSecondaryText)
        Text(
            gramsLeft(eaten, target),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = WarmOnSurface,
            style = TextStyle(fontFeatureSettings = TabularNumbers),
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MealsColors.Soft)) {
            Box(Modifier.fillMaxWidth(fill).height(4.dp).background(MealsColors.Eaten))
        }
    }
}

/** One row: time | rail (line, node, line) | content. The rail lines turn green between eaten meals. */
@Composable
private fun TimelineRowItem(
    row: TimelineRow,
    isFirst: Boolean,
    isLast: Boolean,
    lineAboveEaten: Boolean,
    is24Hour: Boolean,
    onAddToSlot: (MealSlot) -> Unit,
    onToggleEaten: (PlannedMeal, Boolean) -> Unit,
    onRemoveMeal: (PlannedMeal) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            formatMinutes(row.minutes, is24Hour),
            fontSize = 12.sp,
            color = WarmSecondaryText,
            textAlign = TextAlign.End,
            maxLines = 1,
            style = TextStyle(fontFeatureSettings = TabularNumbers),
            modifier = Modifier.width(44.dp).padding(top = 16.dp)
        )
        Rail(row, isFirst, isLast, lineAboveEaten)
        Box(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
            when (row) {
                is TimelineRow.Meal -> TimelineMealCard(row, onToggleEaten, onRemoveMeal)
                is TimelineRow.EmptySlot -> TimelineEmptySlot(row.slot) { onAddToSlot(row.slot) }
                is TimelineRow.Workout -> WorkoutLine(row.name)
            }
        }
    }
}

@Composable
private fun Rail(row: TimelineRow, isFirst: Boolean, isLast: Boolean, lineAboveEaten: Boolean) {
    val idle = WarmCardBorder
    val above by animateColorAsState(if (lineAboveEaten) MealsColors.Eaten else idle, tween(150), label = "above")
    val below by animateColorAsState(if (row.eaten) MealsColors.Eaten else idle, tween(150), label = "below")
    val nodeFill by animateColorAsState(if (row.eaten) MealsColors.Eaten else CardSurface, tween(150), label = "node")
    val nodeRing = when {
        row.eaten -> MealsColors.Eaten
        row is TimelineRow.Workout -> WarmIcon
        else -> WarmButtonOutline
    }
    Column(modifier = Modifier.width(24.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(2.dp).height(18.dp).background(if (isFirst) Color.Transparent else above))
        Box(Modifier.size(14.dp).clip(CircleShape).background(nodeFill).border(2.dp, nodeRing, CircleShape))
        Box(Modifier.width(2.dp).weight(1f).background(if (isLast) Color.Transparent else below))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineMealCard(row: TimelineRow.Meal, onToggleEaten: (PlannedMeal, Boolean) -> Unit, onRemoveMeal: (PlannedMeal) -> Unit) {
    val meal = row.meal
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(CardSurface)
                .border(1.dp, if (meal.eaten) MealsColors.Container else WarmCardBorder, shape)
                // Like the checklist: tapping the card toggles eaten too, long-press offers Remove.
                .combinedClickable(
                    onClickLabel = if (meal.eaten) "Mark not eaten" else "Mark eaten",
                    onLongClickLabel = "More options",
                    onClick = { onToggleEaten(meal, !meal.eaten) },
                    onLongClick = { menuOpen = true }
                )
                .padding(start = 14.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.slot.label.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.02.em,
                    color = MealsColors.Accent
                )
                Text(meal.name, fontSize = 15.sp, color = if (meal.eaten) WarmSecondaryText else WarmOnSurface)
                Text(
                    macroLine(meal.kcal, meal.proteinG, meal.carbsG, meal.fatG),
                    fontSize = 12.sp,
                    color = WarmSecondaryText,
                    style = TextStyle(fontFeatureSettings = TabularNumbers)
                )
            }
            EatButton(eaten = meal.eaten, onClick = { onToggleEaten(meal, !meal.eaten) })
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Remove", color = DeleteRed) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = DeleteRed) },
                onClick = {
                    menuOpen = false
                    onRemoveMeal(meal)
                }
            )
        }
    }
}

@Composable
private fun EatButton(eaten: Boolean, onClick: () -> Unit) {
    val fill by animateColorAsState(if (eaten) MealsColors.Accent else CardSurface, tween(150), label = "eatFill")
    val ink = if (eaten) MaterialTheme.colorScheme.onPrimary else MealsColors.Accent
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, if (eaten) MealsColors.Accent else WarmButtonOutline, shape)
            .clickable(role = Role.Checkbox, onClickLabel = if (eaten) "Mark not eaten" else "Mark eaten", onClick = onClick)
            .padding(start = 8.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
        Text(if (eaten) "Eaten" else "Eat", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ink)
    }
}

@Composable
private fun TimelineEmptySlot(slot: MealSlot, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(MealsColors.Planned, 16.dp, width = 1.5.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = MealsColors.Accent, modifier = Modifier.size(20.dp))
        Text(planSlotLabel(slot.label), fontSize = 14.sp, color = MealsColors.Accent)
    }
}

/** No duration yet - sessions don't store one - so just the workout's name (#56 plan). */
@Composable
private fun WorkoutLine(name: String) {
    Row(
        modifier = Modifier.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = WarmIcon, modifier = Modifier.size(20.dp))
        Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WarmBodyText)
    }
}

private val previewSettings = MealSettings(enabledSlots = MealSlot.entries.toSet())

private fun previewMeal(slot: MealSlot, name: String, kcal: Int, eaten: Boolean = false) =
    PlannedMeal(date = "2026-11-19", slot = slot.name, name = name, kcal = kcal, proteinG = 20, carbsG = 30, fatG = 10, eaten = eaten)

private val previewDay = buildMealsDay(
    date = "2026-11-19",
    meals = listOf(
        previewMeal(MealSlot.BREAKFAST, "Greek yoghurt, oats & berries", 420, eaten = true),
        previewMeal(MealSlot.LUNCH, "Chicken rice bowl", 640, eaten = true),
        previewMeal(MealSlot.SNACKS, "Protein bar", 210),
        previewMeal(MealSlot.PRE_WORKOUT, "Banana & rice cakes", 180)
    ),
    settings = previewSettings,
    workoutName = "Leg day",
    waterGlasses = 5,
    workoutMinutes = 18 * 60
)

private val previewWeek = MealsWeek(
    days = weekOf(LocalDate.of(2026, 11, 19), WeekStart.MONDAY).map { it.toString() },
    mealDates = setOf("2026-11-16", "2026-11-17", "2026-11-19")
)

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun MealsTimelinePreview() {
    OneTheme(darkTheme = false) {
        SectionTheme(SectionAccent.Meals) {
            MealsTimelineContent(previewDay, previewWeek, previewSettings, {}, {}, {}, {}, { _, _ -> }, {}, {})
        }
    }
}

/**
 * Rest day switch for the shown day (not in the handoff - asked for on #56). On by default unless a
 * workout is logged; flipping it switches the targets and the workout slots for that day only.
 */
@Composable
private fun RestDayRow(day: MealsDay, onChange: (Boolean) -> Unit) {
    val rest = !day.isTrainingDay
    // A logged workout locks the switch off: that day is always a training day.
    val locked = day.restDaySwitchLocked
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardSurface)
            .border(1.dp, WarmCardBorder, shape)
            .toggleable(value = rest, enabled = !locked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = WarmIcon, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Rest day", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface)
            Text(
                restDaySubtitle(rest, locked, formatKcal(day.targets.kcal)),
                fontSize = 13.sp,
                color = WarmSecondaryText
            )
        }
        Switch(
            checked = rest,
            onCheckedChange = null,
            enabled = !locked,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MealsColors.Accent,
                checkedBorderColor = MealsColors.Accent,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}
