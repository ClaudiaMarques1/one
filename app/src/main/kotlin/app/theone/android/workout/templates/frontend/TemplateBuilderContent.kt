package app.theone.android.workout.templates.frontend

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.ui.theme.OneTheme
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDisabledFill
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmHandle
import app.theone.android.ui.theme.WarmHint
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmMutedIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface
import app.theone.android.ui.theme.WorkoutContainer
import app.theone.android.ui.theme.WorkoutDeep
import app.theone.android.ui.theme.WorkoutInk
import app.theone.android.ui.theme.WorkoutSoft
import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.TemplateSet
import sh.calvin.reorderable.ReorderableColumn

// 1d in the workout redesign handoff.

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun TemplateBuilderContent(
    name: String,
    description: String,
    exercises: List<TemplateExercise>,
    isEditing: Boolean,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddExercisesClick: () -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onMoveExercise: (from: Int, to: Int) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = WarmIcon)
            }
            Text(
                if (isEditing) "Edit template" else "New template",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = WarmOnSurface
            )
        }
        HorizontalDivider(color = WarmDivider)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LabeledField(label = "Template name", labelColor = WorkoutInk) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LabeledField(label = "Description (optional)", labelColor = WarmSecondaryText) {
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                )
            }

            ExercisesHeader(count = exercises.size, onAddClick = onAddExercisesClick)
            ExerciseListCard(exercises = exercises, onRemove = onRemoveExercise, onMove = onMoveExercise)
            Text(
                "Sets and weights update from your latest workout each time you finish one.",
                fontSize = 13.sp,
                color = WarmHint,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        HorizontalDivider(color = WarmDivider)
        Button(
            onClick = onSave,
            enabled = name.isNotBlank() && exercises.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = WarmDisabledFill,
                disabledContentColor = WarmMutedIcon
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)
                .height(56.dp)
        ) {
            Text(if (isEditing) "Save changes" else "Save template", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = WarmButtonOutline,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

@Composable
private fun LabeledField(label: String, labelColor: Color, field: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = labelColor)
        field()
    }
}

@Composable
private fun ExercisesHeader(count: Int, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            buildAnnotatedString {
                append("Exercises")
                withStyle(SpanStyle(color = WarmSecondaryText)) { append(" · $count") }
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = WarmOnSurface,
            modifier = Modifier.weight(1f)
        )
        FilledTonalButton(
            onClick = onAddClick,
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = WorkoutContainer, contentColor = WorkoutDeep)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Drag the handle to reorder; the move is applied when the drag settles. */
@Composable
private fun ExerciseListCard(exercises: List<TemplateExercise>, onRemove: (Int) -> Unit, onMove: (Int, Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.White)
            .border(1.dp, WarmCardBorder, CardShape)
    ) {
        if (exercises.isEmpty()) {
            Text(
                "No exercises yet. Tap Add to pick some.",
                fontSize = 14.sp,
                color = WarmSecondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        } else {
            ReorderableColumn(list = exercises, onSettle = onMove) { index, exercise, isDragging ->
                // Templates can hold the same exercise twice (Duplicate in a workout), so the name alone isn't a unique key.
                key(exercise.name, exercises.take(index).count { it.name == exercise.name }) {
                    ExerciseRow(
                        number = index + 1,
                        exercise = exercise,
                        showDivider = index > 0 && !isDragging,
                        isDragging = isDragging,
                        handle = Modifier.draggableHandle(),
                        onRemove = { onRemove(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    number: Int,
    exercise: TemplateExercise,
    showDivider: Boolean,
    isDragging: Boolean,
    handle: Modifier,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .then(if (isDragging) Modifier.shadow(8.dp, RoundedCornerShape(12.dp)) else Modifier)
            .background(Color.White)
    ) {
        if (showDivider) HorizontalDivider(color = WarmDivider)
        Row(
            modifier = Modifier.padding(start = 8.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.DragIndicator,
                contentDescription = "Reorder ${exercise.name}",
                tint = WarmHandle,
                modifier = handle.size(22.dp)
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(WorkoutSoft),
                contentAlignment = Alignment.Center
            ) {
                Text("$number", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WorkoutInk)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = WarmOnSurface)
                Text(
                    if (exercise.sets.isEmpty()) "No sets yet" else "Last time: ${setSummary(exercise.sets)}",
                    fontSize = 13.sp,
                    color = WarmSecondaryText
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Remove ${exercise.name}", tint = WarmSecondaryText)
            }
        }
    }
}

@Preview(showBackground = true, name = "Template builder")
@Composable
private fun TemplateBuilderContentPreview() {
    OneTheme {
        TemplateBuilderContent(
            name = "Push Day",
            description = "Chest, shoulders, triceps",
            exercises = listOf(
                TemplateExercise("Bench Press", List(3) { TemplateSet(10, 35.0) }),
                TemplateExercise("Lateral Raise")
            ),
            isEditing = false,
            onNameChange = {},
            onDescriptionChange = {},
            onAddExercisesClick = {},
            onRemoveExercise = {},
            onMoveExercise = { _, _ -> },
            onCancel = {},
            onSave = {}
        )
    }
}
