package app.theone.android.workout.sessions.frontend

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.ui.dashedBorder
import app.theone.android.ui.theme.DeleteRed
import app.theone.android.ui.theme.OneTheme
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDisabledFill
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmField
import app.theone.android.ui.theme.WarmHint
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmMutedIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface
import app.theone.android.ui.theme.WorkoutContainer
import app.theone.android.ui.theme.WorkoutDashedOutline
import app.theone.android.ui.theme.WorkoutDeep
import app.theone.android.ui.theme.WorkoutInk
import app.theone.android.ui.theme.WorkoutRestProgress
import app.theone.android.ui.theme.WorkoutRestSecondary
import app.theone.android.ui.theme.WorkoutSoft
import app.theone.android.workout.sessions.backend.ActiveExercise
import app.theone.android.workout.sessions.backend.ActiveSet
import app.theone.android.workout.sessions.backend.FinishMode
import app.theone.android.workout.sessions.backend.RestState
import app.theone.android.workout.sessions.backend.RestVibrator
import app.theone.android.workout.sessions.backend.StrengthSet
import app.theone.android.workout.sessions.backend.doneSetCount
import app.theone.android.workout.sessions.backend.elapsedFraction
import app.theone.android.workout.sessions.backend.formatWeightInput
import app.theone.android.workout.sessions.backend.hasUnfinishedSets
import app.theone.android.workout.sessions.backend.isRestCountdownTick
import app.theone.android.workout.sessions.backend.remainingSeconds
import app.theone.android.workout.sessions.backend.restOptionsSeconds
import app.theone.android.workout.sessions.backend.totalSetCount
import kotlinx.coroutines.delay

// 1c in the workout redesign handoff. Renders StrengthViewModel's active workout; every change goes
// back through the callbacks, keyed by ActiveExercise.instanceId.

private val CardShape = RoundedCornerShape(20.dp)
private val TabularNumbers = "tnum"
private val ColumnHeaderStyle =
    TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = WarmSecondaryText, letterSpacing = 0.4.sp)

@Composable
fun ActiveWorkoutContent(
    templateName: String,
    startTimeMillis: Long?,
    exercises: List<ActiveExercise>,
    restState: RestState?,
    vibrateOnRestEnd: Boolean,
    onRepsChange: (instanceId: String, setIndex: Int, text: String) -> Unit,
    onWeightChange: (instanceId: String, setIndex: Int, text: String) -> Unit,
    onToggleSetDone: (instanceId: String, setIndex: Int) -> Unit,
    onAddSet: (instanceId: String) -> Unit,
    onRemoveSet: (instanceId: String, setIndex: Int) -> Unit,
    onDuplicateExercise: (instanceId: String) -> Unit,
    onRemoveExercise: (instanceId: String) -> Unit,
    onSetRestSeconds: (instanceId: String, seconds: Int) -> Unit,
    onAdjustRest: (deltaSeconds: Int) -> Unit,
    onEndRest: () -> Unit,
    onAddExerciseClick: () -> Unit,
    onFinish: (FinishMode) -> Unit,
    onDiscard: () -> Unit
) {
    var restSheetFor by rememberSaveable { mutableStateOf<String?>(null) }
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val doneSets = exercises.doneSetCount()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSurface)
    ) {
        ActiveWorkoutHeader(
            templateName = templateName,
            startTimeMillis = startTimeMillis,
            progress = progressLabel(doneSets, exercises.totalSetCount()),
            finishEnabled = doneSets > 0,
            onFinish = {
                if (exercises.hasUnfinishedSets()) showFinishDialog = true else onFinish(FinishMode.ALL_SETS)
            }
        )
        HorizontalDivider(color = WarmDivider)

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Extra bottom room while the rest bar floats over the list.
                contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = if (restState != null) 120.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(exercises, key = { it.instanceId }) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onRepsChange = { setIndex, text -> onRepsChange(exercise.instanceId, setIndex, text) },
                        onWeightChange = { setIndex, text -> onWeightChange(exercise.instanceId, setIndex, text) },
                        onToggleSetDone = { onToggleSetDone(exercise.instanceId, it) },
                        onAddSet = { onAddSet(exercise.instanceId) },
                        onRemoveSet = { onRemoveSet(exercise.instanceId, it) },
                        onDuplicate = { onDuplicateExercise(exercise.instanceId) },
                        onRemove = { onRemoveExercise(exercise.instanceId) },
                        onRestClick = { restSheetFor = exercise.instanceId }
                    )
                }
                item {
                    AddExerciseButton(onClick = onAddExerciseClick)
                }
                item {
                    TextButton(onClick = { showDiscardDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Discard workout", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DeleteRed)
                    }
                }
            }

            if (restState != null) {
                RestBar(
                    rest = restState,
                    vibrate = vibrateOnRestEnd,
                    onAdjust = onAdjustRest,
                    onEnd = onEndRest,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }
        }
    }

    exercises.find { it.instanceId == restSheetFor }?.let { exercise ->
        RestSheet(
            exerciseName = exercise.name,
            selectedSeconds = exercise.restSeconds,
            onSelect = {
                onSetRestSeconds(exercise.instanceId, it)
                restSheetFor = null
            },
            onDismiss = { restSheetFor = null }
        )
    }

    if (showFinishDialog) {
        FinishDialog(
            onSave = {
                showFinishDialog = false
                onFinish(it)
            },
            onDismiss = { showFinishDialog = false }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard this workout?") },
            text = { Text("Logged sets will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDiscard()
                }) {
                    Text("Discard", color = DeleteRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep going") }
            }
        )
    }
}

