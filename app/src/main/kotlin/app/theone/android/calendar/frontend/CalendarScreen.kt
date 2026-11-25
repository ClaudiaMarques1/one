package app.theone.android.calendar.frontend

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.theone.android.calendar.backend.ActivityType
import app.theone.android.calendar.backend.CalendarDateKeys
import app.theone.android.calendar.backend.CalendarView
import app.theone.android.calendar.backend.CalendarViewModel
import app.theone.android.calendar.backend.DayActivity
import app.theone.android.calendar.backend.MonthStats
import app.theone.android.calendar.backend.cellSections
import app.theone.android.calendar.backend.monthStats
import app.theone.android.diary.journal.backend.DiaryEntry
import app.theone.android.diary.journal.frontend.bodyPartLabels
import app.theone.android.meals.backend.PlannedMeal
import app.theone.android.ui.ErrorBanner
import app.theone.android.ui.LocalWeekStart
import app.theone.android.ui.theme.CalendarColors
import app.theone.android.ui.theme.DiaryInk
import app.theone.android.ui.theme.OneTheme
import app.theone.android.workout.planner.backend.DayState
import app.theone.android.workout.planner.backend.EditorKind
import app.theone.android.workout.planner.backend.TrainingPlanViewModel
import app.theone.android.workout.planner.frontend.MissedCard
import app.theone.android.workout.planner.frontend.TrainingPlanDayCard
import app.theone.android.workout.planner.frontend.TrainingPlanSheet
import app.theone.android.workout.planner.frontend.TrainingWeekCard
import app.theone.android.workout.planner.frontend.trainingPlanViewModel
import app.theone.android.workout.sessions.backend.StrengthExercise
import app.theone.android.workout.sessions.backend.StrengthSession
import app.theone.android.workout.sessions.backend.StrengthSet
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.roundToLong
import app.theone.android.ui.dashedBorder as dashedOutline

