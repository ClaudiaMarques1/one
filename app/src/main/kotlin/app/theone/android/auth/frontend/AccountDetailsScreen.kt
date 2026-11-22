package app.theone.android.auth.frontend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theone.android.auth.backend.SignInMethod
import app.theone.android.auth.backend.UserProfile
import app.theone.android.ui.BackTopBar
import app.theone.android.ui.ProfileAvatar
import app.theone.android.ui.profileInitials
import app.theone.android.ui.theme.CardSurface
import app.theone.android.ui.theme.DeleteRed
import app.theone.android.ui.theme.NeutralTheme
import app.theone.android.ui.theme.SignOutPressed
import app.theone.android.ui.theme.WarmButtonOutline
import app.theone.android.ui.theme.WarmCardBorder
import app.theone.android.ui.theme.WarmDivider
import app.theone.android.ui.theme.WarmOnSurface
import app.theone.android.ui.theme.WarmSecondaryText
import app.theone.android.ui.theme.WarmSurface

// 1c in the home handoff. Read-only; Sign out moved here from Home's top bar (no confirmation, as before).

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun AccountDetailsScreen(profile: UserProfile?, onBack: () -> Unit, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmSurface)
    ) {
        BackTopBar(title = "Account details", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileAvatar(
                photoUrl = profile?.photoUrl,
                initials = profileInitials(profile?.name.orEmpty(), profile?.email.orEmpty()),
                size = 88.dp,
                initialsSize = 32.sp
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(CardSurface)
                    .border(1.dp, WarmCardBorder, CardShape)
            ) {
                AccountRow(label = "Name", value = nameLabel(profile?.name.orEmpty()))
                HorizontalDivider(color = WarmDivider)
                AccountRow(label = "Email", value = profile?.email.orEmpty())
                HorizontalDivider(color = WarmDivider)
                AccountRow(label = "Sign-in method", value = (profile?.signInMethod ?: SignInMethod.OTHER).label())
            }
            SignOutButton(onClick = onSignOut)
        }
    }
}

@Composable
private fun AccountRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(label, fontSize = 12.sp, color = WarmSecondaryText)
        Text(value, fontSize = 16.sp, color = WarmOnSurface)
    }
}

@Composable
private fun SignOutButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        border = BorderStroke(1.dp, WarmButtonOutline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (pressed) SignOutPressed else CardSurface,
            contentColor = DeleteRed
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign out", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountDetailsScreenPreview() {
    NeutralTheme {
        AccountDetailsScreen(
            profile = UserProfile(name = "Claudia Marques", email = "claudia@example.com", signInMethod = SignInMethod.GOOGLE),
            onBack = {},
            onSignOut = {}
        )
    }
}
