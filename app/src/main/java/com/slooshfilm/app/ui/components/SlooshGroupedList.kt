package com.slooshfilm.app.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import io.github.neoruaa.glasmio.glass.LocalBlurBackdrop
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList

/**
 * Группированный список sloosh (настройки, карточки-секции) на движке glasmio.
 *
 * Фон, скругления и разделители рисует вендоренный [GlasmioGroupedList] — тот же
 * стеклянный движок, что у нижнего навбара.
 *
 * Провайдер бэкдропа ставится здесь же, внутри списка: строки настроек содержат контролы
 * движка (сегмент-контрол, переключатель, ползунок), которым нужен `LocalGlassBackdrop`.
 * Апстримный `GlasmioGroupedList` его не выставляет, поэтому без этого любой такой контрол
 * оказывался вне хоста. Аргумент [backdrop] позволяет передать бэкдроп экрана явно; без него
 * берётся унаследованный (в настройках это бэкдроп содержимого, см. `SettingsScaffold`).
 */
@Composable
fun SlooshGroupedList(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedBackdrop = backdrop
        ?: LocalSettingsBackdrop.current
        ?: LocalGlassBackdrop.current
    // Если стекло группы уже нарисовано снаружи (`GlassSurface` в `SettingsSection`),
    // внутренняя заливка GlasmioGroupedList не нужна — иначе получается двойная плашка.
    val frameDrawnOutside = backdrop != null
    CompositionLocalProvider(
        LocalGlassBackdrop provides resolvedBackdrop,
        LocalBlurBackdrop provides resolvedBackdrop
    ) {
        GlasmioGroupedList(
            modifier = modifier.fillMaxWidth(),
            framed = !frameDrawnOutside,
            content = content
        )
    }
}
