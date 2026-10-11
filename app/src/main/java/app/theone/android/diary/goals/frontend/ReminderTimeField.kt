package app.theone.android.diary.goals.frontend

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import app.theone.android.diary.goals.backend.parseTime
import app.theone.android.diary.goals.backend.quarterHourOptions
import app.theone.android.ui.theme.DiaryAccent
import app.theone.android.ui.theme.DiaryDeepInk
import app.theone.android.ui.theme.DiaryMenuSurface
import app.theone.android.ui.theme.DiarySelected
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import kotlin.math.roundToInt

/**
 * Reminder time field (Google Calendar style): focusing selects the text and opens a dropdown of
 * 15-minute steps; picking one sets the time. Typed input is parsed on Done, on losing focus, or
 * when tapping outside; an unparseable value quietly reverts. Escape/Back cancels the edit.
 */
@Composable
internal fun ReminderTimeField(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val label = formatReminderTime(hour, minute, is24Hour)
    var text by remember(label) { mutableStateOf(TextFieldValue(label)) }
    var focused by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    // Set when focus is about to drop for a reason that must not commit the typed text.
    var skipCommit by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun commitTyped() {
        val parsed = parseTime(text.text)
        if (parsed != null && parsed != (hour to minute)) {
            onTimeChange(parsed.first, parsed.second)
        } else {
            text = TextFieldValue(label)
        }
    }

    fun closeWithoutCommit() {
        skipCommit = true
        text = TextFieldValue(label)
        expanded = false
        focusManager.clearFocus()
    }

    // Select-all once focus has landed, after the tap has placed its cursor.
    LaunchedEffect(focused) {
        if (focused) text = text.copy(selection = TextRange(0, text.text.length))
    }

    BackHandler(enabled = expanded) { closeWithoutCommit() }

    val active = focused || expanded
    Box(modifier) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = WarmOnSurface,
                fontFeatureSettings = "tnum"
            ),
            cursorBrush = SolidColor(DiaryAccent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                expanded = false
                focusManager.clearFocus()
            }),
            modifier = Modifier
                .size(width = 132.dp, height = 48.dp)
                .semantics { contentDescription = "Reminder time, $label. Double-tap to change" }
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                        closeWithoutCommit()
                        true
                    } else {
                        false
                    }
                }
                .onFocusChanged { state ->
                    if (state.isFocused && !focused) {
                        focused = true
                        expanded = true
                    } else if (!state.isFocused && focused) {
                        focused = false
                        expanded = false
                        if (skipCommit) skipCommit = false else commitTyped()
                    }
                },
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .size(width = 132.dp, height = 48.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(
                            width = if (active) 2.dp else 1.dp,
                            color = if (active) DiaryAccent else WarmButtonOutline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(start = 14.dp, end = 8.dp)
                ) {
                    Box(Modifier.weight(1f)) { innerTextField() }
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = WarmSecondaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )

        TimeOptionsMenu(
            expanded = expanded,
            selectedMinuteOfDay = hour * 60 + minute,
            is24Hour = is24Hour,
            onSelect = { option ->
                skipCommit = true
                expanded = false
                onTimeChange(option / 60, option % 60)
                focusManager.clearFocus()
            },
            // Tapping outside commits whatever was typed, via the focus loss.
            onDismiss = {
                expanded = false
                focusManager.clearFocus()
            }
        )
    }
}

@Composable
private fun TimeOptionsMenu(
    expanded: Boolean,
    selectedMinuteOfDay: Int,
    is24Hour: Boolean,
    onSelect: (minuteOfDay: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    val onStep = selectedMinuteOfDay % 15 == 0
    LaunchedEffect(expanded) {
        if (expanded) {
            val nearest = (selectedMinuteOfDay / 15f).roundToInt().coerceAtMost(quarterHourOptions.lastIndex)
            listState.scrollToItem((nearest - 2).coerceAtLeast(0))
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 4.dp),
        // Not focusable, so the text field keeps focus and the keyboard while the menu is open.
        properties = PopupProperties(focusable = false),
        shape = RoundedCornerShape(12.dp),
        containerColor = DiaryMenuSurface,
        shadowElevation = 6.dp
    ) {
        LazyColumn(state = listState, modifier = Modifier.width(180.dp).height(224.dp)) {
            items(quarterHourOptions) { option ->
                val selected = onStep && option == selectedMinuteOfDay
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(if (selected) DiarySelected else Color.Transparent)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        formatReminderTime(option / 60, option % 60, is24Hour),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) DiaryDeepInk else WarmOnSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = DiaryDeepInk, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
