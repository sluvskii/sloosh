package io.github.neoruaa.glasmio.ui

/* VENDOR-PATCH(sloosh): экстракт из glasmio-ui/GlassControls.kt (снимок daa1b06, v0.1.3).
 * glasmio-ui целиком не вендорится (у приложения своя тема и свои контролы), но
 * вендоренный GlassBottomBar.kt ссылается на этот Modifier: он расширяет контрол
 * на время press/drag-контакта, чтобы линза nativeControlLens получала живую геометрию.
 * Текст функции — вербатим из апстрима. */

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import io.github.neoruaa.glasmio.glass.NativeControlLensStyle

internal fun Modifier.nativeControlLensGeometry(
    style: NativeControlLensStyle,
    horizontalProgress: () -> Float,
    verticalProgress: () -> Float = horizontalProgress,
): Modifier = layout { measurable, constraints ->
    val restingWidth = constraints.maxWidth
    val restingHeight = constraints.maxHeight
    val horizontal = horizontalProgress()
    val vertical = verticalProgress()
    val width = when (style) {
        NativeControlLensStyle.SwitchThumb -> restingWidth * (1f + (58f / 37f - 1f) * horizontal)
        NativeControlLensStyle.SliderThumb -> restingWidth * (1f + .55f * horizontal)
        NativeControlLensStyle.SegmentedIndicator -> restingWidth + 24.dp.toPx() * horizontal
        NativeControlLensStyle.TabBarIndicator -> restingWidth + 16.dp.toPx() * horizontal
    }.fastRoundToInt().coerceAtLeast(1)
    val height = when (style) {
        NativeControlLensStyle.SwitchThumb -> restingHeight * (1f + (38.333333f / 24f - 1f) * vertical)
        NativeControlLensStyle.SliderThumb -> restingHeight * (1f + .55f * vertical)
        NativeControlLensStyle.SegmentedIndicator,
        NativeControlLensStyle.TabBarIndicator -> restingHeight + 16.dp.toPx() * vertical
    }.fastRoundToInt().coerceAtLeast(1)
    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout(restingWidth, restingHeight) {
        placeable.place((restingWidth - width) / 2, (restingHeight - height) / 2)
    }
}
