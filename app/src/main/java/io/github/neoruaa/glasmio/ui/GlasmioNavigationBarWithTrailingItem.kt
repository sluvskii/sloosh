package io.github.neoruaa.glasmio.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.BottomNavigationContact
import io.github.neoruaa.glasmio.glass.GlassSurfaceStyle
import io.github.neoruaa.glasmio.glass.LocalBottomControlContact
import io.github.neoruaa.glasmio.glass.LocalBottomControlContactMemberActive
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.icons.SfSymbol
import kotlin.math.roundToInt

/**
 * Аутентичная нижняя панель навигации iOS 27 Glasmio с контактным жидким стеклом (BottomNavigationContact)
 * и отдельной круглой кнопкой поиска (trailingItem).
 */
@Composable
fun <T> GlasmioNavigationBarWithTrailingItem(
    items: List<GlasmioTabItem<T>>,
    trailingItem: GlasmioTabItem<T>,
    selectedKey: T,
    onSelected: (T) -> Unit,
    onExpand: () -> Unit,
    compactProgress: State<Float>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    detachTrailingItem: Boolean = true,
    compactSize: Dp = CompactBottomControlSize,
    maxMainWidth: Dp? = null,
    onItemBoundsChanged: ((T, Rect) -> Unit)? = null,
) {
    require(items.isNotEmpty()) { "At least one main navigation item is required." }
    require(items.none { it.key == trailingItem.key }) { "The trailing item key must be unique." }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val mainItems = remember(items, trailingItem, detachTrailingItem) {
        if (detachTrailingItem) items else items + trailingItem
    }
    val auxiliaryActive = remember(compactProgress, detachTrailingItem) {
        derivedStateOf { detachTrailingItem || compactProgress.value > 0f }
    }
    val auxiliaryContactActive = remember(auxiliaryActive) { { auxiliaryActive.value } }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    var sourceIconBounds by remember { mutableStateOf<Rect?>(null) }
    val sourceIconCenter = rememberUpdatedState(sourceIconBounds?.center?.minus(hostOrigin))
    BottomNavigationContact(
        backdrop = backdrop,
        modifier = modifier,
        mergedProgress = if (detachTrailingItem) null else compactProgress,
    ) {
        Layout(
            modifier = Modifier.weight(1f).onGloballyPositioned { hostOrigin = it.positionInWindow() },
            content = {
                GlasmioNavigationBar(
                    items = mainItems,
                    selectedKey = selectedKey,
                    onSelected = onSelected,
                    onExpand = onExpand,
                    compactProgress = compactProgress,
                    compactSize = compactSize,
                    backdrop = backdrop,
                    modifier = Modifier.zIndex(1f),
                    compactItem = if (!detachTrailingItem && selectedKey == trailingItem.key) items.first() else null,
                    onItemBoundsChanged = { key, bounds ->
                        if (key != trailingItem.key || detachTrailingItem || compactProgress.value <= 0f) {
                            onItemBoundsChanged?.invoke(key, bounds)
                        }
                    },
                    onItemIconBoundsChanged = { key, bounds ->
                        if (key == trailingItem.key && compactProgress.value <= 0f) sourceIconBounds = bounds
                    },
                )
                CompositionLocalProvider(LocalBottomControlContactMemberActive provides auxiliaryContactActive) {
                    FloatingNavigationControl(
                        compactProgress = compactProgress,
                        onClick = { onSelected(trailingItem.key) },
                        backdrop = backdrop,
                        compactSize = compactSize,
                        modifier = Modifier.zIndex(2f).graphicsLayer {
                            alpha = if (detachTrailingItem) 1f else compactProgress.value.coerceIn(0f, 1f)
                        },
                        itemSelected = trailingItem.key == selectedKey,
                        itemActive = auxiliaryActive.value,
                        itemContentDescription = trailingItem.contentDescription,
                        onBoundsChanged = { bounds -> onItemBoundsChanged?.invoke(trailingItem.key, bounds) },
                    ) { progress ->
                        SfIcon(
                            trailingItem.symbol,
                            contentDescription = trailingItem.contentDescription,
                            tint = LocalGlassColors.current.let { if (trailingItem.key == selectedKey) it.accent else it.content },
                            size = CompactBottomControlIconSize,
                            fontSize = 18.sp,
                            weight = FontWeight.Medium,
                            fontFeatureSettings = "'ss16' 1",
                            fitGlyphToBounds = false,
                            modifier = Modifier.graphicsLayer {
                                if (!detachTrailingItem) {
                                    translationY = ((sourceIconCenter.value?.y ?: size.height / 2f) -
                                        NavigationBarSurfaceHeight.toPx() / 2f) * (1f - progress)
                                }
                            },
                        )
                    }
                }
            },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = NavigationBarSurfaceHeight.roundToPx()
            val auxiliaryWidth = height
            val gap = 8.dp.roundToPx()
            val maxMainWidthPx = maxMainWidth?.let { with(density) { it.roundToPx() } }
            val availableMainWidth = if (detachTrailingItem) (width - auxiliaryWidth - gap).coerceAtLeast(1) else width
            val mainWidth = maxMainWidthPx?.let { minOf(it, availableMainWidth) } ?: availableMainWidth
            val main = measurables[0].measure(Constraints.fixed(mainWidth, height))
            val auxiliary = measurables[1].measure(Constraints.fixed(auxiliaryWidth, height))
            val progress = compactProgress.value.coerceIn(0f, 1f)
            val expandedCenter = sourceIconCenter.value?.x ?: if (isLtr) {
                width * (mainItems.lastIndex + .5f) / mainItems.size
            } else width * .5f / mainItems.size
            val sourceX = expandedCenter - auxiliaryWidth / 2f
            val targetX = if (isLtr) width - auxiliaryWidth.toFloat() else 0f
            val auxiliaryX = if (detachTrailingItem) targetX else sourceX + (targetX - sourceX) * progress
            layout(width, height) {
                main.place(if (isLtr) 0 else width - mainWidth, 0)
                if (detachTrailingItem || progress > 0f) auxiliary.place(auxiliaryX.roundToInt(), 0)
            }
        }
    }
}

