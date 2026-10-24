package com.slooshfilm.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.annotation.FloatRange
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.LayoutDirection
import com.slooshfilm.app.ui.backdrop.RuntimeShader
import com.slooshfilm.app.ui.backdrop.RuntimeShaderCache
import com.slooshfilm.app.ui.backdrop.asComposeShader
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.PI
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.backdrop.BackdropEffectScope
import com.slooshfilm.app.ui.backdrop.backdrops.emptyBackdrop
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.backdrop.effects.blur
import com.slooshfilm.app.ui.backdrop.effects.colorControls
import com.slooshfilm.app.ui.backdrop.effects.lens
import com.slooshfilm.app.ui.backdrop.effects.runtimeShaderEffect
import com.slooshfilm.app.ui.backdrop.effects.vibrancy
import com.slooshfilm.app.ui.backdrop.highlight.Highlight
import com.slooshfilm.app.ui.backdrop.highlight.HighlightStyle
import com.slooshfilm.app.ui.backdrop.isRuntimeShaderSupported
import com.slooshfilm.app.ui.backdrop.shadow.InnerShadow
import com.slooshfilm.app.ui.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.RoundedRectangularShape
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import com.slooshfilm.app.ui.theme.LiquidGlassSurfaceDark
import com.slooshfilm.app.ui.theme.LiquidGlassSurfaceLight
import android.os.Build
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.BlendMode as AndroidBlendMode
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader.TileMode as AndroidTileMode
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import org.intellij.lang.annotations.Language

val SlooshCapsule: Shape = Capsule()
val LocalProgressiveBlur = compositionLocalOf { true }
val LocalGlassBlurMultiplier = compositionLocalOf { 0.70f }
val LocalGlassEnabled = compositionLocalOf { true }

/**
 * Единая спецификация прогрессивного блюра шапок (iOS: `VariableBlurView`).
 *
 * Радиус пика одинаковый на всех экранах, а зона затухания всегда равна высоте полосы шапки,
 * поэтому блюр везде гаснет до нуля ровно к её нижней границе (без жёсткого обрыва).
 */
object ProgressiveBlurTokens {
    /** Пиковый радиус размытия — один и тот же на всех экранах. */
    val MaxRadius = 4.dp

    /**
     * Продолжение полосы блюра под шапкой.
     * iOS: `VariableBlurView(...).padding(.bottom, -30)` — блюр выходит за пределы бара на 30pt.
     */
    val Extension = 30.dp
}

fun computeNoGlassContainerColor(customColor: Color?, isLightTheme: Boolean): Color {
    if (customColor != null && customColor != LiquidGlassSurfaceLight && customColor != LiquidGlassSurfaceDark) {
        if (customColor.alpha >= 0.70f) {
            return customColor
        }
        return if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF242428)
    }
    return if (isLightTheme) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)
}

fun computeNoGlassButtonColor(customColor: Color?, isLightTheme: Boolean): Color {
    return computeNoGlassContainerColor(customColor, isLightTheme)
}

fun computeEffectiveBlurOpacity(baseOpacity: Float, multiplier: Float): Float {
    val p = multiplier.coerceIn(0f, 1f)
    if (p <= 0.001f) return 0f
    return if (p <= 0.50f) {
        (p / 0.50f) * baseOpacity
    } else {
        val t = (p - 0.50f) / 0.50f
        val smoothT = t * t * (3f - 2f * t)
        lerp(baseOpacity, 1.0f, smoothT)
    }
}

fun computeEffectiveContainerColor(baseColor: Color, multiplier: Float): Color {
    val p = multiplier.coerceIn(0f, 1f)
    val baseAlpha = baseColor.alpha
    val isDarkBase = baseColor == LiquidGlassSurfaceDark ||
        (baseColor.red >= 0.85f && baseColor.green >= 0.85f && baseColor.blue >= 0.85f && baseAlpha <= 0.15f)

    if (!isDarkBase) {
        return if (p <= 0.50f) {
            baseColor.copy(alpha = (baseAlpha * (p / 0.50f)).coerceIn(0f, 1f))
        } else {
            val t = (p - 0.50f) / 0.50f
            val smoothT = t * t * (3f - 2f * t)
            baseColor.copy(alpha = lerp(baseAlpha, 0.95f, smoothT))
        }
    }

    // Тёмная тема:
    // До 0.50 (50%): лёгкий кристальный отблеск (alpha до 0.05f).
    // Начиная с 0.50 (50%) и плавно до 1.00 (100%): мягко нарастает благородный тёмный тинт
    // (дымчатое матовое стекло, lerp в Color(0xFF141417) с alpha до 0.42f).
    // Благодаря широкому диапазону 50%-100% и smoothstep-интерполяции затемнение происходит максимально естественно.
    if (p <= 0.50f) {
        val progressFactor = p / 0.50f
        return baseColor.copy(alpha = (baseAlpha * progressFactor).coerceIn(0f, 1f))
    }

    val t = ((p - 0.50f) / 0.50f).coerceIn(0f, 1f)
    val smoothT = t * t * (3f - 2f * t)
    return Color(
        red = lerp(baseColor.red, 0.08f, smoothT),
        green = lerp(baseColor.green, 0.08f, smoothT),
        blue = lerp(baseColor.blue, 0.09f, smoothT),
        alpha = lerp(baseAlpha, 0.42f, smoothT)
    )
}

@Language("AGSL")
private const val IosVibrancyShaderString = """
uniform shader content;
uniform float exposure;
uniform float saturation;
uniform float highlightRolloff;
uniform float contrast;
uniform float highlightBoost;
uniform float isLight;

// Apple Satin Micro-Dither: breaks color banding on 8-bit OLED buffers
// and creates an authentic frosted satin glass micro-texture.
float satinDither(float2 p) {
    float h1 = fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    float h2 = fract(sin(dot(p + float2(0.5, 0.5), float2(39.346, 11.135))) * 23421.631);
    return (h1 + h2 - 1.0) * 0.0075;
}

half4 main(float2 coord) {
    half4 c = content.eval(coord);
    if (c.a <= 0.0) {
        return c;
    }
    
    half3 rgb = c.rgb / max(c.a, 0.001);
    
    // 1. Воспринимаемая яркость пикселя фона (Rec. 709 Luma)
    half rawLuma = dot(rgb, half3(0.2126, 0.7152, 0.0722));
    
    // 2. Двухфазная экспозиция:
    // - В тёмной теме (isLight <= 0.5): приглушаем светлые пятна фона (до 0.80x), предотвращая клиппинг.
    // - В светлой теме (isLight > 0.5): сохраняем кристально чистый белый свет (1.05x), не допуская посерения стекла.
    half brightFactor = isLight > 0.5 ? smoothstep(0.50, 0.95, rawLuma) : smoothstep(0.40, 0.90, rawLuma);
    half adaptiveExposure = isLight > 0.5 ? mix(exposure, 1.05, brightFactor) : mix(exposure, 0.80, brightFactor);
    rgb = rgb * adaptiveExposure;
    
    // 3. Мягкая компрессия пиков (Quadratic Soft-Knee Rolloff)
    half rolloff = isLight > 0.5 ? (highlightRolloff * 0.05) : highlightRolloff;
    half3 knee = rgb * rgb * rolloff;
    rgb = rgb / (half3(1.0) + knee / (half3(1.0) + rgb));
    
    // 4. Контрастность
    half effContrast = isLight > 0.5 ? 1.0 : contrast;
    rgb = pow(max(rgb, half3(0.0)), half3(effContrast));
    
    // 5. Интеллектуальная сочность (Apple Vibrancy)
    half maxC = max(rgb.r, max(rgb.g, rgb.b));
    half minC = min(rgb.r, min(rgb.g, rgb.b));
    half currentSat = (maxC - minC) / max(maxC, 0.001);
    half vibFactor = saturation * (1.0 + (1.0 - currentSat) * 0.35);
    
    half luma = dot(rgb, half3(0.2126, 0.7152, 0.0722));
    rgb = mix(half3(luma), rgb, vibFactor);
    
    // 6. Сатиновый микро-шум / дизеринг против бандинга на темных градиентах (Apple Frosted Satin)
    rgb += half3(satinDither(coord));
    
    rgb = clamp(rgb, 0.0, 1.0);
    return half4(rgb * c.a, c.a);
}
"""

