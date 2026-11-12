package io.github.neoruaa.glasmio.icons

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import com.slooshfilm.app.ui.sficons.SfIcon as SlooshSfIcon
import com.slooshfilm.app.ui.sficons.SfSymbol as SlooshSfSymbol

typealias SfSymbol = SlooshSfSymbol

val LocalGlasmioSymbolTypeface = com.slooshfilm.app.ui.sficons.LocalGlasmioSymbolTypeface
val LocalGlasmioIconTint = com.slooshfilm.app.ui.sficons.LocalGlasmioIconTint

@Composable
fun SfIcon(
    symbol: SfSymbol,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.Unspecified,
    weight: FontWeight = FontWeight.Normal,
    fontSize: TextUnit = 24.sp,
    fontFeatureSettings: String? = null,
    fitGlyphToBounds: Boolean = false,
) {
    SlooshSfIcon(
        symbol = symbol,
        contentDescription = contentDescription,
        modifier = modifier,
        size = size,
        tint = tint,
    )
}

@Composable
fun SfIcon(
    systemName: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.Unspecified,
) {
    SlooshSfIcon(
        systemName = systemName,
        contentDescription = contentDescription,
        modifier = modifier,
        size = size,
        tint = tint,
    )
}
