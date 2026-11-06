package com.slooshfilm.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.theme.SlooshFontFamily
import com.slooshfilm.app.utils.NetworkMonitor

/**
 * Компактная стеклянная плашка «Нет подключения» под статус-баром.
 * Появляется только при потере сети; glass-off обрабатывается внутри liquidGlassSurface.
 */
@Composable
fun OfflineBanner(
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    val isOnline by NetworkMonitor.isOnline.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val rawStatusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarPadding = if (rawStatusBarPadding > 0.dp) rawStatusBarPadding else (if (isLandscape) 8.dp else 0.dp)

    AnimatedVisibility(
        visible = !isOnline,
        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { -it },
        exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(top = statusBarPadding + (if (isLandscape) 4.dp else 10.dp))
                .liquidGlassSurface(
                    style = LiquidGlassTokens.CompactPill,
                    shape = Capsule(),
                    containerColor = Color.Black.copy(alpha = 0.55f),
                    backdrop = backdrop
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет подключения",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SlooshFontFamily
            )
        }
    }
}
