package app.theone.android.diary.frontend

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import app.theone.android.diary.goals.backend.GoalViewModel
import app.theone.android.diary.goals.backend.applyReminderScheduling
import app.theone.android.diary.goals.backend.cancelAllReminders
import app.theone.android.diary.goals.frontend.GoalBuilderContent
import app.theone.android.diary.goals.frontend.GoalsHomeContent
import app.theone.android.diary.journal.backend.DiaryEntry
import app.theone.android.diary.journal.backend.DiaryViewModel
import app.theone.android.diary.journal.frontend.DiaryEntryComposerDialog
import app.theone.android.diary.journal.frontend.JournalContent
import app.theone.android.ui.ErrorBanner
import kotlinx.coroutines.launch

private val Accent = Color(0xFF8E24AA)
private val AccentInk = Color(0xFF6A1B86)
private val TabInactive = Color(0xFF49454F)
private val TabDivider = Color(0xFFEAE4DE)

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
    val canSaveGoal by goalViewModel.canSave.collectAsState()
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
        // The goal builder is full-screen with its own top bar, so the tabs step aside while it's open.
        topBar = { if (!isBuildingGoal) DiaryTabRow(selectedTab = selectedTab, onSelect = { selectedTab = it }) },
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
                        canSave = canSaveGoal,
                        onAddHabit = { goalViewModel.addDraftHabit() },
                        onRenameHabit = { habitId, name -> goalViewModel.renameDraftHabit(habitId, name) },
                        onAddTime = { goalViewModel.addReminderTime(it) },
                        onChangeTime = { habitId, reminderId, hour, minute ->
                            goalViewModel.changeReminderTime(habitId, reminderId, hour, minute)
                        },
                        onToggleReminder = { habitId, reminderId -> goalViewModel.toggleReminder(habitId, reminderId) },
                        onRemoveTime = { habitId, reminderId -> goalViewModel.removeReminderTime(habitId, reminderId) },
                        onRemoveHabit = { goalViewModel.removeDraftHabit(it) },
                        onCancel = {
                            goalViewModel.clearDraft()
                            isBuildingGoal = false
                        },
                        onSave = {
                            applyReminderScheduling(context, draftName.trim(), editingGoal?.habits.orEmpty(), goalViewModel.habitsToSave())
                            goalViewModel.saveDraftGoal()
                            isBuildingGoal = false
                        }
                    )
                } else {
                    GoalsHomeContent(
                        goals = goals,
                        onCreateGoalClick = {
                            goalViewModel.addDraftHabit()
                            isBuildingGoal = true
                        },
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
