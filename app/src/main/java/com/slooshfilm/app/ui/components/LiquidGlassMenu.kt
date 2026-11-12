package com.slooshfilm.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import com.slooshfilm.app.ui.sficons.SfIcon
import com.slooshfilm.app.R
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import com.slooshfilm.app.ui.theme.SlooshAccent
import com.slooshfilm.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Глобальный регулятор Liquid Glass Menu:
 * Единый источник правды для ВСЕХ слоёв меню (контейнер, бэкдроп, линза, контур, тень, плашка пункта).
 * Синхронизирует все радиусы и отступы, исключая расхождение слоёв.
 */
object LiquidMenuTokens {
    /** 
     * Базовый глобальный радиус скругления меню (эталонный Apple 26dp squircle).
     * Единый регулятор для внешнего контейнера, размытия, границы и тени.
     */
    var cornerRadius: Dp = 26.dp

    /** Внутренний горизонтальный отступ плашки пункта от края меню */
    var itemHorizontalPadding: Dp = 6.dp

    /** Внутренний вертикальный отступ пункта */
    var itemVerticalPadding: Dp = 2.dp

    /**
     * Концентрический радиус скругления плашки пункта:
     * Строго вычисляется от глобального радиуса меню: R_inner = R_menu - padding.
     * При cornerRadius = 26.dp и padding = 6.dp -> itemCornerRadius = 20.dp.
     * При любом изменении cornerRadius скругление пунктов автоматически остаётся
     * идеально концентрическим и пропорциональным внешнему меню!
     */
    val itemCornerRadius: Dp
        get() = (cornerRadius - itemHorizontalPadding).coerceAtLeast(4.dp)

    /** Высота строки каждого пункта меню */
    var itemHeight: Dp = 44.dp

    /** 
     * Глобальная минимальная ширина любого меню (эталонный Apple 220dp).
     * Гарантирует, что меню с коротким текстом (скорость, качество, переключатели)
     * не будут выглядеть сжатыми, узкими или куцыми, сохраняя единый гармоничный стиль.
     */
    var minMenuWidth: Dp = 220.dp

    /** Стандартная базовая ширина меню */
    var menuWidth: Dp = 220.dp

    /** 
     * Вертикальный внутренний отступ контейнера меню:
     * Строго равен (itemHorizontalPadding - itemVerticalPadding), чтобы суммарный внешний отступ
     * от края меню до плашки выделения (menuVerticalPadding + itemVerticalPadding) был
     * ИДЕАЛЬНО РАВЕН горизонтальному отступу (itemHorizontalPadding = 6dp) со ВСЕХ четырёх сторон!
     */
    val menuVerticalPadding: Dp
        get() = (itemHorizontalPadding - itemVerticalPadding).coerceAtLeast(0.dp)

    /** Единая форма для всех слоёв меню (бэкдроп, заливка, граница, тень) */
    val shape: Shape
        get() = RoundedRectangle(cornerRadius)

    /** Единая форма для внутренней плашки активного/нажатого пункта */
    val itemShape: Shape
        get() = RoundedRectangle(itemCornerRadius)

    /**
     * Фоновый тинт меню — 1:1 с модальными окнами (`LiquidGlassDialog`):
     * нейтральный дымчато-серый (iOS systemGray5, dark), а не почти чёрный.
     */
    var containerColor: Color = Color(0xFF2C2C2E)

    /**
     * Плотность тинта меню — ровно как у окон (0.62f): блюр и линза должны читаться,
     * но плотности хватает, чтобы текст не тонул в видео.
     */
    var containerAlpha: Float = 0.62f

    /** Дополнительный серый подслой поверх стекла — как у окон (0xFF1C1C1E @ 0.16). */
    val tintOverlay: Color = Color(0xFF1C1C1E).copy(alpha = 0.16f)

    /** Цвет подсветки пункта при нажатии/скраббинге */
    val itemHighlightColor: Color = Color.White.copy(alpha = 0.12f)
}

/**
 * Максимальная упругая деформация меню при перетаскивании.
 * Меню — крупная поверхность, поэтому тянется намеренно слабее кнопок,
 * ровно как модальные окна (`LiquidGlassDialog.DialogDragScale`).
 */
