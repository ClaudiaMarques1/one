package app.theone.android.meals.frontend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.theone.android.meals.backend.ChipState
import app.theone.android.meals.backend.DayState
import app.theone.android.meals.backend.MealsWeekPlan
import app.theone.android.meals.backend.MealsWeekViewModel
import app.theone.android.meals.backend.WeekPlanDay
import app.theone.android.settings.backend.DataStoreSettingsRepository
import app.theone.android.ui.ErrorBanner
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.DeleteRed
import app.theone.android.ui.theme.MealsColors
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmField
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmMutedIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// 1d in the meals handoff: the week at a glance, with copy / clear per day and Copy week. The
// Shopping list button arrives with phase 4 (#58 plan).

private val ChipLetterStyle = TextStyle(
    lineHeight = 10.sp,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
)

/** A [MealsWeekViewModel] reading meal settings from the device's DataStore. */
@Composable
fun mealsWeekViewModel(): MealsWeekViewModel = viewModel(
    factory = viewModelFactory {
        initializer { MealsWeekViewModel(DataStoreSettingsRepository(checkNotNull(this[APPLICATION_KEY]))) }
    }
)

@Composable
fun MealsWeekScreen(
    onBack: () -> Unit,
    onOpenShoppingList: (dayInWeek: String) -> Unit = {},
    viewModel: MealsWeekViewModel = mealsWeekViewModel()
) {
    val plan by viewModel.plan.collectAsState()
    val error by viewModel.mealsError.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        MealsWeekContent(
            plan = plan,
            onBack = onBack,
            onPreviousWeek = viewModel::showPreviousWeek,
            onNextWeek = viewModel::showNextWeek,
            onCopyToNextDay = viewModel::copyToNextDay,
            onCopyTo = viewModel::copyDay,
            onClearDay = viewModel::clearDay,
            onCopyWeek = viewModel::copyWeek,
            onOpenShoppingList = onOpenShoppingList
        )
        ErrorBanner(message = error, onDismiss = viewModel::clearMealsError, modifier = Modifier.align(Alignment.TopCenter))
    }
}

/** What a confirmation dialog is asking about. */
private sealed interface Confirm {
    data class ClearDay(val date: String) : Confirm

    data object CopyWeek : Confirm
}

@Composable
private fun MealsWeekContent(
    plan: MealsWeekPlan,
    onBack: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCopyToNextDay: (String) -> Unit,
    onCopyTo: (String, String) -> Unit,
    onClearDay: (String) -> Unit,
    onCopyWeek: () -> Unit,
    onOpenShoppingList: (String) -> Unit
) {
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var pickingDateFor by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(WarmSurface)) {
        WeekTopBar(plan, onBack, onPreviousWeek, onNextWeek)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp)
        ) {
            item {
                val shape = RoundedCornerShape(20.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(CardSurface)
                        .border(1.dp, WarmCardBorder, shape)
                ) {
                    plan.rows.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider(color = WarmDivider)
                        WeekDayRow(
                            row = row,
                            onCopyToNextDay = { onCopyToNextDay(row.date) },
                            onCopyTo = { pickingDateFor = row.date },
                            onClear = { confirm = Confirm.ClearDay(row.date) }
                        )
                    }
                }
            }
            item { WeekLegend() }
        }
        Column {
            HorizontalDivider(color = WarmDivider)
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { confirm = Confirm.CopyWeek },
                    enabled = plan.rows.any { it.meals.isNotEmpty() },
                    border = BorderStroke(1.dp, WarmButtonOutline),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = MealsColors.Accent, modifier = Modifier.size(20.dp))
                    Text("  Copy week", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MealsColors.Accent)
                }
                Button(
                    onClick = { plan.days.firstOrNull()?.let(onOpenShoppingList) },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("  Shopping list", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    when (val c = confirm) {
        is Confirm.ClearDay -> ConfirmDialog(
            title = "Clear this day?",
            text = "Removes every meal planned or eaten on this day. Water and the rest day setting stay.",
            confirmLabel = "Clear day",
            destructive = true,
            onConfirm = {
                onClearDay(c.date)
                confirm = null
            },
            onDismiss = { confirm = null }
        )
        Confirm.CopyWeek -> ConfirmDialog(
            title = "Copy this week?",
            text = "Adds this week's meals to the same days next week, not eaten. Nothing already planned there is removed.",
            confirmLabel = "Copy week",
            destructive = false,
            onConfirm = {
                onCopyWeek()
                confirm = null
            },
            onDismiss = { confirm = null }
        )
        null -> Unit
    }
    pickingDateFor?.let { from ->
        CopyToDateDialog(
            from = from,
            onPick = { to ->
                onCopyTo(from, to)
                pickingDateFor = null
            },
            onDismiss = { pickingDateFor = null }
        )
    }
}

@Composable
private fun WeekTopBar(plan: MealsWeekPlan, onBack: () -> Unit, onPreviousWeek: () -> Unit, onNextWeek: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmOnSurface)
        }
        Text("Week", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface, modifier = Modifier.weight(1f))
        IconButton(onClick = onPreviousWeek, modifier = Modifier.size(44.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week", tint = WarmIcon)
        }
        if (plan.days.isNotEmpty()) {
            Text(
                weekRangeLabel(LocalDate.parse(plan.days.first()), LocalDate.parse(plan.days.last())),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = WarmOnSurface
            )
        }
        IconButton(onClick = onNextWeek, modifier = Modifier.size(44.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week", tint = WarmIcon)
        }
    }
}

