/*
 * Граница подмножества вендора glasmio-glass в sloosh.
 *
 * Функция скопирована дословно из Glasmio (glass/GlassBackdrop.kt, снимок 0.1.3),
 * НО ветки `is CrossWindowBackdrop -> true` и `is SharedGlassBackdrop -> ...`
 * опущены: сами классы в подмножество не входят (GlassBackdrop тянет каскад
 * glasmio.core/glasmio.icons). Для всех типов, существующих в вендоре
 * (CanvasBackdrop-мосты, LayerBackdrop, EmptyBackdrop, CombinedBackdrop),
 * результат `false` семантически идентичен оригиналу — быстрый путь
 * layer-block-independent источника в CombinedBackdrop просто отключён.
 */
package io.github.neoruaa.glasmio.glass

import io.github.neoruaa.glasmio.glass.render.Backdrop

internal fun Backdrop.hasBuiltInGlassLayerBlockIndependentSource(): Boolean = false
