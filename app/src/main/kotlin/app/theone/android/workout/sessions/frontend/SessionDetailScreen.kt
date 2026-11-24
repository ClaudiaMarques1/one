package app.theone.android.workout.sessions.frontend

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.theone.android.exercise.backend.ExerciseCatalogLoader
import app.theone.android.ui.ErrorBanner
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.DeleteRed
import app.theone.android.ui.theme.OnPrBadge
import app.theone.android.ui.theme.PrBadge
import app.theone.android.ui.theme.ProgressUp
import app.theone.android.ui.theme.WarmBodyText
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmField
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmMutedIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface
import app.theone.android.ui.theme.WorkoutInk
import app.theone.android.workout.sessions.backend.ActiveExercise
import app.theone.android.workout.sessions.backend.ExerciseLog
import app.theone.android.workout.sessions.backend.SessionDetail
import app.theone.android.workout.sessions.backend.SessionDetailViewModel
import app.theone.android.workout.sessions.backend.StrengthSession
import app.theone.android.workout.sessions.backend.isUp
import app.theone.android.workout.templates.backend.WorkoutTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

// 3a in the session detail handoff (#48): a finished workout, opened from Recent on Workout home.

/** The catalog's bodyweight exercises ("body weight" equipment) feed the `Bodyweight + 10 kg` labels. */
@Composable
fun sessionDetailViewModel(): SessionDetailViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val bodyweight = flow {
                emit(ExerciseCatalogLoader.load(app).filter { it.equipment == "body weight" }.map { it.name.lowercase() }.toSet())
            }.flowOn(Dispatchers.IO)
            SessionDetailViewModel(savedStateHandle = createSavedStateHandle(), bodyweightNames = bodyweight)
        }
    }
)

private val CardShape = RoundedCornerShape(20.dp)

@Composable
private fun Modifier.detailCard(shape: RoundedCornerShape = CardShape): Modifier =
    fillMaxWidth()
        .clip(shape)
        .background(CardSurface)
        .border(1.dp, WarmCardBorder, shape)

@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    onRepeat: (StrengthSession, WorkoutTemplate?) -> Unit,
    viewModel: SessionDetailViewModel = sessionDetailViewModel()
) {
    val detail by viewModel.detail.collectAsState()
    val sourceTemplate by viewModel.sourceTemplate.collectAsState()
    val editDraft by viewModel.editDraft.collectAsState()
    val notesDraft by viewModel.notesDraft.collectAsState()
    val error by viewModel.sessionError.collectAsState()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val editing = editDraft != null

    BackHandler(enabled = editing, onBack = viewModel::cancelEditing)

    Box(modifier = Modifier.fillMaxSize().background(WarmSurface)) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            DetailTopBar(
                editing = editing,
                onBack = if (editing) viewModel::cancelEditing else onBack,
                onEditSets = viewModel::startEditing,
                onSaveAsTemplate = {
                    viewModel.saveAsTemplate { name -> Toast.makeText(context, "Saved as template \"$name\"", Toast.LENGTH_SHORT).show() }
                },
                onDelete = { showDeleteDialog = true }
            )
            val current = detail
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (current != null) {
                    item { DetailHeader(current) }
                    val draft = editDraft
                    if (draft != null) {
                        items(draft, key = { it.instanceId }) { exercise ->
                            EditExerciseCard(
                                exercise = exercise,
                                onRepsChange = { index, text -> viewModel.updateSetReps(exercise.instanceId, index, text) },
                                onWeightChange = { index, text -> viewModel.updateSetWeight(exercise.instanceId, index, text) },
                                onAddSet = { viewModel.addSet(exercise.instanceId) },
                                onRemoveSet = { index -> viewModel.removeSet(exercise.instanceId, index) }
                            )
                        }
                        item { NotesCard(notes = notesDraft, onNotesChange = viewModel::updateNotes) }
                    } else {
                        item { StatsRow(current) }
                        items(current.exercises) { exercise -> ExerciseLogCard(exercise) }
                        if (current.session.notes.isNotBlank()) {
                            item { NotesReadCard(current.session.notes) }
                        }
                    }
                }
            }
            if (current != null) {
                DetailFooter(
                    label = if (editing) "Save changes" else "Repeat workout",
                    showReplayIcon = !editing,
                    onClick = { if (editing) viewModel.saveEditing() else onRepeat(current.session, sourceTemplate) }
                )
            }
        }
        ErrorBanner(message = error, onDismiss = viewModel::clearSessionError, modifier = Modifier.align(Alignment.TopCenter))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this workout?") },
            text = { Text("Its sets and notes will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteSession(onDeleted = onBack)
                }) { Text("Delete", color = DeleteRed) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    // A session deleted elsewhere (or a stale link): nothing to show, so go back.
    var everLoaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(detail) {
        if (detail != null) {
            everLoaded = true
        } else if (everLoaded) {
            onBack()
        }
    }
}