@Composable
private fun WeekDayRow(row: WeekPlanDay, onCopyToNextDay: () -> Unit, onCopyTo: () -> Unit, onClear: () -> Unit) {
    val date = LocalDate.parse(row.date)
    val isToday = row.state == DayState.TODAY
    val ink = if (isToday) MealsColors.Accent else WarmOnSurface
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isToday) MealsColors.TodayRow else Color.Transparent)
            .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(shortWeekday(date), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = ink)
            Text("${date.dayOfMonth}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = ink, lineHeight = 23.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    weekDaySummary(row.state, row.eatenKcal, row.plannedKcal, row.targetKcal),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = WarmOnSurface
                )
                if (row.onTarget) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "On target",
                        tint = MealsColors.Accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.chips.forEach { (slot, state) -> SlotChip(slot.label.take(1), state) }
                row.trainingLabel?.let { label ->
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = WarmIcon, modifier = Modifier.size(15.dp))
                        Text(label, fontSize = 12.sp, color = WarmIcon, maxLines = 1)
                    }
                }
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Day options", tint = WarmIcon, modifier = Modifier.size(22.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                val hasMeals = row.meals.isNotEmpty()
                DropdownMenuItem(
                    text = { Text("Copy to tomorrow") },
                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = WarmIcon) },
                    enabled = hasMeals,
                    onClick = {
                        menuOpen = false
                        onCopyToNextDay()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy to…") },
                    leadingIcon = { Icon(Icons.Outlined.Event, contentDescription = null, tint = WarmIcon) },
                    enabled = hasMeals,
                    onClick = {
                        menuOpen = false
                        onCopyTo()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Clear day", color = if (hasMeals) DeleteRed else WarmMutedIcon) },
                    leadingIcon = {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = if (hasMeals) DeleteRed else WarmMutedIcon)
                    },
                    enabled = hasMeals,
                    onClick = {
                        menuOpen = false
                        onClear()
                    }
                )
            }
        }
    }
}

@Composable
private fun SlotChip(letter: String, state: ChipState) {
    val (fill, text) = when (state) {
        ChipState.EATEN -> MealsColors.Eaten to MaterialTheme.colorScheme.onPrimary
        ChipState.PLANNED -> MealsColors.Container to MealsColors.Deep
        ChipState.EMPTY -> WarmField to WarmMutedIcon
    }
    Box(
        modifier = Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(fill),
        contentAlignment = Alignment.Center
    ) {
        Text(
            letter,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = text,
            textAlign = TextAlign.Center,
            // Centre the letter in the 18dp chip: no extra font padding above or below.
            style = ChipLetterStyle
        )
    }
}

@Composable
private fun WeekLegend() {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Planned" to MealsColors.Container, "Eaten" to MealsColors.Eaten, "Empty" to WarmField).forEach { (label, color) ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(5.dp)).background(color))
                Text(label, fontSize = 12.sp, color = WarmSecondaryText)
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) DeleteRed else MealsColors.Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = WarmOnSurface) } },
        containerColor = CardSurface
    )
}

/** Day menu -> Copy to...: a date picker starting on the next day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyToDateDialog(from: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val start = LocalDate.parse(from).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = start)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                }
            ) { Text("Copy", color = MealsColors.Accent, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = WarmOnSurface) } }
    ) {
        DatePicker(state = state)
    }
}
