package app.theone.android.workout.planner.frontend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.theone.android.settings.backend.DataStoreSettingsRepository
import app.theone.android.ui.theme.CalendarColors
import app.theone.android.workout.planner.backend.TrainingPlanViewModel
import app.theone.android.workout.planner.backend.TrainingWeekUi
import app.theone.android.workout.planner.backend.WeekPip

private val C = CalendarColors

/** The planner's ViewModel with the app's DataStore settings (week start, card expanded state). */
@Composable
fun trainingPlanViewModel(): TrainingPlanViewModel = viewModel(
    factory = viewModelFactory {
        initializer { TrainingPlanViewModel(DataStoreSettingsRepository(checkNotNull(this[APPLICATION_KEY]))) }
    }
)

/**
 * 5a "Training this week": collapsed by default with the week's progress line; expanded shows the progress
 * bars, the routine and Edit routine. With no routine yet, the whole card opens 4a to set one up.
 */
@Composable
fun TrainingWeekCard(week: TrainingWeekUi, onToggle: () -> Unit, onEditRoutine: () -> Unit, modifier: Modifier = Modifier) {
    if (!week.loaded) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(C.AccentSoft)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
    ) {
        val expandedText = if (week.expanded) "Expanded" else "Collapsed"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = if (week.hasRoutine) onToggle else onEditRoutine)
                .semantics { if (week.hasRoutine) stateDescription = expandedText }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Training this week", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface))
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (week.hasRoutine) week.note else "No routine yet. Tap to plan your training days.",
                    style = TextStyle(fontSize = 13.sp, color = C.BodySecondary)
                )
            }
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        !week.hasRoutine -> Icons.Outlined.ChevronRight
                        week.expanded -> Icons.Outlined.ExpandLess
                        else -> Icons.Outlined.ExpandMore
                    },
                    contentDescription = null,
                    tint = C.IconTint
                )
            }
        }

        AnimatedVisibility(
            visible = week.hasRoutine && week.expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 4.dp, end = 12.dp, bottom = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.week.pips.forEach { pip ->
                        val color = when (pip) {
                            WeekPip.DONE -> C.WorkoutDot
                            WeekPip.PLANNED -> C.PlannedPip
                            WeekPip.EMPTY -> C.EmptyPip
                        }
                        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(week.routineLine, style = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, color = C.SecondaryText))
                TextButton(onClick = onEditRoutine, modifier = Modifier.padding(top = 2.dp).height(36.dp)) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, tint = C.AccentOnContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit routine", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.AccentOnContainer))
                }
            }
        }
    }
}