fun BackdropEffectScope.iosVibrantGlass(
    exposure: Float = 1.18f,
    saturation: Float = 1.22f,
    highlightRolloff: Float = 0.28f,
    contrast: Float = 1.02f,
    highlightBoost: Float = 0.20f,
    isLight: Boolean = false
) {
    if (isRuntimeShaderSupported()) {
        runtimeShaderEffect(
            key = "IosVibrancyShader",
            shaderString = IosVibrancyShaderString,
            uniformShaderName = "content"
        ) {
            setFloatUniform("exposure", if (isLight) 1.04f else exposure)
            setFloatUniform("saturation", saturation)
            setFloatUniform("highlightRolloff", if (isLight) 0.02f else highlightRolloff)
            setFloatUniform("contrast", if (isLight) 1.0f else contrast)
            setFloatUniform("highlightBoost", highlightBoost)
            setFloatUniform("isLight", if (isLight) 1.0f else 0.0f)
        }
    } else {
        colorControls(
            contrast = if (isLight) 1.0f else contrast,
            saturation = saturation
        )
    }
}

@Language("AGSL")
private const val IosLensShaderString = """
uniform shader content;

uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
uniform float chromaticAberration;

float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = (coord + offset) - halfSize;
    float radius = radiusAt(coord, cornerRadii);
    
    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);
    
    float edgeFactor = 1.0 - clamp(-sd / max(refractionHeight, 0.001), 0.0, 1.0);
    float d = circleMap(edgeFactor) * refractionAmount;
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord));
    
    float2 refractedCoord = coord + d * grad;
    
    // Subtle Fresnel rim reflection: delicate optical edge without standing out
    float fresnel = pow(edgeFactor, 4.5) * 0.07;
    
    if (chromaticAberration <= 0.0) {
        half4 color = content.eval(refractedCoord);
        color.rgb += half3(fresnel) * (half3(1.0) - color.rgb * 0.35);
        return color;
    }
    
    // Physically authentic isotropic chromatic dispersion along the surface gradient normal
    float dispersionIntensity = chromaticAberration * edgeFactor;
    float2 dispersedCoord = grad * (d * dispersionIntensity * 0.75);
    
    half4 color = half4(0.0);
    
    half4 red = content.eval(refractedCoord + dispersedCoord);
    color.r += red.r / 3.5;
    color.a += red.a / 7.0;
    
    half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
    color.r += orange.r / 3.5;
    color.g += orange.g / 7.0;
    color.a += orange.a / 7.0;
    
    half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
    color.r += yellow.r / 3.5;
    color.g += yellow.g / 3.5;
    color.a += yellow.a / 7.0;
    
    half4 green = content.eval(refractedCoord);
    color.g += green.g / 3.5;
    color.a += green.a / 7.0;
    
    half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
    color.g += cyan.g / 3.5;
    color.b += cyan.b / 3.0;
    color.a += cyan.a / 7.0;
    
    half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
    color.b += blue.b / 3.0;
    color.a += blue.a / 7.0;
    
    half4 purple = content.eval(refractedCoord - dispersedCoord);
    color.r += purple.r / 7.0;
    color.b += purple.b / 3.0;
    color.a += purple.a / 7.0;
    
    // Apply Fresnel rim sheen to dispersed optical glass
    color.rgb += half3(fresnel) * (half3(1.0) - color.rgb * 0.35);
    
    return color;
}
"""

fun BackdropEffectScope.iosLens(
    @FloatRange(from = 0.0) refractionHeight: Float,
    @FloatRange(from = 0.0) refractionAmount: Float,
    depthEffect: Boolean = false,
    @FloatRange(from = 0.0) chromaticAberration: Float = 0f
) {
    if (!isRuntimeShaderSupported()) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return

    if (padding > 0f) {
        padding = (padding - refractionHeight).coerceAtLeast(0f)
    }

    val isSizeValid = size != Size.Unspecified && size.width.isFinite() && size.height.isFinite()
    val safeW = if (isSizeValid) size.width else 100f
    val safeH = if (isSizeValid) size.height else 100f
    val safeMinDim = if (isSizeValid) size.minDimension else 100f
    val maxRadius = safeMinDim / 2f
    val cornerRadii = when (val s = shape) {
        is Capsule -> floatArrayOf(maxRadius, maxRadius, maxRadius, maxRadius)
        is RoundedRectangularShape -> {
            val corners = s.corners(size, layoutDirection, this)
            floatArrayOf(
                corners.topLeft.fastCoerceAtMost(maxRadius),
                corners.topRight.fastCoerceAtMost(maxRadius),
                corners.bottomRight.fastCoerceAtMost(maxRadius),
                corners.bottomLeft.fastCoerceAtMost(maxRadius)
            )
        }
        is AbsoluteRoundedCornerShape -> {
            val topLeft = s.topStart.toPx(size, this)
            val topRight = s.topEnd.toPx(size, this)
            val bottomRight = s.bottomEnd.toPx(size, this)
            val bottomLeft = s.bottomStart.toPx(size, this)
            floatArrayOf(
                topLeft.fastCoerceAtMost(maxRadius),
                topRight.fastCoerceAtMost(maxRadius),
                bottomRight.fastCoerceAtMost(maxRadius),
                bottomLeft.fastCoerceAtMost(maxRadius)
            )
        }
        is CornerBasedShape -> {
            val isLtr = layoutDirection == LayoutDirection.Ltr
            val topLeft = if (isLtr) s.topStart.toPx(size, this) else s.topEnd.toPx(size, this)
            val topRight = if (isLtr) s.topEnd.toPx(size, this) else s.topStart.toPx(size, this)
            val bottomRight = if (isLtr) s.bottomEnd.toPx(size, this) else s.bottomStart.toPx(size, this)
            val bottomLeft = if (isLtr) s.bottomStart.toPx(size, this) else s.bottomEnd.toPx(size, this)
            floatArrayOf(
                topLeft.fastCoerceAtMost(maxRadius),
                topRight.fastCoerceAtMost(maxRadius),
                bottomRight.fastCoerceAtMost(maxRadius),
                bottomLeft.fastCoerceAtMost(maxRadius)
            )
        }
        else -> floatArrayOf(maxRadius, maxRadius, maxRadius, maxRadius)
    }

    runtimeShaderEffect(
        key = "IosLensShader",
        shaderString = IosLensShaderString,
        uniformShaderName = "content"
    ) {
        setFloatUniform("size", safeW, safeH)
        setFloatUniform("offset", -padding, -padding)
        setFloatUniform("cornerRadii", cornerRadii)
        setFloatUniform("refractionHeight", refractionHeight)
        setFloatUniform("refractionAmount", -refractionAmount)
        setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
        setFloatUniform("chromaticAberration", chromaticAberration)
    }
}

/**
 * Аутентичный полупрозрачный блюр в стиле iOS:
 * Размытие подмешивается поверх резкого исходного фона с пониженной плотностью (80% блюра + 20% резкости),
 * при этом результирующий альфа-канал слоя остаётся 100% сплошным (alpha = 1.0).
 * Благодаря этому линза и преломления остаются абсолютно монолитными, чёткими и не просвечивают насквозь!
 */
