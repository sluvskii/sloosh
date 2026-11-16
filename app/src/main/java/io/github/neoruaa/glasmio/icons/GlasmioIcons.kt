/*
 * VENDOR-PATCH(sloosh): мост иконок для вендоренных компонентов glasmio-ui
 * (снимок daa1b06, v0.1.3). Апстрим-модуль glasmio-icons в проект не вендорится:
 * у sloosh уже есть собственный рендерер SF Symbols (`com.slooshfilm.app.ui.sficons`),
 * и по AGENTS.md §1 весь UI обязан рисовать иконки через `SfIcon(systemName = …)`.
 *
 * Апстримный GlasmioIcons.kt подменял отсутствующие глифы набором Material Icons
 * (androidx.compose.material.icons), что AGENTS.md запрещает. Поэтому здесь остаются
 * только совместимые по сигнатуре обёртки: имя пакета `io.github.neoruaa.glasmio.icons`
 * нужно, чтобы вендоренные файлы (`import io.github.neoruaa.glasmio.icons.*`) собирались
 * без правок, а рисование уходит в наш движок символов.
 */
package io.github.neoruaa.glasmio.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.slooshfilm.app.ui.sficons.LocalGlasmioIconTint
import com.slooshfilm.app.ui.sficons.LocalGlasmioSymbolTypeface
import com.slooshfilm.app.ui.sficons.SfIcon as SlooshSfIcon
import com.slooshfilm.app.ui.sficons.SfSymbol as SlooshSfSymbol

/** Глифы берутся из единого каталога sloosh; см. `ui/sficons/SfSymbolCatalog.kt`. */
typealias SfSymbol = SlooshSfSymbol

/**
 * Хост-шрифт символов (res/font/sf_pro_symbols.ttf), как и в `ui/sficons`.
 *
 * `typealias`, а не копия: так это один и тот же CompositionLocal, а не второй с пустым
 * значением по умолчанию (иначе шрифт, положенный нашим провайдером, не был бы виден
 * вендоренным компонентам).
 */
typealias LocalGlasmioSymbolTypeface = com.slooshfilm.app.ui.sficons.LocalGlasmioSymbolTypeface

/** Тинт по умолчанию, вычисляемый на каждом месте вызова. */
typealias LocalGlasmioIconTint = com.slooshfilm.app.ui.sficons.LocalGlasmioIconTint

/**
 * Совместимая обёртка над `com.slooshfilm.app.ui.sficons.SfIcon` для типизированных символов.
 * Апстримный `inkHeight` принимается и игнорируется — оптику задаёт наш рендерер.
 */
@Composable
fun SfIcon(
    symbol: SfSymbol,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalGlasmioIconTint.current(),
    size: Dp = 24.dp,
    fontSize: TextUnit = TextUnit.Unspecified,
    weight: FontWeight? = null,
    mirrored: Boolean = false,
    fontFeatureSettings: String = "'ss16' 1",
    fitGlyphToBounds: Boolean = true,
    inkHeight: Dp? = null,
) {
    SlooshSfIcon(
        symbol = symbol,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
        size = size,
        fontSize = fontSize,
        weight = weight,
        mirrored = mirrored,
        fontFeatureSettings = fontFeatureSettings,
        fitGlyphToBounds = fitGlyphToBounds,
        inkHeight = inkHeight,
    )
}

/** Совместимая обёртка над `com.slooshfilm.app.ui.sficons.SfIcon` для имён системных символов. */
@Composable
fun SfIcon(
    systemName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalGlasmioIconTint.current(),
    size: Dp = 24.dp,
    fontSize: TextUnit = TextUnit.Unspecified,
    weight: FontWeight? = null,
    mirrored: Boolean = false,
    fontFeatureSettings: String = "'ss16' 1",
    fitGlyphToBounds: Boolean = true,
    inkHeight: Dp? = null,
) {
    SlooshSfIcon(
        systemName = systemName,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
        size = size,
        fontSize = fontSize,
        weight = weight,
        mirrored = mirrored,
        fontFeatureSettings = fontFeatureSettings,
        fitGlyphToBounds = fitGlyphToBounds,
        inkHeight = inkHeight,
    )
}

/**
 * Оставлено для совместимости: вендоренные компоненты вызывают `GlasmioIcon`
 * в нескольких местах. Рисуется тем же движком sloosh.
 */
@Composable
fun GlasmioIcon(
    systemName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalGlasmioIconTint.current(),
    size: Dp = 24.dp,
) = SfIcon(systemName, contentDescription, modifier, tint, size)
