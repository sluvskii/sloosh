package com.slooshfilm.app.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

private val ambientColorCache = LruCache<String, Color>(100)

@Composable
fun rememberAdaptiveAmbientColor(imageUrl: String?): Color {
    val context = LocalContext.current
    val cached = remember(imageUrl) { if (imageUrl != null) ambientColorCache.get(imageUrl) else null }
    var ambientColor by remember(imageUrl) { mutableStateOf(cached ?: Color.Transparent) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            ambientColor = Color.Transparent
            return@LaunchedEffect
        }
        val memoryHit = ambientColorCache.get(imageUrl)
        if (memoryHit != null) {
            ambientColor = memoryHit
            return@LaunchedEffect
        }
        val extracted = extractAmbientColor(context, imageUrl)
        if (extracted != null) {
            ambientColorCache.put(imageUrl, extracted)
            ambientColor = extracted
        }
    }

    return ambientColor
}

/**
 * Fast average color extraction matching iOS `UIImage.averageColor` 1:1.
 * Downsamples the image to 16x16 and computes the exact arithmetic mean of RGB channels.
 */
private suspend fun extractAmbientColor(context: Context, url: String): Color? = withContext(Dispatchers.IO) {
    try {
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(16, 16)
            .scale(Scale.FILL)
            .allowHardware(false)
            .build()

        val result = loader.execute(request)
        if (result is SuccessResult) {
            val bitmap = result.drawable.toBitmap(width = 16, height = 16, config = Bitmap.Config.ARGB_8888)
            val pixels = IntArray(16 * 16)
            bitmap.getPixels(pixels, 0, 16, 0, 0, 16, 16)
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L

            for (pixel in pixels) {
                rSum += (pixel shr 16) and 0xFF
                gSum += (pixel shr 8) and 0xFF
                bSum += pixel and 0xFF
            }

            val totalPixels = pixels.size
            if (totalPixels > 0) {
                val avgR = (rSum / totalPixels).toInt().coerceIn(0, 255)
                val avgG = (gSum / totalPixels).toInt().coerceIn(0, 255)
                val avgB = (bSum / totalPixels).toInt().coerceIn(0, 255)
                return@withContext Color(avgR, avgG, avgB)
            }
        }
        null
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
