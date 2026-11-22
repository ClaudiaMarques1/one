package app.theone.android.settings.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import app.theone.android.ui.theme.CardSurface
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

// Settings -> Meals. Not in a design mock (the meals handoff points to an unexported Home 1d), so it
// reuses this screen's own card + row style, as agreed on #53.

private val CardShape = RoundedCornerShape(20.dp)
private const val MIN_WATER_GLASSES = 4

@Composable
internal fun MealSettingsSection(settings: MealSettings, onChange: (MealSettings) -> Unit) {
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
                    }
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
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
