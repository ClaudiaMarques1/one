package app.theone.android.meals.frontend

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.theone.android.meals.backend.ShoppingGroup
import app.theone.android.meals.backend.ShoppingItem
import app.theone.android.meals.backend.ShoppingListUiState
import app.theone.android.meals.backend.ShoppingListViewModel
import app.theone.android.settings.backend.DataStoreSettingsRepository
import app.theone.android.ui.ErrorBanner
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.MealsColors
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmField
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmMutedIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface
import java.time.LocalDate

// 1e in the meals handoff: the week's shopping from planned recipes, grouped by aisle (#60).

/** A [ShoppingListViewModel] for the week holding [dayInWeek], reading the week start from DataStore. */
@Composable
fun shoppingListViewModel(dayInWeek: String): ShoppingListViewModel = viewModel(
    key = "shopping-$dayInWeek",
    factory = viewModelFactory {
        initializer {
            ShoppingListViewModel(DataStoreSettingsRepository(checkNotNull(this[APPLICATION_KEY])), LocalDate.parse(dayInWeek))
        }
    }
)

@Composable
fun ShoppingListScreen(dayInWeek: String, onBack: () -> Unit, viewModel: ShoppingListViewModel = shoppingListViewModel(dayInWeek)) {
    val list by viewModel.list.collectAsState()
    val error by viewModel.shoppingError.collectAsState()
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        ShoppingListContent(
            list = list,
            onBack = onBack,
            onShare = {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, shoppingShareText(list.groups))
                context.startActivity(Intent.createChooser(send, "Share shopping list"))
            },
            onToggle = viewModel::toggle,
            onAddItem = viewModel::addItem
        )
        ErrorBanner(message = error, onDismiss = viewModel::clearShoppingError, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun ShoppingListContent(
    list: ShoppingListUiState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onToggle: (ShoppingItem) -> Unit,
    onAddItem: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(WarmSurface).imePadding()) {
        Row(modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmOnSurface)
            }
            Text("Shopping list", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = WarmOnSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = onShare, enabled = list.toBuy > 0, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Share, contentDescription = "Share", tint = if (list.toBuy > 0) WarmOnSurface else WarmMutedIcon)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (list.from.isNotEmpty()) {
                item {
                    Text(
                        shoppingSubtitle(LocalDate.parse(list.from), LocalDate.parse(list.to), list.toBuy),
                        fontSize = 13.sp,
                        color = WarmSecondaryText,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            item { AddItemField(onAddItem) }
            if (list.groups.isEmpty()) {
                item {
                    Text(
                        "Nothing to buy yet. Meals added from your recipes put their ingredients here.",
                        fontSize = 14.sp,
                        color = WarmSecondaryText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
            items(list.groups, key = { it.aisle }) { group -> ShoppingGroupCard(group, onToggle) }
        }
    }
}

@Composable
private fun AddItemField(onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val submit = {
        onAdd(text)
        text = ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(WarmField)
            .padding(start = 4.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = submit, enabled = text.isNotBlank(), modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "Add item", tint = WarmIcon, modifier = Modifier.size(22.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            if (text.isEmpty()) Text("Add item", fontSize = 15.sp, color = WarmSecondaryText)
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = WarmOnSurface),
                cursorBrush = SolidColor(MealsColors.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ShoppingGroupCard(group: ShoppingGroup, onToggle: (ShoppingItem) -> Unit) {
    Column {
        Text(
            group.aisle,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = WarmSecondaryText,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 6.dp)
        )
        val shape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(CardSurface)
                .border(1.dp, WarmCardBorder, shape)
        ) {
            group.items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = WarmDivider)
                ShoppingRow(item) { onToggle(item) }
            }
        }
    }
}

@Composable
private fun ShoppingRow(item: ShoppingItem, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = item.checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val box = RoundedCornerShape(6.dp)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(box)
                .background(if (item.checked) MealsColors.Eaten else CardSurface)
                .border(2.dp, if (item.checked) MealsColors.Eaten else WarmButtonOutline, box),
            contentAlignment = Alignment.Center
        ) {
            if (item.checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            item.name,
            fontSize = 15.sp,
            color = if (item.checked) WarmMutedIcon else WarmOnSurface,
            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
        Text(formatQty(item.qty, item.unit), fontSize = 13.sp, color = WarmSecondaryText)
    }
}
