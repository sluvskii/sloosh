package com.slooshfilm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.slooshfilm.app.R

// 1. Define the font family
private val sfProFamily = FontFamily(
    Font(R.font.sf_pro_display_semibold, FontWeight.Normal),
    Font(R.font.sf_pro_display_semibold, FontWeight.Medium),
    Font(R.font.sf_pro_display_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_display_bold, FontWeight.Bold)
)

// 2. Create the Typography object with the new font family and adjusted letter spacing
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp
    ),
    bodySmall = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp
    ),
    labelMedium = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = sfProFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandYellowGreen,
    secondary = BrandYellowGreen,
    background = AppBackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    surfaceTint = Color.Transparent,
    onPrimary = Black,
    onSecondary = Black,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = BrandYellowGreen,
    secondary = BrandYellowGreen,
    background = AppBackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    surfaceTint = Color.Transparent,
    onPrimary = Black,
    onSecondary = Black,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun SlooshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val finalColorScheme = if (isAmoled && darkTheme) {
        baseColorScheme.copy(
            background = AppBackgroundAmoled,
            surface = AppBackgroundAmoled
        )
    } else {
        baseColorScheme
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = AppTypography,
        content = content
    )
}