fun BackdropEffectScope.iosFrostedBlur(
    radius: Float,
    blurOpacity: Float = 0.72f
) {
    if (radius <= 0f || blurOpacity <= 0.001f) return
    if (radius > padding) {
        padding = radius
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurOpacity < 0.999f) {
        val sharpEffect = AndroidRenderEffect.createOffsetEffect(0f, 0f)
        val blurEffect = AndroidRenderEffect.createBlurEffect(
            radius,
            radius,
            AndroidTileMode.CLAMP
        )

        val alphaMatrix = ColorMatrix().apply {
            setScale(1f, 1f, 1f, blurOpacity.coerceIn(0f, 1f))
        }
        val alphaFilter = ColorMatrixColorFilter(alphaMatrix)
        val blurWithAlpha = AndroidRenderEffect.createColorFilterEffect(alphaFilter, blurEffect)

        val blendedEffect = AndroidRenderEffect.createBlendModeEffect(
            sharpEffect,
            blurWithAlpha,
            AndroidBlendMode.SRC_OVER
        )

        renderEffect = blendedEffect.asComposeRenderEffect()
    } else {
        blur(radius)
    }
}

/**
 * Аутентичный двойной многоуровневый блюр Apple (Compound Dual-Radius Blur):
 * Комбинирует базовое мягкое размытие малого радиуса [primaryRadius] (сохраняющее читаемые,
 * красивые очертания силуэтов и ярких элементов) с диффузным широким ореолом [secondaryRadius]
 * на прозрачности [blurOpacity]. Точно воссоздаёт фирменную оптику плотного дымчатого стекла iOS.
 */
fun BackdropEffectScope.iosDualFrostedBlur(
    primaryRadius: Float,
    secondaryRadius: Float,
    blurOpacity: Float = 0.72f
) {
    val maxRadius = maxOf(primaryRadius, secondaryRadius)
    if (maxRadius <= 0f || blurOpacity <= 0.001f) return
    if (maxRadius > padding) {
        padding = maxRadius
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val primaryBlur = AndroidRenderEffect.createBlurEffect(
            primaryRadius,
            primaryRadius,
            AndroidTileMode.CLAMP
        )
        val secondaryBlur = AndroidRenderEffect.createBlurEffect(
            secondaryRadius,
            secondaryRadius,
            AndroidTileMode.CLAMP
        )

        val alphaMatrix = ColorMatrix().apply {
            setScale(1f, 1f, 1f, blurOpacity.coerceIn(0f, 1f))
        }
        val alphaFilter = ColorMatrixColorFilter(alphaMatrix)
        val secondaryWithAlpha = AndroidRenderEffect.createColorFilterEffect(alphaFilter, secondaryBlur)

        val blendedEffect = AndroidRenderEffect.createBlendModeEffect(
            primaryBlur,
            secondaryWithAlpha,
            AndroidBlendMode.SRC_OVER
        )

        renderEffect = blendedEffect.asComposeRenderEffect()
    } else {
        blur(maxRadius)
    }
}

@Language("AGSL")
private const val IosProgressiveBlurHShaderString = """
uniform shader content;

uniform float2 size;
uniform float padding;
uniform float maxRadius;
uniform float topOffset;
uniform float falloffDistance;
uniform float curvePower;

half4 main(float2 coord) {
    float localY = coord.y - padding;
    float t = clamp((localY - topOffset) / max(falloffDistance, 1.0), 0.0, 1.0);
    float smoothT = t * t * (3.0 - 2.0 * t);
    float progress = pow(clamp(1.0 - smoothT, 0.0, 1.0), curvePower);
    float r = maxRadius * progress;
    
    // Fast path: if blur radius is near zero, return sharp content directly
    if (progress <= 0.001 || r <= 0.5) {
        return content.eval(coord);
    }
    
    half4 accum = half4(0.0);
    float totalW = 0.0;
    
    // 9-tap continuous horizontal Gaussian pass (120 FPS high-efficiency)
    for (int k = -4; k <= 4; ++k) {
        float fk = float(k);
        float distNorm = fk * 0.25;
        float2 offset = float2(distNorm * r, 0.0);
        float w = exp(-2.0 * distNorm * distNorm);
        accum += content.eval(coord + offset) * half(w);
        totalW += w;
    }
    
    return accum / half(max(totalW, 0.0001));
}
"""

@Language("AGSL")
private const val IosProgressiveBlurVShaderString = """
uniform shader content;

uniform float2 size;
uniform float padding;
uniform float maxRadius;
uniform float topOffset;
uniform float falloffDistance;
uniform float curvePower;
uniform float exposure;
uniform float saturation;
uniform float highlightRolloff;
uniform float contrast;
uniform float isLight;

half4 main(float2 coord) {
    float localY = coord.y - padding;
    float t = clamp((localY - topOffset) / max(falloffDistance, 1.0), 0.0, 1.0);
    float smoothT = t * t * (3.0 - 2.0 * t);
    float progress = pow(clamp(1.0 - smoothT, 0.0, 1.0), curvePower);
    float r = maxRadius * progress;
    
    // Fast path: if blur radius is near zero, return sharp content directly
    if (progress <= 0.001 || r <= 0.5) {
        return content.eval(coord);
    }
    
    half4 accum = half4(0.0);
    float totalW = 0.0;
    
    // 9-tap continuous vertical Gaussian pass (120 FPS high-efficiency)
    for (int k = -4; k <= 4; ++k) {
        float fk = float(k);
        float distNorm = fk * 0.25;
        float2 offset = float2(0.0, distNorm * r);
        float w = exp(-2.0 * distNorm * distNorm);
        accum += content.eval(coord + offset) * half(w);
        totalW += w;
    }
    half4 c = accum / half(max(totalW, 0.0001));
    
    if (c.a <= 0.0) {
        return c;
    }
    
    half3 rgb = c.rgb / max(c.a, 0.001);
    
    // Apple Vibrancy & Exposure gracefully scaled with progress to zero at bottom
    half rawLuma = dot(rgb, half3(0.2126, 0.7152, 0.0722));
    half brightFactor = isLight > 0.5 ? smoothstep(0.50, 0.95, rawLuma) : smoothstep(0.40, 0.90, rawLuma);
    half targetExposure = isLight > 0.5 ? mix(half(exposure), half(1.02), brightFactor) : mix(half(exposure), half(0.80), brightFactor);
    half currentExposure = mix(half(1.0), targetExposure, half(progress));
    rgb = rgb * currentExposure;
    
    half rolloff = isLight > 0.5 ? (highlightRolloff * 0.05) : highlightRolloff;
    half3 knee = rgb * rgb * half(rolloff * progress);
    rgb = rgb / (half3(1.0) + knee / (half3(1.0) + rgb));
    
    half effContrast = isLight > 0.5 ? 1.0 : contrast;
    half currentContrast = mix(half(1.0), half(effContrast), half(progress));
    rgb = pow(max(rgb, half3(0.0)), half3(currentContrast));
    
    half maxC = max(rgb.r, max(rgb.g, rgb.b));
    half minC = min(rgb.r, min(rgb.g, rgb.b));
    half currentSat = (maxC - minC) / max(maxC, 0.001);
    half targetVib = half(saturation) * (half(1.0) + (half(1.0) - currentSat) * half(0.35));
    half vibFactor = mix(half(1.0), targetVib, half(progress));
    
    half luma = dot(rgb, half3(0.2126, 0.7152, 0.0722));
    rgb = mix(half3(luma), rgb, vibFactor);
    
    rgb = clamp(rgb, 0.0, 1.0);
    return half4(rgb * c.a, c.a);
}
"""

