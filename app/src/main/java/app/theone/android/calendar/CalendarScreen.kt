package app.theone.android.calendar

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.theone.android.diary.journal.backend.DiaryEntry
import app.theone.android.diary.journal.frontend.bodyPartLabels
import app.theone.android.ui.ErrorBanner
import app.theone.android.ui.theme.OneTheme
import app.theone.android.workout.StrengthExercise
import app.theone.android.workout.StrengthSession
import app.theone.android.workout.StrengthSet
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

private val C = CalendarColors

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(),
    onNavigateToWorkout: () -> Unit = {},
    onNavigateToDiary: () -> Unit = {}
) {
    val view by viewModel.view.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dayByDate by viewModel.dayByDate.collectAsState()
    val calendarError by viewModel.calendarError.collectAsState()

    CalendarScreenContent(
        view = view,
        selectedDate = selectedDate,
        dayByDate = dayByDate,
        showMeals = viewModel.showMeals,
        errorMessage = calendarError,
        onDismissError = viewModel::clearCalendarError,
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
        // That route doesn't exist yet - see the handoff README's Workout card note.
        onOpenWorkoutSession = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarScreenContent(
    view: CalendarView,
    selectedDate: String,
    dayByDate: Map<String, DayActivity>,
    showMeals: Boolean,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {},
    onViewChange: (CalendarView) -> Unit = {},
    onSelectDate: (String) -> Unit = {},
    onOpenDay: (String) -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onToday: () -> Unit = {},
    onLogWorkout: () -> Unit = {},
    onAddDiaryEntry: () -> Unit = {},
    onOpenWorkoutSession: (String) -> Unit = {}
) {
    val todayKey = CalendarDateUtils.todayKey()
    val firstDayOfWeek = CalendarDateUtils.firstDayOfWeek()
    val weekKeys = CalendarDateUtils.weekDateKeys(selectedDate, firstDayOfWeek)

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
                    onSelectDate = onSelectDate
                )
                CalendarView.WEEK, CalendarView.DAY -> WeekStrip(
                    weekKeys = weekKeys,
                    selectedDate = selectedDate,
                    todayKey = todayKey,
                    dayByDate = dayByDate,
                    showMeals = showMeals,
                    onSelectDate = onSelectDate
                )
            }

            SectionDivider()

            when (view) {
                CalendarView.WEEK -> WeekAgendaSection(
                    weekKeys = weekKeys,
                    selectedDate = selectedDate,
                    todayKey = todayKey,
                    dayByDate = dayByDate,
                    showMeals = showMeals,
                    onSelectRow = { dateKey -> onOpenDay(dateKey) }
                )
                CalendarView.MONTH, CalendarView.DAY -> DayDetailsSection(
                    view = view,
                    selectedDate = selectedDate,
                    todayKey = todayKey,
                    activity = dayByDate[selectedDate] ?: DayActivity(),
                    showMeals = showMeals,
                    onOpenDay = onOpenDay,
                    onOpenWorkoutSession = onOpenWorkoutSession,
                    onLogWorkout = onLogWorkout,
                    onAddDiaryEntry = onAddDiaryEntry
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
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
        CalendarView.MONTH, CalendarView.WEEK -> CalendarDateUtils.monthTitle(selectedDate)
        CalendarView.DAY -> CalendarDateUtils.dayTitle(selectedDate)
    }
    val subtitle = when (view) {
        CalendarView.MONTH -> {
            val year = CalendarDateUtils.yearOf(selectedDate)
            val activeDays = CalendarDateUtils.monthKeysUpToToday(
                CalendarDateUtils.yearOf(selectedDate),
                CalendarDateUtils.monthOf(selectedDate),
                todayKey
            )
            "$year · ${activeDays.size} ${if (activeDays.size == 1) "active day" else "active days"}"
        }
        CalendarView.WEEK -> CalendarDateUtils.weekRangeLabel(
            CalendarDateUtils.weekDateKeys(selectedDate, CalendarDateUtils.firstDayOfWeek())
        )
        CalendarView.DAY -> {
            val base = "${CalendarDateUtils.dayOfWeekFullLabel(selectedDate)} · ${CalendarDateUtils.yearOf(selectedDate)}"
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
            if (selectedDate != todayKey) {
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
    onSelectDate: (String) -> Unit
) {
    val year = CalendarDateUtils.yearOf(selectedDate)
    val month = CalendarDateUtils.monthOf(selectedDate)

    MonthTotalsRow(year = year, month = month, todayKey = todayKey, dayByDate = dayByDate)

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CalendarDateUtils.weekdayLetterLabels(firstDayOfWeek).forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = C.SecondaryText)
            )
        }
    }

    val cells = CalendarDateUtils.monthGridCells(year, month, firstDayOfWeek)
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
                                activity = dayByDate[dateKey],
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

@Composable
private fun MonthTotalsRow(year: Int, month: Int, todayKey: String, dayByDate: Map<String, DayActivity>) {
    val keysUpToToday = CalendarDateUtils.monthKeysUpToToday(year, month, todayKey)
    val workoutDays = keysUpToToday.count { dayByDate[it]?.sessions?.isNotEmpty() == true }
    val diaryDays = keysUpToToday.count { dayByDate[it]?.diary != null }
    val bestStreak = CalendarDateUtils.longestActiveStreak(keysUpToToday) { dayByDate[it]?.isActive == true }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MonthTotalCard(
            modifier = Modifier.weight(1f),
            background = C.WorkoutSoft,
            numberColor = C.WorkoutInk,
            number = workoutDays.toString(),
            label = "Workouts"
        )
        MonthTotalCard(
            modifier = Modifier.weight(1f),
            background = C.DiarySoft,
            numberColor = C.DiaryInk,
            number = diaryDays.toString(),
            label = "Diary entries"
        )
        MonthTotalCard(
            modifier = Modifier.weight(1f),
            background = C.AccentSoft,
            numberColor = C.AccentOnContainer,
            number = if (bestStreak == 1) "1 day" else "$bestStreak days",
            label = "Best streak"
        )
    }
}

