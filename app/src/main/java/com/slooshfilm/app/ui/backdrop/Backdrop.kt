package com.slooshfilm.app.ui.backdrop

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density

interface Backdrop {

    val isCoordinatesDependent: Boolean

    /**
     * true, если бэкдроп гарантированно ничего не рисует (например, когда стекло выключено).
     * Позволяет полностью пропустить запись offscreen-слоя стекла.
     */
    val isEmpty: Boolean get() = false

    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null
    )
}