private const val MenuDragScale = 0.03f

/**
 * Насколько меню следует за пальцем при перетаскивании (1f — как у кнопок).
 * Как и окна, меню лишь слегка смещается за пальцем, а не «ездит» под ним.
 */
private const val MenuDragTranslation = 0.25f

/**
 * State holder for the active Liquid Glass pull-down context menu.
 */
@Stable
class LiquidMenuState {
    var isVisible by mutableStateOf(false)
        internal set

    var options by mutableStateOf<List<String>>(emptyList())
        internal set

    var itemIcons by mutableStateOf<List<Int?>?>(null)
        internal set

    var selectedIndex by mutableIntStateOf(-1)
        internal set

    var anchorBounds by mutableStateOf(Rect.Zero)
        internal set

    var customWidth by mutableStateOf<Dp?>(null)
        internal set

    var onSelectCallback by mutableStateOf<(Int) -> Unit>({})
        internal set

    fun show(
        options: List<String>,
        selectedIndex: Int,
        anchorBounds: Rect,
        customWidth: Dp? = null,
        itemIcons: List<Int?>? = null,
        onSelect: (Int) -> Unit
    ) {
        this.options = options
        this.itemIcons = itemIcons
        this.selectedIndex = selectedIndex
        this.anchorBounds = anchorBounds
        this.customWidth = customWidth
        this.onSelectCallback = onSelect
        this.isVisible = true
    }

    fun dismiss() {
        this.isVisible = false
    }
}

val LocalLiquidMenuHostState = staticCompositionLocalOf<LiquidMenuState?> { null }

/**
 * Trigger component for settings rows: renders the current selected value with double chevron (↕)
 * in SlooshAccent, records its screen coordinates, and triggers the Liquid Glass Menu on tap.
 * Features an authentic iOS tactile spring press compression.
 */
@Composable
fun SettingsMenuTrigger(
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val menuState = LocalLiquidMenuHostState.current
    val haptic = LocalHapticFeedback.current
    var anchorBounds by remember { mutableStateOf(Rect.Zero) }

    val selectedIndex = remember(selectedValue, options) {
        options.indexOf(selectedValue).takeIf { it >= 0 } ?: 0
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.75f),
        label = "triggerPressScale"
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = if (isPressed) 0.80f else 1f
            }
            .onGloballyPositioned { coordinates ->
                anchorBounds = coordinates.boundsInRoot()
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                menuState?.show(
                    options = options,
                    selectedIndex = selectedIndex,
                    anchorBounds = anchorBounds,
                    onSelect = { newIndex ->
                        if (newIndex in options.indices) {
                            onOptionSelected(options[newIndex])
                        }
                    }
                )
            }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = selectedValue,
            color = SlooshAccent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.2).sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        SfIcon(
            systemName = "chevron.up.chevron.down",
            contentDescription = null,
            tint = SlooshAccent.copy(alpha = 0.90f),
            size = 14.dp
        )
    }
}

/**
 * Root overlay host for rendering the authentic iOS Liquid Glass pull-down context menu.
 * Places the menu on a top-level z-layer above all cards and lists, allowing real AGSL
 * refraction of the underlying screen content via [backdrop].
 *
 * Kinematics & Physics:
 * - Liquid Glass Button Kinematics (liquidGlassInteractive): Consumes the exact Kyant liquid
 *   interactive mechanics as glass buttons (tanh elastic displacement, directional drag stretch,
 *   InteractiveHighlight radial glow ripple, velocity-based fling spring recoil).
 * - Static 1:1 Backdrop: The backdrop engine's InverseLayerScope automatically inverts layer transforms,
 *   guaranteeing the refracted screen image behind the glass remains 100% STATIC in screen space.
 * - Morphing Liquid Selection Pill: A smooth sliding highlight pill that follows the finger across items.
 * - Resting Purity: Strictly zero background box at rest, pure smoked glass with checkmark.
 */