private val C = CalendarColors

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(),
    onNavigateToWorkout: () -> Unit = {},
    onNavigateToDiary: () -> Unit = {},
    onOpenWorkoutSession: (sessionId: String) -> Unit = {},
    onOpenMeals: (dateKey: String) -> Unit = {},
    // Training planner (#76): the "Training this week" card and the 4a sheet it opens.
    trainingPlan: TrainingPlanViewModel = trainingPlanViewModel()
) {
    val view by viewModel.view.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dayByDate by viewModel.dayByDate.collectAsState()
    val calendarError by viewModel.calendarError.collectAsState()
    val trainingWeek by trainingPlan.week.collectAsState()
    val planSheet by trainingPlan.sheet.collectAsState()
    val planError by trainingPlan.planError.collectAsState()
    val planStates by trainingPlan.monthStates.collectAsState()
    val dayEditor by trainingPlan.dayEditor.collectAsState()
    val planTemplates by trainingPlan.templates.collectAsState()

    // #78: the planner loads the shown month's day states and overrides.
    val shownMonth = selectedDate.take(7)
    LaunchedEffect(shownMonth) { trainingPlan.showMonth(YearMonth.parse(shownMonth)) }

    CalendarScreenContent(
        view = view,
        selectedDate = selectedDate,
        dayByDate = dayByDate,
        planStates = planStates,
        dayPlan = { dateKey, follow ->
            // The editor follows the open day sheet; a move or reset re-points the sheet through [follow].
            DisposableEffect(dateKey) {
                trainingPlan.openDay(LocalDate.parse(dateKey))
                onDispose { trainingPlan.closeDay() }
            }
            val editor = dayEditor?.takeIf { it.date.toString() == dateKey }
            when {
                editor == null -> Unit
                editor.kind == EditorKind.PAST -> editor.missedLine?.let { MissedCard(it) }
                else -> TrainingPlanDayCard(editor, planTemplates, trainingPlan) { follow(it.toString()) }
            }
        },
        // A planner error shows in its sheet while that's open, otherwise here.
        errorMessage = calendarError ?: planError.takeIf { planSheet == null },
        onDismissError = {
            viewModel.clearCalendarError()
            trainingPlan.clearPlanError()
        },
        weekCard = {
            TrainingWeekCard(week = trainingWeek, onToggle = trainingPlan::toggleWeekExpanded, onEditRoutine = trainingPlan::openSheet)
        },
        onViewChange = viewModel::setView,
        onSelectDate = viewModel::selectDate,
        onOpenDay = { dateKey ->
            viewModel.selectDate(dateKey)
            viewModel.setView(CalendarView.DAY)
        },
        onPrevious = viewModel::stepPrevious,
        onNext = viewModel::stepNext,
        onToday = viewModel::goToToday,
        onLogWorkout = onNavigateToWorkout,
        onAddDiaryEntry = onNavigateToDiary,
        onOpenWorkoutSession = onOpenWorkoutSession,
        onOpenMeals = onOpenMeals
    )

    planSheet?.let { sheet -> TrainingPlanSheet(sheet = sheet, viewModel = trainingPlan, errorMessage = planError) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarScreenContent(
    view: CalendarView,
    selectedDate: String,
    dayByDate: Map<String, DayActivity>,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {},
    weekCard: @Composable () -> Unit = {},
    planStates: Map<String, DayState> = emptyMap(),
    dayPlan: @Composable (dateKey: String, follow: (String) -> Unit) -> Unit = { _, _ -> },
    onViewChange: (CalendarView) -> Unit = {},
    onSelectDate: (String) -> Unit = {},
    onOpenDay: (String) -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onToday: () -> Unit = {},
    onLogWorkout: () -> Unit = {},
    onAddDiaryEntry: () -> Unit = {},
    onOpenWorkoutSession: (String) -> Unit = {},
    onOpenMeals: (String) -> Unit = {}
) {
    val todayKey = CalendarDateKeys.todayKey()
    val firstDayOfWeek = LocalWeekStart.current.toCalendarDay()
    val weekKeys = CalendarDateKeys.weekDateKeys(selectedDate, firstDayOfWeek)
    // 5a: tapping a day in Month opens its day sheet (Week and Day keep their inline details).
    var sheetDate by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(containerColor = C.Surface) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ErrorBanner(message = errorMessage, onDismiss = onDismissError)

            HeaderRow(
                view = view,
                selectedDate = selectedDate,
                todayKey = todayKey,
                onPrevious = onPrevious,
                onNext = onNext,
                onToday = onToday
            )

            ViewSwitcher(view = view, onViewChange = onViewChange)

            when (view) {
                CalendarView.MONTH -> MonthGridSection(
                    selectedDate = selectedDate,
                    todayKey = todayKey,
                    firstDayOfWeek = firstDayOfWeek,
                    dayByDate = dayByDate,
                    planStates = planStates,
                    weekCard = weekCard,
                    onSelectDate = { dateKey ->
                        onSelectDate(dateKey)
                        sheetDate = dateKey
                    }
                )
                CalendarView.WEEK, CalendarView.DAY -> WeekStrip(
                    weekKeys = weekKeys,
                    selectedDate = selectedDate,
                    todayKey = todayKey,
                    dayByDate = dayByDate,
                    onSelectDate = onSelectDate
                )
            }

            when (view) {
                CalendarView.MONTH -> Unit
                CalendarView.WEEK -> {
                    SectionDivider()
                    WeekAgendaSection(
                        weekKeys = weekKeys,
                        selectedDate = selectedDate,
                        todayKey = todayKey,
                        dayByDate = dayByDate,
                        onSelectRow = { dateKey -> onOpenDay(dateKey) }
                    )
                }
                CalendarView.DAY -> {
                    SectionDivider()
                    DayDetailsSection(
                        selectedDate = selectedDate,
                        todayKey = todayKey,
                        activity = dayByDate[selectedDate] ?: DayActivity(),
                        onOpenWorkoutSession = onOpenWorkoutSession,
                        onOpenMeals = onOpenMeals,
                        onLogWorkout = onLogWorkout,
                        onAddDiaryEntry = onAddDiaryEntry
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    val openSheet = sheetDate
    if (view == CalendarView.MONTH && openSheet != null) {
        DaySheet(
            dateKey = openSheet,
            todayKey = todayKey,
            activity = dayByDate[openSheet] ?: DayActivity(),
            planCard = {
                dayPlan(openSheet) { newDate ->
                    // A moved session takes the sheet with it (5a: "keeps the sheet on it").
                    onSelectDate(newDate)
                    sheetDate = newDate
                }
            },
            onDismiss = { sheetDate = null },
            // Leaving from the sheet closes it - otherwise it reopened on coming back, over the bottom nav.
            onOpenWorkoutSession = {
                sheetDate = null
                onOpenWorkoutSession(it)
            },
            onOpenMeals = {
                sheetDate = null
                onOpenMeals(it)
            },
            onLogWorkout = {
                sheetDate = null
                onLogWorkout()
            },
            onAddDiaryEntry = {
                sheetDate = null
                onAddDiaryEntry()
            }
        )
    }
}

// region Header

@Composable
private fun HeaderRow(
    view: CalendarView,
    selectedDate: String,
    todayKey: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val title = when (view) {
        CalendarView.MONTH, CalendarView.WEEK -> CalendarDateLabels.monthTitle(selectedDate)
        CalendarView.DAY -> CalendarDateLabels.dayTitle(selectedDate)
    }
    val subtitle = when (view) {
        // 5a: just the year under the month.
        CalendarView.MONTH -> CalendarDateKeys.yearOf(selectedDate).toString()
        CalendarView.WEEK -> CalendarDateLabels.weekRangeLabel(
            CalendarDateKeys.weekDateKeys(selectedDate, LocalWeekStart.current.toCalendarDay())
        )
        CalendarView.DAY -> {
            val base = "${CalendarDateLabels.dayOfWeekFullLabel(selectedDate)} · ${CalendarDateKeys.yearOf(selectedDate)}"
            if (selectedDate == todayKey) "$base · Today" else base
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, color = C.OnSurface),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = TextStyle(fontSize = 14.sp, color = C.SecondaryText))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 5a: in Month, Today only shows off the current month (picking another day this month keeps it hidden).
            val showToday = if (view == CalendarView.MONTH) selectedDate.take(7) != todayKey.take(7) else selectedDate != todayKey
            if (showToday) {
                OutlinedButton(
                    onClick = onToday,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, C.OutlineButton),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Today", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.AccentOnContainer))
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Previous ${view.name.lowercase()}",
                    tint = C.IconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Next ${view.name.lowercase()}",
                    tint = C.IconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// endregion

// region View switcher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewSwitcher(view: CalendarView, onViewChange: (CalendarView) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            .height(40.dp)
    ) {
        CalendarView.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = view == option,
                onClick = { onViewChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, CalendarView.entries.size, RoundedCornerShape(20.dp)),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = C.AccentContainer,
                    activeContentColor = C.AccentOnContainer,
                    activeBorderColor = C.OutlineSegmented,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = C.OnSurface,
                    inactiveBorderColor = C.OutlineSegmented
                ),
                icon = { SegmentedButtonDefaults.Icon(active = view == option) },
                label = {
                    Text(
                        text = when (option) {
                            CalendarView.MONTH -> "Month"
                            CalendarView.WEEK -> "Week"
                            CalendarView.DAY -> "Day"
                        },
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    )
                }
            )
        }
    }
}

// endregion

// region Month view

@Composable
private fun MonthGridSection(
    selectedDate: String,
    todayKey: String,
    firstDayOfWeek: Int,
    dayByDate: Map<String, DayActivity>,
    planStates: Map<String, DayState>,
    weekCard: @Composable () -> Unit,
    onSelectDate: (String) -> Unit
) {
    val year = CalendarDateKeys.yearOf(selectedDate)
    val month = CalendarDateKeys.monthOf(selectedDate)

    MonthTotalsRow(stats = monthStats(year, month, dayByDate))
    // 5a section 3, between the stat tiles and the grid.
    weekCard()

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CalendarDateLabels.weekdayLetterLabels(firstDayOfWeek).forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = C.SecondaryText)
            )
        }
    }

    val cells = CalendarDateKeys.monthGridCells(year, month, firstDayOfWeek)
    val rows = cells.chunked(7)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { rowCells ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowCells.forEach { dateKey ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (dateKey == null) {
                            Spacer(modifier = Modifier.aspectRatio(1f).height(46.dp))
                        } else {
                            MonthDayCell(
                                dateKey = dateKey,
                                isSelected = dateKey == selectedDate,
                                isToday = dateKey == todayKey,
                                isFuture = dateKey > todayKey,
                                sections = cellSectionsWithPlan(
                                    dayByDate[dateKey]?.cellSections(dateKey, todayKey).orEmpty(),
                                    planStates[dateKey]
                                ),
                                plan = planStates[dateKey],
                                onClick = { onSelectDate(dateKey) }
                            )
                        }
                    }
                }
                repeat(7 - rowCells.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }

    LegendRow()
}

