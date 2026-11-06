package com.slooshfilm.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slooshfilm.app.ui.theme.TextPrimaryDark
import com.slooshfilm.app.ui.theme.TextSecondaryDark
import com.slooshfilm.app.ui.theme.TextTertiaryDark

/**
 * Единая информационная заглушка / пустой экран в аутентичном стиле sloosh (Apple TV+):
 * - Элегантная иконка SF Symbol (52dp) в приглушённом цвете TextTertiaryDark
 * - Заголовок (18sp, Bold, TextPrimaryDark)
 * - Описание (14sp, Regular, TextSecondaryDark, выравнивание по центру)
 * - Исключены любые ядовитые зелёные заливки, круги и несогласованные шрифты.
 */
@Composable
fun EmptyStateView(
    @DrawableRes iconRes: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 52.dp,
    iconTint: Color = TextTertiaryDark
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.3).sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondaryDark,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.widthIn(max = 320.dp)
        )
    }
}