@Composable
private fun ActiveWorkoutHeader(
    templateName: String,
    startTimeMillis: Long?,
    progress: String,
    finishEnabled: Boolean,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 8.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                templateName,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = WarmOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (startTimeMillis != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Timer, contentDescription = null, tint = WorkoutInk, modifier = Modifier.size(16.dp))
                        ElapsedText(startTimeMillis)
                    }
                }
                Text(progress, fontSize = 13.sp, color = WarmSecondaryText)
            }
        }
        Button(
            onClick = onFinish,
            enabled = finishEnabled,
            modifier = Modifier.height(40.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = WarmDisabledFill,
                disabledContentColor = WarmMutedIcon
            )
        ) {
            Text("Finish", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ElapsedText(startTimeMillis: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startTimeMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - (now - startTimeMillis).mod(1000L))
        }
    }
    Text(
        formatElapsed((now - startTimeMillis) / 1000),
        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WorkoutInk, fontFeatureSettings = TabularNumbers)
    )
}

@Composable
private fun ExerciseCard(
    exercise: ActiveExercise,
    onRepsChange: (Int, String) -> Unit,
    onWeightChange: (Int, String) -> Unit,
    onToggleSetDone: (Int) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
    onRestClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.White)
            .border(1.dp, WarmCardBorder, CardShape)
            .padding(start = 8.dp, top = 14.dp, end = 8.dp, bottom = 8.dp)
    ) {
        ExerciseCardHeader(
            name = exercise.name,
            restSeconds = exercise.restSeconds,
            onRestClick = onRestClick,
            onDuplicate = onDuplicate,
            onRemove = onRemove
        )
        SetGridRow(modifier = Modifier.padding(start = 4.dp, top = 10.dp, end = 4.dp, bottom = 4.dp)) {
            Text("SET", style = ColumnHeaderStyle, textAlign = TextAlign.Center, modifier = Modifier.width(SetColumnWidth))
            Text("PREVIOUS", style = ColumnHeaderStyle, modifier = Modifier.weight(1f))
            Text("KG", style = ColumnHeaderStyle, textAlign = TextAlign.Center, modifier = Modifier.width(KgColumnWidth))
            Text("REPS", style = ColumnHeaderStyle, textAlign = TextAlign.Center, modifier = Modifier.width(RepsColumnWidth))
            Spacer(modifier = Modifier.width(TickColumnWidth))
        }
        exercise.sets.forEachIndexed { setIndex, set ->
            SetRow(
                number = setIndex + 1,
                set = set,
                previous = exercise.previous(setIndex),
                onRepsChange = { onRepsChange(setIndex, it) },
                onWeightChange = { onWeightChange(setIndex, it) },
                onToggleDone = { onToggleSetDone(setIndex) },
                onRemove = { onRemoveSet(setIndex) }
            )
        }
        TextButton(
            onClick = onAddSet,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = WorkoutInk, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add set", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WorkoutInk)
        }
    }
}