/**
 * Аутентичный Apple TV+ прогрессивный блюр (Progressive / Variable Blur):
 * Раздельный двухпроходный гауссов блюр (Separable 1D Horizontal + 1D Vertical),
 * дающий эффективное ядро в 289 точек (17x17) без артефактов дискретности, колец или ребристости.
 * Физический радиус размытия плавно снижается от [maxRadius] до 0.0 вдоль вертикальной оси Y.
 */
fun BackdropEffectScope.iosProgressiveBlur(
    maxRadius: Float = ProgressiveBlurTokens.MaxRadius.toPx(),
    topOffset: Float = 0f,
    falloffDistance: Float = if (size != Size.Unspecified && size.height.isFinite()) (size.height - topOffset).coerceAtLeast(1f) else 100f,
    curvePower: Float = 1.5f,
    exposure: Float = 1.0f,
    saturation: Float = 1.05f,
    highlightRolloff: Float = 0.15f,
    contrast: Float = 1.0f,
    isLight: Boolean = false
) {
    if (!maxRadius.isFinite() || maxRadius <= 0f) return
    if (maxRadius > padding) {
        padding = maxRadius
    }

    if (isRuntimeShaderSupported()) {
        val isSizeValid = size != Size.Unspecified && size.width.isFinite() && size.height.isFinite()
        val safeW = if (isSizeValid) size.width else 1f
        val safeH = if (isSizeValid) size.height else 1f
        val safeTop = if (topOffset.isFinite()) topOffset else 0f
        val safeFalloff = if (falloffDistance.isFinite() && falloffDistance > 0f) falloffDistance else safeH
        val safeCurvePower = if (curvePower.isFinite() && curvePower > 0f) curvePower else 1.5f

        // Проход 1: 1D непрерывное горизонтальное размытие вдоль оси X
        runtimeShaderEffect(
            key = "IosProgressiveBlurH",
            shaderString = IosProgressiveBlurHShaderString,
            uniformShaderName = "content"
        ) {
            setFloatUniform("size", safeW, safeH)
            setFloatUniform("padding", padding)
            setFloatUniform("maxRadius", maxRadius)
            setFloatUniform("topOffset", safeTop)
            setFloatUniform("falloffDistance", safeFalloff)
            setFloatUniform("curvePower", safeCurvePower)
        }

        // Проход 2: 1D непрерывное вертикальное размытие вдоль оси Y + Apple Vibrancy & Exposure
        runtimeShaderEffect(
            key = "IosProgressiveBlurV",
            shaderString = IosProgressiveBlurVShaderString,
            uniformShaderName = "content"
        ) {
            setFloatUniform("size", safeW, safeH)
            setFloatUniform("padding", padding)
            setFloatUniform("maxRadius", maxRadius)
            setFloatUniform("topOffset", safeTop)
            setFloatUniform("falloffDistance", safeFalloff)
            setFloatUniform("curvePower", safeCurvePower)
            setFloatUniform("exposure", if (exposure.isFinite()) exposure else 1.0f)
            setFloatUniform("saturation", if (saturation.isFinite()) saturation else 1.05f)
            setFloatUniform("highlightRolloff", if (highlightRolloff.isFinite()) highlightRolloff else 0.15f)
            setFloatUniform("contrast", if (contrast.isFinite()) contrast else 1.0f)
            setFloatUniform("isLight", if (isLight) 1.0f else 0.0f)
        }
    } else {
        iosFrostedBlur(maxRadius * 0.7f)
    }
}

/**
 * iOS 27 authentic glass border with Overlay blend mode:
 * A razor-thin hairline contour (0.5dp) rendered with [BlendMode.Overlay].
 * In iOS, glass borders use overlay blending so that:
 * 1. At specular highlight areas (top/bottom rim), the bright white reflection overcomes
 *    the dark border, keeping the highlight clean, radiant, and uninterrupted.
 * 2. At neutral/dark edges (sides), the border multiplies into the glass to create
 *    a crisp, physical contour without muddy flat gray tones.
 */
private class GlassBorderCache {
    var lastSize: Size = Size.Unspecified
    var lastLayoutDirection: LayoutDirection? = null
    var safeSize: IntSize = IntSize.Zero
    var lastShape: Shape? = null
    var outline: Outline? = null
    var clipPath: Path? = null
    var isRecorded: Boolean = false
}

/**
 * iOS 27 authentic glass border with Overlay blend mode:
 * A razor-thin hairline contour (0.5dp) rendered with [BlendMode.Overlay].
 * In iOS, glass borders use overlay blending so that:
 * 1. At specular highlight areas (top/bottom rim), the bright white reflection overcomes
 *    the dark border, keeping the highlight clean, radiant, and uninterrupted.
 * 2. At neutral/dark edges (sides), the border multiplies into the glass to create
 *    a crisp, physical contour without muddy flat gray tones.
 *
 * Optimized with GPU display-list caching:
 * - When dimensions remain constant during animations/dragging, outlines and paths
 *   are not recalculated, and the graphics layer is not re-recorded.
 * - When alpha <= 0, drawing is skipped entirely.
 */
@Composable
fun Modifier.glassBorder(
    shape: Shape,
    width: Dp = 0.5.dp,
    alpha: Float? = null,
    blendMode: BlendMode = BlendMode.Overlay
): Modifier = glassBorderInternal(
    shape = shape,
    width = width,
    alpha = alpha,
    alphaProvider = null,
    blendMode = blendMode
)

@Composable
fun Modifier.glassBorder(
    shape: Shape,
    width: Dp = 0.5.dp,
    blendMode: BlendMode = BlendMode.Overlay,
    alpha: () -> Float
): Modifier = glassBorderInternal(
    shape = shape,
    width = width,
    alpha = null,
    alphaProvider = alpha,
    blendMode = blendMode
)

@Composable
private fun Modifier.glassBorderInternal(
    shape: Shape,
    width: Dp,
    alpha: Float?,
    alphaProvider: (() -> Float)?,
    blendMode: BlendMode
): Modifier {
    val isLightTheme = !LocalSlooshColors.current.isDark
    val defaultAlpha = if (isLightTheme) 0.30f else 0.55f

    // If static alpha is provided and <= 0, we can short-circuit immediately
    if (alphaProvider == null && alpha != null && alpha <= 0f) return this

    val graphicsLayer = rememberGraphicsLayer()
    val cache = remember { GlassBorderCache() }
    val paint = remember {
        Paint().apply {
            style = PaintingStyle.Stroke
            color = Color.Black
        }
    }

    val physicalBorderPaint = remember {
        Paint().apply {
            style = PaintingStyle.Stroke
            color = Color.Black
        }
    }

    return this.drawWithContent {
        drawContent()

        val currentAlpha = alphaProvider?.invoke() ?: (alpha ?: defaultAlpha)
        if (currentAlpha <= 0f) return@drawWithContent

        val strokeWidthPx = width.toPx()
        val safeWidth = ceil(size.width).toInt() + 2
        val safeHeight = ceil(size.height).toInt() + 2
        val safeSize = IntSize(safeWidth, safeHeight)

        val needsReRecord = !cache.isRecorded ||
            cache.lastSize != size ||
            cache.lastLayoutDirection != layoutDirection ||
            cache.safeSize != safeSize ||
            cache.lastShape != shape

        if (needsReRecord) {
            cache.lastSize = size
            cache.lastLayoutDirection = layoutDirection
            cache.safeSize = safeSize
            cache.lastShape = shape

            val outline = shape.createOutline(size, layoutDirection, this)
            cache.outline = outline

            val path = if (outline is Outline.Rounded) {
                (cache.clipPath ?: Path().also { cache.clipPath = it }).apply {
                    rewind()
                    addRoundRect(outline.roundRect)
                }
            } else {
                null
            }

            paint.strokeWidth = ceil(strokeWidthPx.fastCoerceAtMost(size.minDimension / 2f)) * 2f

            graphicsLayer.record(safeSize) {
                translate(1f, 1f) {
                    val canvas = drawContext.canvas
                    canvas.save()
                    canvas.clipOutline(outline, path)
                    canvas.drawOutline(outline, paint)
                    canvas.restore()
                }
            }
            cache.isRecorded = true
        }

        graphicsLayer.alpha = currentAlpha
        graphicsLayer.blendMode = blendMode

        translate(-1f, -1f) {
            drawLayer(graphicsLayer)
        }

        // Physical hairline edge for bright/white backgrounds:
        // Ensures the glass contour remains distinctly visible on pure white or high-luminance backgrounds
        // where BlendMode.Overlay mathematically produces zero effect.
        val basePhysicalAlpha = if (isLightTheme) 0.35f else 0.12f
        val currentPhysicalAlpha = (basePhysicalAlpha * (currentAlpha / defaultAlpha)).coerceIn(0f, 1f)
        if (currentPhysicalAlpha > 0f) {
            cache.outline?.let { outline ->
                val resolvedStrokeWidth = maxOf(strokeWidthPx, 1.dp.toPx())
                physicalBorderPaint.strokeWidth = resolvedStrokeWidth
                physicalBorderPaint.color = Color.Black.copy(alpha = currentPhysicalAlpha)
                drawContext.canvas.drawOutline(outline, physicalBorderPaint)
            }
        }
    }
}

