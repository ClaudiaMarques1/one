package app.theone.android.diary.goals.frontend

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.theone.android.diary.goals.backend.Goal
import app.theone.android.diary.goals.backend.Habit
import app.theone.android.diary.goals.backend.HabitReminder
import app.theone.android.diary.goals.backend.TimeOfDay
import app.theone.android.ui.theme.OneTheme

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
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            goal.habits.forEach { habit ->
                Text(
                    "${habit.name} — ${habit.timesOfDay.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun GoalBuilderContent(
    name: String,
    habits: List<Habit>,
    isEditing: Boolean,
    onNameChange: (String) -> Unit,
    onAddHabit: (String, List<String>) -> Unit,
    onRemoveHabit: (Int) -> Unit,
    onUpdateHabitReminders: (String, List<HabitReminder>) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    var habitName by remember { mutableStateOf("") }
    var selectedTimes by remember { mutableStateOf(setOf<String>()) }
    var reminderDialogHabit by remember { mutableStateOf<Habit?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(if (isEditing) "Edit goal" else "New goal", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Goal name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Add a habit", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = habitName,
                        onValueChange = { habitName = it },
                        label = { Text("Habit name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TimeOfDay.entries.forEach { time ->
                            val selected = time.name in selectedTimes
                            val toggle = {
                                selectedTimes = if (selected) selectedTimes - time.name else selectedTimes + time.name
                            }
                            if (selected) {
                                Button(onClick = toggle, modifier = Modifier.weight(1f)) {
                                    Text(time.label, style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                OutlinedButton(onClick = toggle, modifier = Modifier.weight(1f)) {
                                    Text(time.label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            onAddHabit(habitName, selectedTimes.toList())
                            habitName = ""
                            selectedTimes = emptySet()
                        },
                        enabled = habitName.isNotBlank() && selectedTimes.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add habit")
                    }
                }
            }
        }

        if (habits.isNotEmpty()) {
            item { Text("Habits in this goal", style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(habits) { index, habit ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(habit.name)
                            Text(habit.timesOfDay.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                        }
                        Row {
                            IconButton(onClick = { reminderDialogHabit = habit }) {
                                Icon(
                                    Icons.Filled.Timer,
                                    contentDescription = "Set reminders for ${habit.name}"
                                )
                            }
                            IconButton(onClick = { onRemoveHabit(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove ${habit.name}")
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSave,
                    enabled = name.isNotBlank() && habits.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isEditing) "Save changes" else "Save goal")
                }
            }
        }
    }

    reminderDialogHabit?.let { habit ->
        HabitReminderDialog(
            habit = habit,
            onDismiss = { reminderDialogHabit = null },
            onSave = { reminders ->
                onUpdateHabitReminders(habit.id, reminders)
                reminderDialogHabit = null
            }
        )
    }
}

@Composable
private fun HabitReminderDialog(habit: Habit, onDismiss: () -> Unit, onSave: (List<HabitReminder>) -> Unit) {
    var state by remember(habit.id) {
        mutableStateOf(
            habit.timesOfDay.associateWith { tod ->
                val existing = habit.reminders.find { it.timeOfDay == tod }
                (existing?.enabled ?: false) to (existing?.hour ?: TimeOfDay.valueOf(tod).startHour)
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminders for ${habit.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                habit.timesOfDay.forEach { tod ->
                    val timeOfDay = TimeOfDay.valueOf(tod)
                    val (enabled, hour) = state[tod] ?: (false to timeOfDay.startHour)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(timeOfDay.label)
                            Switch(
                                checked = enabled,
                                onCheckedChange = { checked -> state = state + (tod to (checked to hour)) }
                            )
                        }
                        if (enabled) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                hourOptionsFor(timeOfDay).forEach { h ->
                                    val selected = h == hour
                                    val label = formatHour(h)
                                    if (selected) {
                                        Button(onClick = { state = state + (tod to (true to h)) }) { Text(label) }
                                    } else {
                                        OutlinedButton(onClick = { state = state + (tod to (true to h)) }) { Text(label) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val reminders = habit.timesOfDay.map { tod ->
                    val (enabled, hour) = state[tod] ?: (false to 0)
                    HabitReminder(timeOfDay = tod, hour = hour, minute = 0, enabled = enabled)
                }
                onSave(reminders)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun hourOptionsFor(timeOfDay: TimeOfDay): List<Int> =
    if (timeOfDay.startHour < timeOfDay.endHour) {
        (timeOfDay.startHour until timeOfDay.endHour).toList()
    } else {
        (timeOfDay.startHour..23).toList() + (0 until timeOfDay.endHour).toList()
    }

private fun formatHour(hour: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:00 $period"
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
                        Habit(id = "h1", name = "No screens", timesOfDay = listOf("NIGHT")),
                        Habit(id = "h2", name = "Drink water", timesOfDay = listOf("MORNING", "AFTERNOON", "EVENING"))
                    )
                )
            ),
            onCreateGoalClick = {},
            onEditGoal = {},
            onDeleteGoal = {}
        )
    }
}
