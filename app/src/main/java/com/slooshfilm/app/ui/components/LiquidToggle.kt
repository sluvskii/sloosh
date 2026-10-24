package com.slooshfilm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.shapes.Capsule
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.backdrop.backdrops.emptyBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberCanvasBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberCombinedBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.backdrop.effects.blur
import com.slooshfilm.app.ui.backdrop.effects.lens
import com.slooshfilm.app.ui.backdrop.highlight.Highlight
import com.slooshfilm.app.ui.backdrop.shadow.InnerShadow
import com.slooshfilm.app.ui.backdrop.shadow.Shadow
import com.slooshfilm.app.ui.theme.SlooshAccent
import kotlinx.coroutines.flow.collectLatest

/**
 * Authentic LiquidToggle from Kyant's Backdrop engine with sleek horizontal proportions (58dp x 26dp)
 * and themed with sloosh lime accent color.
 */
@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = SlooshAccent
) {
    val isLightTheme = !LocalSlooshColors.current.isDark
    val isGlassEnabled = LocalGlassEnabled.current
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 18f.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }
    // Колбэки читаем через rememberUpdatedState: сам DampedDragAnimation создаётся один раз
    // в remember и иначе навсегда запоминал бы selected/onSelect из первой композиции.
    // Из-за этого переключатель после первого тапа начинал всегда отправлять одно и то же
    // значение и «залипал» (стекло нельзя было вернуть без перезапуска приложения).
    val latestSelected by rememberUpdatedState(selected)
    val latestOnSelect by rememberUpdatedState(onSelect)
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.45f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    latestOnSelect(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (latestSelected()) 0f else 1f
                    latestOnSelect(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidth
                fraction =
                    if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                    else (fraction - delta).fastCoerceIn(0f, 1f)
            }
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { fraction ->
                dampedDragAnimation.updateValue(fraction)
            }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { latestSelected() }
            .collectLatest { isSelected ->
                val target = if (isSelected) 1f else 0f
                if (target != fraction) {
                    fraction = target
                    dampedDragAnimation.animateToValue(target)
                }
            }
    }

    val trackBackdrop = rememberLayerBackdrop()
    val fallbackBackdrop = rememberCanvasBackdrop {
        drawRect(if (isLightTheme) Color.White else Color(0xFF1C1C1E))
    }
    val effectiveBackdrop = backdrop ?: fallbackBackdrop

    Box(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .drawBehind {
                    val currentFraction = dampedDragAnimation.value
                    drawRect(lerp(trackColor, accentColor, currentFraction))
                }
                .size(58f.dp, 26f.dp)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val currentFraction = dampedDragAnimation.value
                    val padding = 2f.dp.toPx()
                    translationX =
                        if (isLtr) lerp(padding, padding + dragWidth, currentFraction)
                        else lerp(-padding, -(padding + dragWidth), currentFraction)
                }
                .semantics {
                    role = Role.Switch
                }
                .then(dampedDragAnimation.modifier)
                .size(36f.dp, 22f.dp)
                .drawBackdrop(
                    backdrop = if (isGlassEnabled) {
                        rememberCombinedBackdrop(
                            effectiveBackdrop,
                            rememberBackdrop(trackBackdrop) { drawBackdrop ->
                                val progress = dampedDragAnimation.pressProgress
                                if (progress > 0.001f) {
                                    val scaleX = lerp(2f / 3f, 0.75f, progress)
                                    val scaleY = lerp(0.001f, 0.75f, progress)
                                    scale(scaleX, scaleY) {
                                        drawBackdrop()
                                    }
                                }
                            }
                        )
                    } else emptyBackdrop(),
                    shape = { Capsule() },
                    effects = {
                        if (isGlassEnabled) {
                            val progress = dampedDragAnimation.pressProgress
                            blur(7f.dp.toPx() * (1f - progress))
                            lens(
                                4.5f.dp.toPx() * progress,
                                9f.dp.toPx() * progress,
                                chromaticAberration = true
                            )
                        }
                    },
                                highlight = {
                                    val progress = dampedDragAnimation.pressProgress
                                    if (progress > 0.01f) {
                                        Highlight(
                                            alpha = progress * 0.50f,
                                            style = ConcentratedHighlightStyle(
                                                angle = 90f,
                                                falloff = 2.2f,
                                                lengthFactor = 0.65f
                                            )
                                        )
                                    } else null
                                },
                                shadow = {
                                    Shadow(
                                        radius = 3.5f.dp,
                                        color = Color.Black.copy(alpha = if (isLightTheme) 0.10f else 0.25f)
                                    )
                                },
                                innerShadow = {
                                    if (isGlassEnabled) {
                                        val progress = dampedDragAnimation.pressProgress
                                        if (progress > 0.01f) {
                                            InnerShadow(
                                                radius = 3.5f.dp * progress,
                                                alpha = progress
                                            )
                                        } else null
                                    } else null
                                },
                                layerBlock = {
                                    scaleX = dampedDragAnimation.scaleX
                                    scaleY = dampedDragAnimation.scaleY
                                    val velocity = dampedDragAnimation.velocity / 50f
                                    scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                    scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                                },
                                onDrawSurface = {
                                    val progress = dampedDragAnimation.pressProgress
                                    drawRect(Color.White.copy(alpha = if (isGlassEnabled) (1f - progress) else 1f))
                                }
                            )
                            .glassBorder(
                                shape = Capsule(),
                                width = 0.5.dp,
                                alpha = { dampedDragAnimation.pressProgress * (if (isLightTheme) 0.30f else 0.55f) }
                            )
            )
    }
}

@Composable
fun LiquidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    accentColor: Color = SlooshAccent
) {
    LiquidToggle(
        selected = { checked },
        onSelect = onCheckedChange,
        backdrop = backdrop,
        modifier = modifier,
        accentColor = accentColor
    )
}
