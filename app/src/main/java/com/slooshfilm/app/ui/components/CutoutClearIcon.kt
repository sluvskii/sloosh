package com.slooshfilm.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Authentic Apple iOS `xmark.circle.fill` with a solid white circle and a bold cross
 * cut out using hardware subtractive blending ([BlendMode.Clear]) on an isolated offscreen layer.
 */
@Composable
fun CutoutClearIcon(
    modifier: Modifier = Modifier,
    circleColor: Color = Color.White,
    sizeDp: Dp = 15.dp,
    strokeWidthDp: Dp = 1.8.dp
) {
    Canvas(
        modifier = modifier
            .size(sizeDp)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        // 1. Solid white circle disc
        drawCircle(
            color = circleColor,
            radius = size.minDimension / 2f
        )

        // 2. Compact bold cross cut out with subtraction (BlendMode.Clear)
        val strokeWidthPx = strokeWidthDp.toPx()
        val crossRadius = size.minDimension * 0.14f
        val center = this.center

        // Diagonal 1: top-left to bottom-right
        drawLine(
            color = Color.Black,
            start = Offset(center.x - crossRadius, center.y - crossRadius),
            end = Offset(center.x + crossRadius, center.y + crossRadius),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Clear
        )

        // Diagonal 2: bottom-left to top-right
        drawLine(
            color = Color.Black,
            start = Offset(center.x - crossRadius, center.y + crossRadius),
            end = Offset(center.x + crossRadius, center.y - crossRadius),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Clear
        )
    }
}
