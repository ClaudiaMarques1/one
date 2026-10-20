package app.theone.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.theone.android.ui.theme.DeleteRed
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmOnSurface

/**
 * The overflow menu for a card's Edit / Delete actions: Edit in black, Delete in red, each with
 * its icon. Pass no [onEdit] for a Delete-only menu. Closes itself (via [onDismiss]) before
 * running the chosen action.
 */
@Composable
fun EditDeleteMenu(expanded: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit, onEdit: (() -> Unit)? = null) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (onEdit != null) {
            DropdownMenuItem(
                text = { Text("Edit", color = WarmOnSurface) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = WarmIcon) },
                onClick = {
                    onDismiss()
                    onEdit()
                }
            )
        }
        DropdownMenuItem(
            text = { Text("Delete", color = DeleteRed) },
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = DeleteRed) },
            onClick = {
                onDismiss()
                onDelete()
            }
        )
    }
}