@Composable
fun LiquidGlassMenuOverlay(
    state: LiquidMenuState,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val expansionAnim = remember { Animatable(0f) }

    // Morphing liquid selection pill physics:
    val pillOffsetY = remember { Animatable(0f) }
    val pillAlpha = remember { Animatable(0f) }

    var hoveredIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(state.isVisible) {
        if (state.isVisible) {
            hoveredIndex = -1 // Zero inner highlight in resting state (clean unbordered smoked glass)
            pillAlpha.snapTo(0f)

            expansionAnim.snapTo(0f)
            expansionAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    stiffness = 330f,
                    dampingRatio = 0.80f // Silky fluid flow with gentle ~2% liquid overshoot settling
                )
            )
        } else {
            hoveredIndex = -1
            launch { pillAlpha.animateTo(0f, spring(stiffness = 600f, dampingRatio = 0.90f)) }

            expansionAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    stiffness = 500f,
                    dampingRatio = 0.88f
                )
            )
        }
    }

    if (expansionAnim.value <= 0.001f && !state.isVisible) {
        return
    }

    val optionsCount = state.options.size
    if (optionsCount == 0) return

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        val itemHeightPx = with(density) { LiquidMenuTokens.itemHeight.toPx() }
        val itemHorizontalPaddingPx = with(density) { LiquidMenuTokens.itemHorizontalPadding.toPx() }
        val itemVerticalPaddingPx = with(density) { LiquidMenuTokens.itemVerticalPadding.toPx() }
        val menuVerticalPaddingPx = with(density) { LiquidMenuTokens.menuVerticalPadding.toPx() }
        val targetMenuWidthDp = maxOf(state.customWidth ?: LiquidMenuTokens.menuWidth, LiquidMenuTokens.minMenuWidth)
        val rawMenuWidthPx = with(density) { targetMenuWidthDp.toPx() }
        val maxAllowedWidthPx = screenWidthPx - with(density) { 32.dp.toPx() }
        val menuWidthPx = minOf(rawMenuWidthPx, maxAllowedWidthPx)

        val anchor = state.anchorBounds
        val activeIndex = state.selectedIndex.coerceIn(0, (optionsCount - 1).coerceAtLeast(0))

        val isBottomAnchored = anchor.bottom > screenHeightPx * 0.50f

        val totalContentHeightPx = optionsCount * itemHeightPx + menuVerticalPaddingPx * 2

        // When bottom-anchored, available vertical space is strictly between top margin (16dp) and anchor.bottom!
        // This guarantees the menu never expands downwards or climbs onto the seekbar / progress bar below anchor.bottom.
        val maxAllowedHeightPx = if (isBottomAnchored) {
            (anchor.bottom - with(density) { 16.dp.toPx() }).coerceIn(with(density) { 80.dp.toPx() }, screenHeightPx - with(density) { 32.dp.toPx() })
        } else {
            screenHeightPx - with(density) { 32.dp.toPx() }
        }
        val menuHeightPx = minOf(totalContentHeightPx, maxAllowedHeightPx)
        val isScrollable = totalContentHeightPx > maxAllowedHeightPx

        // Final spatial destination coordinates
        val finalTopPx: Float
        val finalBottomPx: Float
        if (isBottomAnchored) {
            // Strictly pin bottom edge to anchor.bottom so it never climbs onto the progress bar underneath
            finalBottomPx = anchor.bottom.coerceAtMost(screenHeightPx - with(density) { 16.dp.toPx() })
            finalTopPx = finalBottomPx - menuHeightPx
        } else {
            // Spatial Alignment (iOS 1:1): Center active item over trigger or align with top
            val activeItemCenterRelY = menuVerticalPaddingPx + activeIndex * itemHeightPx + itemHeightPx / 2f
            val rawTopPx = anchor.center.y - activeItemCenterRelY
            val minTopPx = with(density) { 16.dp.toPx() }
            val maxTopPx = (screenHeightPx - menuHeightPx - with(density) { 16.dp.toPx() }).coerceAtLeast(minTopPx)
            finalTopPx = rawTopPx.coerceIn(minTopPx, maxTopPx)
            finalBottomPx = finalTopPx + menuHeightPx
        }

        // Align left/right edge with anchor intelligently
        val finalLeftPx: Float
        val finalRightPx: Float
        if (anchor.center.x >= screenWidthPx * 0.50f) {
            val rawRightPx = anchor.right
            val minRightPx = with(density) { 16.dp.toPx() } + menuWidthPx
            val maxRightPx = screenWidthPx - with(density) { 16.dp.toPx() }
            finalRightPx = rawRightPx.coerceIn(minRightPx, maxRightPx)
            finalLeftPx = finalRightPx - menuWidthPx
        } else {
            val rawLeftPx = anchor.left
            val minLeftPx = with(density) { 16.dp.toPx() }
            val maxLeftPx = (screenWidthPx - menuWidthPx - with(density) { 16.dp.toPx() }).coerceAtLeast(minLeftPx)
            finalLeftPx = rawLeftPx.coerceIn(minLeftPx, maxLeftPx)
            finalRightPx = finalLeftPx + menuWidthPx
        }

        val scrollState = rememberScrollState()

        // Auto-scroll to selected item on open so active item is visible in long lists
        LaunchedEffect(state.isVisible, state.selectedIndex) {
            if (state.isVisible && optionsCount > 0 && activeIndex in 0 until optionsCount && isScrollable) {
                val targetScrollPx = ((activeIndex * itemHeightPx) - (maxAllowedHeightPx / 2f) + (itemHeightPx / 2f))
                    .coerceIn(0f, (totalContentHeightPx - maxAllowedHeightPx).coerceAtLeast(0f))
                scrollState.scrollTo(targetScrollPx.roundToInt())
            }
        }

        val progress = expansionAnim.value
        val progressClamped = progress.coerceIn(0f, 1f)

        // Non-linear fluid kinematic curves (Fast burst -> Viscous deceleration):
        val tPos = 1f - (1f - progressClamped) * (1f - progressClamped) * (1f - progressClamped)
        val tW = 1f - (1f - progressClamped) * (1f - progressClamped) * (1f - progressClamped)
        val tHBase = 1f - (1f - progressClamped) * (1f - progressClamped) * (1f - progressClamped)
        val liquidWave = sin(progressClamped * PI.toFloat()).coerceAtLeast(0f)
        val liquidSquish = liquidWave * 0.03f

        // Natural anchor footprint as the birth seed (emerging organically from the exact trigger capsule bounds):
        val birthWidthPx = if (anchor.width > 0f) anchor.width else menuWidthPx
        val birthHeightPx = if (anchor.height > 0f) anchor.height else itemHeightPx
        val birthRadiusDp = with(density) { (birthHeightPx / 2f).toDp() }

        val currentWidthPx = lerp(birthWidthPx, menuWidthPx, tW).coerceAtLeast(1f)
        val currentHeightPx = (lerp(birthHeightPx, menuHeightPx, tHBase) * (1f + liquidSquish)).coerceAtLeast(1f)

        val currentLeftPx: Float
        val currentTopPx: Float
        if (isBottomAnchored) {
            if (anchor.center.x >= screenWidthPx * 0.50f) {
                val curRightPx = lerp(anchor.right, finalRightPx, tPos)
                val curBottomPx = lerp(anchor.bottom, finalBottomPx, tPos)
                currentLeftPx = curRightPx - currentWidthPx
                currentTopPx = curBottomPx - currentHeightPx
            } else {
                val curLeftPx = lerp(anchor.left, finalLeftPx, tPos)
                val curBottomPx = lerp(anchor.bottom, finalBottomPx, tPos)
                currentLeftPx = curLeftPx
                currentTopPx = curBottomPx - currentHeightPx
            }
        } else {
            val targetCenterX = finalLeftPx + menuWidthPx / 2f
            val targetCenterY = finalTopPx + menuHeightPx / 2f
            val currentCenterX = lerp(anchor.center.x, targetCenterX, tPos)
            val currentCenterY = lerp(anchor.center.y, targetCenterY, tPos)
            currentLeftPx = currentCenterX - currentWidthPx / 2f
            currentTopPx = currentCenterY - currentHeightPx / 2f
        }

        val currentWidthDp = with(density) { currentWidthPx.toDp() }
        val currentHeightDp = with(density) { currentHeightPx.toDp() }

        // Dynamic morphing squircle radius (from trigger capsule radius to full 26dp squircle):
        val currentCornerRadius = birthRadiusDp + (LiquidMenuTokens.cornerRadius - birthRadiusDp) * tHBase
        val currentShape = RoundedRectangle(currentCornerRadius)

        // Dynamic optical shimmer during fluid blooming:
        val dynamicMenuStyle = remember(progressClamped, liquidWave) {
            if (progressClamped >= 0.999f) {
                LiquidGlassTokens.Menu
            } else {
                LiquidGlassTokens.Menu.copy(
                    refractionAmount = 12.dp + (8.dp * progressClamped) + (5.dp * liquidWave),
                    chromaticAberration = 0f,
                    highlightAlpha = lerp(0.55f, 0.45f, progressClamped) + (0.10f * liquidWave)
                )
            }
        }

        // Ultra-smooth Hermite smoothstep alpha curve (zero popping on frame 0/1):
        val rawAlpha = (progressClamped / 0.15f).coerceIn(0f, 1f)
        val alpha = rawAlpha * rawAlpha * (3f - 2f * rawAlpha)

        // Glass button interactive kinematics for the menu card:
        val interactiveHighlight = remember(scope, optionsCount, itemHeightPx, menuVerticalPaddingPx, density, menuHeightPx, scrollState) {
            var dragStartPos = Offset.Zero
            InteractiveHighlight(
                animationScope = scope,
                onDragStart = { down ->
                    dragStartPos = down.position
                    val startY = down.position.y - menuVerticalPaddingPx + scrollState.value
                    val initialIndex = (startY / itemHeightPx).toInt().coerceIn(0, optionsCount - 1)
                    hoveredIndex = initialIndex

                    val initialPillTop = menuVerticalPaddingPx + initialIndex * itemHeightPx + itemVerticalPaddingPx
                    scope.launch {
                        pillOffsetY.snapTo(initialPillTop)
                        pillAlpha.animateTo(1f, spring(stiffness = 600f, dampingRatio = 0.85f))
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onDrag = { change, _ ->
                    val cancelThresholdPx = with(density) { 110.dp.toPx() }
                    val isOutsideX = abs(change.position.x - dragStartPos.x) > cancelThresholdPx
                    val isOutsideY = change.position.y < -cancelThresholdPx ||
                        change.position.y > menuHeightPx + cancelThresholdPx
                    if (isOutsideX || isOutsideY) {
                        if (hoveredIndex != -1) {
                            hoveredIndex = -1
                            scope.launch {
                                pillAlpha.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.85f))
                            }
                        }
                    } else {
                        val currentY = change.position.y - menuVerticalPaddingPx + scrollState.value
                        val newIndex = (currentY / itemHeightPx).toInt().coerceIn(0, optionsCount - 1)
                        if (hoveredIndex != newIndex) {
                            hoveredIndex = newIndex
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val targetPillTop = menuVerticalPaddingPx + newIndex * itemHeightPx + itemVerticalPaddingPx
                            scope.launch {
                                pillOffsetY.animateTo(targetPillTop, spring(stiffness = 650f, dampingRatio = 0.82f))
                            }
                            scope.launch {
                                pillAlpha.animateTo(1f, spring(stiffness = 600f, dampingRatio = 0.85f))
                            }
                        }
                    }
                },
                onDragEnd = { _ ->
                    val selectedOptionIndex = hoveredIndex
                    hoveredIndex = -1
                    if (selectedOptionIndex in 0 until optionsCount) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        state.onSelectCallback(selectedOptionIndex)
                        state.dismiss()
                    } else {
                        scope.launch {
                            pillAlpha.animateTo(0f, spring(stiffness = 450f, dampingRatio = 0.85f))
                        }
                    }
                },
                onDragCancel = {
                    hoveredIndex = -1
                    scope.launch {
                        pillAlpha.animateTo(0f, spring(stiffness = 450f, dampingRatio = 0.85f))
                    }
                },
                washAlpha = 0.02f,
                spotAlpha = 0.05f
            )
        }

        // Full-screen touch shield to dismiss menu when tapping outside (100% transparent, zero background dimming)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.isVisible) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var hasDragged = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUp()) {
                                change.consume()
                                if (!hasDragged) {
                                    state.dismiss()
                                }
                                break
                            }
                            if (change.positionChange() != Offset.Zero) {
                                hasDragged = true
                            }
                            change.consume()
                        }
                    }
                }
        )

        val isGlassEnabled = LocalGlassEnabled.current
        val isLightTheme = !LocalSlooshColors.current.isDark
        // Тинт меню 1:1 с окнами: тот же серый systemGray5 и та же плотность 0.62.
        val menuContainerColor = when {
            isGlassEnabled -> if (isLightTheme) {
                Color.White.copy(alpha = 0.80f)
            } else {
                LiquidMenuTokens.containerColor.copy(alpha = LiquidMenuTokens.containerAlpha)
            }
            isLightTheme -> Color.White
            else -> Color(0xFF1E1E22)
        }
        val menuTintOverlay = if (isLightTheme) Color.Transparent else LiquidMenuTokens.tintOverlay
        val menuHighlightColor = if (isLightTheme) Color.Black.copy(alpha = 0.05f) else LiquidMenuTokens.itemHighlightColor

        // The Liquid Glass Menu Card (consumes genuine liquidGlassInteractive kinematics & tokens)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(currentLeftPx.roundToInt(), currentTopPx.roundToInt())
                }
                .width(currentWidthDp)
                .height(currentHeightDp)
                .graphicsLayer {
                    this.alpha = alpha
                }
                .liquidGlassInteractive(
                    interactiveHighlight = interactiveHighlight,
                    style = dynamicMenuStyle,
                    shape = currentShape,
                    containerColor = menuContainerColor,
                    backdrop = backdrop,
                    maxDragScale = MenuDragScale,
                    dragTranslationFactor = MenuDragTranslation,
                    onDrawExtraSurface = {
                        if (menuTintOverlay.alpha > 0f) {
                            drawRect(menuTintOverlay)
                        }
                    }
                )
                .clip(currentShape)
        ) {
            // Layer 1: Selection Pill (matches active item bounds perfectly)
            val currentPillAlpha = pillAlpha.value
            if (currentPillAlpha > 0.001f) {
                val pillLeftPx = itemHorizontalPaddingPx
                val pillTopPx = pillOffsetY.value - scrollState.value
                val pillWidthDp = with(density) { (menuWidthPx - itemHorizontalPaddingPx * 2).toDp() }
                val pillHeightDp = with(density) { (itemHeightPx - itemVerticalPaddingPx * 2).toDp() }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(pillLeftPx.roundToInt(), pillTopPx.roundToInt())
                        }
                        .width(pillWidthDp)
                        .height(pillHeightDp)
                        .graphicsLayer {
                            this.alpha = currentPillAlpha
                        }
                        .clip(LiquidMenuTokens.itemShape)
                        .background(menuHighlightColor)
                )
            }

            // Layer 2: Menu Options (Text & Checkmarks)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier)
                    .padding(vertical = LiquidMenuTokens.menuVerticalPadding)
            ) {
                state.options.forEachIndexed { index, option ->
                    val isSelected = index == state.selectedIndex
                    val distFromActive = abs(index - activeIndex)
                    val itemProgress = ((progressClamped - 0.10f - distFromActive * 0.035f) / 0.70f).coerceIn(0f, 1f)
                    val itemEase = 1f - (1f - itemProgress) * (1f - itemProgress)
                    val itemAlpha = itemEase
                    val itemOffsetY = ((1f - itemEase) * (4f + distFromActive * 2f)).dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LiquidMenuTokens.itemHeight)
                            .padding(horizontal = LiquidMenuTokens.itemHorizontalPadding)
                            .padding(horizontal = 12.dp)
                            .graphicsLayer {
                                this.alpha = itemAlpha
                                this.translationY = with(density) { itemOffsetY.toPx() }
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                state.onSelectCallback(index)
                                state.dismiss()
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.size(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    SfIcon(
                                        systemName = "checkmark",
                                        contentDescription = null,
                                        tint = TextPrimary,
                                        size = 18.dp
                                    )
                                }
                            }

                            val iconResId = state.itemIcons?.getOrNull(index)
                            if (iconResId != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                ) {
                                    Image(
                                        painter = painterResource(iconResId),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(
                                                width = 0.5.dp,
                                                color = Color.White.copy(alpha = 0.14f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            } else {
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            Text(
                                text = option,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
