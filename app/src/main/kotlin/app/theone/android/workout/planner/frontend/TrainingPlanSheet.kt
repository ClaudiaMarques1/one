package app.theone.android.workout.planner.frontend

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.ui.ErrorBanner
import app.theone.android.ui.theme.CalendarColors
import app.theone.android.workout.planner.backend.DEFAULT_TRAINING_TIME
import app.theone.android.workout.planner.backend.PlanMode
import app.theone.android.workout.planner.backend.PlanSheetUi
import app.theone.android.workout.planner.backend.TrainingPlanText
import app.theone.android.workout.planner.backend.TrainingPlanViewModel
import app.theone.android.workout.templates.backend.WorkoutTemplate
import java.time.DayOfWeek
import java.time.LocalTime

private val C = CalendarColors
private val CardShape = RoundedCornerShape(20.dp)
private val ChipShape = RoundedCornerShape(8.dp)

/**
 * 4a Training plan sheet - Routine tab (#76). Edits a draft in [viewModel]; Save plan stores it, a scrim
 * tap or back closes without saving. The Upcoming tab comes with the day editor (phase 3), Reminders and
 * Google Calendar with ticket 2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlanSheet(sheet: PlanSheetUi, viewModel: TrainingPlanViewModel, errorMessage: String?) {
    ModalBottomSheet(
        onDismissRequest = viewModel::closeSheet,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = C.Surface,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 4.dp).size(width = 32.dp, height = 4.dp).clip(CircleShape).background(C.ChevronWeak))
        }
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.92f)) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp)) {
                Text("Training plan", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface))
                Spacer(Modifier.height(2.dp))
                Text(sheet.nextUp, style = TextStyle(fontSize = 14.sp, color = C.SecondaryText))
            }
            ErrorBanner(message = errorMessage, onDismiss = viewModel::clearPlanError)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeCards(mode = sheet.draft.mode, onModeChange = viewModel::setMode)
                if (sheet.templates.isEmpty()) {
                    Text(
                        "Create a workout template in Workout first - the plan picks from your templates.",
                        style = TextStyle(fontSize = 13.sp, color = C.SecondaryText),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                when (sheet.draft.mode) {
                    PlanMode.WEEKDAY -> WeekdayCard(sheet, viewModel)
                    PlanMode.SPLIT -> SplitCards(sheet, viewModel)
                }
                TargetCard(sheet, onChange = viewModel::changeTarget)
            }

            HorizontalDivider(color = C.InnerDivider)
            Button(
                onClick = { viewModel.save {} },
                enabled = !sheet.saving,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp).height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = C.PrimaryButton, contentColor = C.OnPrimaryButton)
            ) {
                Text("Save plan", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun ModeCards(mode: PlanMode, onModeChange: (PlanMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(PlanMode.WEEKDAY, "Weekday", "Each day keeps its own workout"),
            Triple(PlanMode.SPLIT, "Rotating split", "Workouts take turns on your training days")
        ).forEach { (value, title, subtitle) ->
            val selected = value == mode
            val shape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (selected) C.AccentSoft else C.Card)
                    .border(if (selected) 2.dp else 1.dp, if (selected) C.PrimaryButton else C.OutlineButton, shape)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onModeChange(value) })
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (selected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) C.PrimaryButton else C.IconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(title, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface))
                }
                Text(subtitle, style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, color = C.SecondaryText))
            }
        }
    }
}

@Composable
private fun PlanCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(C.Card)
            .border(1.dp, C.CardBorder, CardShape)
    ) { content() }
}

@Composable
private fun WeekdayCard(sheet: PlanSheetUi, viewModel: TrainingPlanViewModel) {
    PlanCard {
        sheet.weekDays.forEachIndexed { i, day ->
            if (i > 0) HorizontalDivider(color = C.InnerDivider)
            val slot = sheet.draft.weekdayPlan[day.name]
            val template = sheet.templates.firstOrNull { it.id == slot?.templateId }
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    day.fullName(),
                    modifier = Modifier.weight(1f),
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.OnSurface)
                )
                if (template != null) {
                    TimeChip(
                        time = slot?.time ?: DEFAULT_TRAINING_TIME,
                        label = "${day.fullName()} time"
                    ) { viewModel.setWeekdayTime(day, it) }
                }
                TemplateDropdown(
                    templates = sheet.templates,
                    selected = template,
                    allowRest = true,
                    label = "${day.fullName()} workout",
                    onSelect = { viewModel.setWeekdayTemplate(day, it?.id) }
                )
            }
        }
    }
}

@Composable
private fun SplitCards(sheet: PlanSheetUi, viewModel: TrainingPlanViewModel) {
    PlanCard {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Training days", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                sheet.weekDays.forEach { day ->
                    val on = day.name in sheet.draft.splitDays
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (on) C.PrimaryButton else C.Card)
                            .border(1.dp, if (on) C.PrimaryButton else C.OutlineButton, RoundedCornerShape(20.dp))
                            .toggleable(value = on, role = Role.Checkbox, onValueChange = { viewModel.toggleSplitDay(day) })
                            .semantics { contentDescription = day.fullName() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day.fullName().take(1),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (on) C.OnPrimaryButton else C.OnSurface
                            )
                        )
                    }
                }
            }
        }
    }

    PlanCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "Order",
                modifier = Modifier.weight(1f),
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface)
            )
            Text("Cycles A to the end, then repeats", style = TextStyle(fontSize = 12.sp, color = C.SecondaryText))
        }
        val order = sheet.draft.splitOrder
        order.forEachIndexed { i, templateId ->
            HorizontalDivider(color = C.InnerDivider)
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(C.WorkoutTint), contentAlignment = Alignment.Center) {
                    Text(('A' + i).toString(), style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.WorkoutInk))
                }
                Spacer(Modifier.width(8.dp))
                OrderTemplateMenu(
                    templates = sheet.templates,
                    selected = sheet.templates.firstOrNull { it.id == templateId },
                    label = "Workout ${'A' + i}",
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.setSplitTemplate(i, it.id) }
                )
                IconButton(onClick = { viewModel.moveSplitUp(i) }, enabled = i > 0) {
                    Icon(
                        Icons.Outlined.ArrowUpward,
                        contentDescription = "Move ${'A' + i} up",
                        tint = if (i > 0) C.IconTint else C.OutlineButton
                    )
                }
                IconButton(onClick = { viewModel.removeSplit(i) }, enabled = order.size > 1) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Remove ${'A' + i}",
                        tint = if (order.size > 1) C.IconTint else C.OutlineButton
                    )
                }
            }
        }
        if (sheet.canAddSplit) {
            HorizontalDivider(color = C.InnerDivider)
            Row(
                modifier = Modifier.fillMaxWidth().height(
                    48.dp
                ).clickable(role = Role.Button, onClick = viewModel::addSplit).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = C.AccentOnContainer, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add a workout", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.AccentOnContainer))
            }
        }
    }

    PlanCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Start time", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface))
                Spacer(Modifier.height(2.dp))
                Text("Same on every training day", style = TextStyle(fontSize = 12.sp, color = C.SecondaryText))
            }
            TimeChip(time = sheet.draft.splitTime, label = "Start time", onChange = viewModel::setSplitTime)
        }
    }
}

@Composable
private fun TargetCard(sheet: PlanSheetUi, onChange: (Int) -> Unit) {
    val target = sheet.draft.weeklyTarget
    PlanCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Weekly target", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface))
                Spacer(Modifier.height(2.dp))
                Text(
                    sheet.targetNote,
                    style = TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = if (sheet.targetShort) C.PrimaryButton else C.SecondaryText
                    )
                )
            }
            Spacer(Modifier.width(12.dp))
            RoundButton(Icons.Outlined.Remove, "Fewer days", enabled = target > 1) { onChange(-1) }
            Text(
                TrainingPlanText.targetLabel(target),
                modifier = Modifier.defaultMinSize(minWidth = 56.dp),
                textAlign = TextAlign.Center,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface)
            )
            RoundButton(Icons.Outlined.Add, "More days", enabled = target < 7) { onChange(1) }
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(C.Card)
            .border(1.dp, C.OutlineButton, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) C.IconTint else C.OutlineButton, modifier = Modifier.size(20.dp))
    }
}

/** Outlined 32dp chip with a clock icon; opens an M3 time picker. */
@Composable
private fun TimeChip(time: String, label: String, onChange: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val parsed = runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.of(18, 0))
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(ChipShape)
            .background(C.Card)
            .border(1.dp, C.OutlineButton, ChipShape)
            .clickable(role = Role.Button, onClickLabel = "Change $label") { open = true }
            .padding(start = 6.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = C.IconTint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(parsed.toString(), style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = C.OnSurface))
    }
    if (open) {
        TimeDialog(title = label, time = parsed, onConfirm = {
            onChange(it)
            open = false
        }, onDismiss = { open = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(title: String, time: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state =
        rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current)
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    selectorColor = C.PrimaryButton,
                    timeSelectorSelectedContainerColor = C.AccentContainer,
                    timeSelectorSelectedContentColor = C.AccentOnContainer,
                    periodSelectorSelectedContainerColor = C.AccentContainer
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }
            ) { Text("OK", color = C.AccentOnContainer) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = C.AccentOnContainer) } },
        containerColor = C.Card
    )
}

