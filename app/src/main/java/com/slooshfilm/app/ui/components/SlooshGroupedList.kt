package com.slooshfilm.app.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList

/**
 * Группированный список sloosh (настройки, карточки-секции) на движке glasmio.
 *
 * Фон, скругления и разделители рисует вендоренный [GlasmioGroupedList] — тот же
 * стеклянный движок, что у нижнего навбара.
 */
@Composable
fun SlooshGroupedList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    GlasmioGroupedList(
        modifier = modifier.fillMaxWidth(),
        framed = true,
        content = content
    )
}
