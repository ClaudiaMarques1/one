package app.theone.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import app.theone.android.ui.theme.NeutralAvatar
import app.theone.android.ui.theme.NeutralAvatarInitials
import coil3.compose.AsyncImage

/**
 * The account photo cropped to a circle; the initials show underneath while it loads, if it fails,
 * or when there's no photo. Sizes: 40dp on Home, 72dp in the drawer, 88dp on Account details.
 */
@Composable
fun ProfileAvatar(photoUrl: String?, initials: String, size: Dp, initialsSize: TextUnit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(NeutralAvatar),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, fontSize = initialsSize, fontWeight = FontWeight.SemiBold, color = NeutralAvatarInitials)
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