private fun Canvas.clipOutline(outline: Outline, path: Path?) {
    when (outline) {
        is Outline.Rectangle -> clipRect(outline.rect)
        is Outline.Rounded -> {
            path?.let {
                it.rewind()
                it.addRoundRect(outline.roundRect)
                clipPath(it)
            }
        }
        is Outline.Generic -> clipPath(outline.path)
    }
}

@Language("AGSL")
private const val ConcentratedHighlightShaderString = """
uniform float2 size;
uniform float4 cornerRadii;
layout(color) uniform half4 color;
uniform float angle;
uniform float falloff;
uniform float lengthFactor;

float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float radius = radiusAt(coord, cornerRadii);
    
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
    float2 normal = float2(cos(angle), sin(angle));
    float d = dot(grad, normal);
    
    // Compute edge taper ratio (0 = center/bright zone, 1 = tip)
    float edgeRatio = 0.0;
    if (lengthFactor < 0.999) {
        float edgeStart = halfSize.x * lengthFactor;
        float xDist = abs(centeredCoord.x);
        edgeRatio = clamp((xDist - edgeStart) / max(halfSize.x - edgeStart, 1.0), 0.0, 1.0);
        edgeRatio = smoothstep(0.0, 1.0, edgeRatio);
    }
    // Taper: ramp up falloff power near edges → beam narrows like a lens tip
    float effectiveFalloff = mix(falloff, falloff * 4.0, edgeRatio);
    float intensity = pow(abs(d), effectiveFalloff);
    // Slight opacity fade at the very tips
    intensity *= (1.0 - edgeRatio * 0.6);
    
    return color * intensity;
}"""

private fun DrawScope.getHighlightCornerRadii(shape: Shape): FloatArray {
    val size = size
    val maxRadius = size.minDimension / 2f
    return when (shape) {
        is Capsule -> FloatArray(4) { maxRadius }
        is RoundedRectangularShape -> {
            val corners = shape.corners(size, layoutDirection, this)
            floatArrayOf(
                corners.topLeft.fastCoerceAtMost(maxRadius),
                corners.topRight.fastCoerceAtMost(maxRadius),
                corners.bottomRight.fastCoerceAtMost(maxRadius),
                corners.bottomLeft.fastCoerceAtMost(maxRadius)
            )
        }
        is AbsoluteRoundedCornerShape -> {
            val topLeft = shape.topStart.toPx(size, this)
            val topRight = shape.topEnd.toPx(size, this)
            val bottomRight = shape.bottomEnd.toPx(size, this)
            val bottomLeft = shape.bottomStart.toPx(size, this)
            floatArrayOf(
                topLeft.fastCoerceAtMost(maxRadius),
                topRight.fastCoerceAtMost(maxRadius),
                bottomRight.fastCoerceAtMost(maxRadius),
                bottomLeft.fastCoerceAtMost(maxRadius)
            )
        }
        is CornerBasedShape -> {
            val isLtr = layoutDirection == LayoutDirection.Ltr
            val topLeft =
                if (isLtr) shape.topStart.toPx(size, this)
                else shape.topEnd.toPx(size, this)
            val topRight =
                if (isLtr) shape.topEnd.toPx(size, this)
                else shape.topStart.toPx(size, this)
            val bottomRight =
                if (isLtr) shape.bottomEnd.toPx(size, this)
                else shape.bottomStart.toPx(size, this)
            val bottomLeft =
                if (isLtr) shape.bottomStart.toPx(size, this)
                else shape.bottomEnd.toPx(size, this)
            floatArrayOf(
                topLeft.fastCoerceAtMost(maxRadius),
                topRight.fastCoerceAtMost(maxRadius),
                bottomRight.fastCoerceAtMost(maxRadius),
                bottomLeft.fastCoerceAtMost(maxRadius)
            )
        }
        else -> FloatArray(4) { maxRadius }
    }
}

/**
 * Concentrated Highlight style:
 * Restricts the horizontal spread of the specular highlight beam so it doesn't stretch
 * endlessly across wide capsules or bars, concentrating the glint cleanly in the center
 * with a smooth organic falloff (like authentic curved Apple glass optics).
 *
 * @param lengthFactor fraction of the half-width (0.0 to 1.0) covered by the beam.
 *        E.g. 0.48f covers the central ~48% of the width.
 */
@Immutable
data class ConcentratedHighlightStyle(
    override val color: Color = Color.White.copy(alpha = 0.5f),
    override val blendMode: BlendMode = BlendMode.Plus,
    val angle: Float = 90f,
    @param:FloatRange(from = 0.0) val falloff: Float = 1f,
    val lengthFactor: Float = 0.55f
) : HighlightStyle {

    override fun DrawScope.createShader(
        shape: Shape,
        runtimeShaderCache: RuntimeShaderCache
    ): RuntimeShader? {
        return if (isRuntimeShaderSupported()) {
            runtimeShaderCache.obtainRuntimeShader(
                "ConcentratedHighlight",
                ConcentratedHighlightShaderString
            ).apply {
                setFloatUniform("size", size.width, size.height)
                setFloatUniform("cornerRadii", getHighlightCornerRadii(shape))
                setColorUniform("color", color.copy(alpha = 1f))
                setFloatUniform("angle", (angle * (PI / 180f)).toFloat())
                setFloatUniform("falloff", falloff)
                setFloatUniform("lengthFactor", lengthFactor)
            }
        } else {
            null
        }
    }
}



val LocalBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * Immutable configuration defining authentic Liquid Glass optical and surface properties.
 */
@Immutable
data class LiquidGlassStyle(
    val blurRadius: Dp = 20.dp,
    val blurOpacity: Float = 0.72f,
    val secondaryBlurRadius: Dp = 0.dp,
    val refractionHeight: Dp = 14.dp,
    val refractionAmount: Dp = 28.dp,
    val depthEffect: Boolean = false,
    val chromaticAberration: Float = 0f,
    val highlightAlpha: Float = 0.68f,
    val highlightStyle: HighlightStyle = HighlightStyle.Default(angle = 90f, falloff = 3.0f),
    val borderAlpha: Float? = null,
    val borderWidth: Dp = 0.5.dp,
    val shadowAlpha: Float = 0f,
    val innerShadowAlpha: Float = 0f,
    val innerShadowRadius: Dp = 8.dp
)

