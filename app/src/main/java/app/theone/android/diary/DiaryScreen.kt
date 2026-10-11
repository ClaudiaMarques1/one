package app.theone.android.diary

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import app.theone.android.notifications.HabitReminderScheduler
import app.theone.android.ui.ErrorBanner
import app.theone.android.ui.theme.OneTheme
import kotlinx.coroutines.launch

private val Accent = Color(0xFF8E24AA)
private val AccentInk = Color(0xFF6A1B86)
private val AccentSoft = Color(0xFFF6EEF8)
private val OnSurface = Color(0xFF1D1B20)
private val SecondaryText = Color(0xFF6B655F)
private val TabInactive = Color(0xFF49454F)
private val CardBorder = Color(0xFFEAE4DE)
private val TabDivider = Color(0xFFEAE4DE)
private val ChipFill = Color(0xFFF4F1EE)
private val BodyText = Color(0xFF3B3733)
private val TodayHelper = Color(0xFF5E5560)

@Composable
fun DiaryScreen(viewModel: DiaryViewModel = viewModel(), goalViewModel: GoalViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(0) }
    var isBuildingGoal by remember { mutableStateOf(false) }

    val entries by viewModel.entries.collectAsState()
    var showEntryComposer by remember { mutableStateOf(false) }
    var entryBeingEdited by remember { mutableStateOf<DiaryEntry?>(null) }

    val goals by goalViewModel.goals.collectAsState()
    val draftName by goalViewModel.draftName.collectAsState()
    val draftHabits by goalViewModel.draftHabits.collectAsState()
    val editingGoal by goalViewModel.editingGoal.collectAsState()
    val diaryError by viewModel.diaryError.collectAsState()
    val goalError by goalViewModel.goalError.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
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
        topBar = { DiaryTabRow(selectedTab = selectedTab, onSelect = { selectedTab = it }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    onWriteTodayClick = {
                        entryBeingEdited = null
                        showEntryComposer = true
                    },
                    onEditEntry = { entry ->
                        entryBeingEdited = entry
                        showEntryComposer = true
                    },
                    onDeleteEntry = { entry ->
                        viewModel.deleteEntry(entry.id)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Entry deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreEntry(entry)
                            }
                        }
                    }
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
            existingEntry = entryBeingEdited,
            onDismiss = {
                showEntryComposer = false
                entryBeingEdited = null
            },
            onSave = { mood, energyLevel, sleepHours, soreness, reflection ->
                val editing = entryBeingEdited
                if (editing != null) {
                    viewModel.updateEntry(
                        id = editing.id,
                        date = editing.date,
                        mood = mood,
                        energyLevel = energyLevel,
                        sleepHours = sleepHours,
                        soreness = soreness,
                        reflection = reflection,
                        createdAt = editing.createdAt
                    )
                } else {
                    viewModel.addEntry(mood, energyLevel, sleepHours, soreness, reflection)
                }
                showEntryComposer = false
                entryBeingEdited = null
            }
        )
    }
}

@Composable
private fun DiaryTabRow(selectedTab: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            // Keep height measured even with no ripple bg.
            .border(0.dp, Color.Transparent)
    ) {
        listOf("Journal", "Goals").forEachIndexed { index, label ->
            val selected = selectedTab == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    label,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) AccentInk else TabInactive
                    ),
                    modifier = Modifier.padding(bottom = 13.dp)
                )
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(3.dp)
                        .background(
                            if (selected) Accent else Color.Transparent,
                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                )
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TabDivider))
}

@Composable
private fun JournalContent(
    entries: List<DiaryEntry>,
    onWriteTodayClick: () -> Unit,
    onEditEntry: (DiaryEntry) -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit
) {
    val todayKey = remember { DiaryDateUtils.todayKey() }
    val todayEntry = entries.firstOrNull { it.date == todayKey }
    val grouped = remember(entries, todayKey) {
        entries.filter { it.date != todayKey }
            .groupBy { DiaryDateUtils.groupLabel(it.date, todayKey) }
            .toList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (todayEntry == null) {
                TodayPromptCard(onWriteTodayClick)
            } else {
                EntryCard(entry = todayEntry, onEdit = { onEditEntry(todayEntry) }, onDelete = { onDeleteEntry(todayEntry) })
            }
        }
        grouped.forEach { (label, groupEntries) ->
            item {
                Text(
                    label,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SecondaryText),
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)
                )
            }
            items(groupEntries, key = { it.id }) { entry ->
                EntryCard(entry = entry, onEdit = { onEditEntry(entry) }, onDelete = { onDeleteEntry(entry) })
            }
        }
    }
}

@Composable
private fun TodayPromptCard(onWriteTodayClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AccentSoft)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                DiaryDateUtils.fullDateLabel(DiaryDateUtils.todayKey()),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AccentInk)
            )
            Text(
                "How did today go?",
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "Takes about a minute. Mood, energy, sleep and anything sore.",
                style = TextStyle(fontSize = 14.sp, color = TodayHelper),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Accent)
                .clickable(onClick = onWriteTodayClick)
                .padding(start = 16.dp, end = 20.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.EditNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text("Write today's entry", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryCard(entry: DiaryEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val soreness = entry.effectiveSoreness

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(start = 14.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(44.dp).background(AccentSoft, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.mood, style = TextStyle(fontSize = 24.sp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    DiaryDateUtils.fullDateLabel(entry.date),
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    moodLabel(entry.mood),
                    style = TextStyle(fontSize = 13.sp, color = SecondaryText),
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color(0xFF49454F), modifier = Modifier.size(22.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = Color(0xFF49454F)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFB3261E)) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFB3261E)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(end = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                Text("Energy ${entry.energyLevel}/10", style = TextStyle(fontSize = 13.sp, color = BodyText))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Bedtime, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                Text("Sleep ${entry.sleepHours} h", style = TextStyle(fontSize = 13.sp, color = BodyText))
            }
        }

        if (soreness.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bodyPartOrder.filter { soreness.containsKey(it) }.forEach { id ->
                    val levelIndex = (soreness[id] ?: 1) - 1
                    val label = (bodyPartLabels[id] ?: id) + " · " + sorenessLevelLabels[levelIndex.coerceIn(0, 2)]
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ChipFill)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(label, style = TextStyle(fontSize = 12.sp, color = BodyText))
                    }
                }
            }
        }

        if (entry.reflection.isNotBlank()) {
            Text(
                entry.reflection,
                style = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, color = BodyText),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 6.dp)
            )
        }
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

@Preview(showBackground = true)
@Composable
private fun JournalContentPreview() {
    OneTheme {
        JournalContent(
            entries = listOf(
                DiaryEntry(
                    id = "1",
                    date = DiaryDateUtils.todayKey(),
                    mood = "💪",
                    energyLevel = 8,
                    sleepHours = 7,
                    soreness = mapOf("quads" to 2, "shoulders" to 1),
                    reflection = "Today's biggest win: hit a new squat PR."
                ),
                DiaryEntry(
                    id = "2",
                    date = "2026-09-12",
                    mood = "😴",
                    energyLevel = 4,
                    sleepHours = 5,
                    reflection = "Held back by poor sleep."
                )
            ),
            onWriteTodayClick = {},
            onEditEntry = {},
            onDeleteEntry = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun JournalContentEmptyPreview() {
    OneTheme {
        JournalContent(entries = emptyList(), onWriteTodayClick = {}, onEditEntry = {}, onDeleteEntry = {})
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
