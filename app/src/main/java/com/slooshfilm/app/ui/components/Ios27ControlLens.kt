package com.slooshfilm.app.ui.components

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangularShape
import com.slooshfilm.app.ui.backdrop.BackdropEffectScope
import com.slooshfilm.app.ui.backdrop.effects.runtimeShaderEffect
import com.slooshfilm.app.ui.backdrop.isRuntimeShaderSupported
import org.intellij.lang.annotations.Language

/**
 * Аутентичные матрицы цветовой коррекции iOS 27 для матового стекла в покое (NativeTabIdle).
 * Извлечены из QuartzCore AIR / UIKitCore:
 */
val NativeTabIdleLight = floatArrayOf(
    1.1850999594f, -.0502000004f, -.0049999999f, -.1999999881f,
    -.0148999998f, 1.1498999596f, -.0050999997f, -.1999999881f,
    -.0148999998f, -.0499999970f, 1.1948999166f, -.1999999881f,
)

val NativeTabIdleDark = floatArrayOf(
    1.1297999620f, -.2360999882f, -.0236999989f, -.0700000003f,
    -.0700999945f, .9639999866f, -.0237999987f, -.0700000003f,
    -.0701999962f, -.2360000014f, 1.1761999130f, -.0700000003f,
)

/**
 * Вычисляет радиусы углов формы в координатах поверхности для SDF-шейдера.
 */
fun glassCornerRadii(
    shape: Shape,
    size: Size,
    direction: LayoutDirection,
    density: Density
): FloatArray {
    val limit = size.minDimension / 2f
    fun bounded(a: Float, b: Float, c: Float, d: Float) =
        floatArrayOf(
            a.coerceIn(0f, limit),
            b.coerceIn(0f, limit),
            c.coerceIn(0f, limit),
            d.coerceIn(0f, limit)
        )
    if (shape is Capsule) {
        return floatArrayOf(limit, limit, limit, limit)
    }
    if (shape is RoundedRectangularShape) {
        val corners = shape.corners(size, direction, density)
        return bounded(corners.topLeft, corners.topRight, corners.bottomRight, corners.bottomLeft)
    }
    if (shape is CornerBasedShape) {
        val ltr = direction == LayoutDirection.Ltr
        return bounded(
            (if (ltr) shape.topStart else shape.topEnd).toPx(size, density),
            (if (ltr) shape.topEnd else shape.topStart).toPx(size, density),
            (if (ltr) shape.bottomEnd else shape.bottomStart).toPx(size, density),
            (if (ltr) shape.bottomStart else shape.bottomEnd).toPx(size, density),
        )
    }
    return when (val outline = shape.createOutline(size, direction, density)) {
        is Outline.Rectangle -> FloatArray(4)
        is Outline.Rounded -> bounded(
            outline.roundRect.topLeftCornerRadius.x,
            outline.roundRect.topRightCornerRadius.x,
            outline.roundRect.bottomRightCornerRadius.x,
            outline.roundRect.bottomLeftCornerRadius.x
        )
        is Outline.Generic -> FloatArray(4) { limit }
    }
}