/**
 * Standardized design tokens for Liquid Glass surfaces across the Sloosh design system.
 */
object LiquidGlassTokens {
    /**
     * Primary navigation bar glass preset (54dp capsule).
     * Features authentic 20dp blur with 72% opacity, 14/28dp enhanced lens refraction,
     * delicate isotropic chromatic dispersion, and a concentrated 90° specular highlight beam.
     */
    val Bar = LiquidGlassStyle(
        blurRadius = 20.dp,
        blurOpacity = 0.72f,
        refractionHeight = 14.dp,
        refractionAmount = 28.dp,
        depthEffect = false,
        chromaticAberration = 0.12f,
        highlightAlpha = 0.68f,
        highlightStyle = ConcentratedHighlightStyle(angle = 90f),
        borderAlpha = null, // Dynamically resolves to 0.30f light / 0.55f dark
        borderWidth = 0.5.dp,
        shadowAlpha = 0f,
        innerShadowAlpha = 0f
    )

    /**
     * Active tab dynamic slider preset (46dp capsule droplet).
     * Base optical values that scale during drag (4.5/8dp lens, 0.22f dispersion).
     */
    val Slider = LiquidGlassStyle(
        blurRadius = 4.dp,
        blurOpacity = 1.0f,
        refractionHeight = 4.5.dp,
        refractionAmount = 8.dp,
        depthEffect = false,
        chromaticAberration = 0.22f,
        highlightAlpha = 0.50f,
        highlightStyle = HighlightStyle.Default(angle = 90f, falloff = 3.0f),
        borderAlpha = null,
        borderWidth = 0.5.dp,
        shadowAlpha = 0.30f,
        innerShadowAlpha = 0f
    )

    /**
     * Floating action button preset (e.g. search button, media controls).
     * Features 20dp smooth frosted blur with 72% opacity, 16/34dp enhanced liquid lens with physical chromatic dispersion,
     * inner glow, drop shadow, and elastic tactile physics.
     */
    val Button = LiquidGlassStyle(
        blurRadius = 20.dp,
        blurOpacity = 0.72f,
        refractionHeight = 16.dp,
        refractionAmount = 34.dp,
        depthEffect = false,
        chromaticAberration = 0.18f,
        highlightAlpha = 0.68f,
        highlightStyle = HighlightStyle.Default(angle = 90f, falloff = 3.0f),
        borderAlpha = null,
        borderWidth = 0.5.dp,
        shadowAlpha = 0.25f,
        innerShadowAlpha = 0.25f,
        innerShadowRadius = 8.dp
    )

    /**
     * Floating action button in active/selected state (e.g. search screen active).
     */
    val ButtonActive = Button.copy(
        chromaticAberration = 0.22f,
        highlightAlpha = 0.85f,
        shadowAlpha = 0.50f,
        innerShadowAlpha = 0.60f
    )

    /**
     * Generic content surface / card / bottom sheet preset.
     */
    val Surface = LiquidGlassStyle(
        blurRadius = 20.dp,
        blurOpacity = 0.72f,
        refractionHeight = 16.dp,
        refractionAmount = 34.dp,
        depthEffect = false,
        chromaticAberration = 0.10f,
        highlightAlpha = 0.68f,
        highlightStyle = HighlightStyle.Default(angle = 90f, falloff = 3.0f),
        borderAlpha = null,
        borderWidth = 0.5.dp,
        shadowAlpha = 0.25f,
        innerShadowAlpha = 0.25f,
        innerShadowRadius = 8.dp
    )

    /**
     * Compact pill / filter chip preset (search input, chips).
     */
    val CompactPill = LiquidGlassStyle(
        blurRadius = 12.dp,
        blurOpacity = 0.72f,
        refractionHeight = 9.dp,
        refractionAmount = 18.dp,
        depthEffect = false,
        chromaticAberration = 0.12f,
        highlightAlpha = 0.35f,
        highlightStyle = HighlightStyle.Default(angle = 90f, falloff = 3.0f),
        borderAlpha = null,
        borderWidth = 0.5.dp,
        shadowAlpha = 0f,
        innerShadowAlpha = 0f
    )

    /**
     * Context menu card preset (iOS Liquid Glass pull-down context menu).
     * Features authentic compound dual-radius blur (10dp core + 20dp ambient glow), 10/20dp liquid lens refraction,
     * delicate chromatic dispersion, 0.45f specular highlight, and 0.5dp hairline border.
     */
    val Menu = LiquidGlassStyle(
        blurRadius = 10.dp,
        blurOpacity = 0.60f,
        secondaryBlurRadius = 20.dp,
        refractionHeight = 10.dp,
        refractionAmount = 20.dp,
        depthEffect = false,
        chromaticAberration = 0.08f,
        highlightAlpha = 0.45f,
        highlightStyle = HighlightStyle.Default(angle = 90f, falloff = 3.0f),
        borderAlpha = null,
        borderWidth = 0.5.dp,
        shadowAlpha = 0.30f,
        innerShadowAlpha = 0f
    )

    /**
     * Modal dialog preset (alerts, confirmations, «в разработке» notices, player selects).
     * Deeper frost than [Menu] (14dp + 28dp) and a denser smoked tint,
     * so the text stays legible over bright posters and video frames.
     */
    val Dialog = LiquidGlassStyle(
        blurRadius = 14.dp,
        blurOpacity = 0.78f,
        secondaryBlurRadius = 28.dp,
        refractionHeight = 10.dp,
        refractionAmount = 20.dp,
        depthEffect = false,
        chromaticAberration = 0.08f,
        highlightAlpha = 0.45f,
        highlightStyle = HighlightStyle.Default(angle = 90f, falloff = 3.0f),
        borderAlpha = null,
        borderWidth = 0.5.dp,
        shadowAlpha = 0.34f,
        innerShadowAlpha = 0f
    )
}

/**
 * Modifier for applying authentic liquid glass optics and shaders to surfaces
 * (capsules, cards, search bars, sheets) using structured [LiquidGlassStyle] tokens.
 */
