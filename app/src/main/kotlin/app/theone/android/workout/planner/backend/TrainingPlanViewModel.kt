package app.theone.android.workout.planner.backend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.theone.android.settings.backend.SettingsRepository
import app.theone.android.workout.sessions.backend.FirestoreStrengthRepository
import app.theone.android.workout.sessions.backend.StrengthRepository
import app.theone.android.workout.templates.backend.FirestoreWorkoutTemplateRepository
import app.theone.android.workout.templates.backend.WorkoutTemplate
import app.theone.android.workout.templates.backend.WorkoutTemplateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Calendar's "Training this week" card (5a section 3). */
data class TrainingWeekUi(
    val loaded: Boolean = false,
    val hasRoutine: Boolean = false,
    val week: TrainingWeek = TrainingWeek(),
    val note: String = "",
    val routineLine: String = "",
    val expanded: Boolean = false
)

/** The open 4a sheet: the routine being edited (not saved until Save plan) and what the sheet shows about it. */
data class PlanSheetUi(
    val draft: TrainingRoutine,
    val templates: List<WorkoutTemplate>,
    val weekDays: List<DayOfWeek>,
    val nextUp: String,
    val targetNote: String,
    /** The routine has fewer days than the target - the note shows in the accent colour. */
    val targetShort: Boolean,
    val saving: Boolean = false,
    val tab: PlanTab = PlanTab.ROUTINE,
    val upcoming: List<UpcomingRow> = emptyList()
) {
    val canAddSplit: Boolean get() = draft.splitOrder.size < MAX_SPLIT && templates.isNotEmpty()
}

const val MAX_SPLIT = 6

/** What the plan looks like right now, from the stored routine versions, overrides, templates and logged workouts. */
private data class PlanData(
    val routine: TrainingRoutine?,
    val routines: List<TrainingRoutine>,
    val overrides: List<DayOverride>,
    val templates: List<WorkoutTemplate>,
    val loggedDates: Set<LocalDate>,
    val firstDayOfWeek: DayOfWeek
) {
    val names: Map<String, String> = templates.associate { it.id to it.name }

    fun sessions(from: LocalDate, to: LocalDate): List<PlannedSession> =
        TrainingSchedule.build(routines, overrides, from, to, templates.firstOrNull()?.id.orEmpty(), names.keys)

    fun routineOn(date: LocalDate): TrainingRoutine? = routines.lastOrNull { it.effectiveFrom <= date.toString() }
}

enum class PlanTab { ROUTINE, UPCOMING }

