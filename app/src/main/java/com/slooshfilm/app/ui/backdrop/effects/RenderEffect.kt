package com.slooshfilm.app.ui.backdrop.effects

import androidx.compose.ui.graphics.RenderEffect
import com.slooshfilm.app.ui.backdrop.BackdropEffectScope
import com.slooshfilm.app.ui.backdrop.RuntimeShader
import com.slooshfilm.app.ui.backdrop.internal.RuntimeShaderEffect
import com.slooshfilm.app.ui.backdrop.internal.chain
import com.slooshfilm.app.ui.backdrop.isRenderEffectSupported
import com.slooshfilm.app.ui.backdrop.isRuntimeShaderSupported
import org.intellij.lang.annotations.Language
import kotlin.contracts.ExperimentalContracts

fun BackdropEffectScope.effect(effect: RenderEffect) {
    if (!isRenderEffectSupported()) return

    renderEffect = renderEffect.chain(effect)
}

@OptIn(ExperimentalContracts::class)
fun BackdropEffectScope.runtimeShaderEffect(
    key: String,
    @Language("AGSL") shaderString: String,
    uniformShaderName: String,
    block: RuntimeShader.() -> Unit
) {
    if (!isRuntimeShaderSupported()) return

    val effect =
        RuntimeShaderEffect(
            runtimeShader = obtainRuntimeShader(key, shaderString).apply(block),
            uniformShaderName = uniformShaderName
        )
    renderEffect = renderEffect.chain(effect)
}
