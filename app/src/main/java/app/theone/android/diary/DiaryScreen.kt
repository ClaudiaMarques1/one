package app.theone.android.diary

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import app.theone.android.notifications.HabitReminderScheduler
import app.theone.android.ui.ErrorBanner
import app.theone.android.ui.theme.OneTheme

@Composable
fun DiaryScreen(viewModel: DiaryViewModel = viewModel(), goalViewModel: GoalViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(0) }
    var isBuildingGoal by remember { mutableStateOf(false) }

    val entries by viewModel.entries.collectAsState()
    var showEntryComposer by remember { mutableStateOf(false) }

    val goals by goalViewModel.goals.collectAsState()
    val draftName by goalViewModel.draftName.collectAsState()
    val draftHabits by goalViewModel.draftHabits.collectAsState()
    val editingGoal by goalViewModel.editingGoal.collectAsState()
    val diaryError by viewModel.diaryError.collectAsState()
    val goalError by goalViewModel.goalError.collectAsState()

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Journal") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Goals") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (selectedTab == 1) {
                if (isBuildingGoal) {
                    GoalBuilderContent(
                        name = draftName,
                        habits = draftHabits,
                        isEditing = editingGoal != null,
                        onNameChange = { goalViewModel.updateDraftName(it) },
                        onAddHabit = { habitName, times -> goalViewModel.addDraftHabit(habitName, times) },
                        onRemoveHabit = { goalViewModel.removeDraftHabit(it) },
                        onUpdateHabitReminders = { habitId, reminders ->
                            goalViewModel.updateHabitReminders(habitId, reminders)
                        },
                        onCancel = {
                            goalViewModel.clearDraft()
                            isBuildingGoal = false
                        },
                        onSave = {
                            applyReminderScheduling(context, draftName, editingGoal?.habits.orEmpty(), draftHabits)
                            goalViewModel.saveDraftGoal()
                            isBuildingGoal = false
                        }
                    )
                } else {
                    GoalsHomeContent(
                        goals = goals,
                        onCreateGoalClick = { isBuildingGoal = true },
                        onEditGoal = {
                            goalViewModel.startEditingGoal(it)
                            isBuildingGoal = true
                        },
                        onDeleteGoal = { goal ->
                            cancelAllReminders(context, goal.habits)
                            goalViewModel.deleteGoal(goal.id)
                        }
                    )
                }
            } else {
                JournalContent(
                    entries = entries,
                    onAddEntryClick = { showEntryComposer = true },
                    onDeleteClick = { viewModel.deleteEntry(it) }
                )
            }

            ErrorBanner(
                message = diaryError ?: goalError,
                onDismiss = {
                    viewModel.clearDiaryError()
                    goalViewModel.clearGoalError()
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (showEntryComposer) {
        DiaryEntryComposerDialog(
            onDismiss = { showEntryComposer = false },
            onSave = { mood, energyLevel, sleepHours, soreParts, reflection ->
                viewModel.addEntry(mood, energyLevel, sleepHours, soreParts, reflection)
                showEntryComposer = false
            }
        )
    }
}

private fun applyReminderScheduling(
    context: Context,
    goalName: String,
    oldHabits: List<Habit>,
    newHabits: List<Habit>
) {
    val newHabitIds = newHabits.map { it.id }.toSet()
    oldHabits.filter { it.id !in newHabitIds }.forEach { removedHabit ->
        removedHabit.timesOfDay.forEach { tod ->
            HabitReminderScheduler.cancel(context, removedHabit.id, tod)
        }
    }

    newHabits.forEach { habit ->
        habit.timesOfDay.forEach { tod ->
            val reminder = habit.reminders.find { it.timeOfDay == tod }
            if (reminder != null && reminder.enabled) {
                val timeOfDay = TimeOfDay.valueOf(tod)
                HabitReminderScheduler.schedule(
                    context = context,
                    habitId = habit.id,
                    habitName = habit.name,
                    goalName = goalName,
                    timeOfDay = tod,
                    timeOfDayLabel = timeOfDay.label,
                    hour = reminder.hour,
                    minute = reminder.minute
                )
            } else {
                HabitReminderScheduler.cancel(context, habit.id, tod)
            }
        }
    }
}

private fun cancelAllReminders(context: Context, habits: List<Habit>) {
    habits.forEach { habit ->
        habit.timesOfDay.forEach { tod ->
            HabitReminderScheduler.cancel(context, habit.id, tod)
        }
    }
}

@Composable
private fun JournalContent(
    entries: List<DiaryEntry>,
    onAddEntryClick: () -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onAddEntryClick, modifier = Modifier.fillMaxWidth()) {
            Text("+ Add Entry")
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(entry.mood, style = MaterialTheme.typography.titleLarge)
                                Text(entry.date, style = MaterialTheme.typography.titleSmall)
                            }
                            IconButton(onClick = { onDeleteClick(entry.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                            }
                        }
                        Text(
                            "Energy: ${entry.energyLevel}/10 · Sleep: ${entry.sleepHours}h",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (entry.sorePartIds.isNotEmpty()) {
                            Text(
                                "Sore: " + entry.sorePartIds.distinct().joinToString(", ") { bodyPartLabels[it] ?: it },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (entry.reflection.isNotBlank()) {
                            Text(entry.reflection, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JournalContentPreview() {
    OneTheme {
        JournalContent(
            entries = listOf(
                DiaryEntry(
                    id = "1",
                    date = "2026-09-12",
                    mood = "💪",
                    energyLevel = 8,
                    sleepHours = 7,
                    sorePartIds = listOf("quads", "shoulders"),
                    reflection = "Todays biggest win: hit a new squat PR."
                ),
                DiaryEntry(
                    id = "2",
                    date = "2026-09-11",
                    mood = "😴",
                    energyLevel = 4,
                    sleepHours = 5,
                    reflection = "Held back by poor sleep."
                )
            ),
            onAddEntryClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun JournalContentEmptyPreview() {
    OneTheme {
        JournalContent(
            entries = emptyList(),
            onAddEntryClick = {},
            onDeleteClick = {}
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
