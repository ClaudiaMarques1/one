package app.theone.android.diary.goals.frontend

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.diary.goals.backend.Goal
import app.theone.android.diary.goals.backend.Habit
import app.theone.android.diary.goals.backend.HabitReminder
import app.theone.android.diary.goals.backend.TimeOfDay
import app.theone.android.diary.goals.backend.partOf
import app.theone.android.diary.goals.backend.sortedByTime
import app.theone.android.ui.EditDeleteMenu
import app.theone.android.ui.dashedBorder
import app.theone.android.ui.theme.DiaryAccent
import app.theone.android.ui.theme.DiaryDeepInk
import app.theone.android.ui.theme.DiaryInk
import app.theone.android.ui.theme.OneTheme
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmMutedIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface

@Composable
fun GoalsHomeContent(
    goals: List<Goal>,
    onCreateGoalClick: () -> Unit,
    onEditGoal: (Goal) -> Unit,
    onDeleteGoal: (Goal) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(onClick = onCreateGoalClick, modifier = Modifier.fillMaxWidth()) {
                Text("New goal")
            }
        }

        if (goals.isEmpty()) {
            item {
                Text(
                    "No goals yet — create one and add habits to build your routine.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                GoalCard(
                    goal = goal,
                    onEdit = { onEditGoal(goal) },
                    onDelete = { onDeleteGoal(goal) }
                )
            }
        }
    }
}

