package com.slooshfilm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.slooshfilm.app.ui.sficons.SfIcon
import com.slooshfilm.app.ui.theme.TextPrimaryDark
import com.slooshfilm.app.ui.theme.TextSecondaryDark

/**
 * Премиальный экран ошибки в стиле Apple TV+ / iOS:
 * - Элегантная иконка SF Symbol в полупрозрачной стеклянной капсуле
 * - Чёткая иерархия типографики без раскрытия внутренних эндпоинтов и URL
 * - Премиальная белая кнопка повтора (Capsule) без дешёвых ярких заливок
 */
@Composable
fun ErrorStateView(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    iconSfName: String = "antenna.radiowaves.left.and.right",
    retryText: String = "Повторить"
) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Элегантная капсула с иконкой
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(Color.White.copy(alpha = 0.06f), CircleShape)
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            SfIcon(
                systemName = iconSfName,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                size = 36.dp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Заголовок
        Text(
            text = title,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.4).sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Описание ошибки
        Text(
            text = message,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondaryDark,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.widthIn(max = 300.dp)
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Премиальная белая кнопка Apple TV+
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = Capsule(),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
            modifier = Modifier
                .height(50.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = Capsule(),
                    spotColor = Color.Black.copy(alpha = 0.35f)
                )
        ) {
            Text(
                text = retryText,
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