/**
 * The training plan for Calendar's week card and the 4a Training plan sheet (#76). Built through
 * `trainingPlanViewModel()` in workout/planner/frontend, which passes the DataStore settings.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrainingPlanViewModel(
    private val settingsRepository: SettingsRepository,
    private val repository: TrainingPlanRepository = FirestoreTrainingPlanRepository(),
    private val templateRepository: WorkoutTemplateRepository = FirestoreWorkoutTemplateRepository(),
    // Done = a workout logged that day, the same source Calendar uses.
    private val strengthRepository: StrengthRepository = FirestoreStrengthRepository(),
    private val today: LocalDate = LocalDate.now()
) : ViewModel() {
    private val _planError = MutableStateFlow<String?>(null)
    val planError: StateFlow<String?> = _planError

    // The month Calendar shows (#78): its past days need their overrides too (skipped vs missed).
    private val visibleMonth = MutableStateFlow(YearMonth.from(today))

    private val plan = combine(
        repository.observeRoutines(),
        // Moves stay within a week, so overrides from a week before the range can still land in it.
        visibleMonth
            .map { minOf(it.atDay(1), today).minusDays(7).toString() }
            .distinctUntilChanged()
            .flatMapLatest { repository.observeOverrides(it) },
        templateRepository.observeTemplates(),
        strengthRepository.observeSessions().map {
                list ->
            list.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
        },
        settingsRepository.observeWeekStart()
    ) { routines, overrides, templates, logged, weekStart ->
        PlanData(
            routine = routines.lastOrNull { it.effectiveFrom <= today.toString() },
            routines = routines,
            overrides = overrides,
            templates = templates.sortedBy { it.name.lowercase() },
            loggedDates = logged,
            firstDayOfWeek = weekStart.dayOfWeek
        )
    }.catch { e -> _planError.value = e.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val week: StateFlow<TrainingWeekUi> = combine(plan, settingsRepository.observeTrainingWeekExpanded()) { data, expanded ->
        val routine = data?.routine
        if (data == null || routine == null) {
            TrainingWeekUi(loaded = data != null, expanded = expanded)
        } else {
            val start = today.with(TemporalAdjusters.previousOrSame(data.firstDayOfWeek))
            val week = TrainingPlanText.week(
                data.sessions(start, start.plusDays(6)),
                data.loggedDates,
                today,
                data.firstDayOfWeek,
                routine.weeklyTarget
            )
            TrainingWeekUi(
                loaded = true,
                hasRoutine = true,
                week = week,
                note = TrainingPlanText.weekNote(week),
                routineLine = TrainingPlanText.routineLine(routine, data.names, data.firstDayOfWeek),
                expanded = expanded
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrainingWeekUi())

    /** Planned / missed / skipped days of the visible month, keyed `yyyy-MM-dd` like Calendar's cells (5a grid). */
    val monthStates: StateFlow<Map<String, DayState>> = combine(plan, visibleMonth) { data, month ->
        if (data == null || data.routines.isEmpty()) return@combine emptyMap()
        val sessions = data.sessions(month.atDay(1), month.atEndOfMonth())
        (1..month.lengthOfMonth()).map { month.atDay(it) }.mapNotNull { date ->
            val state = TrainingSchedule.dayState(date, today, sessions.filter { it.date == date }, date in data.loggedDates)
            if (state == DayState.PLANNED || state == DayState.MISSED || state == DayState.SKIPPED) date.toString() to state else null
        }.toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // The day open in the editor - the 5a day sheet's date, or the expanded 4a Upcoming row.
    private val editorDate = MutableStateFlow<LocalDate?>(null)

    val dayEditor: StateFlow<DayEditorUi?> = combine(editorDate, plan) { date, data ->
        if (date == null || data == null) return@combine null
        // The week (moves) plus two weeks after (skip note's next-up).
        val from = date.minusDays(7)
        DayEditor.build(
            date = date,
            today = today,
            sessions = data.sessions(from, date.plusDays(UPCOMING_DAYS + 7)),
            overrides = data.overrides,
            names = data.names,
            loggedDates = data.loggedDates,
            firstDayOfWeek = data.firstDayOfWeek,
            isSplit = data.routineOn(date)?.mode == PlanMode.SPLIT
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Templates for the editor's workout chips, by name. */
    val templates: StateFlow<List<WorkoutTemplate>> = plan.map { it?.templates.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val draft = MutableStateFlow<TrainingRoutine?>(null)
    private val saving = MutableStateFlow(false)
    private val tab = MutableStateFlow(PlanTab.ROUTINE)

    val sheet: StateFlow<PlanSheetUi?> = combine(draft, plan, saving, tab) { routine, data, isSaving, openTab ->
        if (routine == null || data == null) return@combine null
        val ids = data.names.keys
        val days = TrainingPlanText.trainingDays(routine, ids)
        val coming = data.sessions(today, today.plusDays(UPCOMING_DAYS))
        val next = coming.firstOrNull { !it.skipped && it.date !in data.loggedDates }
        PlanSheetUi(
            tab = openTab,
            upcoming = DayEditor.upcoming(coming, data.names, today, UPCOMING_DAYS + 1),
            draft = routine,
            templates = data.templates,
            weekDays = TrainingPlanText.weekDays(data.firstDayOfWeek),
            nextUp = TrainingPlanText.nextUpLine(next, data.names),
            targetNote = TrainingPlanText.targetNote(days, routine.weeklyTarget),
            targetShort = days < routine.weeklyTarget,
            saving = isSaving
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleWeekExpanded() {
        viewModelScope.launch {
            runCatching { settingsRepository.setTrainingWeekExpanded(!week.value.expanded) }
                .onFailure { _planError.value = it.message }
        }
    }

    /** Opens 4a on the current routine, or a new weekday routine with nothing planned yet. */
    fun openSheet() {
        tab.value = PlanTab.ROUTINE
        editorDate.value = null
        draft.value = plan.value?.routine ?: TrainingRoutine()
    }

    /** Closes 4a without saving (scrim tap, back). Day changes made in Upcoming are already saved. */
    fun closeSheet() {
        draft.value = null
        editorDate.value = null
    }

    fun setTab(newTab: PlanTab) {
        tab.value = newTab
        editorDate.value = null
    }

    // region Day editor (#78) - one-off changes, saved straight away as overrides on the session's original date.

    /** Calendar tells the planner which month it shows, so that month's day states and overrides load. */
    fun showMonth(month: YearMonth) {
        visibleMonth.value = month
    }

    /** Opens [date] in the editor (5a day sheet), or toggles it in 4a Upcoming. */
    fun openDay(date: LocalDate) {
        editorDate.value = date
    }

    fun toggleUpcomingDay(date: LocalDate) {
        editorDate.value = if (editorDate.value == date) null else date
    }

    fun closeDay() {
        editorDate.value = null
    }

    fun setDayTemplate(session: PlannedSession, templateId: String) = changeDay(session.sourceDate) { it.copy(templateId = templateId) }

    fun stepDayTime(session: PlannedSession, minutes: Int) = setDayTime(session, DayEditor.stepTime(session.time, minutes))

    fun setDayTime(session: PlannedSession, time: LocalTime) =
        changeDay(session.sourceDate) { it.copy(time = DayEditor.stepTime(time, 0).toString()) }

    /** Moves the session to [to] in the same week; moving it back to its own day clears the move. The editor follows. */
    fun moveDay(session: PlannedSession, to: LocalDate) {
        changeDay(session.sourceDate) { it.copy(moveTo = to.takeIf { day -> day != session.sourceDate }?.toString()) }
        editorDate.value = to
    }

    fun skipDay(session: PlannedSession) = changeDay(session.sourceDate) { it.copy(skipped = true) }

    fun undoSkip(session: PlannedSession) = changeDay(session.sourceDate) { it.copy(skipped = false) }

    /** Reset (or Remove for an added workout): drops the day's change; the editor goes back to the original day. */
    fun resetDay(session: PlannedSession) {
        editorDate.value = session.sourceDate
        viewModelScope.launch {
            runCatching { repository.deleteOverride(session.sourceDate.toString()) }.onFailure { _planError.value = it.message }
        }
    }

    /** "Plan a workout" on a rest day: the first template at 18:00 until changed. */
    fun planWorkout(date: LocalDate) = changeDay(date) { it.copy(added = true) }

    private fun changeDay(sourceDate: LocalDate, change: (DayOverride) -> DayOverride) {
        val key = sourceDate.toString()
        val existing = plan.value?.overrides?.firstOrNull { it.sourceDate == key } ?: DayOverride(sourceDate = key)
        val updated = change(existing)
        val nothingLeft = updated.templateId == null && updated.time == null && updated.moveTo == null && !updated.skipped && !updated.added
        viewModelScope.launch {
            runCatching {
                if (nothingLeft) repository.deleteOverride(key) else repository.saveOverride(updated)
            }.onFailure { _planError.value = it.message }
        }
    }

    // endregion

    fun setMode(mode: PlanMode) = edit { r ->
        if (mode == PlanMode.SPLIT && r.splitOrder.isEmpty()) {
            // Start the split from the weekday plan, so switching over doesn't begin from nothing.
            val days = TrainingPlanText.weekDays(DayOfWeek.MONDAY).filter { it.name in r.weekdayPlan }
            val order = days.mapNotNull { r.weekdayPlan[it.name]?.templateId }.distinct().take(MAX_SPLIT)
            r.copy(
                mode = mode,
                splitDays = r.splitDays.ifEmpty { days.map { it.name } },
                splitOrder = order.ifEmpty { listOfNotNull(templateIds().firstOrNull()) }
            )
        } else {
            r.copy(mode = mode)
        }
    }

    /** Weekday mode: [templateId] null makes [day] a rest day. */
    fun setWeekdayTemplate(day: DayOfWeek, templateId: String?) = edit { r ->
        val plan = r.weekdayPlan.toMutableMap()
        if (templateId == null) {
            plan.remove(day.name)
        } else {
            plan[day.name] = WeekdaySlot(templateId, r.weekdayPlan[day.name]?.time ?: DEFAULT_TRAINING_TIME)
        }
        r.copy(weekdayPlan = plan)
    }

    fun setWeekdayTime(day: DayOfWeek, time: LocalTime) = edit { r ->
        val slot = r.weekdayPlan[day.name] ?: return@edit r
        r.copy(weekdayPlan = r.weekdayPlan + (day.name to slot.copy(time = time.toString())))
    }

    fun toggleSplitDay(day: DayOfWeek) = edit { r ->
        r.copy(splitDays = if (day.name in r.splitDays) r.splitDays - day.name else r.splitDays + day.name)
    }

    fun setSplitTemplate(index: Int, templateId: String) = edit { r ->
        if (index !in r.splitOrder.indices) r else r.copy(splitOrder = r.splitOrder.toMutableList().also { it[index] = templateId })
    }

    fun moveSplitUp(index: Int) = edit { r ->
        if (index !in 1..r.splitOrder.lastIndex) {
            r
        } else {
            val order = r.splitOrder.toMutableList()
            order[index - 1] = r.splitOrder[index]
            order[index] = r.splitOrder[index - 1]
            r.copy(splitOrder = order)
        }
    }

    fun removeSplit(index: Int) = edit { r ->
        if (r.splitOrder.size < 2 || index !in r.splitOrder.indices) {
            r
        } else {
            r.copy(
                splitOrder = r.splitOrder.filterIndexed {
                        i,
                        _ ->
                    i != index
                }
            )
        }
    }

    /** Adds the template after the last one in the list (wrapping), up to [MAX_SPLIT]. */
    fun addSplit() = edit { r ->
        val ids = templateIds()
        if (r.splitOrder.size >= MAX_SPLIT || ids.isEmpty()) return@edit r
        val after = ids.indexOf(r.splitOrder.lastOrNull())
        r.copy(splitOrder = r.splitOrder + ids[(after + 1) % ids.size])
    }

    fun setSplitTime(time: LocalTime) = edit { it.copy(splitTime = time.toString()) }

    fun changeTarget(delta: Int) = edit { it.copy(weeklyTarget = (it.weeklyTarget + delta).coerceIn(1, 7)) }

    /**
     * Save plan: a new routine version from today (same-day saves overwrite), then [onSaved]. A changed split
     * starts counting from today; an unchanged one keeps its anchor so the order carries on.
     */
    fun save(onSaved: () -> Unit) {
        val edited = draft.value ?: return
        if (edited.mode == PlanMode.SPLIT && (edited.splitDays.isEmpty() || edited.splitOrder.isEmpty())) {
            _planError.value = "Pick at least one training day and one workout for the split."
            return
        }
        val previous = plan.value?.routine?.takeIf {
            it.mode == PlanMode.SPLIT && it.splitDays.toSet() == edited.splitDays.toSet() && it.splitOrder == edited.splitOrder
        }
        val routine = edited.copy(
            effectiveFrom = today.toString(),
            splitAnchor = when {
                edited.mode != PlanMode.SPLIT -> ""
                previous != null -> previous.splitAnchor.ifBlank { previous.effectiveFrom }
                else -> today.toString()
            }
        )
        viewModelScope.launch {
            saving.value = true
            runCatching { repository.saveRoutine(routine) }
                .onSuccess {
                    draft.value = null
                    onSaved()
                }
                .onFailure { _planError.value = it.message }
            saving.value = false
        }
    }

    fun clearPlanError() {
        _planError.value = null
    }

    private fun templateIds(): List<String> = plan.value?.templates?.map { it.id }.orEmpty()

    private fun edit(change: (TrainingRoutine) -> TrainingRoutine) {
        draft.value = draft.value?.let(change)
    }

    private companion object {
        /** 4a's "Next:" looks two weeks ahead, like the Upcoming tab. */
        const val UPCOMING_DAYS = 13L
    }
}