@Language("AGSL")
private const val Ios27ControlLensShaderString = """
uniform shader content;

uniform float2 size;
uniform float4 cornerRadii;
uniform float capturePadding;
uniform float height;
uniform float amount;
uniform float outerHeight;
uniform float outerAmount;
uniform float refractionOpacity;
uniform float refractionEdgeWidth;
uniform float depthEffect;
uniform float chromatic;
uniform float highlightProgress;
uniform float highlightAngle;
uniform float highlightCurvature;
uniform float highlightHeight;
uniform float4 restColor;
uniform float restProgress;
uniform float isTab;
uniform float4 idleRow0;
uniform float4 idleRow1;
uniform float4 idleRow2;

// --- 1. SDF Суперкруга iOS 27 (QuartzCore AIR supercircle_sdf) ---
float2 nativeGlassSafeNormal(float2 v) {
    return v / max(length(v), 0.000001);
}

float3 nativeGlassRoundedField(float2 point, float2 halfExtent, float2 radii, float normalMix) {
    if (min(abs(radii.x), abs(radii.y)) <= 0.000001) {
        float2 edge = abs(point) - halfExtent;
        float2 signs = float2(point.x < 0.0 ? -1.0 : 1.0, point.y < 0.0 ? -1.0 : 1.0);
        float2 normal = signs * (edge.x > edge.y ? float2(1.0, 0.0) : float2(0.0, 1.0));
        float2 oval = nativeGlassSafeNormal(float2(point.x, point.y * halfExtent.x / max(halfExtent.y, 0.000001)));
        return float3(max(edge.x, edge.y), nativeGlassSafeNormal(mix(normal, oval, normalMix)));
    }
    float2 r = max(abs(radii), float2(0.001));
    float radius = min(r.x, r.y);
    float2 inverseAxis = radius / r;
    float2 local = point * inverseAxis;
    float2 extent = halfExtent * inverseAxis;
    float expandedRadius = radius * 1.5286649465560913;
    float2 curvature = clamp(2.8915570351735034 - 1.4457785175867517 * (extent * 2.0) / expandedRadius, 0.0, 1.0);
    float baseRadius = mix(expandedRadius, radius, max(curvature.x, curvature.y));
    float2 edge = abs(local) - extent;
    float2 circle = edge + baseRadius;
    float interior = min(max(circle.x, circle.y), 0.0);
    float2 z = max((edge + expandedRadius) / expandedRadius, 0.0);
    float largest = max(z.x, z.y);
    float t = largest > 0.0 ? clamp(min(z.x, z.y) / largest, 0.0, 1.0) : 0.0;
    float d = length(z);
    float polynomial = ((((-0.926054 * t + 3.15601) * t - 3.64122) * t + 1.26803) * t + 0.268531) * t * t;
    float raw = d + 1.0 - 1.0 / (1.0 - polynomial * clamp(d, 0.0, 1.0));
    float alternate = 0.3458344340324402 + 0.6541655659675598 * length(max(1.5286649465560913 * z - 0.528665, 0.0));
    float weight = clamp((z.y > z.x ? 1.0 : -1.0) * (t - 1.0) + 0.5, 0.0, 1.0);
    float corrected = mix(mix(raw, alternate, curvature.x), mix(raw, alternate, curvature.y), weight);
    float distance = interior + expandedRadius * float(half(corrected - 1.0));
    float2 positiveNormal = nativeGlassSafeNormal(max(circle, 0.0));
    if (positiveNormal.x + positiveNormal.y <= 0.0) {
        positiveNormal = circle.x > circle.y ? float2(1.0, 0.0) : float2(0.0, 1.0);
    }
    float2 normal = positiveNormal * float2(local.x < 0.0 ? -1.0 : 1.0, local.y < 0.0 ? -1.0 : 1.0);
    float2 oval = nativeGlassSafeNormal(float2(local.x, local.y * extent.x / max(extent.y, 0.000001)));
    normal = nativeGlassSafeNormal(mix(normal, oval, normalMix));
    return float3(distance, normal * inverseAxis);
}

float radiusAt(float2 p) {
    return p.y < size.y * 0.5
        ? (p.x < size.x * 0.5 ? cornerRadii.x : cornerRadii.y)
        : (p.x < size.x * 0.5 ? cornerRadii.w : cornerRadii.z);
}

float3 glassField(float2 p, float normalMix) {
    return nativeGlassRoundedField(p - size * 0.5, size * 0.5, float2(radiusAt(p)), normalMix);
}

// --- 2. Функция смещения лучей (QuartzCore AIR glass_background_r) ---
float nativeGlassDisplacement(float signedDistance, float inverseHeight, float signedAmount) {
    float u = clamp(-signedDistance * inverseHeight, 0.0, 1.0);
    return signedAmount * (1.0 - clamp(sqrt(u * (2.0 - u)), 0.0, 1.0));
}

float nativeGlassRefractionWeight(float signedDistance, float threshold0, float threshold1, float opacity) {
    float width = threshold1 - threshold0;
    float progress = width != 0.0 ? clamp((signedDistance - threshold0) / width, 0.0, 1.0)
        : step(threshold1, signedDistance);
    return opacity * progress;
}

// --- 3. 7-лучевая спектральная хроматическая аберрация QuartzCore ---
float3 nativeGlassStraightColor(half4 c) {
    float3 rgb = float3(c.rgb);
    rgb = float3(abs(rgb.r) < 0.000001 ? 0.0 : rgb.r,
                 abs(rgb.g) < 0.000001 ? 0.0 : rgb.g,
                 abs(rgb.b) < 0.000001 ? 0.0 : rgb.b);
    return rgb / max(float(c.a), 0.000001);
}

half4 nativeGlassAberrate(float2 coordinate, float2 delta) {
    half4 red    = content.eval(coordinate + delta);
    half4 orange = content.eval(coordinate + delta * (2.0 / 3.0));
    half4 yellow = content.eval(coordinate + delta * (1.0 / 3.0));
    half4 green  = content.eval(coordinate);
    half4 cyan   = content.eval(coordinate - delta * (1.0 / 3.0));
    half4 blue   = content.eval(coordinate - delta * (2.0 / 3.0));
    half4 purple = content.eval(coordinate - delta);

    float3 r = nativeGlassStraightColor(red);
    float3 o = nativeGlassStraightColor(orange);
    float3 y = nativeGlassStraightColor(yellow);
    float3 g = nativeGlassStraightColor(green);
    float3 c = nativeGlassStraightColor(cyan);
    float3 b = nativeGlassStraightColor(blue);
    float3 p = nativeGlassStraightColor(purple);

    float3 synth = float3(
        (r.r + o.r * (2.0 / 3.0) + y.r * (1.0 / 3.0)) * 0.5,
        (o.g * (1.0 / 3.0) + y.g * (2.0 / 3.0) + g.g + c.g * (2.0 / 3.0) + b.g * (1.0 / 3.0)) / 3.0,
        (c.b * (1.0 / 3.0) + b.b * (2.0 / 3.0) + p.b) * 0.5
    );
    float alpha = float(red.a + orange.a + yellow.a + green.a + cyan.a + blue.a + purple.a) / 7.0;
    return half4(half3(synth * alpha), half(alpha));
}

float2 customGlassChromaticVector(float2 displacement, float2 point, float2 halfExtent) {
    float crossTerm = clamp(point.x * point.y / max(halfExtent.x * halfExtent.y, 0.0001), -1.0, 1.0);
    return displacement * crossTerm;
}

// --- 4. Верхний блик CASDF (QuartzCore AIR sdf_diffuse_kf_highlight) ---
float nativeHighlightStrip(float depth, float h, float curv, float aa) {
    float u = clamp(depth / max(h, 0.000001), 0.0, 1.0);
    float profile = mix(u < 1.0 ? 1.0 : 0.0, 1.0 - u, curv);
    return profile * clamp(depth / aa + 0.5, 0.0, 1.0)
                   * clamp((h - depth) / aa + 0.5, 0.0, 1.0);
}

float nativeHighlightAngular(float alignment, float threshold) {
    return clamp((alignment - threshold) / max(1.0 - threshold, 0.000001), 0.0, 1.0);
}

float nativeHighlightBias(float cov, float bias) {
    return cov / max(1.0 + bias * (1.0 - cov), 0.000001);
}

float nativeCasdfHighlightCoverage(float signedDistance, float2 gradient,
    float h, float angle, float curv) {
    float depth = -signedDistance;
    if (depth < -5.0) return 0.0;
    float2 normal = normalize(gradient);
    float aa = max(abs(normal.x) + abs(normal.y), 0.0001);
    float alignment = dot(normal, float2(sin(angle), -cos(angle)));
    float direct = nativeHighlightStrip(depth, h, curv, aa);
    float diffuse = nativeHighlightStrip(depth, h * 8.0, 1.0, aa);
    float spread = cos(1.3962634015954636);
    float diffuseSpread = cos(1.3962634015954636 * 0.65);
    float key = direct * nativeHighlightAngular(alignment, spread);
    float fill = direct * nativeHighlightAngular(-alignment, spread);
    float diffuseKey = diffuse * nativeHighlightAngular(alignment, diffuseSpread);
    float diffuseFill = diffuse * nativeHighlightAngular(-alignment, diffuseSpread);
    return key + fill + nativeHighlightBias(diffuseKey, 11.333333333333334)
                      + nativeHighlightBias(diffuseFill, 11.333333333333334);
}

// --- Main AGSL Entry Point ---
half4 main(float2 coordinate) {
    float2 p = coordinate - capturePadding;
    float3 field = glassField(p, depthEffect * 0.5);
    float sd = field.x;

    // Вне контура капсулы — возвращаем исходный пиксель
    if (sd > 0.0) return content.eval(coordinate);

    // 1. Полный покой (Resting State): рендерим благородное светящееся матовое стекло
    if (restProgress >= 0.999) {
        half4 bg = content.eval(coordinate);
        half4 body = half4(restColor.rgb, restColor.a);
        if (isTab > 0.5) {
            float3 straight = nativeGlassStraightColor(bg);
            if (length(straight) > 0.05) {
                float4 matrixBody = float4(dot(idleRow0, float4(straight, 1.0)),
                                           dot(idleRow1, float4(straight, 1.0)),
                                           dot(idleRow2, float4(straight, 1.0)),
                                           1.0);
                body.rgb = mix(body.rgb, matrixBody.rgb, 0.5);
            }
        }
        return mix(bg, half4(body.rgb, 1.0), half(body.a));
    }

    // 2. Активное движение: вычисляем истинное смещение линзы
    float innerDistance = nativeGlassDisplacement(sd, 1.0 / max(height, 0.001), -amount);
    float2 displacement = field.yz * innerDistance;
    half4 result = content.eval(coordinate + displacement);

    // 3. Хроматическая спектральная дисперсия (7 лучей)
    if (chromatic > 0.0) {
        float2 chromaticVector = customGlassChromaticVector(displacement, p - size * 0.5, size * 0.5);
        float2 combinedDelta = mix(field.yz * (length(displacement) * 0.35), chromaticVector, 0.5) * chromatic;
        result = nativeGlassAberrate(coordinate + displacement, combinedDelta);
    }

    // 4. Двухлучевое внешнее преломление (Dual-ray refraction)
    if (outerHeight > 0.0 && outerAmount != 0.0 && refractionOpacity > 0.0) {
        float outerDistance = nativeGlassDisplacement(sd, 1.0 / outerHeight, outerAmount);
        half4 outer = content.eval(coordinate + field.yz * outerDistance);
        float weight = nativeGlassRefractionWeight(sd, -refractionEdgeWidth, 0.0, refractionOpacity);
        result = mix(result, outer, half(weight));
    }

    // 5. Блик CASDF (Apple sdf_diffuse_kf_highlight)
    if (highlightProgress > 0.0) {
        float hlMask = nativeCasdfHighlightCoverage(sd, field.yz, highlightHeight, highlightAngle, highlightCurvature);
        result.rgb += half3(hlMask * highlightProgress * 0.75);
    }

    // 6. Плавное затухание матового тела покоя при возвращении
    if (restProgress > 0.01) {
        result = mix(result, half4(restColor.rgb, 1.0), half(restProgress * restColor.a));
    }

    return result;
}
"""

