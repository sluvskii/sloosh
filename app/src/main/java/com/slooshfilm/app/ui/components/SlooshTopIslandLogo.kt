package com.slooshfilm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.slooshfilm.app.ui.theme.SlooshAccent

/**
 * Global Dynamic Island "SLOOSH" logo capsule matching iOS ContentView 1:1.
 */
@Composable
fun SlooshTopIslandLogo(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(Capsule())
                .background(SlooshAccent)
                .padding(horizontal = 14.dp, vertical = 2.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SLOOSH",
                color = Color.Black,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
        }
    }
}
