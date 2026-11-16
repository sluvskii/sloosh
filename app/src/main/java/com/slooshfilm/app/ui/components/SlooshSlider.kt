package com.slooshfilm.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.ui.GlasmioAdaptiveSlider

/**
 * Слайдер sloosh на движке glasmio.
 *
 * Заменяет самописный `SlooshSlider` (удалён): дорожку, стеклянный «палец» с линзовым
 * преломлением и пружинную физику перетаскивания рисует вендоренный
 * [GlasmioAdaptiveSlider] — тот же движок `nativeControlLens`, что у ползунка навбара.
 */
@Composable
fun SlooshSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    backdrop: Backdrop? = null,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    GlasmioAdaptiveSlider(
        value = value.coerceIn(valueRange),
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        backdrop = backdrop ?: LocalGlassBackdrop.current,
    )
}