/**
 * Аутентичная оптическая линза iOS 27 для ползунков и таб-индикаторов.
 */
fun BackdropEffectScope.ios27ControlLens(
    liftProgress: Float,
    isDark: Boolean,
    refractionHeight: Float = 10f * density,
    refractionAmount: Float = 14f * density,
    outerRefractionHeight: Float = 0f,
    outerRefractionAmount: Float = 0f,
    refractionOpacity: Float = 0.6f,
    chromatic: Float = 0.22f,
    highlightProgress: Float = 0.70f,
    highlightAngle: Float = 0f,
    highlightCurvature: Float = 0.75f,
    depthEffect: Boolean = false,
    restingColor: Color = Color.Unspecified
) {
    if (!isRuntimeShaderSupported()) return
    if (size.minDimension <= 0f) return

    val lift = liftProgress.coerceIn(0f, 1f)
    val rest = (1f - lift).coerceIn(0f, 1f)

    val cornerRadii = glassCornerRadii(shape, size, layoutDirection, this)
    val safeRestColor = if (restingColor != Color.Unspecified) restingColor else (if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f))

    val idleMatrix = if (isDark) NativeTabIdleDark else NativeTabIdleLight
    val angleRad = (highlightAngle * (Math.PI / 180.0)).toFloat()

    runtimeShaderEffect(
        key = "Ios27ControlLensShader",
        shaderString = Ios27ControlLensShaderString,
        uniformShaderName = "content"
    ) {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("cornerRadii", cornerRadii[0], cornerRadii[1], cornerRadii[2], cornerRadii[3])
        setFloatUniform("capturePadding", padding)
        setFloatUniform("height", refractionHeight)
        setFloatUniform("amount", refractionAmount * lift)
        setFloatUniform("outerHeight", outerRefractionHeight)
        setFloatUniform("outerAmount", outerRefractionAmount)
        setFloatUniform("refractionOpacity", refractionOpacity)
        setFloatUniform("refractionEdgeWidth", density)
        setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
        setFloatUniform("chromatic", chromatic * lift)
        setFloatUniform("highlightProgress", highlightProgress * lift)
        setFloatUniform("highlightAngle", angleRad)
        setFloatUniform("highlightCurvature", highlightCurvature)
        setFloatUniform("highlightHeight", density)
        setFloatUniform("restColor", safeRestColor.red, safeRestColor.green, safeRestColor.blue, safeRestColor.alpha)
        setFloatUniform("restProgress", rest)
        setFloatUniform("isTab", 1f)
        setFloatUniform("idleRow0", idleMatrix[0], idleMatrix[1], idleMatrix[2], idleMatrix[3])
        setFloatUniform("idleRow1", idleMatrix[4], idleMatrix[5], idleMatrix[6], idleMatrix[7])
        setFloatUniform("idleRow2", idleMatrix[8], idleMatrix[9], idleMatrix[10], idleMatrix[11])
    }
}
