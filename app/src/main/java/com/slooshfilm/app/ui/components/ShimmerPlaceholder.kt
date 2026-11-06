package com.slooshfilm.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.ui.theme.LocalSlooshColors

@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val isLightTheme = !LocalSlooshColors.current.isDark
    val shimmerColors = remember(isLightTheme) {
        if (isLightTheme) {
            listOf(
                Color(0xFFE5E5EA),
                Color(0xFFF2F2F7),
                Color(0xFFE5E5EA)
            )
        } else {
            listOf(
                Color(0xFF141414),
                Color(0xFF282828),
                Color(0xFF141414)
            )
        }
    }

    val shape = remember(cornerRadius) { RoundedRectangle(cornerRadius) }

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                val progress = translateAnim.value
                drawRect(
                    brush = Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(progress - 300f, progress - 300f),
                        end = Offset(progress, progress)
                    )
                )
            }
    )
}