@Composable
private fun MonthTotalCard(modifier: Modifier, background: Color, numberColor: Color, number: String, label: String) {
    Box(
        modifier = modifier
            .background(background, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Text(text = number, style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = numberColor))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, style = TextStyle(fontSize = 12.sp, color = C.BodySecondary))
        }
    }
}

@Composable
private fun MonthDayCell(
    dateKey: String,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    activity: DayActivity?,
    onClick: () -> Unit
) {
    val hasWorkout = activity?.sessions?.isNotEmpty() == true
    val hasDiary = activity?.diary != null
    val day = dateKey.substringAfterLast("-").toInt()

    val borderWidth = when {
        isSelected -> 2.dp
        isToday -> 2.dp
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

    val textColor = when {
        isFuture -> C.Muted
        isToday -> C.AccentOnContainer
        else -> C.OnSurface
    }
    val fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal

    val shape = RoundedCornerShape(12.dp)
    val base = Modifier
        .height(46.dp)
        .fillMaxWidth()
        .clip(shape)
        .clickable(onClick = onClick)
        .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape) else Modifier)

    val background = when {
        isFuture -> Modifier
        hasWorkout && hasDiary -> Modifier.diagonalSplitBackground(C.WorkoutTint, C.DiaryTint, 12.dp)
        hasWorkout -> Modifier.background(C.WorkoutTint, shape)
        hasDiary -> Modifier.background(C.DiaryTint, shape)
        else -> Modifier.background(C.EmptyCell, shape)
    }

    Box(modifier = base.then(background), contentAlignment = Alignment.Center) {
        Text(text = day.toString(), style = TextStyle(fontSize = 15.sp, fontWeight = fontWeight, color = textColor))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LegendRow() {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 14.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LegendItem(swatch = { Box(Modifier.size(12.dp).background(C.WorkoutTint, RoundedCornerShape(4.dp))) }, label = "Workout")
        LegendItem(swatch = { Box(Modifier.size(12.dp).background(C.DiaryTint, RoundedCornerShape(4.dp))) }, label = "Diary")
        LegendItem(
            swatch = { Box(Modifier.size(12.dp).diagonalSplitBackground(C.WorkoutTint, C.DiaryTint, 4.dp)) },
            label = "Both"
        )
        LegendItem(
            swatch = { Box(Modifier.size(12.dp).border(2.dp, C.AccentSolid, RoundedCornerShape(4.dp))) },
            label = "Today"
        )
        LegendItem(
            swatch = { Box(Modifier.size(12.dp).border(2.dp, C.OnSurface, RoundedCornerShape(4.dp))) },
            label = "Selected"
        )
    }
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
    showMeals: Boolean,
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
                showMeals = showMeals,
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
    showMeals: Boolean,
    onClick: () -> Unit
) {
    val day = dateKey.substringAfterLast("-").toInt()
    val circleBackground = if (isSelected) C.AccentSolid else Color.Transparent
    val circleBorder = if (isToday && !isSelected) 1.5.dp else 0.dp
    val numberColor = when {
        isSelected -> C.OnAccentSolid
        isToday -> C.AccentOnContainer
        isFuture -> C.Muted
        else -> C.OnSurface
    }
    val numberWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = CalendarDateUtils.shortWeekdayLabel(dateKey).take(3),
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
            if (showMeals && (activity?.mealCount ?: 0) > 0) ActivityDot(C.MealDot)
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
    showMeals: Boolean,
    onSelectRow: (String) -> Unit
) {
    val workoutCount = weekKeys.sumOf { dayByDate[it]?.sessions?.size ?: 0 }
    val diaryCount = weekKeys.count { dayByDate[it]?.diary != null }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text("This week", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.BodySecondary))
        Text("$workoutCount workouts · $diaryCount diary", style = TextStyle(fontSize = 13.sp, color = C.SecondaryText))
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
                showMeals = showMeals,
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
    showMeals: Boolean,
    onClick: () -> Unit
) {
    val day = dateKey.substringAfterLast("-").toInt()
    val dayNumberColor = when {
        isToday -> C.AccentOnContainer
        isFuture -> C.Muted
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
                CalendarDateUtils.shortWeekdayLabel(dateKey).take(3),
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = C.SecondaryText)
            )
            Text(day.toString(), style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = dayNumberColor))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val sessions = activity?.sessions.orEmpty()
            val diary = activity?.diary
            val mealCount = activity?.mealCount ?: 0
            val hasItems = sessions.isNotEmpty() || diary != null || (showMeals && mealCount > 0)

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
            if (showMeals && mealCount > 0) {
                AgendaItem(
                    background = C.MealTint,
                    icon = Icons.Filled.Restaurant,
                    iconTint = C.MealInk,
                    title = "$mealCount meal(s)",
                    subtitle = "Meals"
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

// region Day details (Month + Day views)

@Composable
private fun DayDetailsSection(
    view: CalendarView,
    selectedDate: String,
    todayKey: String,
    activity: DayActivity,
    showMeals: Boolean,
    onOpenDay: (String) -> Unit,
    onOpenWorkoutSession: (String) -> Unit,
    onLogWorkout: () -> Unit,
    onAddDiaryEntry: () -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (view == CalendarView.MONTH) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${CalendarDateUtils.dayOfWeekFullLabel(selectedDate)}, ${CalendarDateUtils.dayTitle(selectedDate)}",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface)
                )
                TextButton(onClick = { onOpenDay(selectedDate) }) {
                    Text("Open day", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.AccentOnContainer))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = C.AccentOnContainer, modifier = Modifier.size(18.dp))
                }
            }
        }

        val isFuture = selectedDate > todayKey
        val mealCount = activity.mealCount
        val hasAnything = activity.sessions.isNotEmpty() || activity.diary != null || (showMeals && mealCount > 0)

        activity.sessions.forEach { session ->
            WorkoutCard(session = session, onClick = { onOpenWorkoutSession(session.id) })
        }
        activity.diary?.let { diary -> DiaryCard(diary) }
        if (showMeals && mealCount > 0) {
            MealsRow(mealCount)
        }
        if (!hasAnything) {
            EmptyDayState(isFuture = isFuture, onLogWorkout = onLogWorkout, onAddDiaryEntry = onAddDiaryEntry)
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
            .background(Color.White, RoundedCornerShape(16.dp))
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
            .background(Color.White, RoundedCornerShape(16.dp))
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
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0xFFE6DFD8), RoundedCornerShape(2.dp))) {
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

@Composable
private fun MealsRow(mealCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, C.CardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(C.MealTint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Restaurant, contentDescription = null, tint = C.MealInk, modifier = Modifier.size(22.dp))
        }
        Text("$mealCount meal(s) logged", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.OnSurface))
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
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = C.OnSurface, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.OnSurface))
    }
}

