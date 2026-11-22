package app.theone.android.workout.frontend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface
import app.theone.android.ui.theme.WorkoutInk
import app.theone.android.ui.theme.WorkoutSoft
import app.theone.android.workout.backend.setCount
import app.theone.android.workout.templates.backend.WorkoutTemplate
import app.theone.android.workout.templates.frontend.setSummary
import java.time.LocalDate

// 1b in the workout redesign handoff - replaces the "Start workout?" dialog.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartWorkoutSheet(
    template: WorkoutTemplate,
    lastDone: LocalDate?,
    onStart: () -> Unit,
    onEditFirst: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = WarmSurface
    ) {
        Row(
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TemplateIconTile(size = 48, radius = 16, iconSize = 26)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    template.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmOnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${templateMetaLabel(template.exercises.size, template.setCount())} · ${lastDoneShortLabel(lastDone, LocalDate.now())}",
                    fontSize = 13.sp,
                    color = WarmSecondaryText
                )
            }
        }
        HorizontalDivider(color = WarmDivider, modifier = Modifier.padding(horizontal = 24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            itemsIndexed(template.exercises) { index, exercise ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(WorkoutSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WorkoutInk)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exercise.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = WarmOnSurface)
                        Text(setSummary(exercise.sets), fontSize = 13.sp, color = WarmSecondaryText)
                    }
                }
            }
        }

        HorizontalDivider(color = WarmDivider)
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEditFirst,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                border = BorderStroke(1.dp, WarmButtonOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WorkoutInk)
            ) {
                Text("Edit first", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onStart,
                modifier = Modifier
                    .weight(2f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start workout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