@Composable
fun Modifier.liquidGlassSurface(
    style: LiquidGlassStyle = LiquidGlassTokens.Surface,
    shape: Shape = RoundedRectangle(16.dp),
    containerColor: Color? = null,
    backdrop: Backdrop? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    onDrawExtraSurface: (DrawScope.() -> Unit)? = null
): Modifier {
    val isGlassEnabled = LocalGlassEnabled.current
    val isLightTheme = !LocalSlooshColors.current.isDark

    val activeBackdrop = if (isGlassEnabled) (backdrop ?: LocalBackdrop.current ?: emptyBackdrop()) else emptyBackdrop()
    val glassBlurMultiplier = LocalGlassBlurMultiplier.current
    val rawContainerColor = containerColor ?: if (isLightTheme) LiquidGlassSurfaceLight else LiquidGlassSurfaceDark
    val resolvedContainerColor = if (isGlassEnabled) {
        computeEffectiveContainerColor(rawContainerColor, glassBlurMultiplier)
    } else {
        computeNoGlassContainerColor(containerColor, isLightTheme)
    }
    val effectiveBlurOpacity = if (isGlassEnabled) {
        computeEffectiveBlurOpacity(style.blurOpacity, glassBlurMultiplier)
    } else 0f

    return this.drawBackdrop(
        backdrop = activeBackdrop,
        shape = { shape },
        effects = {
            if (isGlassEnabled && effectiveBlurOpacity > 0.001f) {
                if (effectiveBlurOpacity >= 0.999f) {
                    val effectiveRadius = maxOf(style.blurRadius, style.secondaryBlurRadius).toPx()
                    blur(if (effectiveRadius > 0f) effectiveRadius else 20f.dp.toPx())
                } else if (style.secondaryBlurRadius > 0.dp && style.blurRadius > 0.dp) {
                    val r1 = minOf(style.blurRadius, style.secondaryBlurRadius).toPx()
                    val r2 = maxOf(style.blurRadius, style.secondaryBlurRadius).toPx()
                    iosDualFrostedBlur(r1, r2, blurOpacity = effectiveBlurOpacity)
                } else if (style.blurRadius > 0.dp) {
                    iosFrostedBlur(style.blurRadius.toPx(), blurOpacity = effectiveBlurOpacity)
                }
            }
            if (isGlassEnabled) {
                iosVibrantGlass(isLight = isLightTheme)
                if (style.refractionHeight > 0.dp && style.refractionAmount > 0.dp) {
                    iosLens(
                        refractionHeight = style.refractionHeight.toPx(),
                        refractionAmount = style.refractionAmount.toPx(),
                        depthEffect = style.depthEffect,
                        chromaticAberration = style.chromaticAberration
                    )
                }
            }
        },
        highlight = {
            if (style.highlightAlpha > 0f) {
                Highlight(
                    alpha = style.highlightAlpha,
                    style = style.highlightStyle
                )
            } else null
        },
        shadow = {
            if (style.shadowAlpha > 0f) {
                Shadow(
                    radius = if (isLightTheme) 8.dp else 12.dp,
                    offset = androidx.compose.ui.unit.DpOffset(0.dp, 3.dp),
                    color = if (isLightTheme) Color.Black.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.35f),
                    alpha = if (isLightTheme) style.shadowAlpha * 0.40f else style.shadowAlpha
                )
            } else null
        },
        innerShadow = {
            if (isGlassEnabled && style.innerShadowAlpha > 0f && !isLightTheme) {
                InnerShadow(
                    radius = style.innerShadowRadius,
                    alpha = style.innerShadowAlpha,
                    color = Color.Black.copy(alpha = 0.25f)
                )
            } else null
        },
        layerBlock = layerBlock ?: {},
        onDrawSurface = {
            drawRect(resolvedContainerColor)
            onDrawExtraSurface?.invoke(this)
        }
    ).glassBorder(shape = shape, width = style.borderWidth, alpha = style.borderAlpha)
}

/**
 * Modifier for interactive liquid glass elements (floating buttons, pills) featuring:
 * - Kyant Liquid elastic physics (tanh damping, directional touch drag stretch);
 * - Directional highlight and specular glints;
 * - Interactive touch ripple and overlay glass border.
 */
@Composable
fun Modifier.liquidGlassInteractive(
    interactiveHighlight: InteractiveHighlight,
    style: LiquidGlassStyle = LiquidGlassTokens.Button,
    shape: Shape = CircleShape,
    containerColor: Color? = null,
    backdrop: Backdrop? = null,
    maxDragScale: Float? = null,
    /**
     * Множитель следования за пальцем (1f — полное, как у кнопок).
     * Крупные окна просят двигаться лишь слегка, не теряя упругую деформацию.
     */
    dragTranslationFactor: Float = 1f,
    /**
     * Окна (диалоги) не подчиняются пользовательской настройке «Размытие стекла»:
     * у них фиксированная оптика из [style] и собственный тинт.
     */
    applyBlurSetting: Boolean = true,
    onDrawExtraSurface: (DrawScope.() -> Unit)? = null
): Modifier {
    val isGlassEnabled = LocalGlassEnabled.current
    val isLightTheme = !LocalSlooshColors.current.isDark

    val interactiveLayerBlock: GraphicsLayerScope.() -> Unit = {
        val width = size.width
        val height = size.height

        val progress = interactiveHighlight.pressProgress
        val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

        val maxOffset = size.minDimension
        val initialDerivative = if (maxDragScale != null) 0.12f else 0.05f
        val offset = interactiveHighlight.offset
        translationX =
            maxOffset * tanh(initialDerivative * offset.x / maxOffset) * dragTranslationFactor
        translationY =
            maxOffset * tanh(initialDerivative * offset.y / maxOffset) * dragTranslationFactor

        val resolvedDragScale = maxDragScale ?: (4f.dp.toPx() / size.height)
        val offsetAngle = atan2(offset.y, offset.x)
        val dragFractionX = if (maxDragScale != null) {
            tanh(abs(offset.x) / 80f.dp.toPx())
        } else {
            abs(offset.x / size.maxDimension)
        }
        val dragFractionY = if (maxDragScale != null) {
            tanh(abs(offset.y) / 80f.dp.toPx())
        } else {
            abs(offset.y / size.maxDimension)
        }
        val aspectX = if (maxDragScale != null) 1f else (width / height).fastCoerceAtMost(1f)
        val aspectY = if (maxDragScale != null) 1f else (height / width).fastCoerceAtMost(1f)
        scaleX =
            scale +
                resolvedDragScale * abs(cos(offsetAngle)) * dragFractionX * aspectX
        scaleY =
            scale +
                resolvedDragScale * abs(sin(offsetAngle)) * dragFractionY * aspectY
    }

    val activeBackdrop = if (isGlassEnabled) (backdrop ?: LocalBackdrop.current ?: emptyBackdrop()) else emptyBackdrop()
    val glassBlurMultiplier = LocalGlassBlurMultiplier.current
    val rawContainerColor = containerColor ?: if (isLightTheme) LiquidGlassSurfaceLight else LiquidGlassSurfaceDark
    val resolvedContainerColor = when {
        !isGlassEnabled -> computeNoGlassButtonColor(containerColor, isLightTheme)
        !applyBlurSetting -> rawContainerColor
        else -> computeEffectiveContainerColor(rawContainerColor, glassBlurMultiplier)
    }
    val effectiveBlurOpacity = when {
        !isGlassEnabled -> 0f
        !applyBlurSetting -> style.blurOpacity
        else -> computeEffectiveBlurOpacity(style.blurOpacity, glassBlurMultiplier)
    }

    return this.drawBackdrop(
        backdrop = activeBackdrop,
        shape = { shape },
        effects = {
            if (isGlassEnabled && effectiveBlurOpacity > 0.001f) {
                if (effectiveBlurOpacity >= 0.999f) {
                    val effectiveRadius = maxOf(style.blurRadius, style.secondaryBlurRadius).toPx()
                    blur(if (effectiveRadius > 0f) effectiveRadius else 20f.dp.toPx())
                } else if (style.secondaryBlurRadius > 0.dp && style.blurRadius > 0.dp) {
                    val r1 = minOf(style.blurRadius, style.secondaryBlurRadius).toPx()
                    val r2 = maxOf(style.blurRadius, style.secondaryBlurRadius).toPx()
                    iosDualFrostedBlur(r1, r2, blurOpacity = effectiveBlurOpacity)
                } else if (style.blurRadius > 0.dp) {
                    iosFrostedBlur(style.blurRadius.toPx(), blurOpacity = effectiveBlurOpacity)
                }
            }
            if (isGlassEnabled) {
                iosVibrantGlass(isLight = isLightTheme)
                if (style.refractionHeight > 0.dp && style.refractionAmount > 0.dp) {
                    iosLens(
                        refractionHeight = style.refractionHeight.toPx(),
                        refractionAmount = style.refractionAmount.toPx(),
                        depthEffect = style.depthEffect,
                        chromaticAberration = style.chromaticAberration
                    )
                }
            }
        },
        highlight = {
            if (style.highlightAlpha > 0f) {
                Highlight(
                    alpha = style.highlightAlpha,
                    style = style.highlightStyle
                )
            } else null
        },
        shadow = {
            if (style.shadowAlpha > 0f) {
                Shadow(
                    radius = if (isLightTheme) 8.dp else 12.dp,
                    offset = androidx.compose.ui.unit.DpOffset(0.dp, 3.dp),
                    color = if (isLightTheme) Color.Black.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.35f),
                    alpha = if (isLightTheme) style.shadowAlpha * 0.40f else style.shadowAlpha
                )
            } else null
        },
        innerShadow = {
            if (isGlassEnabled && style.innerShadowAlpha > 0f && !isLightTheme) {
                InnerShadow(
                    radius = style.innerShadowRadius,
                    alpha = style.innerShadowAlpha,
                    color = Color.Black.copy(alpha = 0.25f)
                )
            } else null
        },
        layerBlock = interactiveLayerBlock,
        onDrawSurface = {
            drawRect(resolvedContainerColor)
            onDrawExtraSurface?.invoke(this)
        }
    )
    .glassBorder(shape = shape, width = style.borderWidth, alpha = style.borderAlpha)
    .then(interactiveHighlight.modifier)
    .then(interactiveHighlight.gestureModifier)
}