/** 5a stat tiles: Workouts, Diary entries, Meals eaten / planned (the Best streak tile went with 5a). */
@Composable
private fun MonthTotalsRow(stats: MonthStats) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MonthTotalCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            background = C.WorkoutSoft,
            label = "Workouts"
        ) {
            Text(stats.workouts.toString(), style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = C.WorkoutInk))
        }
        MonthTotalCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            background = C.DiarySoft,
            label = "Diary entries"
        ) {
            Text(stats.diaryEntries.toString(), style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = C.DiaryInk))
        }
        MonthTotalCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            background = C.MealSoft,
            label = "Meals eaten / planned"
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = C.MealNumber)) {
                        append(stats.mealsEaten.toString())
                    }
                    withStyle(SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.MealInk)) {
                        append(" / ${stats.mealsPlanned}")
                    }
                }
            )
        }
    }
}

@Composable
private fun MonthTotalCard(modifier: Modifier, background: Color, label: String, number: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .background(background, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        number()
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, style = TextStyle(fontSize = 12.sp, color = C.BodySecondary))
    }
}

@Composable
private fun MonthDayCell(
    dateKey: String,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    sections: List<ActivityType>,
    plan: DayState?,
    onClick: () -> Unit
) {
    val day = dateKey.substringAfterLast("-").toInt()
    // 5a outline (#78): dashed blue on a planned day, dashed grey on a missed one; the ring sits inside it.
    val outline = when (plan) {
        DayState.PLANNED -> C.WorkoutDot
        DayState.MISSED -> C.ChevronWeak
        else -> null
    }

    val borderWidth = when {
        isSelected -> 2.dp
        isToday -> 2.dp
        outline != null -> 0.dp
        isFuture -> 1.dp
        else -> 0.dp
    }
    val targetBorderColor = when {
        isSelected -> C.OnSurface
        isToday -> C.AccentSolid
        isFuture -> C.CardBorder
        else -> Color.Transparent
    }
    val borderColor by animateColorAsState(targetValue = targetBorderColor, animationSpec = tween(150), label = "dayCellBorder")

    // Future numbers stay greyer than past ones but readable (#69): the handoff's muted grey was 2.6:1 on the page
    // and 1.6:1 on a tinted cell. Secondary text on the page (5.3+), body-secondary on a tint (5.9+).
    val textColor = when {
        isFuture && sections.isNotEmpty() -> C.BodySecondary
        isFuture -> C.SecondaryText
        isToday -> C.AccentOnContainer
        else -> C.OnSurface
    }
    val fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal

    val shape = RoundedCornerShape(12.dp)
    // With a dashed outline the today / selected ring is drawn inside it (handover: ring inset 2dp).
    val ringInside = outline != null && borderWidth > 0.dp
    val base = Modifier
        .height(46.dp)
        .fillMaxWidth()
        .clip(shape)
        .clickable(onClick = onClick)
        .then(if (borderWidth > 0.dp && !ringInside) Modifier.border(borderWidth, borderColor, shape) else Modifier)

    // 5a: one diagonal band per section with activity; nothing -> grey in the past, clear from today on.
    val tints = sections.map { section ->
        when (section) {
            ActivityType.WORKOUT -> C.WorkoutTint
            ActivityType.MEAL -> C.MealTint
            ActivityType.DIARY -> C.DiaryTint
        }
    }
    val background = when {
        tints.size > 1 -> Modifier.diagonalBandsBackground(tints, 12.dp)
        tints.size == 1 -> Modifier.background(tints.single(), shape)
        isFuture -> Modifier
        else -> Modifier.background(C.EmptyCell, shape)
    }

    Box(modifier = base.then(background), contentAlignment = Alignment.Center) {
        if (outline != null) {
            // Over the tint, so the bands don't paint over it. The edge clips half the stroke: 3dp shows as 1.5dp.
            Box(Modifier.matchParentSize().dashedOutline(outline, 12.dp, width = 3.dp, dash = 4.dp, gap = 3.dp))
        }
        if (ringInside) {
            Box(Modifier.matchParentSize().padding(3.dp).border(borderWidth, borderColor, RoundedCornerShape(9.dp)))
        }
        Text(text = day.toString(), style = TextStyle(fontSize = 15.sp, fontWeight = fontWeight, color = textColor))
    }
}

