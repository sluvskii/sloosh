package com.slooshfilm.app.ui.splash

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slooshfilm.app.R
import com.slooshfilm.app.ui.theme.BackgroundDark
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import com.slooshfilm.app.ui.theme.RawSlooshAccentDark
import com.slooshfilm.app.ui.theme.RawSlooshAccentLight
import com.slooshfilm.app.ui.theme.SlooshFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Премиальный экран запуска (Splash Screen) в точном соответствии с iOS sloosh LaunchSplashView:
 * - Глубокий премиальный тёмный фон (BackgroundDark #050505)
 * - Атмосферная круговая неоновая подсветка логотипа фирменного цвета slooshAccent с плавным радиальным затуханием в ноль
 * - Векторный логотип `LogoText` в цвете slooshAccent с пружинной физикой появления (Spring)
 * - Световой луч (Shimmer beam), плавно скользящий строго по контуру букв логотипа
 * - Номер версии приложения внизу экрана
 * - Тактильный отклик (Haptic Feedback)
 * - Мягкое бесшовное растворение при переходе к контенту приложения
 */
@Composable
fun LaunchSplashView(
    onDismiss: () -> Unit
) {
    val logoScale = remember { Animatable(0.92f) }
    val glowScale = remember { Animatable(0.85f) }
    val glowOpacity = remember { Animatable(0.65f) }
    val shimmerOffset = remember { Animatable(-260f) }
    val contentOpacity = remember { Animatable(1.0f) }
    val versionOpacity = remember { Animatable(0.0f) }

    val view = LocalView.current
    val context = LocalContext.current

    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val vName = pInfo.versionName ?: "1.0"
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            "Версия $vName ($vCode)"
        } catch (_: Exception) {
            "Версия 1.0 (1)"
        }
    }

    LaunchedEffect(Unit) {
        // Фаза 1: Нежное пружинное раскрытие логотипа, неоновой подсветки и версии
        launch {
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = 300f
                )
            )
        }
        launch {
            glowScale.animateTo(
                targetValue = 1.15f,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = 300f
                )
            )
        }
        launch {
            glowOpacity.animateTo(
                targetValue = 0.85f,
                animationSpec = tween(durationMillis = 600)
            )
        }
        launch {
            versionOpacity.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 600)
            )
        }

        // Тактильный отклик через 120ms
        launch {
            delay(120)
            try {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            } catch (_: Exception) {}
        }

        // Фаза 2: Скольжение светового луча по буквам логотипа (через 250ms)
        launch {
            delay(250)
            shimmerOffset.animateTo(
                targetValue = 260f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
        }

        // Фаза 3: Плавное растворение экрана заставки и переход в приложение (через 1450ms)
        delay(1450)
        launch {
            logoScale.animateTo(
                targetValue = 1.08f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
        launch {
            glowOpacity.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(durationMillis = 400)
            )
        }
        launch {
            contentOpacity.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }

        delay(420)
        onDismiss()
    }

    val isDark = LocalSlooshColors.current.isDark
    val splashBg = if (isDark) BackgroundDark else Color.White
    val logoTint = if (isDark) RawSlooshAccentDark else RawSlooshAccentLight
    val glowColor = if (isDark) RawSlooshAccentDark else RawSlooshAccentLight
    val peakAlpha = if (isDark) 0.18f else 0.10f
    val shimmerColor = if (isDark) Color.White else RawSlooshAccentDark
    val versionTextColor = if (isDark) Color.White.copy(alpha = 0.38f) else Color.Black.copy(alpha = 0.38f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(splashBg)
            .graphicsLayer { alpha = contentOpacity.value }
    ) {
        // Атмосферная мягкая неоновая подсветка логотипа:
        // Эллиптическая форма, плавно обнимающая горизонтальный логотип sloosh,
        // с непрерывным гауссовым затуханием без ступеней, полос и жестких границ
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(320.dp)
                .graphicsLayer {
                    val scale = glowScale.value
                    scaleX = scale * 1.35f
                    scaleY = scale * 0.72f
                    alpha = glowOpacity.value
                }
        ) {
            val radiusPx = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    0.00f to glowColor.copy(alpha = peakAlpha * 1.00f),
                    0.15f to glowColor.copy(alpha = peakAlpha * 0.88f),
                    0.30f to glowColor.copy(alpha = peakAlpha * 0.65f),
                    0.45f to glowColor.copy(alpha = peakAlpha * 0.40f),
                    0.60f to glowColor.copy(alpha = peakAlpha * 0.20f),
                    0.75f to glowColor.copy(alpha = peakAlpha * 0.08f),
                    0.88f to glowColor.copy(alpha = peakAlpha * 0.02f),
                    1.00f to Color.Transparent,
                    center = center,
                    radius = radiusPx
                ),
                radius = radiusPx,
                center = center
            )
        }

        // Центральный векторный логотип со скользящим световым лучом (Shimmer beam)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .scale(logoScale.value)
        ) {
            // Базовый векторный логотип sloosh в фирменном акцентном цвете
            Image(
                painter = painterResource(id = R.drawable.ic_sloosh_logo),
                contentDescription = "sloosh",
                colorFilter = ColorFilter.tint(logoTint),
                modifier = Modifier.size(width = 240.dp, height = 61.dp)
            )

            // Бегущий луч света строго по геометрии букв логотипа
            Image(
                painter = painterResource(id = R.drawable.ic_sloosh_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(shimmerColor),
                modifier = Modifier
                    .size(width = 240.dp, height = 61.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        val shimmerWidthPx = 100.dp.toPx()
                        val startX = (size.width / 2f) + shimmerOffset.value.dp.toPx() - (shimmerWidthPx / 2f)
                        val endX = startX + shimmerWidthPx
                        val beamAlpha = if (isDark) 0.95f else 0.85f
                        drawRect(
                            brush = Brush.linearGradient(
                                0.0f to Color.Transparent,
                                0.5f to shimmerColor.copy(alpha = beamAlpha),
                                1.0f to Color.Transparent,
                                start = Offset(startX, 0f),
                                end = Offset(endX, 0f)
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )
        }

        // Аккуратная версия приложения снизу экрана (как в настройках)
        Text(
            text = appVersion,
            fontFamily = SlooshFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = versionTextColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { alpha = versionOpacity.value }
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        )
    }
}