/** 56 tall, back on the left, the overflow menu on the right; no title (it's in the content). */
@Composable
private fun DetailTopBar(
    editing: Boolean,
    onBack: () -> Unit,
    onEditSets: () -> Unit,
    onSaveAsTemplate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (editing) "Cancel editing" else "Back",
                tint = WarmIcon
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (!editing) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = WarmIcon)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MenuItem("Edit sets", Icons.Filled.Edit, WarmOnSurface, WarmIcon) {
                        menuOpen = false
                        onEditSets()
                    }
                    MenuItem("Save as template", Icons.Filled.BookmarkAdd, WarmOnSurface, WarmIcon) {
                        menuOpen = false
                        onSaveAsTemplate()
                    }
                    MenuItem("Delete", Icons.Filled.Delete, DeleteRed, DeleteRed) {
                        menuOpen = false
                        onDelete()
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    label: String,
    icon: ImageVector,
    textColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 14.sp, color = textColor) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
        modifier = Modifier.width(200.dp)
    )
}

@Composable
private fun DetailHeader(detail: SessionDetail) {
    Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp)) {
        Text(
            detail.session.templateName.ifBlank { "Workout" },
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = WarmOnSurface
        )
        Text(
            sessionSubtitle(detail.date, detail.session.startedAt, detail.session.createdAt),
            fontSize = 14.sp,
            color = WarmSecondaryText,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** Duration / Sets / PRs - no volume tile: a summed total isn't meaningful. */
@Composable
private fun StatsRow(detail: SessionDetail) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Duration", durationLabel(detail.durationMinutes), Modifier.weight(1f))
        StatTile("Sets", detail.setCount.toString(), Modifier.weight(1f))
        StatTile("PRs", detail.prCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.detailCard(RoundedCornerShape(16.dp)).padding(12.dp)) {
        Text(label, fontSize = 12.sp, color = WarmSecondaryText)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ExerciseLogCard(exercise: ExerciseLog) {
    Column(modifier = Modifier.detailCard().padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                exercise.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = WarmOnSurface,
                modifier = Modifier.weight(1f).alignByBaseline()
            )
            exercise.comparison?.let { comparison ->
                Text(
                    comparisonLabel(comparison),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (comparison.isUp) ProgressUp else WarmSecondaryText,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        exercise.sets.forEachIndexed { index, set ->
            if (index > 0) HorizontalDivider(color = WarmField)
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}", fontSize = 13.sp, color = WarmMutedIcon, modifier = Modifier.width(28.dp))
                Text(
                    loadLabel(set, exercise.bodyweight),
                    fontSize = 14.sp,
                    color = WarmOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (set.isPr) PrBadgeChip()
            }
        }
    }
}

@Composable
private fun PrBadgeChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PrBadge)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = OnPrBadge, modifier = Modifier.size(14.dp))
        Text("PR", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnPrBadge)
    }
}

@Composable
private fun NotesReadCard(notes: String) {
    Column(modifier = Modifier.detailCard().padding(16.dp)) {
        Text("Notes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WarmSecondaryText)
        Text(notes, fontSize = 14.sp, lineHeight = 20.sp, color = WarmBodyText, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Edit sets: the 1c set table without ticks; Previous shows the value from before editing. */
@Composable
private fun EditExerciseCard(
    exercise: ActiveExercise,
    onRepsChange: (Int, String) -> Unit,
    onWeightChange: (Int, String) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit
) {
    Column(modifier = Modifier.detailCard().padding(start = 8.dp, top = 14.dp, end = 8.dp, bottom = 8.dp)) {
        Text(
            exercise.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = WorkoutInk,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        SetTable(
            exercise = exercise,
            onRepsChange = onRepsChange,
            onWeightChange = onWeightChange,
            onToggleSetDone = null,
            onAddSet = onAddSet,
            onRemoveSet = onRemoveSet
        )
    }
}

/** Fixed footer: Repeat workout (or Save changes while editing), 52 tall, pill. */
@Composable
private fun DetailFooter(label: String, showReplayIcon: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(WarmSurface)) {
        HorizontalDivider(color = WarmDivider)
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 28.dp)
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (showReplayIcon) {
                Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
