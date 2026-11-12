package com.slooshfilm.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberCombinedBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.backdrop.effects.blur
import com.slooshfilm.app.ui.backdrop.effects.colorControls
import com.slooshfilm.app.ui.backdrop.effects.lens
import com.slooshfilm.app.ui.backdrop.effects.vibrancy
import com.slooshfilm.app.ui.backdrop.highlight.Highlight
import com.slooshfilm.app.ui.backdrop.highlight.HighlightStyle
import com.slooshfilm.app.ui.backdrop.shadow.InnerShadow
import com.slooshfilm.app.ui.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.slooshfilm.app.ui.theme.LiquidGlassSurfaceDark
import com.slooshfilm.app.ui.theme.LiquidGlassSurfaceLight
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

internal val SharedCapsule: Shape = SlooshCapsule

internal val LocalLiquidBottomTabScale =
    staticCompositionLocalOf { { 1f } }

internal val LocalLiquidBottomTabTint =
    staticCompositionLocalOf<ColorFilter?> { null }

internal val LocalLiquidBottomTabTextColor =
    staticCompositionLocalOf { Color.White }

internal val LocalLiquidBottomTabMinimized =
    staticCompositionLocalOf { false }

@Composable
fun LiquidBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    clip: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scale = LocalLiquidBottomTabScale.current
    val tabTint = LocalLiquidBottomTabTint.current
    val isMinimized = LocalLiquidBottomTabMinimized.current
    val textColor = LocalLiquidBottomTabTextColor.current
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .then(if (clip) Modifier.clip(SharedCapsule) else Modifier)
            .then(
                if (!isMinimized) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Tab,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        }
                    )
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
                if (tabTint != null) {
                    colorFilter = tabTint
                }
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(LocalContentColor provides textColor) {
            content()
        }
    }
}