@Composable
private fun ExerciseCardHeader(
    name: String,
    restSeconds: Int,
    onRestClick: () -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(start = 8.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = WorkoutInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(WorkoutSoft)
                .clickable(onClickLabel = "Change rest time", onClick = onRestClick)
                .padding(start = 8.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Timer, contentDescription = "Rest", tint = WorkoutInk, modifier = Modifier.size(18.dp))
            Text(restLabel(restSeconds), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = WorkoutInk)
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options for $name", tint = WarmIcon)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Duplicate", color = WarmOnSurface) },
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = WarmIcon) },
                    onClick = {
                        menuOpen = false
                        onDuplicate()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Remove exercise", color = DeleteRed) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = DeleteRed) },
                    onClick = {
                        menuOpen = false
                        onRemove()
                    }
                )
            }
        }
    }
}

private val SetColumnWidth = 36.dp
private val KgColumnWidth = 72.dp
private val RepsColumnWidth = 64.dp
private val TickColumnWidth = 48.dp

/** The set grid: 36 | flexible | 72 | 64 | 48, 6dp gaps - shared by the column headers and set rows. */
@Composable
private fun SetGridRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetRow(
    number: Int,
    set: ActiveSet,
    previous: StrengthSet?,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onToggleDone: () -> Unit,
    onRemove: () -> Unit
) {
    val rowColor by animateColorAsState(if (set.done) WorkoutSoft else Color.Transparent, tween(150), label = "setRow")
    val fieldColor by animateColorAsState(if (set.done) Color.White else WarmField, tween(150), label = "setField")
    val tickColor by animateColorAsState(if (set.done) MaterialTheme.colorScheme.primary else WarmField, tween(150), label = "tick")
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        SetGridRow(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(rowColor)
                // Long press is the only way to remove a set - the row has no trash icon.
                .combinedClickable(onClick = {}, onLongClickLabel = "Remove set", onLongClick = { menuOpen = true })
                .padding(horizontal = 4.dp, vertical = 3.dp)
        ) {
            Text(
                "$number",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (set.done) WorkoutInk else WarmSecondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(SetColumnWidth)
            )
            Text(
                previousLabel(previous),
                style = TextStyle(fontSize = 13.sp, color = WarmHint, fontFeatureSettings = TabularNumbers),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            SetField(
                value = set.weightText,
                placeholder = previous?.weightKg?.let(::formatWeightInput).orEmpty(),
                keyboardType = KeyboardType.Decimal,
                fill = fieldColor,
                onValueChange = { text -> if (text.all { it.isDigit() || it == '.' || it == ',' }) onWeightChange(text) },
                modifier = Modifier.width(KgColumnWidth)
            )
            SetField(
                value = set.repsText,
                placeholder = previous?.reps?.toString().orEmpty(),
                keyboardType = KeyboardType.Number,
                fill = fieldColor,
                onValueChange = { text -> if (text.all { it.isDigit() }) onRepsChange(text) },
                modifier = Modifier.width(RepsColumnWidth)
            )
            Box(modifier = Modifier.width(TickColumnWidth), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tickColor)
                        .clickable(onClick = onToggleDone),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = if (set.done) "Set done" else "Mark set done",
                        tint = if (set.done) Color.White else WarmMutedIcon,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Remove set", color = DeleteRed) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = DeleteRed) },
                onClick = {
                    menuOpen = false
                    onRemove()
                }
            )
        }
    }
}