/** A planned day (today or later) shows the workout band even before anything is logged (5a fill rule). */
private fun cellSectionsWithPlan(sections: List<ActivityType>, plan: DayState?): List<ActivityType> =
    if (plan == DayState.PLANNED && ActivityType.WORKOUT !in sections) listOf(ActivityType.WORKOUT) + sections else sections

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LegendRow() {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 14.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 5a legend, bottom-nav order, then the training plan's outlines (#78).
        LegendItem(swatch = { Box(Modifier.size(12.dp).background(C.WorkoutTint, RoundedCornerShape(4.dp))) }, label = "Workout")
        LegendItem(swatch = { Box(Modifier.size(12.dp).background(C.MealTint, RoundedCornerShape(4.dp))) }, label = "Meals")
        LegendItem(swatch = { Box(Modifier.size(12.dp).background(C.DiaryTint, RoundedCornerShape(4.dp))) }, label = "Diary")
        LegendItem(swatch = { DashedSwatch(C.WorkoutDot) }, label = "Planned")
        LegendItem(swatch = { DashedSwatch(C.ChevronWeak) }, label = "Missed")
        LegendItem(
            swatch = { Box(Modifier.size(12.dp).border(2.dp, C.AccentSolid, RoundedCornerShape(4.dp))) },
            label = "Today"
        )
    }
    Text(
        text = "Tap a day to see what you logged or change what is planned",
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        textAlign = TextAlign.Center,
        style = TextStyle(fontSize = 13.sp, color = C.Hint)
    )
}

