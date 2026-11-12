package com.slooshfilm.app.ui.messenger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slooshfilm.app.R
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.components.EmptyStateView
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.ProgressiveBlurTokens
import com.slooshfilm.app.ui.components.iosProgressiveBlur
import com.slooshfilm.app.ui.components.rememberNavBarBottomPadding
import com.slooshfilm.app.ui.theme.*

/**
 * Аутентичный минималистичный экран «Чаты».
 * Лаконичный статус разработки в нативном стиле без лишней перегрузки и свечений.
 */
@Composable
fun MessengerScreen(
    onMediaClick: (mediaId: String, isTv: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val screenBg = MaterialTheme.colorScheme.background
    val isProgressiveBlurEnabled = LocalProgressiveBlur.current

    val contentBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val rawStatusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarPadding = if (rawStatusBarPadding > 0.dp) rawStatusBarPadding else (if (isLandscape) 8.dp else 0.dp)
    val navBarBottomPadding = rememberNavBarBottomPadding()
    val topHeaderHeight = statusBarPadding + (if (isLandscape) 44.dp else 56.dp)
    val headerBlurHeight = statusBarPadding + (if (isLandscape) 52.dp else 70.dp)
    val bottomTotalPadding = if (isLandscape) navBarBottomPadding + 48.dp else navBarBottomPadding + 72.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // 1. Content (captured in contentBackdrop)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(contentBackdrop)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(screenBg)
            )

            // Centered Clean Status
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = topHeaderHeight,
                        bottom = bottomTotalPadding,
                        start = 32.dp,
                        end = 32.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    iconSfName = "bubble.fill",
                    title = "Скоро будет доступно",
                    message = "Общение с друзьями и обсуждение фильмов появятся в следующем обновлении."
                )
            }
        }

        // 2. Top Header (Floating Progressive Blur Bar over the content)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerBlurHeight)
                .align(Alignment.TopCenter)
        ) {
            if (isProgressiveBlurEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = contentBackdrop,
                            shape = { RectangleShape },
                            effects = {
                                val topOffsetPx = statusBarPadding.toPx()
                                iosProgressiveBlur(
                                    maxRadius = ProgressiveBlurTokens.MaxRadius.toPx(),
                                    topOffset = topOffsetPx,
                                    falloffDistance = (headerBlurHeight.toPx() - topOffsetPx).coerceAtLeast(1f)
                                )
                            },
                            highlight = null,
                            shadow = null
                        )
                )
            }

            // Top ambient gradient fade
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                screenBg,
                                screenBg.copy(alpha = 0f)
                            )
                        )
                    )
            )

            // Header Title
            val cutoutPadding = com.slooshfilm.app.ui.components.rememberCutoutHorizontalPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding)
                    .height(if (isLandscape) 44.dp else 56.dp)
                    .padding(horizontal = 20.dp + cutoutPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Чаты",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryDark,
                    letterSpacing = (-0.3).sp
                )
            }
        }
    }
}