@Composable
private fun TabsLayout(
    stepPx: Float,
    insetPx: Float,
    pillWidthPx: Float,
    normalWidthPx: Float,
    searchProgress: Float = 0f,
    focusProgress: Float = 0f,
    activeTabIndex: Int = 0,
    isLtr: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val maxSlotPx = maxOf(stepPx, pillWidthPx).fastRoundToInt().coerceAtLeast(with(density) { 56.dp.toPx().fastRoundToInt() })
        val itemConstraints = constraints.copy(
            minWidth = maxSlotPx,
            maxWidth = maxSlotPx,
            minHeight = constraints.maxHeight,
            maxHeight = constraints.maxHeight
        )
        val placeables = measurables.fastMap { it.measure(itemConstraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            val centerTargetX = constraints.maxWidth / 2f
            placeables.fastForEachIndexed { index, placeable ->
                val normalCenterX = if (isLtr) {
                    insetPx + pillWidthPx / 2f + index * stepPx
                } else {
                    normalWidthPx - (insetPx + pillWidthPx / 2f + index * stepPx)
                }
                if (index == activeTabIndex) {
                    val currentCenterX = lerp(normalCenterX, centerTargetX, searchProgress)
                    val x = (currentCenterX - placeable.width / 2f).fastRoundToInt()
                    val y = (constraints.maxHeight - placeable.height) / 2
                    val focusScale = (1f - focusProgress).coerceIn(0f, 1f)
                    val iconScale = lerp(1f, 20f / 26f, searchProgress) * focusScale
                    placeable.placeWithLayer(x, y) {
                        scaleX = iconScale
                        scaleY = iconScale
                        alpha = 1f
                    }
                } else {
                    val inactiveAlpha = (1f - searchProgress * 2.5f).coerceIn(0f, 1f)
                    if (inactiveAlpha > 0f) {
                        val currentCenterX = lerp(normalCenterX, centerTargetX, searchProgress)
                        val x = (currentCenterX - placeable.width / 2f).fastRoundToInt()
                        val y = (constraints.maxHeight - placeable.height) / 2
                        placeable.placeWithLayer(x, y) {
                            alpha = inactiveAlpha
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveTabSlotLayout(
    activeTabIndex: Int,
    accentColor: Color,
    scale: Float = 1f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            layout(constraints.maxWidth, constraints.maxHeight) {}
        } else {
            val safeIndex = activeTabIndex.coerceIn(0, measurables.lastIndex)
            val measurable = measurables[safeIndex]
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = 0,
                    maxWidth = constraints.maxWidth,
                    minHeight = constraints.maxHeight,
                    maxHeight = constraints.maxHeight
                )
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                val x = (constraints.maxWidth - placeable.width) / 2
                val y = (constraints.maxHeight - placeable.height) / 2
                placeable.placeWithLayer(x, y) {
                    scaleX = scale
                    scaleY = scale
                }
            }
        }
    }
}

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    barHeight: Dp = 54.dp,
    pillHeight: Dp = barHeight - 8.dp,
    pillExtraWidth: Dp = 10.dp,
    isStaticBar: Boolean = false,
    searchProgress: Float = 0f,
    focusProgress: Float = 0f,
    activeTabIndex: Int = selectedTabIndex(),
    normalWidth: Dp? = null,
    accentColor: Color = com.slooshfilm.app.ui.theme.SlooshAccent,
    content: @Composable () -> Unit
) {
    val isLightTheme = !LocalSlooshColors.current.isDark
    // Тема в состоянии: заливка пилюли рисуется внутри записанного слоя, и без чтения
    // состояния при отрисовке смена темы не инвалидировала бы этот слой.
    val isLightThemeState = rememberUpdatedState(isLightTheme)
    val isGlassEnabled = LocalGlassEnabled.current
    val glassBlurMultiplier = LocalGlassBlurMultiplier.current
    val rawContainerColor =
        if (isLightTheme) LiquidGlassSurfaceLight
        else LiquidGlassSurfaceDark
    val effectiveContainerColor = computeEffectiveContainerColor(rawContainerColor, glassBlurMultiplier)
    val effectiveBarBlurOpacity = computeEffectiveBlurOpacity(LiquidGlassTokens.Bar.blurOpacity, glassBlurMultiplier)

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val totalWidthPx = constraints.maxWidth.toFloat()
        val normalWidthPx = if (normalWidth != null) {
            with(density) { normalWidth.toPx() }
        } else {
            totalWidthPx
        }
        val verticalPadding = ((barHeight - pillHeight) / 2).coerceAtLeast(0.dp)
        val insetPx = with(density) { verticalPadding.toPx() }
        val pillHeightPx = with(density) { pillHeight.toPx() }
        val normalInnerWidthPx = (normalWidthPx - 2f * insetPx).coerceAtLeast(0f)

        val pillExtraPx = with(density) { pillExtraWidth.toPx() }
        val normalBaseSlotPx = if (tabsCount > 0) normalInnerWidthPx / tabsCount else 0f
        val normalPillWidthPx = if (tabsCount > 1) {
            (normalBaseSlotPx + pillExtraPx).coerceIn(normalBaseSlotPx, normalInnerWidthPx - with(density) { 30f.dp.toPx() })
        } else {
            normalInnerWidthPx
        }
        val normalStepPx = if (tabsCount > 1) {
            (normalInnerWidthPx - normalPillWidthPx) / (tabsCount - 1)
        } else {
            0f
        }
        val pillWidthDp = with(density) { normalPillWidthPx.toDp() }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density, totalWidthPx, isStaticBar) {
            derivedStateOf {
                if (isStaticBar || totalWidthPx <= 0f) 0f
                else {
                    val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                    with(density) {
                        4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                    }
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val currentStepPx by rememberUpdatedState(normalStepPx)
        val currentIsLtr by rememberUpdatedState(isLtr)
        val currentTabsCount by rememberUpdatedState(tabsCount)
        val currentOnTabSelected by rememberUpdatedState(onTabSelected)
        val currentSelectedTabIndex by rememberUpdatedState(selectedTabIndex)

        var lastHapticIndex by remember {
            mutableIntStateOf(currentSelectedTabIndex().coerceIn(0, (tabsCount - 1).coerceAtLeast(0)))
        }

        val dampedDragAnimation = remember(animationScope, isStaticBar, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = currentSelectedTabIndex().coerceIn(0, (tabsCount - 1).coerceAtLeast(0)).toFloat(),
                valueRange = 0f..(tabsCount - 1).coerceAtLeast(0).toFloat(),
                visibilityThreshold = 0.001f,
                density = density.density,
                initialScale = 1f,
                pressedScale = 60f / 44f,
                pressedScaleY = 68f / 44f,
                liftSpringStiffness = 631.65468f,
                liftHangTimeMs = 220L,
                onDragStarted = {},
                onDragStopped = {
                    val count = currentTabsCount
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, (count - 1).coerceAtLeast(0))
                    lastHapticIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    if (!isStaticBar) {
                        animationScope.launch {
                            offsetAnimation.animateTo(
                                0f,
                                spring(1f, 300f, 0.5f)
                            )
                        }
                    }
                    currentOnTabSelected(targetIndex)
                },
                onDrag = { _, dragAmount ->
                    val step = currentStepPx
                    val count = currentTabsCount
                    if (step > 0f && count > 1) {
                        val delta = dragAmount.x / step * if (currentIsLtr) 1f else -1f
                        val nextVal = (targetValue + delta).fastCoerceIn(0f, (count - 1).toFloat())
                        updateValue(nextVal)
                        val nearest = nextVal.fastRoundToInt().fastCoerceIn(0, count - 1)
                        if (nearest != lastHapticIndex) {
                            lastHapticIndex = nearest
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                    if (!isStaticBar) {
                        animationScope.launch {
                            offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                        }
                    }
                }
            )
        }

        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentSelectedTabIndex() }
                .collectLatest { index ->
                    val count = currentTabsCount
                    if (index in 0 until count) {
                        val target = index.toFloat()
                        if (abs(dampedDragAnimation.targetValue - target) > 0.001f) {
                            dampedDragAnimation.animateToValue(target)
                        }
                    }
                }
        }

        // Layer 1: Inactive tabs container & layout (NEVER faded, deforms physically)
        val layer1TextColor = if (isLightTheme) Color.Black else Color.White
        CompositionLocalProvider(
            LocalLiquidBottomTabMinimized provides (searchProgress >= 0.5f),
            LocalLiquidBottomTabTextColor provides layer1TextColor,
            LocalContentColor provides layer1TextColor
        ) {
            TabsLayout(
                stepPx = normalStepPx,
                insetPx = insetPx,
                pillWidthPx = normalPillWidthPx,
                normalWidthPx = normalWidthPx,
                searchProgress = searchProgress,
                focusProgress = focusProgress,
                activeTabIndex = activeTabIndex.coerceIn(0, (tabsCount - 1).coerceAtLeast(0)),
                isLtr = isLtr,
                modifier = Modifier
                    .then(
                        if (isStaticBar) {
                            Modifier.background(
                                color = if (isLightTheme) Color.Black.copy(alpha = 0.08f) else Color(0xFF2C2C2E),
                                shape = SharedCapsule
                            )
                        } else {
                            Modifier
                                .graphicsLayer {
                                    translationX = panelOffset * (1f - searchProgress)
                                }
                                .liquidGlassSurface(
                                    style = LiquidGlassTokens.Bar,
                                    shape = SharedCapsule,
                                    containerColor = rawContainerColor,
                                    backdrop = backdrop,
                                    layerBlock = {
                                        val progress = dampedDragAnimation.pressProgress * (1f - searchProgress)
                                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                                        scaleX = scale
                                        scaleY = 1f
                                    }
                                )
                        }
                    )
                    .height(barHeight)
                    .fillMaxWidth()
                    .padding(vertical = verticalPadding),
                content = content
            )
        }

        val sliderAlpha = (1f - searchProgress * 2.5f).coerceIn(0f, 1f)

        // Layer 2: Off-screen active tab capture layer with accent color tint (Glass mode only)
        if (sliderAlpha > 0f && isGlassEnabled) {
            val layer2TextColor = if (isLightTheme) Color.Black else Color.White
            CompositionLocalProvider(
                LocalLiquidBottomTabMinimized provides true,
                LocalLiquidBottomTabScale provides { lerp(1f, 1.2f, dampedDragAnimation.pressProgress) },
                LocalLiquidBottomTabTint provides (if (isStaticBar || accentColor == Color.White) null else ColorFilter.tint(accentColor)),
                LocalLiquidBottomTabTextColor provides layer2TextColor,
                LocalContentColor provides layer2TextColor
            ) {
                TabsLayout(
                    stepPx = normalStepPx,
                    insetPx = insetPx,
                    pillWidthPx = normalPillWidthPx,
                    normalWidthPx = normalWidthPx,
                    searchProgress = searchProgress,
                    focusProgress = focusProgress,
                    activeTabIndex = activeTabIndex.coerceIn(0, (tabsCount - 1).coerceAtLeast(0)),
                    isLtr = isLtr,
                    modifier = Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer {
                            translationX = panelOffset * (1f - searchProgress)
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { SharedCapsule },
                            effects = {
                                if (!isStaticBar) {
                                    val progress = dampedDragAnimation.pressProgress * sliderAlpha
                                    val barBlurPx = LiquidGlassTokens.Bar.blurRadius.toPx()
                                    val sliderFrostPx = LiquidGlassTokens.Slider.blurRadius.toPx()
                                    val effBarBlur = barBlurPx * effectiveBarBlurOpacity * 0.50f
                                    val restBlurRadiusPx = sqrt(effBarBlur * effBarBlur + sliderFrostPx * sliderFrostPx)
                                    val restBlurOpacity = 1.0f

                                    val activeBlur = lerp(restBlurRadiusPx, barBlurPx, progress)
                                    val activeOpacity = lerp(restBlurOpacity, effectiveBarBlurOpacity, progress)

                                    iosFrostedBlur(activeBlur, blurOpacity = activeOpacity)
                                    iosVibrantGlass(isLight = isLightTheme)
                                    lens(
                                        refractionHeight = LiquidGlassTokens.Bar.refractionHeight.toPx(),
                                        refractionAmount = LiquidGlassTokens.Bar.refractionAmount.toPx()
                                    )
                                    // Адаптивное тело ползунка (glasmio NativeTabIdle):
                                    // цвет из материала бара через UIKit-матрицы, живёт от контента.
                                    if (isLtr) {
                                        iosTabIdleBody(
                                            pillLeft = insetPx + dampedDragAnimation.value * normalStepPx + dampedDragAnimation.inertialShiftPx,
                                            pillTop = (size.height - pillHeightPx * dampedDragAnimation.scaleY) / 2f,
                                            pillWidth = normalPillWidthPx * dampedDragAnimation.scaleX,
                                            pillHeight = pillHeightPx * dampedDragAnimation.scaleY,
                                            isLight = isLightTheme,
                                            liftProgress = progress
                                        )
                                    } else {
                                        iosTabIdleBody(
                                            pillLeft = size.width - (insetPx + normalPillWidthPx + dampedDragAnimation.value * normalStepPx) - dampedDragAnimation.inertialShiftPx,
                                            pillTop = (size.height - pillHeightPx * dampedDragAnimation.scaleY) / 2f,
                                            pillWidth = normalPillWidthPx * dampedDragAnimation.scaleX,
                                            pillHeight = pillHeightPx * dampedDragAnimation.scaleY,
                                            isLight = isLightTheme,
                                            liftProgress = progress
                                        )
                                    }
                                }
                            },
                            highlight = null,
                            onDrawSurface = {
                                // Читаем тему здесь, в момент отрисовки: это инвалидирует
                                // записанный слой при смене темы.
                                val isLight = isLightThemeState.value
                                val progress = dampedDragAnimation.pressProgress
                                // Заливка пилюли рисуется здесь — до drawContent(), то есть ПОД подписью.
                                if (isStaticBar) {
                                    // Настройки: пилюля чуть светлее трека, при нажатии заливка уходит.
                                    val lift = if (isLight) 0.90f else 0.16f
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = lift * (1f - progress).fastCoerceIn(0f, 1f)),
                                        topLeft = Offset(
                                            if (isLtr) insetPx + dampedDragAnimation.value * normalStepPx + dampedDragAnimation.inertialShiftPx
                                            else size.width - (insetPx + normalPillWidthPx + dampedDragAnimation.value * normalStepPx) - dampedDragAnimation.inertialShiftPx,
                                            size.height / 2f - pillHeightPx * dampedDragAnimation.scaleY / 2f
                                        ),
                                        size = Size(
                                            normalPillWidthPx * dampedDragAnimation.scaleX,
                                            pillHeightPx * dampedDragAnimation.scaleY
                                        ),
                                        cornerRadius = CornerRadius(pillHeightPx * dampedDragAnimation.scaleY / 2f)
                                    )
                                } else {
                                    // Стеклянный бар: статичное чёрное затемнение удалено —
                                    // тело ползунка теперь считает шейдер iosTabIdleBody
                                    // (адаптивный матричный tint из NativeTabIdle).
                                    drawRect(effectiveContainerColor)
                                }
                            }
                        )
                        .then(
                            if (!isStaticBar) {
                                Modifier.glassBorder(
                                    shape = SharedCapsule,
                                    alpha = { dampedDragAnimation.pressProgress * sliderAlpha * (if (isLightTheme) 0.30f else 0.55f) }
                                )
                            } else {
                                Modifier
                            }
                        )
                        .height(barHeight)
                        .fillMaxWidth()
                        .padding(vertical = verticalPadding),
                    content = content
                )
            }
        }

        // Layer 3: Dynamic sliding active tab liquid lens pill
        val isSelectedTabValid = selectedTabIndex() in 0 until tabsCount
        if (sliderAlpha > 0f) {
            if (isGlassEnabled) {
                val activeBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)
                Box(
                    Modifier
                        .layout { measurable, _ ->
                            val velocity = dampedDragAnimation.velocity / 10f
                            val velScaleX = 1f / (1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f))
                            val velScaleY = 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            val currentScaleX = dampedDragAnimation.scaleX * velScaleX
                            val currentScaleY = dampedDragAnimation.scaleY * velScaleY
                            val targetW = (normalPillWidthPx * currentScaleX).fastRoundToInt()
                            val targetH = (pillHeightPx * currentScaleY).fastRoundToInt()
                            val placeable = measurable.measure(Constraints.fixed(targetW, targetH))
                            layout(targetW, targetH) {
                                placeable.place(0, 0)
                            }
                        }
                        .graphicsLayer {
                            val shift = dampedDragAnimation.inertialShiftPx
                            val velocity = dampedDragAnimation.velocity / 10f
                            val velScaleX = 1f / (1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f))
                            val velScaleY = 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            val currentScaleX = dampedDragAnimation.scaleX * velScaleX
                            val currentScaleY = dampedDragAnimation.scaleY * velScaleY
                            val currentW = normalPillWidthPx * currentScaleX
                            val currentH = pillHeightPx * currentScaleY

                            val baseLeft =
                                if (isLtr) insetPx + dampedDragAnimation.value * normalStepPx + (panelOffset * (1f - searchProgress)) + shift
                                else size.width - (insetPx + normalPillWidthPx + dampedDragAnimation.value * normalStepPx) + (panelOffset * (1f - searchProgress)) - shift

                            translationX = baseLeft - (currentW - normalPillWidthPx) / 2f
                            translationY = 0f
                            alpha = (if (isSelectedTabValid) 1f else 0f) * sliderAlpha
                        }
                        .then(if (searchProgress < 0.01f) dampedDragAnimation.modifier else Modifier)
                        .drawBackdrop(
                            backdrop = activeBackdrop,
                            shape = { SharedCapsule },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress * sliderAlpha
                                val activity = dampedDragAnimation.fluidActivity
                                if (progress > 0f) {
                                    val baseHeight = LiquidGlassTokens.Slider.refractionHeight.value
                                    val baseAmount = LiquidGlassTokens.Slider.refractionAmount.value
                                    val dynHeight = (baseHeight + 1.2f * activity).dp.toPx() * progress
                                    val dynAmount = (baseAmount + 3.5f * activity).dp.toPx() * progress
                                    val dynAberration = (LiquidGlassTokens.Slider.chromaticAberration + 0.04f * activity) * progress
                                    val dynFishEye = (LiquidGlassTokens.Slider.fishEyeStrength + 0.03f * activity) * progress
                                    iosLens(
                                        refractionHeight = dynHeight,
                                        refractionAmount = dynAmount,
                                        chromaticAberration = dynAberration,
                                        fishEyeStrength = dynFishEye
                                    )
                                    // Портальная деформация контента + френелевское кольцо
                                    // (glasmio NativeControlLens, TabBarIndicator lift).
                                    iosTabLift(lift = progress)
                                }
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress * sliderAlpha
                                val activity = dampedDragAnimation.fluidActivity
                                if (progress > 0f) {
                                    Highlight(
                                        alpha = (0.50f + 0.25f * activity) * progress,
                                        style = ConcentratedHighlightStyle(
                                            angle = 90f,
                                            falloff = 2.2f,
                                            lengthFactor = 0.55f + 0.15f * activity
                                        )
                                    )
                                } else null
                            },
                            shadow = {
                                val progress = dampedDragAnimation.pressProgress * sliderAlpha
                                if (progress > 0f) {
                                    Shadow(
                                        color = if (isLightTheme) Color.Black.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.35f),
                                        alpha = progress * (if (isLightTheme) 0.10f else 0.35f)
                                    )
                                } else null
                            },
                            layerBlock = null
                        )
                        .glassBorder(
                            shape = SharedCapsule,
                            alpha = {
                                dampedDragAnimation.pressProgress * sliderAlpha * (if (isLightTheme) 0.30f else 0.55f)
                            }
                        )
                )
            } else {
                // High-performance No-Glass Mode: Single Solid iOS Segmented Droplet
                val nearestTabIndex = dampedDragAnimation.value.fastRoundToInt().fastCoerceIn(0, (tabsCount - 1).coerceAtLeast(0))
                val activeScale = if (isStaticBar) 1f else lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                val progress = dampedDragAnimation.pressProgress

                val pillBgColor = if (isStaticBar) {
                    // Без стекла пилюля сегментов — сплошная, чуть светлее трека.
                    if (isLightTheme) Color.White else Color(0xFF4E4E50)
                } else {
                    val navBarBg = if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)
                    val darkAlpha = (if (isLightTheme) lerp(0.18f, 0.08f, progress) else lerp(0.52f, 0.0f, progress)) * sliderAlpha
                    val restColor = lerpColor(navBarBg, Color.Black, darkAlpha)
                    if (progress > 0.001f) {
                        val activeDragColor = if (isLightTheme) Color(0xFFF2F2F7) else Color(0xFF2C2C2E)
                        lerpColor(restColor, activeDragColor, progress)
                    } else {
                        restColor
                    }
                }

                val shadowModifier = if (isStaticBar) {
                    Modifier.shadow(
                        elevation = if (isLightTheme) 2.dp else 2.5.dp,
                        shape = SharedCapsule,
                        ambientColor = Color.Black.copy(alpha = if (isLightTheme) 0.08f else 0.25f),
                        spotColor = Color.Black.copy(alpha = if (isLightTheme) 0.12f else 0.40f)
                    )
                } else if (progress > 0.01f) {
                    Modifier.shadow(
                        elevation = (if (isLightTheme) 2.dp else 2.5.dp) * progress,
                        shape = SharedCapsule,
                        ambientColor = Color.Black.copy(alpha = (if (isLightTheme) 0.08f else 0.25f) * progress),
                        spotColor = Color.Black.copy(alpha = (if (isLightTheme) 0.12f else 0.40f) * progress)
                    )
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val shift = dampedDragAnimation.inertialShiftPx
                            translationX =
                                if (isLtr) insetPx + dampedDragAnimation.value * normalStepPx + (panelOffset * (1f - searchProgress)) + shift
                                else size.width - (insetPx + normalPillWidthPx + dampedDragAnimation.value * normalStepPx) + (panelOffset * (1f - searchProgress)) - shift
                            alpha = (if (isSelectedTabValid) 1f else 0f) * sliderAlpha
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                            val velocity = dampedDragAnimation.velocity / 10f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        }
                        .then(if (searchProgress < 0.01f) dampedDragAnimation.modifier else Modifier)
                        .height(pillHeight)
                        .width(pillWidthDp)
                        .then(shadowModifier)
                        .clip(SharedCapsule)
                        .background(color = pillBgColor, shape = SharedCapsule)
                        .glassBorder(
                            shape = SharedCapsule,
                            alpha = {
                                if (isStaticBar) 0f
                                else dampedDragAnimation.pressProgress * sliderAlpha * (if (isLightTheme) 0.30f else 0.55f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(
                        LocalLiquidBottomTabMinimized provides true,
                        LocalLiquidBottomTabScale provides { 1f },
                        LocalLiquidBottomTabTint provides (if (isStaticBar || accentColor == Color.White) null else ColorFilter.tint(accentColor)),
                        LocalLiquidBottomTabTextColor provides (if (isLightTheme) Color.Black else Color.White),
                        LocalContentColor provides (if (isLightTheme) Color.Black else Color.White)
                    ) {
                        ActiveTabSlotLayout(
                            activeTabIndex = nearestTabIndex,
                            scale = activeScale,
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize(),
                            content = content
                        )
                    }
                }
            }
        }
    }
}