/** A 12dp legend swatch with a short-dash outline (the edge clips half the 3dp stroke: 1.5dp shows). */
@Composable
private fun DashedSwatch(color: Color) {
    Box(Modifier.size(12.dp).dashedOutline(color, 4.dp, width = 3.dp, dash = 3.dp, gap = 2.dp))
}

@Composable
private fun LegendItem(swatch: @Composable () -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        swatch()
        Text(text = label, style = TextStyle(fontSize = 12.sp, color = C.SecondaryText))
    }
}

// endregion

// region Week strip (Week + Day views)

@Composable
private fun WeekStrip(
    weekKeys: List<String>,
    selectedDate: String,
    todayKey: String,
    dayByDate: Map<String, DayActivity>,
    onSelectDate: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weekKeys.forEach { dateKey ->
            WeekStripColumn(
                modifier = Modifier.weight(1f),
                dateKey = dateKey,
                isSelected = dateKey == selectedDate,
                isToday = dateKey == todayKey,
                isFuture = dateKey > todayKey,
                activity = dayByDate[dateKey],
                onClick = { onSelectDate(dateKey) }
            )
        }
    }
}

@Composable
private fun WeekStripColumn(
    modifier: Modifier,
    dateKey: String,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    activity: DayActivity?,
    onClick: () -> Unit
) {
    val day = dateKey.substringAfterLast("-").toInt()
    val circleBackground = if (isSelected) C.AccentSolid else Color.Transparent
    val circleBorder = if (isToday && !isSelected) 1.5.dp else 0.dp
    val numberColor = when {
        isSelected -> C.OnAccentSolid
        isToday -> C.AccentOnContainer
        isFuture -> C.SecondaryText
        else -> C.OnSurface
    }
    val numberWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = CalendarDateLabels.shortWeekdayLabel(dateKey).take(3),
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = C.SecondaryText)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(circleBackground, CircleShape)
                .then(if (circleBorder > 0.dp) Modifier.border(circleBorder, C.AccentSolid, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(text = day.toString(), style = TextStyle(fontSize = 16.sp, fontWeight = numberWeight, color = numberColor))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (activity?.sessions?.isNotEmpty() == true) ActivityDot(C.WorkoutDot)
            if (activity?.diary != null) ActivityDot(C.DiaryDot)
            if (activity?.meals?.isNotEmpty() == true) ActivityDot(C.MealDot)
        }
    }
}

@Composable
private fun ActivityDot(color: Color) {
    Box(modifier = Modifier.size(5.dp).background(color, CircleShape))
}

// endregion

@Composable
private fun SectionDivider() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(C.Divider))
}

// region Week agenda

@Composable
private fun WeekAgendaSection(
    weekKeys: List<String>,
    selectedDate: String,
    todayKey: String,
    dayByDate: Map<String, DayActivity>,
    onSelectRow: (String) -> Unit
) {
    val workoutCount = weekKeys.sumOf { dayByDate[it]?.sessions?.size ?: 0 }
    val diaryCount = weekKeys.count { dayByDate[it]?.diary != null }
    val mealDays = weekKeys.count { dayByDate[it]?.meals?.isNotEmpty() == true }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text("This week", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.BodySecondary))
        Text(weekSummaryLabel(workoutCount, diaryCount, mealDays), style = TextStyle(fontSize = 13.sp, color = C.SecondaryText))
    }

    Column(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weekKeys.forEach { dateKey ->
            AgendaRow(
                dateKey = dateKey,
                isSelected = dateKey == selectedDate,
                isToday = dateKey == todayKey,
                isFuture = dateKey > todayKey,
                activity = dayByDate[dateKey],
                onClick = { onSelectRow(dateKey) }
            )
        }
    }
}

