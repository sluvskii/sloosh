package com.slooshfilm.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kyant.shapes.Capsule
import com.slooshfilm.app.ui.sficons.SfIcon
import com.slooshfilm.app.R
import com.slooshfilm.app.data.models.auth.UserProfile
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.components.LiquidGlassDialog
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import com.slooshfilm.app.ui.theme.SlooshAccent
import com.slooshfilm.app.ui.theme.TextPrimary
import com.slooshfilm.app.ui.theme.TextSecondary

@Composable
fun ProfileAccountDialog(
    user: UserProfile,
    onDismissRequest: () -> Unit,
    onSignOutClick: () -> Unit,
    backdrop: Backdrop? = null
) {
    val haptic = LocalHapticFeedback.current
    val isLightTheme = !LocalSlooshColors.current.isDark

    val dismissBg = if (isLightTheme) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)
    val dismissBorder = if (isLightTheme) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
    val dismissTextColor = if (isLightTheme) TextPrimary else Color.White.copy(alpha = 0.85f)

    val avatarFallbackBg = if (isLightTheme) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.08f)
    val avatarFallbackBorder = if (isLightTheme) SlooshAccent.copy(alpha = 0.45f) else SlooshAccent.copy(alpha = 0.35f)

    val signOutBg = Color(0xFFFF453A).copy(alpha = if (isLightTheme) 0.10f else 0.14f)
    val signOutBorder = Color(0xFFFF453A).copy(alpha = if (isLightTheme) 0.22f else 0.28f)

    LiquidGlassDialog(
        title = "Аккаунт",
        onDismissRequest = onDismissRequest,
        backdrop = backdrop,
        showCloseButton = false
    ) { dismiss ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big Avatar (Google photo or Initials)
            val photoUrl = user.photoURL
            if (!photoUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, SlooshAccent.copy(alpha = 0.5f), CircleShape)
                ) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Аватар",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(avatarFallbackBg)
                        .border(1.dp, avatarFallbackBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.avatarInitials,
                        color = SlooshAccent,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Display Title & Email / Tag
            Text(
                text = user.displayTitle,
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            if (!user.email.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.email,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            } else if (!user.tag.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@${user.tag}",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sign Out Button (Destructive Red translucent capsule)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(Capsule())
                    .background(signOutBg)
                    .border(0.5.dp, signOutBorder, Capsule())
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dismiss()
                    onSignOutClick()
                },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    SfIcon(
                        systemName = "rectangle.portrait.and.arrow.right",
                        contentDescription = null,
                        tint = Color(0xFFFF453A),
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Выйти из аккаунта",
                        color = Color(0xFFFF453A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dismiss Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(Capsule())
                    .background(dismissBg)
                    .border(0.5.dp, dismissBorder, Capsule())
                    .clickable { dismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Закрыть",
                    color = dismissTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
