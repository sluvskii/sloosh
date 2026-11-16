package com.slooshfilm.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.ui.GlasmioSwitch

/**
 * Переключатель sloosh на движке glasmio (iOS 27 1:1).
 *
 * Заменяет самописный `SlooshToggle` (он больше не существует): дорожка, «палец»,
 * оптический линзовый сдвиг и тактильный отклик рисует вендоренный [GlasmioSwitch] —
 * тот же движок, что у нижнего навбара. Цвет включённого состояния остаётся брендовым.
 *
 * @param selected лямбда-состояние (сохранена ради существующих мест вызова)
 */
@Composable
fun SlooshToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    GlasmioSwitch(
        checked = selected(),
        onCheckedChange = onSelect,
        modifier = modifier,
        backdrop = backdrop ?: LocalGlassBackdrop.current,
        onTintColor = accentColor ?: Color.Unspecified,
    )
}

/** Перегрузка для состояния, переданного значением. */
@Composable
fun SlooshToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    accentColor: Color? = null
) = SlooshToggle(
    selected = { checked },
    onSelect = onCheckedChange,
    backdrop = backdrop,
    modifier = modifier,
    accentColor = accentColor,
)