// endregion

// region Drawing helpers

private fun Modifier.diagonalSplitBackground(topLeft: Color, bottomRight: Color, cornerRadius: Dp): Modifier =
    this.drawWithCache {
        val brush = Brush.linearGradient(
            colorStops = arrayOf(0f to topLeft, 0.5f to topLeft, 0.5f to bottomRight, 1f to bottomRight),
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
    val today = CalendarDateUtils.todayKey()
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
                    diary = DiaryEntry(mood = "💪", energyLevel = 8, sleepHours = 7, reflection = "Good session.")
                )
            ),
            showMeals = false
        )
    }
}

@Preview(showBackground = true, name = "Week")
@Composable
private fun CalendarScreenWeekPreview() {
    OneTheme {
        CalendarScreenContent(
            view = CalendarView.WEEK,
            selectedDate = CalendarDateUtils.todayKey(),
            dayByDate = emptyMap(),
            showMeals = false
        )
    }
}

@Preview(showBackground = true, name = "Day - empty")
@Composable
private fun CalendarScreenDayEmptyPreview() {
    OneTheme {
        CalendarScreenContent(
            view = CalendarView.DAY,
            selectedDate = CalendarDateUtils.todayKey(),
            dayByDate = emptyMap(),
            showMeals = false
        )
    }
}