@Composable
fun FloatingNavigationControl(
    compactProgress: State<Float>,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    compactSize: Dp = CompactBottomControlSize,
    itemSelected: Boolean? = null,
    itemActive: Boolean = true,
    itemContentDescription: String? = null,
    onBoundsChanged: ((Rect) -> Unit)? = null,
    content: @Composable (Float) -> Unit,
) {
    val contact = LocalBottomControlContact.current
    Box(
        modifier.size(NavigationBarSurfaceHeight)
            .then(contact?.takeIf { it.capturesLensSource.value }?.neighborRecordingModifier ?: Modifier)
            .then(if (!itemActive) Modifier.clearAndSetSemantics {} else if (itemSelected != null) Modifier.clearAndSetSemantics {
                role = Role.Tab
                selected = itemSelected
                itemContentDescription?.let { contentDescription = it }
                onClick { onClick(); true }
            } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val progress = compactProgress.value
        val controlSize = NavigationBarSurfaceHeight -
            (NavigationBarSurfaceHeight - compactSize) * progress
        GlasmioIconButton(
            onClick = onClick,
            enabled = itemActive,
            backdrop = backdrop,
            style = GlassSurfaceStyle.Navigation,
            nativeNavigationFlex = true,
            modifier = Modifier.size(controlSize)
                .onGloballyPositioned { coordinates ->
                    if (itemActive) onBoundsChanged?.invoke(coordinates.boundsInWindow())
                },
            morphProgress = compactProgress,
            morphCaptureWidth = NavigationBarSurfaceHeight,
        ) {
            content(progress)
        }
    }
}
