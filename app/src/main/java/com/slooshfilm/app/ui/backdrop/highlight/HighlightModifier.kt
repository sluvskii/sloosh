package com.slooshfilm.app.ui.backdrop.highlight

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtMost
import com.slooshfilm.app.ui.backdrop.RuntimeShaderCacheImpl
import com.slooshfilm.app.ui.backdrop.internal.ShapeProvider
import com.slooshfilm.app.ui.backdrop.internal.blur
import com.slooshfilm.app.ui.backdrop.internal.clipOutline
import com.slooshfilm.app.ui.backdrop.internal.setRuntimeShader
import com.slooshfilm.app.ui.backdrop.isRuntimeShaderSupported
import kotlin.math.ceil

fun Modifier.highlight(
    shape: () -> Shape,
    highlight: () -> Highlight?
): Modifier = this.then(
    HighlightElement(
        shapeProvider = ShapeProvider(shape),
        highlight = highlight
    )
)

fun Modifier.highlight(
    shape: Shape,
    highlight: () -> Highlight?
): Modifier = highlight({ shape }, highlight)

internal class HighlightElement(
    val shapeProvider: ShapeProvider,
    val highlight: () -> Highlight?
) : ModifierNodeElement<HighlightNode>() {

    override fun create(): HighlightNode {
        return HighlightNode(shapeProvider, highlight)
    }

    override fun update(node: HighlightNode) {
        node.shapeProvider = shapeProvider
        node.highlight = highlight
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "highlight"
        properties["shapeProvider"] = shapeProvider
        properties["highlight"] = highlight
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HighlightElement) return false

        if (shapeProvider != other.shapeProvider) return false
        if (highlight != other.highlight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shapeProvider.hashCode()
        result = 31 * result + highlight.hashCode()
        return result
    }
}

internal class HighlightNode(
    var shapeProvider: ShapeProvider,
    var highlight: () -> Highlight?
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var highlightLayer: GraphicsLayer? = null

    private val paint =
        Paint().apply {
            style = PaintingStyle.Stroke
        }
    private var clipPath: Path? = null

    private val runtimeShaderCache = RuntimeShaderCacheImpl()

    private var prevStyle: HighlightStyle? = null

    /**
     * Ключ последней записи слоя подсветки. Контур, кисть и AGSL-шейдер зависят только
     * от геометрии и стиля, но не от [Highlight.alpha] — поэтому при анимациях прозрачности
     * слой не перезаписывается (GPU только меняет alpha готового слоя).
     */
    private var recordKey: HighlightRecordKey? = null

    override fun ContentDrawScope.draw() {
        val highlight = highlight()
        if (highlight == null || highlight.alpha <= 0f || highlight.width.value <= 0f) {
            return drawContent()
        }

        drawContent()

        val highlightLayer = highlightLayer
        if (highlightLayer != null) {
            val size = size
            val density: Density = this
            val layoutDirection = layoutDirection

            val safeSize =
                IntSize(
                    ceil(size.width).toInt() + 2,
                    ceil(size.height).toInt() + 2
                )

            val outline = shapeProvider.shape.createOutline(size, layoutDirection, density)
            val clipPath =
                if (outline is Outline.Rounded) {
                    clipPath ?: Path().also { clipPath = it }
                } else {
                    null
                }

            val key = HighlightRecordKey(
                width = size.width,
                height = size.height,
                density = density.density,
                layoutDirection = layoutDirection,
                outline = outline,
                strokeWidth = highlight.width,
                blurRadius = highlight.blurRadius,
                style = highlight.style
            )

            if (key != recordKey) {
                configurePaint(highlight)

                highlightLayer.record(safeSize) {
                    translate(1f, 1f) {
                        val canvas = drawContext.canvas
                        canvas.save()
                        canvas.clipOutline(outline, clipPath)
                        canvas.drawOutline(outline, paint)
                        canvas.restore()
                    }
                }

                recordKey = key
            }

            if (highlightLayer.alpha != highlight.alpha) {
                highlightLayer.alpha = highlight.alpha
            }
            if (highlightLayer.blendMode != highlight.style.blendMode) {
                highlightLayer.blendMode = highlight.style.blendMode
            }

            translate(-1f, -1f) {
                drawLayer(highlightLayer)
            }
        }
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        highlightLayer = graphicsContext.createGraphicsLayer()
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        highlightLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            highlightLayer = null
        }
        clipPath = null
        runtimeShaderCache.clear()
        prevStyle = null
        recordKey = null
    }

    private fun DrawScope.configurePaint(highlight: Highlight) {
        paint.color = highlight.style.color
        paint.strokeWidth = ceil(highlight.width.toPx().fastCoerceAtMost(size.minDimension / 2f)) * 2f
        paint.blur(highlight.blurRadius.toPx())
        if (isRuntimeShaderSupported()) {
            val shader =
                with(highlight.style) {
                    createShader(
                        shape = shapeProvider.shape,
                        runtimeShaderCache = runtimeShaderCache
                    )
                }
            paint.setRuntimeShader(shader)
        }
    }
}

private data class HighlightRecordKey(
    val width: Float,
    val height: Float,
    val density: Float,
    val layoutDirection: LayoutDirection,
    val outline: Outline,
    val strokeWidth: Dp,
    val blurRadius: Dp,
    val style: HighlightStyle
)