/** Weekday row's workout menu: a filled chip when a workout is set, an outlined "Rest" chip otherwise. */
@Composable
private fun TemplateDropdown(
    templates: List<WorkoutTemplate>,
    selected: WorkoutTemplate?,
    allowRest: Boolean,
    label: String,
    onSelect: (WorkoutTemplate?) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .defaultMinSize(minWidth = 104.dp)
                .height(32.dp)
                .clip(ChipShape)
                .background(if (selected != null) C.WorkoutTint else C.Card)
                .border(BorderStroke(1.dp, if (selected != null) C.WorkoutTint else C.OutlineButton), ChipShape)
                .clickable(role = Role.DropdownList, onClickLabel = "Choose $label") { open = true }
                .padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                selected?.name ?: "Rest",
                modifier = Modifier.weight(1f, fill = false).widthIn(max = IntrinsicWidthCap),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected != null) C.WorkoutInk else C.SecondaryText
                )
            )
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = if (selected != null) C.WorkoutInk else C.IconTint)
        }
        TemplateMenu(open = open, templates = templates, allowRest = allowRest, onDismiss = { open = false }) {
            onSelect(it)
            open = false
        }
    }
}

/** Split order row: the template name as plain text with a drop-down arrow. */
@Composable
private fun OrderTemplateMenu(
    templates: List<WorkoutTemplate>,
    selected: WorkoutTemplate?,
    label: String,
    modifier: Modifier,
    onSelect: (WorkoutTemplate) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(ChipShape)
                .clickable(role = Role.DropdownList, onClickLabel = "Choose $label") { open = true }
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                selected?.name ?: "Choose a workout",
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontSize = 15.sp, color = if (selected != null) C.OnSurface else C.SecondaryText)
            )
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = C.IconTint)
        }
        TemplateMenu(open = open, templates = templates, allowRest = false, onDismiss = { open = false }) { template ->
            template?.let(onSelect)
            open = false
        }
    }
}

@Composable
private fun TemplateMenu(
    open: Boolean,
    templates: List<WorkoutTemplate>,
    allowRest: Boolean,
    onDismiss: () -> Unit,
    onSelect: (WorkoutTemplate?) -> Unit
) {
    DropdownMenu(expanded = open, onDismissRequest = onDismiss, containerColor = C.Card) {
        templates.forEach { template ->
            DropdownMenuItem(text = { Text(template.name, color = C.OnSurface) }, onClick = { onSelect(template) })
        }
        if (allowRest) {
            DropdownMenuItem(text = { Text("Rest", color = C.SecondaryText) }, onClick = { onSelect(null) })
        }
    }
}

/** Long template names stop growing the weekday chip past this. */
private val IntrinsicWidthCap = 140.dp

private fun DayOfWeek.fullName(): String = name.take(1) + name.drop(1).lowercase()
