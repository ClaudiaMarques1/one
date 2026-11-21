package app.theone.android.dashboard.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.auth.backend.UserProfile
import app.theone.android.ui.ProfileAvatar
import app.theone.android.ui.profileInitials
import app.theone.android.ui.theme.NeutralTheme
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmIcon
import app.theone.android.ui.theme.WarmMutedIcon
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmPressedRow
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface

// 1b in the home handoff: the sheet inside OneNavHost's ModalNavigationDrawer.

@Composable
fun ProfileDrawerSheet(profile: UserProfile?, onAccountDetails: () -> Unit, onSettings: () -> Unit) {
    ModalDrawerSheet(
        modifier = Modifier.width(320.dp),
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        drawerContainerColor = WarmSurface,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, top = 56.dp, end = 12.dp, bottom = 24.dp)) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileAvatar(
                    photoUrl = profile?.photoUrl,
                    initials = profileInitials(profile?.name.orEmpty(), profile?.email.orEmpty()),
                    size = 72.dp,
                    initialsSize = 26.sp
                )
                Column {
                    if (!profile?.name.isNullOrBlank()) {
                        Text(
                            profile?.name.orEmpty(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        profile?.email.orEmpty(),
                        fontSize = 14.sp,
                        color = WarmSecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(color = WarmCardBorder, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
            DrawerItem(icon = Icons.Outlined.Person, label = "Account details", onClick = onAccountDetails)
            DrawerItem(icon = Icons.Outlined.Settings, label = "Settings", onClick = onSettings)
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (pressed) WarmPressedRow else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(PaddingValues(horizontal = 16.dp)),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = WarmIcon, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = WarmOnSurface, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = WarmMutedIcon, modifier = Modifier.size(22.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileDrawerSheetPreview() {
    NeutralTheme {
        ProfileDrawerSheet(
            profile = UserProfile(name = "Claudia Marques", email = "claudia@example.com"),
            onAccountDetails = {},
            onSettings = {}
        )
    }
}
