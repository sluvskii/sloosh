package com.slooshfilm.app.ui.details

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.UnevenRoundedRectangle
import androidx.compose.ui.res.painterResource
import com.slooshfilm.app.ui.sficons.SfIcon
import com.slooshfilm.app.R
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.slooshfilm.app.ui.backdrop.backdrops.rememberCanvasBackdrop
import com.slooshfilm.app.ui.components.LiquidGlassFloatingButton
import com.slooshfilm.app.ui.theme.LiquidGlassSurfaceDark
import com.slooshfilm.app.ui.theme.SurfaceDark
import com.slooshfilm.app.ui.theme.TextPrimaryDark
import com.slooshfilm.app.ui.theme.TextSecondaryDark
import com.slooshfilm.app.ui.theme.TextTertiaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailerPlayerSheet(
    trailerKey: String,
    title: String,
    onDismiss: () -> Unit
) {
    // The close button sits on the opaque sheet container, so this canvas backdrop
    // is exactly the surface behind it.
    val sheetBackground = SurfaceDark
    val glassBackdrop = rememberCanvasBackdrop {
        drawRect(sheetBackground)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TextTertiaryDark)
            )
        },
        shape = UnevenRoundedRectangle(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Трейлер: $title",
                    fontSize = 18.sp,
                    color = TextPrimaryDark,
                    maxLines = 1
                )

                LiquidGlassFloatingButton(
                    onClick = onDismiss,
                    backdrop = glassBackdrop,
                    shape = CircleShape,
                    tint = LiquidGlassSurfaceDark,
                    modifier = Modifier.size(34.dp)
                ) {
                    SfIcon(
                        systemName = "xmark",
                        contentDescription = "Закрыть",
                        tint = TextSecondaryDark,
                        size = 20.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Embedded YouTube player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedRectangle(16.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            @SuppressLint("SetJavaScriptEnabled")
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            val embedHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <style>body { margin: 0; background: black; }</style>
                                </head>
                                <body>
                                    <iframe width="100%" height="100%" src="https://www.youtube.com/embed/$trailerKey?autoplay=1&playsinline=1" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