@Composable
private fun AgendaRow(
    dateKey: String,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    activity: DayActivity?,
    onClick: () -> Unit
) {
    val day = dateKey.substringAfterLast("-").toInt()
    val dayNumberColor = when {
        isToday -> C.AccentOnContainer
        isFuture -> C.SecondaryText
        else -> C.OnSurface
    }
    val background = if (isSelected) C.AccentContainer else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                CalendarDateLabels.shortWeekdayLabel(dateKey).take(3),
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = C.SecondaryText)
            )
            Text(day.toString(), style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = dayNumberColor))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val sessions = activity?.sessions.orEmpty()
            val diary = activity?.diary
            val meals = activity?.meals.orEmpty()
            val hasItems = activity?.hasAnything == true

            sessions.forEach { session ->
                AgendaItem(
                    background = C.WorkoutTint,
                    icon = Icons.Filled.FitnessCenter,
                    iconTint = C.WorkoutInk,
                    title = session.templateName,
                    subtitle = "${session.exercises.size} exercises · ${session.exercises.sumOf { it.sets.size }} sets"
                )
            }
            if (diary != null) {
                AgendaItem(
                    background = C.DiaryTint,
                    icon = Icons.Filled.Book,
                    iconTint = C.DiaryInk,
                    title = "Diary · ${diary.mood}",
                    subtitle = "Energy ${diary.energyLevel}/10 · Sleep ${diary.sleepHours} h"
                )
            }
            if (meals.isNotEmpty()) {
                AgendaItem(
                    background = C.MealTint,
                    icon = Icons.Filled.Restaurant,
                    iconTint = C.MealInk,
                    title = mealCountLabel(meals.size),
                    subtitle = kcalLabel(meals.sumOf { it.kcal })
                )
            }
            if (!hasItems) {
                Text(
                    text = if (isFuture) "Upcoming" else "Rest day",
                    style = TextStyle(fontSize = 14.sp, color = C.Hint)
                )
            }
        }

        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = C.ChevronWeak, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AgendaItem(background: Color, icon: ImageVector, iconTint: Color, title: String, subtitle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(32.dp).background(background, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(
                title,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(subtitle, style = TextStyle(fontSize = 12.sp, color = C.SecondaryText))
        }
    }
}

// endregion

// region Day details (Day view) and day sheet (Month, 5a)

@Composable
private fun DayDetailsSection(
    selectedDate: String,
    todayKey: String,
    activity: DayActivity,
    onOpenWorkoutSession: (String) -> Unit,
    onOpenMeals: (String) -> Unit,
    onLogWorkout: () -> Unit,
    onAddDiaryEntry: () -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DayCards(
            dateKey = selectedDate,
            todayKey = todayKey,
            activity = activity,
            onOpenWorkoutSession = onOpenWorkoutSession,
            onOpenMeals = onOpenMeals,
            onLogWorkout = onLogWorkout,
            onAddDiaryEntry = onAddDiaryEntry
        )
    }
}

/** The day's cards, top to bottom as in 5a: workouts done, meals, diary - or the empty state. */
@Composable
private fun DayCards(
    dateKey: String,
    todayKey: String,
    activity: DayActivity,
    onOpenWorkoutSession: (String) -> Unit,
    onOpenMeals: (String) -> Unit,
    onLogWorkout: () -> Unit,
    onAddDiaryEntry: () -> Unit,
    showFutureEmpty: Boolean = true
) {
    activity.sessions.forEach { session ->
        WorkoutCard(session = session, onClick = { onOpenWorkoutSession(session.id) })
    }
    if (activity.meals.isNotEmpty()) {
        MealsCard(
            title = mealsCardTitle(activity.meals.size, activity.mealsEaten, isPast = dateKey < todayKey),
            onClick = { onOpenMeals(dateKey) }
        )
    }
    activity.diary?.let { diary -> DiaryCard(diary) }
    if (!activity.hasAnything && (showFutureEmpty || dateKey <= todayKey)) {
        EmptyDayState(isFuture = dateKey > todayKey, onLogWorkout = onLogWorkout, onAddDiaryEntry = onAddDiaryEntry)
    }
}

