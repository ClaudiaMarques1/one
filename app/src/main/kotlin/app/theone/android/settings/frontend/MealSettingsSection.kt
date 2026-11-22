package app.theone.android.settings.frontend

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.meals.backend.MAX_WATER_GLASSES
import app.theone.android.meals.backend.MealSettings
import app.theone.android.meals.backend.MealSlot
import app.theone.android.meals.backend.MealTargets
import app.theone.android.settings.backend.WeekStart
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.NeutralSelected
import app.theone.android.ui.theme.NeutralSwitchOff
import app.theone.android.ui.theme.NeutralSwitchOnThumb
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDisabledFill
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import java.math.BigDecimal
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Settings -> Meals. Not in a design mock (the meals handoff points to an unexported Home 1d), so it
// reuses this screen's own card + row style, as agreed on #53.

private val CardShape = RoundedCornerShape(20.dp)
private const val MIN_WATER_GLASSES = 4

@Composable
internal fun MealSettingsSection(settings: MealSettings, onChange: (MealSettings) -> Unit) {
    var editingTimeOf by remember { mutableStateOf<MealSlot?>(null) }
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsCard {
            MealSlot.entries.forEachIndexed { index, slot ->
                if (index > 0) HorizontalDivider(color = WarmDivider)
                val on = slot in settings.enabledSlots
                SwitchRow(
                    title = slot.label,
                    subtitle = if (slot.optional) "Training days only" else null,
                    checked = on,
                    onCheckedChange = { checked ->
                        val slots = if (checked) settings.enabledSlots + slot else settings.enabledSlots - slot
                        onChange(settings.copy(enabledSlots = slots))
                    },
                    // The slot's time orders the Timeline view.
                    time = formatSlotTime(settings.timeOf(slot), is24Hour),
                    onTimeClick = { editingTimeOf = slot }
                )
            }
        }
        SettingsCard {
            TargetsBlock(
                title = "Training day targets",
                targets = settings.trainingTargets,
                onChange = { onChange(settings.copy(trainingTargets = it)) }
            )
            HorizontalDivider(color = WarmDivider)
            TargetsBlock(
                title = "Rest day targets",
                targets = settings.restTargets,
                onChange = { onChange(settings.copy(restTargets = it)) }
            )
        }
        SettingsCard {
            WaterRow(
                glasses = settings.waterGlasses,
                onChange = { onChange(settings.copy(waterGlasses = it)) }
            )
        }
    }
    editingTimeOf?.let { slot ->
        SlotTimeDialog(
            slotLabel = slot.label,
            minutes = settings.timeOf(slot),
            is24Hour = is24Hour,
            onConfirm = { minutes ->
                onChange(settings.copy(slotTimes = settings.slotTimes + (slot to minutes)))
                editingTimeOf = null
            },
            onDismiss = { editingTimeOf = null }
        )
    }
}

/** "08:00" / "8:00 AM" - local to Settings, as features don't share frontend helpers. */
private fun formatSlotTime(minutes: Int, is24Hour: Boolean): String =
    LocalTime.of(minutes / 60, minutes % 60)
        .format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault()))

/** A title with a two-way segmented choice below it, in this screen's outline style. */
@Composable
private fun <T> ChoiceRow(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 16.sp, color = WarmOnSurface)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(shape)
                .border(1.dp, WarmButtonOutline, shape)
                .selectableGroup()
        ) {
            options.forEachIndexed { index, (value, label) ->
                if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(WarmButtonOutline))
                val isSelected = value == selected
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isSelected) NeutralSelected else CardSurface)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) }),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = WarmOnSurface, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = WarmOnSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotTimeDialog(slotLabel: String, minutes: Int, is24Hour: Boolean, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$slotLabel time") },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    selectorColor = WarmOnSurface,
                    timeSelectorSelectedContainerColor = NeutralSelected,
                    periodSelectorSelectedContainerColor = NeutralSelected
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK", color = WarmOnSurface) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = WarmOnSurface) }
        },
        containerColor = CardSurface
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CardSurface)
            .border(1.dp, WarmCardBorder, CardShape)
    ) { content() }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    time: String? = null,
    onTimeClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = WarmOnSurface)
            subtitle?.let { Text(it, fontSize = 13.sp, color = WarmSecondaryText) }
        }
        if (time != null) {
            TextButton(onClick = onTimeClick, modifier = Modifier.padding(end = 8.dp)) {
                Text(time, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = WarmOnSurface)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = WarmOnSurface,
                checkedThumbColor = NeutralSwitchOnThumb,
                checkedBorderColor = WarmOnSurface,
                uncheckedTrackColor = WarmDisabledFill,
                uncheckedThumbColor = NeutralSwitchOff,
                uncheckedBorderColor = NeutralSwitchOff
            )
        )
    }
}

@Composable
private fun TargetsBlock(title: String, targets: MealTargets, onChange: (MealTargets) -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontSize = 16.sp, color = WarmOnSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TargetField("Calories", "kcal", targets.kcal, { onChange(targets.copy(kcal = it)) }, Modifier.weight(1f))
            TargetField("Protein", "g", targets.proteinG, { onChange(targets.copy(proteinG = it)) }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TargetField("Carbs", "g", targets.carbsG, { onChange(targets.copy(carbsG = it)) }, Modifier.weight(1f))
            TargetField("Fat", "g", targets.fatG, { onChange(targets.copy(fatG = it)) }, Modifier.weight(1f))
        }
    }
}

/** Saves each valid whole number as it's typed; blank or invalid text shows an error and isn't saved. */
@Composable
private fun TargetField(label: String, suffix: String, value: Int, onValue: (Int) -> Unit, modifier: Modifier) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    val parsed = text.trim().toIntOrNull()?.takeIf { it > 0 }
    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            newText.trim().toIntOrNull()?.takeIf { it > 0 && it != value }?.let(onValue)
        },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        isError = parsed == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = WarmButtonOutline,
            focusedBorderColor = WarmOnSurface,
            focusedLabelColor = WarmOnSurface,
            cursorColor = WarmOnSurface
        ),
        modifier = modifier
    )
}

@Composable
private fun WaterRow(glasses: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Daily water", fontSize = 16.sp, color = WarmOnSurface)
            Text("0.25 L per glass", fontSize = 13.sp, color = WarmSecondaryText)
        }
        StepButton(Icons.Filled.Remove, "Less water", enabled = glasses > MIN_WATER_GLASSES) { onChange(glasses - 1) }
        Text(
            "${BigDecimal(glasses).multiply(BigDecimal("0.25")).stripTrailingZeros().toPlainString()} L",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = WarmOnSurface,
            textAlign = TextAlign.Center,
            style = TextStyle(fontFeatureSettings = "tnum"),
            modifier = Modifier.widthIn(min = 64.dp)
        )
        StepButton(Icons.Filled.Add, "More water", enabled = glasses < MAX_WATER_GLASSES) { onChange(glasses + 1) }
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .border(1.dp, WarmButtonOutline, CircleShape)
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) WarmIcon else WarmButtonOutline)
    }
}

/** Settings -> General -> Week starts on: the first day of every week in the app (#62). */
@Composable
internal fun WeekStartCard(weekStart: WeekStart, onChange: (WeekStart) -> Unit) {
    SettingsCard {
        ChoiceRow(
            title = "Week starts on",
            options = listOf(WeekStart.MONDAY to "Monday", WeekStart.SUNDAY to "Sunday"),
            selected = weekStart,
            onSelect = onChange
        )
    }
}
