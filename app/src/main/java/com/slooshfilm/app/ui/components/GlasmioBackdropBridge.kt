package com.slooshfilm.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.slooshfilm.app.ui.backdrop.Backdrop as SlooshBackdrop
import io.github.neoruaa.glasmio.glass.render.Backdrop as GlasmioBackdrop

/**
 * Мост из движка sloosh (com.slooshfilm.app.ui.backdrop, Kyant 2.0.1) в вендоренный
 * движок glasmio-glass для ползунка навбара: pill рисуется ИХ drawPlainBackdrop +
 * nativeControlLens (дословный NativeControlLens), а сэмплирует ОН наш контент
 * (экран + записанный бар с акцентными иконками) через этот адаптер.
 * Интерфейсы движков идентичны по сигнатуре (один Kyant-родоначальник).
 */
private class SlooshToGlasmioBackdrop(
    val sloosh: SlooshBackdrop
) : GlasmioBackdrop {

    override val isCoordinatesDependent: Boolean
        get() = sloosh.isCoordinatesDependent

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)?
    ) {
        with(sloosh) {
            drawBackdrop(density, coordinates, layerBlock)
        }
    }
}

@Composable
fun rememberGlasmioBackdrop(slooshBackdrop: SlooshBackdrop): GlasmioBackdrop {
    return remember(slooshBackdrop) { SlooshToGlasmioBackdrop(slooshBackdrop) }
}