/** 5a day sheet: tapping a day in Month. Scrolls inside the sheet; closes on the scrim or the close button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(
    dateKey: String,
    todayKey: String,
    activity: DayActivity,
    planCard: @Composable () -> Unit,
    onDismiss: () -> Unit,
    onOpenWorkoutSession: (String) -> Unit,
    onOpenMeals: (String) -> Unit,
    onLogWorkout: () -> Unit,
    onAddDiaryEntry: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = C.Surface
    ) {
        // 5a: at most 86% of the screen, scrolling inside.
        val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.86f).dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${CalendarDateLabels.dayOfWeekFullLabel(dateKey)}, ${CalendarDateLabels.dayTitle(dateKey)}",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (dateKey == todayKey) {
                        Text(
                            "Today",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.AccentOnContainer),
                            modifier = Modifier
                                .background(C.AccentContainer, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = C.IconTint)
                }
            }
            // 5a card order: Training plan (or Missed) first, then what was logged.
            planCard()
            DayCards(
                dateKey = dateKey,
                todayKey = todayKey,
                activity = activity,
                // The Training plan card covers a future day (rest day -> Plan a workout).
                showFutureEmpty = false,
                onOpenWorkoutSession = onOpenWorkoutSession,
                onOpenMeals = onOpenMeals,
                onLogWorkout = onLogWorkout,
                onAddDiaryEntry = onAddDiaryEntry
            )
        }
    }
}

@Composable
private fun WorkoutCard(session: StrengthSession, onClick: () -> Unit) {
    val totalSets = session.exercises.sumOf { it.sets.size }
    val volumeKg = session.exercises.sumOf { exercise -> exercise.sets.sumOf { it.reps * it.weightKg } }
    val volumeLabel = NumberFormat.getIntegerInstance(Locale.getDefault()).format(volumeKg.roundToLong())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CalendarColors.Card, RoundedCornerShape(16.dp))
            .border(1.dp, C.CardBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).background(C.WorkoutTint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = C.WorkoutInk, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(session.templateName, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface))
                Text(
                    "${session.exercises.size} exercises · $totalSets sets · $volumeLabel kg",
                    style = TextStyle(fontSize = 13.sp, color = C.SecondaryText)
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = C.ChevronStrong, modifier = Modifier.size(20.dp))
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(C.InnerDivider))

        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)) {
            session.exercises.forEach { exercise -> ExerciseRow(exercise) }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: StrengthExercise) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(exercise.name, style = TextStyle(fontSize = 14.sp, color = C.OnSurface))
        Text(exerciseSummaryLabel(exercise.sets), style = TextStyle(fontSize = 14.sp, color = C.SecondaryText))
    }
}

internal fun exerciseSummaryLabel(sets: List<StrengthSet>): String {
    if (sets.isEmpty()) return "0 × 0"
    val reps = sets.map { it.reps }
    val repsLabel = if (reps.distinct().size == 1) reps.first().toString() else "${reps.min()}-${reps.max()}"
    val weightLabel = weightRangeLabel(sets.map { it.weightKg })
    val base = "${sets.size} × $repsLabel"
    return if (weightLabel == null) base else "$base · $weightLabel kg"
}

internal fun weightRangeLabel(weights: List<Double>): String? {
    if (weights.all { it == 0.0 }) return null
    val distinct = weights.distinct()
    return if (distinct.size == 1) formatWeight(distinct.first()) else "${formatWeight(weights.min())}-${formatWeight(weights.max())}"
}

internal fun formatWeight(weightKg: Double): String =
    if (weightKg == weightKg.toLong().toDouble()) weightKg.toLong().toString() else weightKg.toString()

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiaryCard(diary: DiaryEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CalendarColors.Card, RoundedCornerShape(16.dp))
            .border(1.dp, C.CardBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).background(C.DiaryTint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Book, contentDescription = null, tint = C.DiaryInk, modifier = Modifier.size(22.dp))
            }
            Text(
                "Diary entry",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(modifier = Modifier.weight(1f), label = "Mood") {
                Text(diary.mood, style = TextStyle(fontSize = 22.sp))
            }
            StatTile(modifier = Modifier.weight(1f), label = "Energy") {
                Text("${diary.energyLevel}/10", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface))
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(CalendarColors.ProgressTrack, RoundedCornerShape(2.dp))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (diary.energyLevel.coerceIn(0, 10)) / 10f)
                            .height(4.dp)
                            .background(C.DiaryDot, RoundedCornerShape(2.dp))
                    )
                }
            }
            StatTile(modifier = Modifier.weight(1f), label = "Sleep") {
                Text("${diary.sleepHours} h", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface))
            }
        }

        if (diary.sorePartIds.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Sore", style = TextStyle(fontSize = 12.sp, color = C.SecondaryText), modifier = Modifier.padding(end = 2.dp))
                diary.sorePartIds.distinct().forEach { partId ->
                    Box(
                        modifier = Modifier
                            .border(1.dp, C.ChipBorder, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(bodyPartLabels[partId] ?: partId, style = TextStyle(fontSize = 12.sp, color = C.BodySecondary))
                    }
                }
            }
        }

        if (diary.reflection.isNotBlank()) {
            Text(
                diary.reflection,
                style = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, color = C.BodySecondary),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier, label: String, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .background(C.StatTile, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, style = TextStyle(fontSize = 12.sp, color = C.SecondaryText))
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

/** 5a Meals card: "1 of 4 meals eaten" / "3 meals logged"; opens Meals on that date. */
@Composable
private fun MealsCard(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CalendarColors.Card, RoundedCornerShape(16.dp))
            .border(1.dp, C.CardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(C.MealTint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Restaurant, contentDescription = null, tint = C.MealInk, modifier = Modifier.size(22.dp))
        }
        Text(
            title,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface),
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = C.ChevronStrong, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EmptyDayState(isFuture: Boolean, onLogWorkout: () -> Unit, onAddDiaryEntry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(C.DashedBorder, 16.dp)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isFuture) "Nothing planned for this day yet" else "Nothing logged this day",
            style = TextStyle(fontSize = 14.sp, color = C.SecondaryText),
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmptyStateButton(icon = Icons.Filled.FitnessCenter, label = "Log workout", onClick = onLogWorkout)
            EmptyStateButton(icon = Icons.Filled.EditNote, label = "Diary entry", onClick = onAddDiaryEntry)
        }
    }
}