@Composable
private fun GoalCard(goal: Goal, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(goal.name, style = MaterialTheme.typography.titleMedium)
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Goal options")
                    }
                    EditDeleteMenu(
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
            val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
            goal.habits.forEach { habit ->
                Text(goalCardHabitLine(habit, is24Hour), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** "Drink water · 7:30 AM, 1:15 PM" - the habit's enabled reminder times. */
internal fun goalCardHabitLine(habit: Habit, is24Hour: Boolean): String {
    val times = habit.reminders.filter { it.enabled }.sortedByTime().map { it.timeLabel(is24Hour) }
    return if (times.isEmpty()) habit.name else "${habit.name} · ${times.joinToString(", ")}"
}

@Composable
fun GoalBuilderContent(
    name: String,
    habits: List<Habit>,
    isEditing: Boolean,
    canSave: Boolean,
    onNameChange: (String) -> Unit,
    onAddHabit: () -> String,
    onRenameHabit: (habitId: String, name: String) -> Unit,
    onAddTime: (habitId: String) -> Unit,
    onChangeTime: (habitId: String, reminderId: String, hour: Int, minute: Int) -> Unit,
    onToggleReminder: (habitId: String, reminderId: String) -> Unit,
    onRemoveTime: (habitId: String, reminderId: String) -> Unit,
    onRemoveHabit: (habitId: String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    var expandedHabitId by remember { mutableStateOf(habits.firstOrNull()?.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSurface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp)
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = WarmIcon, modifier = Modifier.size(24.dp))
            }
            Text(
                if (isEditing) "Edit goal" else "New goal",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = WarmOnSurface,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        HorizontalDivider(color = WarmDivider)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { GoalNameField(name = name, onNameChange = onNameChange) }

            items(habits, key = { it.id }) { habit ->
                if (habit.id == expandedHabitId) {
                    ExpandedHabitCard(
                        habit = habit,
                        is24Hour = is24Hour,
                        onRename = { onRenameHabit(habit.id, it) },
                        onAddTime = { onAddTime(habit.id) },
                        onChangeTime = { reminderId, h, m -> onChangeTime(habit.id, reminderId, h, m) },
                        onToggleReminder = { onToggleReminder(habit.id, it) },
                        onRemoveTime = { onRemoveTime(habit.id, it) },
                        onDelete = { onRemoveHabit(habit.id) }
                    )
                } else {
                    CollapsedHabitCard(
                        habit = habit,
                        is24Hour = is24Hour,
                        onExpand = { expandedHabitId = habit.id },
                        onDelete = { onRemoveHabit(habit.id) }
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .dashedBorder(WarmButtonOutline, 24.dp)
                        .clickable { expandedHabitId = onAddHabit() }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = DiaryDeepInk, modifier = Modifier.size(20.dp))
                    Text(
                        "Add another habit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DiaryDeepInk,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = WarmDivider)
        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)) {
            Button(
                onClick = onSave,
                enabled = canSave,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = DiaryAccent, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(if (isEditing) "Save changes" else "Save goal", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun GoalNameField(name: String, onNameChange: (String) -> Unit) {
    Column {
        Text(
            "Goal name",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = DiaryInk,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 16.sp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DiaryAccent,
                unfocusedBorderColor = WarmButtonOutline,
                cursorColor = DiaryAccent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val HabitCardShape = RoundedCornerShape(20.dp)

private fun Modifier.habitCard(): Modifier =
    fillMaxWidth()
        .clip(HabitCardShape)
        .background(Color.White)
        .border(1.dp, WarmCardBorder, HabitCardShape)

@Composable
private fun ExpandedHabitCard(
    habit: Habit,
    is24Hour: Boolean,
    onRename: (String) -> Unit,
    onAddTime: () -> Unit,
    onChangeTime: (reminderId: String, hour: Int, minute: Int) -> Unit,
    onToggleReminder: (reminderId: String) -> Unit,
    onRemoveTime: (reminderId: String) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .habitCard()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (habit.name.isEmpty()) {
                    Text("Habit name", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WarmMutedIcon)
                }
                BasicTextField(
                    value = habit.name,
                    onValueChange = onRename,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface),
                    cursorBrush = SolidColor(DiaryAccent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                timesCountLabel(habit.reminders.size),
                fontSize = 13.sp,
                color = WarmSecondaryText,
                modifier = Modifier.padding(start = 12.dp)
            )
            HabitMenuButton(habitName = habit.name, onDelete = onDelete)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            habit.reminders.forEach { reminder ->
                key(reminder.id) {
                    ReminderRow(
                        reminder = reminder,
                        is24Hour = is24Hour,
                        onChangeTime = { h, m -> onChangeTime(reminder.id, h, m) },
                        onToggle = { onToggleReminder(reminder.id) },
                        onRemove = { onRemoveTime(reminder.id) }
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onAddTime,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, WarmButtonOutline),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = DiaryDeepInk),
            contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
            modifier = Modifier.height(40.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Add time", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: HabitReminder,
    is24Hour: Boolean,
    onChangeTime: (hour: Int, minute: Int) -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ReminderTimeField(hour = reminder.hour, minute = reminder.minute, is24Hour = is24Hour, onTimeChange = onChangeTime)
        PartOfDayLabel(partOf(reminder.hour))
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggle, modifier = Modifier.size(44.dp)) {
            if (reminder.enabled) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = "Reminder on", tint = DiaryInk)
            } else {
                Icon(Icons.Filled.NotificationsOff, contentDescription = "Reminder off", tint = WarmMutedIcon)
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Remove time", tint = WarmSecondaryText, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PartOfDayLabel(part: TimeOfDay) {
    val (background, textColor) = part.labelColors()
    Text(
        part.label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = textColor,
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun CollapsedHabitCard(habit: Habit, is24Hour: Boolean, onExpand: () -> Unit, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .habitCard()
            .clickable(onClick = onExpand)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                habit.name.ifBlank { "Unnamed habit" },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (habit.name.isBlank()) WarmMutedIcon else WarmOnSurface
            )
            if (habit.reminders.isNotEmpty()) {
                Text(
                    habit.reminders.joinToString(" · ") { it.timeLabel(is24Hour) },
                    fontSize = 13.sp,
                    color = WarmSecondaryText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        HabitMenuButton(habitName = habit.name, onDelete = onDelete)
        Icon(Icons.Filled.ExpandMore, contentDescription = "Expand", tint = WarmMutedIcon)
    }
}

/** The habit card's ⋮ button: a Delete-only menu, styled like the journal and goal card menus. */
@Composable
private fun HabitMenuButton(habitName: String, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "More options for ${habitName.ifBlank { "habit" }}",
                tint = WarmIcon,
                modifier = Modifier.size(22.dp)
            )
        }
        EditDeleteMenu(expanded = menuOpen, onDismiss = { menuOpen = false }, onDelete = onDelete)
    }
}

@Preview(showBackground = true, name = "Goal builder")
@Composable
private fun GoalBuilderContentPreview() {
    OneTheme {
        GoalBuilderContent(
            name = "Stay hydrated",
            habits = listOf(
                Habit(
                    id = "h1",
                    name = "Drink water",
                    reminders = listOf(
                        HabitReminder(id = "r1", hour = 7, minute = 30),
                        HabitReminder(id = "r2", hour = 13, minute = 15),
                        HabitReminder(id = "r3", hour = 18, minute = 45, enabled = false)
                    )
                ),
                Habit(
                    id = "h2",
                    name = "Refill bottle at work",
                    reminders = listOf(
                        HabitReminder(id = "r4", hour = 11, minute = 45),
                        HabitReminder(id = "r5", hour = 15, minute = 15)
                    )
                )
            ),
            isEditing = false,
            canSave = true,
            onNameChange = {},
            onAddHabit = { "" },
            onRenameHabit = { _, _ -> },
            onAddTime = {},
            onChangeTime = { _, _, _, _ -> },
            onToggleReminder = { _, _ -> },
            onRemoveTime = { _, _ -> },
            onRemoveHabit = {},
            onCancel = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, name = "Goals")
@Composable
private fun GoalsHomeContentPreview() {
    OneTheme {
        GoalsHomeContent(
            goals = listOf(
                Goal(
                    id = "1",
                    name = "Better sleep",
                    habits = listOf(
                        Habit(id = "h1", name = "No screens", reminders = listOf(HabitReminder(id = "r1", hour = 21, minute = 30))),
                        Habit(
                            id = "h2",
                            name = "Drink water",
                            reminders = listOf(
                                HabitReminder(id = "r2", hour = 7, minute = 30),
                                HabitReminder(id = "r3", hour = 13, minute = 15)
                            )
                        )
                    )
                )
            ),
            onCreateGoalClick = {},
            onEditGoal = {},
            onDeleteGoal = {}
        )
    }
}
