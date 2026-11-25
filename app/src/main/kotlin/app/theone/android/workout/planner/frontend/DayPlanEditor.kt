package app.theone.android.workout.planner.frontend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.ui.dashedBorder
import app.theone.android.ui.theme.CalendarColors
import app.theone.android.workout.planner.backend.DayEditorUi
import app.theone.android.workout.planner.backend.EditorKind
import app.theone.android.workout.planner.backend.TrainingPlanViewModel
import app.theone.android.workout.templates.backend.WorkoutTemplate
import java.time.LocalDate

private val C = CalendarColors

/**
 * 5a day sheet "Training plan" card (#78): header + [DayPlanEditor]. Shown for today and later; a past day
 * gets [MissedCard] instead when something was missed. [onDateChange] lets the sheet follow a moved session.
 */
@Composable
fun TrainingPlanDayCard(
    editor: DayEditorUi,
    templates: List<WorkoutTemplate>,
    viewModel: TrainingPlanViewModel,
    onDateChange: (LocalDate) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(C.Card)
            .border(1.dp, C.CardBorder, shape)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(C.WorkoutTint), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.EventRepeat, contentDescription = null, tint = C.WorkoutInk)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Training plan", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface))
                Text(editor.status, style = TextStyle(fontSize = 13.sp, color = C.SecondaryText))
            }
        }
        DayPlanEditor(editor = editor, templates = templates, viewModel = viewModel, onDateChange = onDateChange)
    }
}

/** 5a "Missed" card for a past day: dashed grey box. */
@Composable
fun MissedCard(line: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(C.ChevronWeak, 16.dp, 1.5.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.EventBusy, contentDescription = null, tint = C.Hint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(line, style = TextStyle(fontSize = 14.sp, color = C.BodySecondary))
    }
}

/** The day editor body - shared by the 5a day sheet card and an open 4a Upcoming row. Changes save at once. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayPlanEditor(
    editor: DayEditorUi,
    templates: List<WorkoutTemplate>,
    viewModel: TrainingPlanViewModel,
    onDateChange: (LocalDate) -> Unit
) {
    val session = editor.session
    when {
        editor.kind == EditorKind.REST -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Rest day in your routine", style = TextStyle(fontSize = 14.sp, color = C.SecondaryText))
            PillButton(Icons.Outlined.Add, "Plan a workout", enabled = templates.isNotEmpty()) { viewModel.planWorkout(editor.date) }
        }

        editor.kind == EditorKind.SKIPPED && session != null -> Row(
            modifier = Modifier.fillMaxWidth().clip(
                RoundedCornerShape(12.dp)
            ).background(C.EmptyCell).padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                editor.skipNote,
                modifier = Modifier.weight(1f),
                style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, color = C.BodySecondary)
            )
            TextButton(onClick = { viewModel.undoSkip(session) }) {
                Text("Undo", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.AccentOnContainer))
            }
        }

        editor.kind == EditorKind.PLANNED && session != null -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Section("Workout") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { template ->
                        WorkoutChip(
                            template.name,
                            selected = template.id == session.templateId
                        ) { viewModel.setDayTemplate(session, template.id) }
                    }
                }
            }
            Section("Time") {
                var picking by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundButton(
                        Icons.Outlined.Remove,
                        "30 minutes earlier",
                        enabled = editor.canTimeDown
                    ) { viewModel.stepDayTime(session, -30) }
                    Text(
                        session.time.toString(),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 60.dp)
                            .clickable(role = Role.Button, onClickLabel = "Pick a time") { picking = true }
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface)
                    )
                    RoundButton(Icons.Outlined.Add, "30 minutes later", enabled = editor.canTimeUp) { viewModel.stepDayTime(session, 30) }
                }
                if (picking) {
                    TimeDialog(title = "Time", time = session.time, onConfirm = {
                        viewModel.setDayTime(session, it)
                        picking = false
                    }, onDismiss = { picking = false })
                }
            }
            Section("Move to another day this week") {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    editor.moveDays.forEach { day ->
                        val shape = RoundedCornerShape(12.dp)
                        val (bg, fg, border) = when {
                            day.current -> Triple(C.WorkoutDot, C.Card, C.WorkoutDot)
                            day.free -> Triple(C.Card, C.OnSurface, C.OutlineButton)
                            else -> Triple(C.EmptyCell, C.ChevronWeak, C.EmptyCell)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shape)
                                .background(bg)
                                .border(1.dp, border, shape)
                                .clickable(enabled = day.free, role = Role.Button) {
                                    viewModel.moveDay(session, day.date)
                                    onDateChange(day.date)
                                }
                                .semantics { contentDescription = "${day.weekday} ${day.date.dayOfMonth}" }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(day.weekday, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = fg))
                            Text(
                                day.date.dayOfMonth.toString(),
                                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = fg)
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(Icons.Outlined.EventBusy, "Skip this day") { viewModel.skipDay(session) }
                if (editor.canReset) {
                    TextButton(onClick = {
                        viewModel.resetDay(session)
                        onDateChange(session.sourceDate)
                    }) {
                        Text(
                            editor.resetLabel,
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.AccentOnContainer)
                        )
                    }
                }
            }
        }

        else -> Unit
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = C.SecondaryText))
        content()
    }
}

@Composable
private fun WorkoutChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(shape)
            .background(if (selected) C.WorkoutTint else C.Card)
            .border(1.dp, if (selected) C.WorkoutTint else C.OutlineButton, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = C.WorkoutInk, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            label,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (selected) C.WorkoutInk else C.OnSurface)
        )
    }
}

@Composable
private fun PillButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, C.OutlineButton),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = C.Card),
        contentPadding = PaddingValues(start = 12.dp, end = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = C.OnSurface, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.OnSurface))
    }
}