@Composable
private fun EmptyStateButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        border = BorderStroke(1.dp, C.OutlineButton),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CalendarColors.Card),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = C.OnSurface, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.OnSurface))
    }
}

// endregion

// region Drawing helpers

/** Equal diagonal bands from top-left to bottom-right with hard stops, one per colour (5a month cells). */
private fun Modifier.diagonalBandsBackground(colors: List<Color>, cornerRadius: Dp): Modifier =
    this.drawWithCache {
        val stops = colors.flatMapIndexed { index, color ->
            listOf(index.toFloat() / colors.size to color, (index + 1).toFloat() / colors.size to color)
        }
        val brush = Brush.linearGradient(
            colorStops = stops.toTypedArray(),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
        onDrawBehind {
            drawRoundRect(brush = brush, cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()))
        }
    }

private fun Modifier.dashedBorder(color: Color, cornerRadius: Dp): Modifier =
    this
        .clip(RoundedCornerShape(cornerRadius))
        .drawWithCache {
            val stroke = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), 0f)
            )
            onDrawBehind {
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                    style = stroke
                )
            }
        }

// endregion

@Preview(showBackground = true, name = "Month")
@Composable
private fun CalendarScreenMonthPreview() {
    val today = CalendarDateKeys.todayKey()
    OneTheme {
        CalendarScreenContent(
            view = CalendarView.MONTH,
            selectedDate = today,
            dayByDate = mapOf(
                today to DayActivity(
                    sessions = listOf(
                        StrengthSession(
                            templateName = "Push Day",
                            exercises = listOf(StrengthExercise("Bench Press", listOf(StrengthSet(8, 60.0))))
                        )
                    ),
                    diary = DiaryEntry(mood = "💪", energyLevel = 8, sleepHours = 7, reflection = "Good session."),
                    meals = listOf(
                        PlannedMeal(name = "Oats", slot = "BREAKFAST", kcal = 450, eaten = true),
                        PlannedMeal(name = "Chicken rice", slot = "LUNCH", kcal = 700)
                    )
                )
            )
        )
    }
}

@Preview(showBackground = true, name = "Week")
@Composable
private fun CalendarScreenWeekPreview() {
    OneTheme {
        CalendarScreenContent(
            view = CalendarView.WEEK,
            selectedDate = CalendarDateKeys.todayKey(),
            dayByDate = emptyMap()
        )
    }
}

@Preview(showBackground = true, name = "Day - empty")
@Composable
private fun CalendarScreenDayEmptyPreview() {
    OneTheme {
        CalendarScreenContent(
            view = CalendarView.DAY,
            selectedDate = CalendarDateKeys.todayKey(),
            dayByDate = emptyMap()
        )
    }
}