/**
 * Authentic Liquid Glass floating button from AndroidLiquidGlass (Kyant),
 * strictly matching the search button optics, physical deformation, and shaders:
 * - Real optical liquid lens refraction: lens(14dp, 28dp)
 * - Vibrancy and blur: iosFrostedBlur(10dp, 0.80f) + iosVibrantGlass()
 * - Directional 90° top and bottom rim illumination: HighlightStyle.Default(angle = 90f)
 * - Physical inner shadow and drop shadow
 * - Kyant LiquidButton elastic physics (tanh damping, directional touch drag stretch)
 * - InteractiveHighlight touch ripple glow under the user's finger
 */
@Composable
fun LiquidGlassFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    shape: Shape = CircleShape,
    blurRadius: Dp = 20.dp,
    tint: Color? = null,
    borderAlpha: Float = 0.68f,
    elevation: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val style = remember(blurRadius, borderAlpha) {
        LiquidGlassTokens.Button.copy(
            blurRadius = blurRadius,
            highlightAlpha = borderAlpha
        )
    }

    val isLightTheme = !LocalSlooshColors.current.isDark
    val isDarkButton = tint == LiquidGlassSurfaceDark || !isLightTheme
    val defaultContentColor = if (isDarkButton) Color.White else Color.Black

    Box(
        modifier = modifier
            .liquidGlassInteractive(
                interactiveHighlight = interactiveHighlight,
                style = style,
                shape = shape,
                containerColor = tint,
                backdrop = backdrop
            )
            .clip(shape)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides defaultContentColor) {
            content()
        }
    }
}

/**
 * First-class unified alias for LiquidGlassFloatingButton.
 */
@Composable
fun SlooshFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    containerColor: Color? = null,
    borderAlpha: Float = 0.38f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) = LiquidGlassFloatingButton(
    onClick = onClick,
    modifier = modifier,
    shape = shape,
    tint = containerColor,
    borderAlpha = borderAlpha,
    contentPadding = contentPadding,
    content = content
)

/**
 * Authentic Liquid Glass dual button capsule (two actions in one glass capsule).
 * Stretches and deforms elastically with tanh damping and InteractiveHighlight touch ripple,
 * exactly matching the physics, optics, and kinematics of single liquid glass buttons.
 */
@Composable
fun LiquidGlassDualButton(
    firstOnClick: () -> Unit,
    secondOnClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    shape: Shape = SlooshCapsule,
    blurRadius: Dp = 20.dp,
    tint: Color? = null,
    borderAlpha: Float = 0.68f,
    elevation: Dp = 4.dp,
    firstContent: @Composable BoxScope.() -> Unit,
    secondContent: @Composable BoxScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val style = remember(blurRadius, borderAlpha) {
        LiquidGlassTokens.Button.copy(
            blurRadius = blurRadius,
            highlightAlpha = borderAlpha
        )
    }
    val isLightTheme = !LocalSlooshColors.current.isDark
    val isDarkButton = tint == LiquidGlassSurfaceDark || !isLightTheme
    val defaultContentColor = if (isDarkButton) Color.White else Color.Black

    Box(
        modifier = modifier
            .liquidGlassInteractive(
                interactiveHighlight = interactiveHighlight,
                style = style,
                shape = shape,
                containerColor = tint,
                backdrop = backdrop
            )
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides defaultContentColor) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Button,
                            onClick = firstOnClick
                        ),
                    contentAlignment = Alignment.Center,
                    content = firstContent
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Button,
                            onClick = secondOnClick
                        ),
                    contentAlignment = Alignment.Center,
                    content = secondContent
                )
            }
        }
    }
}

/**
 * Authentic Liquid Glass multi-segmented button capsule (three or more actions in one continuous glass capsule).
 * Stretches and deforms elastically with tanh damping and InteractiveHighlight touch ripple,
 * exactly matching the physics, optics, and kinematics of single/dual liquid glass buttons.
 */
@Composable
fun LiquidGlassMultiButton(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    shape: Shape = SlooshCapsule,
    blurRadius: Dp = 20.dp,
    tint: Color? = null,
    borderAlpha: Float = 0.68f,
    elevation: Dp = 4.dp,
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val style = remember(blurRadius, borderAlpha) {
        LiquidGlassTokens.Button.copy(
            blurRadius = blurRadius,
            highlightAlpha = borderAlpha
        )
    }
    val isLightTheme = !LocalSlooshColors.current.isDark
    val isDarkButton = tint == LiquidGlassSurfaceDark || !isLightTheme
    val defaultContentColor = if (isDarkButton) Color.White else Color.Black

    Box(
        modifier = modifier
            .liquidGlassInteractive(
                interactiveHighlight = interactiveHighlight,
                style = style,
                shape = shape,
                containerColor = tint,
                backdrop = backdrop
            )
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides defaultContentColor) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * Modifier for applying authentic liquid glass optics and shaders to non-button surfaces (capsules, search bars, etc.).
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: Backdrop? = null,
    shape: Shape = RoundedRectangle(16.dp),
    blurRadius: Dp = 20.dp,
    tint: Color? = null,
    borderAlpha: Float = 0.68f,
    elevation: Dp = 4.dp
): Modifier = liquidGlassSurface(
    style = LiquidGlassTokens.Surface.copy(
        blurRadius = blurRadius,
        highlightAlpha = borderAlpha
    ),
    shape = shape,
    containerColor = tint,
    backdrop = backdrop
)

/**
 * Convenience alias for liquidGlass surface styling.
 */
@Composable
fun Modifier.slooshSurface(
    shape: Shape = RoundedRectangle(16.dp),
    containerColor: Color? = null,
    borderAlpha: Float = 0.68f,
    elevation: Dp = 4.dp
): Modifier = liquidGlass(
    shape = shape,
    tint = containerColor,
    borderAlpha = borderAlpha,
    elevation = elevation
)

/**
 * Applies iOS-style .blendMode(.plusLighter) to any composable via [android.graphics.Canvas.saveLayer].
 *
 * Renders content onto an isolated offscreen hardware layer and composites it back onto the
 * underlying canvas using [BlendMode.Plus], producing the authentic vibrant screen-additive
 * effect used by all iOS sloosh tabs, icons, and labels. Safe to use on any content.
 */
@Composable
fun Modifier.plusBlendMode(): Modifier {
    val isLightTheme = !LocalSlooshColors.current.isDark
    if (isLightTheme) return this
    val paint = remember {
        androidx.compose.ui.graphics.Paint().apply {
            blendMode = BlendMode.Plus
        }
    }
    return this.drawWithContent {
        drawIntoCanvas { canvas ->
            canvas.saveLayer(
                androidx.compose.ui.geometry.Rect(
                    androidx.compose.ui.geometry.Offset.Zero,
                    size
                ),
                paint
            )
            drawContent()
            canvas.restore()
        }
    }
}