@Composable
private fun SetField(
    value: String,
    placeholder: String,
    keyboardType: KeyboardType,
    fill: Color,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = WarmOnSurface,
        textAlign = TextAlign.Center,
        fontFeatureSettings = TabularNumbers
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(fill),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                if (value.isEmpty()) {
                    Text(placeholder, style = textStyle.copy(color = WarmHint), maxLines = 1)
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun AddExerciseButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .dashedBorder(WorkoutDashedOutline, 24.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = WorkoutInk, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add exercise", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WorkoutInk)
    }
}

/**
 * Floating rest countdown. Ticks on whole seconds from the wall clock. If [vibrate]: a quick tick at
 * 3, 2 and 1 seconds left, and a longer buzz when it reaches 0 and ends the rest.
 */
@Composable
private fun RestBar(rest: RestState, vibrate: Boolean, onAdjust: (Int) -> Unit, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val vibrator = remember(context) { RestVibrator(context) }
    val currentOnEnd by rememberUpdatedState(onEnd)
    val currentVibrate by rememberUpdatedState(vibrate)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(rest) {
        var lastTick: Int? = null
        while (true) {
            now = System.currentTimeMillis()
            val remaining = rest.remainingSeconds(now)
            if (remaining <= 0) {
                // No buzz when coming back long after the rest ran out.
                if (currentVibrate && now - rest.endsAtMillis < 2000) vibrator.end()
                currentOnEnd()
                break
            }
            if (currentVibrate && isRestCountdownTick(remaining) && remaining != lastTick) vibrator.tick()
            lastTick = remaining
            delay((rest.endsAtMillis - now).mod(1000L).takeIf { it > 0 } ?: 1000L)
        }
    }
    val progress by animateFloatAsState(rest.elapsedFraction(now), tween(1000, easing = LinearEasing), label = "restProgress")
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(20.dp, shape, ambientColor = WorkoutDeep, spotColor = WorkoutDeep)
            .clip(shape)
            .background(WorkoutDeep)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Rest · ${rest.exerciseName}",
                    fontSize = 12.sp,
                    color = WorkoutRestSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatRestRemaining(rest.remainingSeconds(now)),
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontFeatureSettings = TabularNumbers
                    )
                )
            }
            RestAdjustButton("-30") { onAdjust(-30) }
            RestAdjustButton("+30") { onAdjust(30) }
            Button(
                onClick = onEnd,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = WorkoutDeep)
            ) {
                Text("Skip", fontWeight = FontWeight.SemiBold)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progress)
                .height(4.dp)
                .background(WorkoutRestProgress)
        )
    }
}

@Composable
private fun RestAdjustButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Text(label, style = TextStyle(fontFeatureSettings = TabularNumbers))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestSheet(exerciseName: String, selectedSeconds: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = WarmSurface
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Text("Rest timer", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface)
            Text("$exerciseName · starts when you tick a set", fontSize = 14.sp, color = WarmSecondaryText)
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                restOptionsSeconds.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { seconds ->
                            val selected = seconds == selectedSeconds
                            val shape = RoundedCornerShape(14.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(shape)
                                    .background(if (selected) WorkoutContainer else Color.White)
                                    .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else WarmButtonOutline, shape)
                                    .clickable { onSelect(seconds) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    restLabel(seconds),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) WorkoutDeep else WarmOnSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Finish with unticked sets: the user chooses each time (decision 2026-10-11 in the plan). */
@Composable
private fun FinishDialog(onSave: (FinishMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finish workout?") },
        text = { Text("Some sets aren't ticked. Exercises left with no sets aren't saved.") },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = { onSave(FinishMode.DONE_SETS_ONLY) }) { Text("Save only done sets") }
                TextButton(onClick = { onSave(FinishMode.ALL_SETS) }) { Text("Save all sets") }
                TextButton(onClick = onDismiss) { Text("Keep going") }
            }
        }
    )
}

@Preview(showBackground = true, name = "Active workout")
@Composable
private fun ActiveWorkoutContentPreview() {
    OneTheme {
        ActiveWorkoutContent(
            templateName = "Push Day",
            startTimeMillis = System.currentTimeMillis() - 125_000,
            exercises = listOf(
                ActiveExercise(
                    name = "Bench Press",
                    sets = listOf(ActiveSet("8", "60", done = true), ActiveSet()),
                    previousSets = listOf(StrengthSet(8, 60.0), StrengthSet(6, 62.5)),
                    instanceId = "a"
                ),
                ActiveExercise(name = "Push-up", sets = listOf(ActiveSet()), restSeconds = 0, instanceId = "b")
            ),
            restState = RestState("Bench Press", 90, System.currentTimeMillis() + 84_000),
            vibrateOnRestEnd = true,
            onRepsChange = { _, _, _ -> },
            onWeightChange = { _, _, _ -> },
            onToggleSetDone = { _, _ -> },
            onAddSet = {},
            onRemoveSet = { _, _ -> },
            onDuplicateExercise = {},
            onRemoveExercise = {},
            onSetRestSeconds = { _, _ -> },
            onAdjustRest = {},
            onEndRest = {},
            onAddExerciseClick = {},
            onFinish = {},
            onDiscard = {}
        )
    }
}
