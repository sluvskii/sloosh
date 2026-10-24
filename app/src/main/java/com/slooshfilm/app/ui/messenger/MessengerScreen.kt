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

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottomPadding = rememberNavBarBottomPadding()
    val topHeaderHeight = statusBarPadding + 56.dp
    val headerBlurHeight = statusBarPadding + 70.dp
    val bottomTotalPadding = navBarBottomPadding + 72.dp

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = topHeaderHeight,
                        bottom = bottomTotalPadding,
                        start = 24.dp,
                        end = 24.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SlooshAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sf_bubble_fill),
                        contentDescription = null,
                        tint = SlooshAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Чаты",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "В разработке",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 20.dp),
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
