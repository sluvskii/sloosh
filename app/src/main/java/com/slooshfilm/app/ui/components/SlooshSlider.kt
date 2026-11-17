package com.slooshfilm.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.ui.GlasmioSlider

/**
 * Ползунок sloosh на движке glasmio.
 *
 * Дорожку и стеклянный «палец» с линзовым преломлением рисует вендоренный [GlasmioSlider] —
 * тот же `nativeControlLens`, что у ползунка навбара. Это обычный непрерывный ползунок
 * (как в iOS `.slider`), а не адаптивный трёхъячеечный `GlasmioAdaptiveSlider`: последний
 * для «Размытия стекла» рисовал бы подписи 0/50/100% и сегменты, которых в настройках нет.
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
    GlasmioSlider(
        value = value.coerceIn(valueRange),
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        backdrop = backdrop ?: LocalGlassBackdrop.current,
    )
}
