package app.theone.android.workout.frontend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.ui.EditDeleteMenu
import app.theone.android.ui.LocalWeekStart
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.DeleteRed
import app.theone.android.ui.theme.OneTheme
import app.theone.android.ui.theme.WarmBodyText
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmMutedIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface
import app.theone.android.ui.theme.WorkoutContainer
import app.theone.android.ui.theme.WorkoutInk
import app.theone.android.workout.backend.lastDoneByTemplate
import app.theone.android.workout.backend.recentSessions
import app.theone.android.workout.backend.sessionsThisWeek
import app.theone.android.workout.backend.setCount
import app.theone.android.workout.sessions.backend.StrengthExercise
import app.theone.android.workout.sessions.backend.StrengthSession
import app.theone.android.workout.sessions.backend.StrengthSet
import app.theone.android.workout.sessions.backend.durationMinutes
import app.theone.android.workout.sessions.backend.localDate
import app.theone.android.workout.templates.backend.TemplateExercise
import app.theone.android.workout.templates.backend.TemplateSet
import app.theone.android.workout.templates.backend.WorkoutTemplate
import java.time.LocalDate

// 1a in the workout redesign handoff.

private val CardShape = RoundedCornerShape(20.dp)

@Composable
private fun Modifier.workoutCard(): Modifier =
    fillMaxWidth()
        .clip(CardShape)
        .background(CardSurface)
        .border(1.dp, WarmCardBorder, CardShape)

@Composable
fun WorkoutHomeContent(
    templates: List<WorkoutTemplate>,
    sessions: List<StrengthSession>,
    onCreateTemplateClick: () -> Unit,
    onStartClick: (WorkoutTemplate) -> Unit,
    onEditTemplate: (WorkoutTemplate) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onOpenSession: (StrengthSession) -> Unit = {}
) {
    val lastDone = remember(sessions) { lastDoneByTemplate(sessions) }
    val recent = remember(sessions) { recentSessions(sessions) }
    val firstDay = LocalWeekStart.current.dayOfWeek
    val weekCount = remember(sessions, firstDay) { sessionsThisWeek(sessions, LocalDate.now(), firstDay) }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSurface),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HomeHeader(weekCount = weekCount, onCreateTemplateClick = onCreateTemplateClick)
        }

        if (templates.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .workoutCard()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Create your first template to start logging workouts.", fontSize = 15.sp, color = WarmBodyText)
                    TemplateButton(onClick = onCreateTemplateClick)
                }
            }
        } else {
            item { SectionLabel("Templates") }
            items(templates, key = { it.id }) { template ->
                TemplateCard(
                    template = template,
                    lastDone = lastDone[template.name],
                    onStart = { onStartClick(template) },
                    onEdit = { onEditTemplate(template) },
                    onDelete = { pendingDelete = template.id }
                )
            }
        }

        if (recent.isNotEmpty()) {
            item { SectionLabel("Recent", topPadding = 12.dp) }
            item {
                Column(modifier = Modifier.workoutCard()) {
                    recent.forEachIndexed { index, session ->
                        if (index > 0) HorizontalDivider(color = WarmDivider)
                        RecentRow(session, onClick = { onOpenSession(session) })
                    }
                }
            }
        }
    }

    templates.find { it.id == pendingDelete }?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${template.name}?") },
            text = { Text("The template is removed. Workouts you logged with it stay in Recent.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDeleteTemplate(template.id)
                }) {
                    Text("Delete", color = DeleteRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HomeHeader(weekCount: Int, onCreateTemplateClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Workout", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, color = WarmOnSurface)
            Text(weekCountLabel(weekCount), fontSize = 14.sp, color = WarmSecondaryText)
        }
        TemplateButton(onClick = onCreateTemplateClick)
    }
}

@Composable
private fun TemplateButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
        border = BorderStroke(1.dp, WarmButtonOutline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardSurface, contentColor = WorkoutInk)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Template", fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionLabel(text: String, topPadding: Dp = 8.dp) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = WarmSecondaryText,
        modifier = Modifier.padding(start = 8.dp, top = topPadding, end = 8.dp)
    )
}

/** Card tap does nothing on purpose - a workout starts only from Start (decision 2026-10-11). */
@Composable
private fun TemplateCard(
    template: WorkoutTemplate,
    lastDone: LocalDate?,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .workoutCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TemplateIconTile(size = 44, radius = 14, iconSize = 24)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    template.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(templateMetaLabel(template.exercises.size, template.setCount()), fontSize = 13.sp, color = WarmSecondaryText)
            }
            Box(modifier = Modifier.offset(x = 6.dp, y = (-6).dp)) {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options for ${template.name}", tint = WarmIcon)
                }
                EditDeleteMenu(expanded = menuOpen, onDismiss = { menuOpen = false }, onDelete = onDelete, onEdit = onEdit)
            }
        }
        if (template.exercises.isNotEmpty()) {
            Text(
                template.exercises.joinToString(" · ") { it.name },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = WarmBodyText
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(lastDoneLabel(lastDone), fontSize = 13.sp, color = WarmSecondaryText, modifier = Modifier.weight(1f))
            Button(
                onClick = onStart,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 18.dp),
                colors = ButtonDefaults.buttonColors()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** The fitness_center tile on template cards (44dp) and the start sheet header (48dp). */
@Composable
internal fun TemplateIconTile(size: Int, radius: Int, iconSize: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(radius.dp))
            .background(WorkoutContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = WorkoutInk, modifier = Modifier.size(iconSize.dp))
    }
}

/** Opens the session detail (3a, #48). Date block: month, day, weekday. */
@Composable
private fun RecentRow(session: StrengthSession, onClick: () -> Unit) {
    val date = session.localDate()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date?.let(::shortMonth).orEmpty(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.44.sp,
                color = WarmSecondaryText
            )
            Text(date?.dayOfMonth?.toString().orEmpty(), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface)
            Text(date?.let(::shortWeekday).orEmpty(), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = WarmSecondaryText)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.templateName.ifBlank { "Workout" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = WarmOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(sessionMetaLabel(session.setCount(), session.durationMinutes()), fontSize = 13.sp, color = WarmSecondaryText)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = WarmMutedIcon, modifier = Modifier.size(22.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutHomeContentPreview() {
    OneTheme {
        WorkoutHomeContent(
            templates = listOf(
                WorkoutTemplate(
                    id = "1",
                    name = "Push Day",
                    exercises = listOf(
                        TemplateExercise("Bench Press", List(3) { TemplateSet(8, 60.0) }),
                        TemplateExercise("Lateral Raise", List(3) { TemplateSet(12, 8.0) }),
                        TemplateExercise("Tricep Pushdown")
                    )
                )
            ),
            sessions = listOf(
                StrengthSession(
                    id = "1",
                    date = "2026-10-08",
                    templateName = "Push Day",
                    exercises = listOf(StrengthExercise("Bench Press", listOf(StrengthSet(8, 60.0))))
                )
            ),
            onCreateTemplateClick = {},
            onStartClick = {},
            onEditTemplate = {},
            onDeleteTemplate = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun WorkoutHomeContentEmptyPreview() {
    OneTheme {
        WorkoutHomeContent(
            templates = emptyList(),
            sessions = emptyList(),
            onCreateTemplateClick = {},
            onStartClick = {},
            onEditTemplate = {},
            onDeleteTemplate = {}
        )
    }
}
