package app.theone.android.exercise.frontend

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.theone.android.exercise.backend.EXERCISE_MEDIA_ATTRIBUTION
import app.theone.android.exercise.backend.Exercise
import app.theone.android.exercise.backend.ExerciseCatalogLoader
import app.theone.android.exercise.backend.categoriesOf
import app.theone.android.exercise.backend.preselectedIds
import app.theone.android.exercise.backend.selectedCountByCategory
import app.theone.android.exercise.backend.selectedInOrder
import app.theone.android.exercise.backend.visibleExercises
import app.theone.android.ui.theme.WarmBodyText
import app.theone.android.ui.theme.WarmButtonOutline
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
import app.theone.android.ui.theme.WorkoutDeep
import app.theone.android.ui.theme.WorkoutSoft
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 2b in the workout redesign handoff: category rail with selection counts, search across all
// categories, and a tray of picked exercises in the order they were picked.

/**
 * [alreadySelectedNames] start selected, in their order; [onConfirm] gets the full selection in
 * selection order, so exercises already in a template keep their place and new picks follow.
 */
@Composable
fun ExercisePickerDialog(
    alreadySelectedNames: Collection<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (List<Exercise>) -> Unit
) {
    val context = LocalContext.current
    var allExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val selectedIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { ExerciseCatalogLoader.load(context) }
        allExercises = loaded
        selectedCategory = categoriesOf(loaded).firstOrNull()
        selectedIds.addAll(preselectedIds(loaded, alreadySelectedNames))
        isLoading = false
    }

    val categories = remember(allExercises) { categoriesOf(allExercises) }
    val visible = remember(query, selectedCategory, allExercises) { visibleExercises(allExercises, query, selectedCategory) }
    val countByCategory = selectedCountByCategory(allExercises, selectedIds)
    val selectedExercises = selectedInOrder(allExercises, selectedIds)
    val toggle: (String) -> Unit = { id -> if (id in selectedIds) selectedIds.remove(id) else selectedIds.add(id) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Back clears an active search before closing the picker.
        BackHandler(enabled = query.isNotEmpty()) { query = "" }

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
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = WarmIcon)
                }
                Text("Add exercises", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface)
            }
            HorizontalDivider(color = WarmDivider)

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Row(modifier = Modifier.weight(1f)) {
                    CategoryRail(
                        categories = categories,
                        // While searching, results span every category, so none is highlighted.
                        selectedCategory = selectedCategory.takeIf { query.isBlank() },
                        countByCategory = countByCategory,
                        onSelect = {
                            selectedCategory = it
                            query = ""
                        }
                    )
                    VerticalDivider(color = WarmDivider)
                    Column(modifier = Modifier.weight(1f)) {
                        SearchField(query = query, onQueryChange = { query = it })
                        if (visible.isEmpty()) {
                            Text(
                                "No matches",
                                fontSize = 14.sp,
                                color = WarmSecondaryText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
                                items(visible, key = { it.id }) { exercise ->
                                    ExercisePickerRow(
                                        exercise = exercise,
                                        selected = exercise.id in selectedIds,
                                        onToggle = { toggle(exercise.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = WarmDivider)
            Column(
                modifier = Modifier.padding(top = 10.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedExercises.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedExercises, key = { it.id }) { exercise ->
                            SelectionChip(name = exercise.name, onRemove = { toggle(exercise.id) })
                        }
                    }
                }
                Text(
                    EXERCISE_MEDIA_ATTRIBUTION,
                    fontSize = 11.sp,
                    color = WarmHint,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Button(
                    onClick = { onConfirm(selectedExercises) },
                    enabled = selectedExercises.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = WarmDisabledFill,
                        disabledContentColor = WarmMutedIcon
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp)
                ) {
                    Text(confirmLabel(selectedExercises.size), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CategoryRail(
    categories: List<String>,
    selectedCategory: String?,
    countByCategory: Map<String, Int>,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .width(104.dp)
            .fillMaxHeight()
            .background(WarmSurface)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            val count = countByCategory[category] ?: 0
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .background(if (isSelected) WorkoutSoft else Color.Transparent)
                        .clickable { onSelect(category) }
                        .padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        category.toDisplayLabel(),
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) WorkoutDeep else WarmBodyText,
                        modifier = Modifier.weight(1f)
                    )
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .heightIn(min = 20.dp)
                                .widthIn(min = 20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$count", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
                HorizontalDivider(color = WarmField)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp, color = WarmOnSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 4.dp)
            .height(44.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, WarmButtonOutline, shape),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = WarmIcon, modifier = Modifier.size(20.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search", fontSize = 15.sp, color = WarmHint)
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun ExercisePickerRow(exercise: Exercise, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = if (selected) "Deselect" else "Select", onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(WarmField)
        ) {
            AsyncImage(model = exercise.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                exercise.name,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) WorkoutDeep else WarmOnSurface
            )
            Text(exercise.metaLabel(), fontSize = 12.sp, color = WarmSecondaryText)
        }
        if (selected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        } else {
            Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Not selected", tint = WarmMutedIcon)
        }
    }
}

@Composable
private fun SelectionChip(name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(WorkoutContainer)
            .clickable(onClickLabel = "Remove $name", onClick = onRemove)
            .padding(start = 12.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = WorkoutDeep,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(Icons.Filled.Close, contentDescription = null, tint = WorkoutDeep, modifier = Modifier.size(16.dp))
    }
}
