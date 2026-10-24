package com.slooshfilm.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.util.fastCoerceIn
import com.slooshfilm.app.ui.backdrop.RuntimeShader
import com.slooshfilm.app.ui.backdrop.asComposeShader
import com.slooshfilm.app.ui.backdrop.isRuntimeShaderSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset },
    val onDragStart: ((down: PointerInputChange) -> Unit)? = null,
    val onDragEnd: ((change: PointerInputChange) -> Unit)? = null,
    val onDragCancel: (() -> Unit)? = null,
    val onDrag: ((change: PointerInputChange, dragAmount: Offset) -> Unit)? = null,
    val washAlpha: Float = 0.02f,
    val spotAlpha: Float = 0.05f
) {

    private val pressProgressAnimationSpec =
        spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec =
        spring(0.55f, 280f, Offset.VisibilityThreshold)

    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val positionAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    var startPosition = Offset.Zero
        private set
    private var lastDragTime = 0L
    private var lastDragPosition = Offset.Zero
    private var dragVelocity = Offset.Zero

    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = positionAnimation.value - startPosition

    private val shader =
        if (isRuntimeShaderSupported()) {
            RuntimeShader(
                """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""
            )
        } else {
            null
        }

    private val shaderBrush: ShaderBrush? = shader?.let { ShaderBrush(it.asComposeShader()) }

    val modifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressProgressAnimation.value
            if (progress > 0f) {
                if (shader != null && shaderBrush != null) {
                    if (washAlpha > 0f) {
                        drawRect(
                            Color.White.copy(washAlpha * progress),
                            blendMode = BlendMode.Plus
                        )
                    }
                    if (spotAlpha > 0f) {
                        shader.apply {
                            val position = position(size, positionAnimation.value)
                            setFloatUniform("size", size.width, size.height)
                            setColorUniform("color", Color.White.copy(spotAlpha * progress))
                            setFloatUniform("radius", size.minDimension * 1.5f)
                            setFloatUniform(
                                "position",
                                position.x.fastCoerceIn(0f, size.width),
                                position.y.fastCoerceIn(0f, size.height)
                            )
                        }
                        drawRect(
                            shaderBrush,
                            blendMode = BlendMode.Plus
                        )
                    }
                } else {
                    val fallbackAlpha = (washAlpha + spotAlpha).coerceAtMost(0.25f)
                    if (fallbackAlpha > 0f) {
                        drawRect(
                            Color.White.copy(fallbackAlpha * progress),
                            blendMode = BlendMode.Plus
                        )
                    }
                }
            }

            drawContent()
        }

    val gestureModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            inspectDragGestures(
                onDragStart = { down ->
                    startPosition = down.position
                    lastDragPosition = down.position
                    lastDragTime = down.uptimeMillis
                    dragVelocity = Offset.Zero
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                        launch { positionAnimation.snapTo(startPosition) }
                    }
                    onDragStart?.invoke(down)
                },
                onDragEnd = { change ->
                    val now = change.uptimeMillis
                    val isRecent = (now - lastDragTime) < 100L
                    val fling = if (isRecent) {
                        Offset(
                            dragVelocity.x.coerceIn(-2500f, 2500f),
                            dragVelocity.y.coerceIn(-2500f, 2500f)
                        )
                    } else {
                        Offset.Zero
                    }
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch {
                            positionAnimation.animateTo(
                                targetValue = startPosition,
                                animationSpec = positionAnimationSpec,
                                initialVelocity = fling
                            )
                        }
                    }
                    onDragEnd?.invoke(change)
                },
                onDragCancel = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                    onDragCancel?.invoke()
                }
            ) { change, dragAmount ->
                val now = change.uptimeMillis
                val dt = (now - lastDragTime).coerceAtLeast(1L)
                if (dt < 200L) {
                    val instantVelocity = (change.position - lastDragPosition) * (1000f / dt)
                    dragVelocity = dragVelocity * 0.4f + instantVelocity * 0.6f
                } else {
                    dragVelocity = Offset.Zero
                }
                lastDragTime = now
                lastDragPosition = change.position
                animationScope.launch { positionAnimation.snapTo(change.position) }
                onDrag?.invoke(change, dragAmount)
            }
        }
}
