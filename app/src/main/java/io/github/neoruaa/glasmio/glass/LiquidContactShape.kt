/*
 * Vendored extract from Glasmio (https://github.com/NEORUAA/Glasmio), module glasmio-glass,
 * file IosToolbarActionGroup.kt (data class LiquidContactShape, строки 925-1007 снимка 0.1.3).
 * Apache-2.0. Copyright 2026 NEORUAA. Пакет оригинальный; экстракт выделен, чтобы
 * не тянуть каскад glasmio.core/glasmio.icons — тип используется render/highlight и
 * render/shadow только в type-checks контакт-морфа.
 */
package io.github.neoruaa.glasmio.glass

data class LiquidContactShape(
    val first: LiquidContactCapsule,
    val second: LiquidContactCapsule,
    val contactDistance: Float,
    private val lensTopStart: CornerSize = CornerSize(50),
    private val lensTopEnd: CornerSize = CornerSize(50),
    private val lensBottomEnd: CornerSize = CornerSize(50),
    private val lensBottomStart: CornerSize = CornerSize(50),
    val outlineScale: Offset = Offset(1f, 1f),
    val outlineTranslation: Offset = Offset.Zero,
) : CornerBasedShape(lensTopStart, lensTopEnd, lensBottomEnd, lensBottomStart) {
    private var baseGeometry = lazy(LazyThreadSafetyMode.NONE) {
        contactCapsulePath(first, second, contactDistance)
    }
    private var opticalContour = lazy(LazyThreadSafetyMode.NONE) {
        NavigationContactContour(baseGeometry.value.path.asAndroidPath().approximate(0.25f))
    }
    private var opticalField: NavigationContactField? = null
    internal var nativeUnion: NavigationNativeUnion? = null
        private set
    internal fun withNativeUnion(value: NavigationNativeUnion): LiquidContactShape = apply {
        nativeUnion = value
        baseGeometry = lazy(LazyThreadSafetyMode.NONE) { ContactGeometry(value.path, true) }
        opticalContour = lazy(LazyThreadSafetyMode.NONE) {
            NavigationContactContour(baseGeometry.value.path.asAndroidPath().approximate(.125f))
        }
        cachedOutline = null
    }
    internal var refractionBand: Float = 0f
        private set
    internal fun refractionContour(): NavigationContactContour = opticalContour.value
    internal fun refractionField(): NavigationContactField = opticalField ?: NavigationContactField().also { opticalField = it }
    internal fun withRefractionField(field: NavigationContactField, band: Float): LiquidContactShape = apply {
        opticalField = field
        refractionBand = band
    }
    private var cachedOutline: Outline? = null
    val isConnected: Boolean get() = nativeUnion?.isConnected ?: !(second.bounds.left - first.bounds.right >= contactDistance ||
        min(first.bounds.bottom, second.bounds.bottom) <= maxOf(first.bounds.top, second.bounds.top))
    var hasConsistentWinding: Boolean = false
        private set

    override fun copy(topStart: CornerSize, topEnd: CornerSize, bottomEnd: CornerSize, bottomStart: CornerSize): CornerBasedShape =
        LiquidContactShape(first, second, contactDistance, topStart, topEnd, bottomEnd, bottomStart,
            outlineScale, outlineTranslation).also {
                it.baseGeometry = baseGeometry
                it.opticalContour = opticalContour
                it.opticalField = opticalField
                it.refractionBand = refractionBand
                it.nativeUnion = nativeUnion
            }

    fun withOutlineTransform(scale: Offset, translation: Offset): LiquidContactShape =
        copy(outlineScale = scale, outlineTranslation = translation).also {
            it.baseGeometry = baseGeometry
            it.opticalContour = opticalContour
            it.opticalField = opticalField
            it.refractionBand = refractionBand
            it.nativeUnion = nativeUnion
        }

    override fun createOutline(
        size: Size, topStart: Float, topEnd: Float, bottomEnd: Float, bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline = cachedOutline ?: baseGeometry.value.let { geometry ->
        hasConsistentWinding = geometry.hasConsistentWinding
        val path = if (outlineScale != Offset(1f, 1f) || outlineTranslation != Offset.Zero) {
            // Keep the shared native contour immutable across visible and sampled outlines.
            Path().apply {
                fillType = geometry.path.fillType
                addPath(geometry.path)
            }.also { transformed ->
                transformed.asAndroidPath().transform(android.graphics.Matrix().apply {
                    setValues(floatArrayOf(
                        outlineScale.x, 0f, outlineTranslation.x,
                        0f, outlineScale.y, outlineTranslation.y,
                        0f, 0f, 1f,
                    ))
                })
            }
        } else geometry.path
        Outline.Generic(path).also { cachedOutline = it }
    }
