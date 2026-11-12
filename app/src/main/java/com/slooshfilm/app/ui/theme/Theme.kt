package com.slooshfilm.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import io.github.neoruaa.glasmio.core.GlassColors
import io.github.neoruaa.glasmio.core.GlassAppearance
import io.github.neoruaa.glasmio.core.GlassDimensions
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.core.LocalGlassAppearance
import io.github.neoruaa.glasmio.core.LocalGlassDimensions

@Immutable
data class SlooshColors(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,
    val textPlaceholder: Color,
    val liquidGlassSurface: Color,
    val liquidGlassBorder: Color,
    val outline: Color,
    val outlineVariant: Color,
    val isDark: Boolean
)

val DarkSlooshColors = SlooshColors(
    primary = SlooshAccentDark,
    onPrimary = Color.Black,
    background = RawBackgroundDark,
    onBackground = RawTextPrimaryDark,
    surface = RawSurfaceDark,
    onSurface = RawTextPrimaryDark,
    surfaceVariant = RawSurfaceVariantDark,
    onSurfaceVariant = RawTextSecondaryDark,
    textPrimary = RawTextPrimaryDark,
    textSecondary = RawTextSecondaryDark,
    textTertiary = RawTextTertiaryDark,
    textMuted = RawTextMutedDark,
    textPlaceholder = RawTextPlaceholderDark,
    liquidGlassSurface = LiquidGlassSurfaceDark,
    liquidGlassBorder = GlassBorderDark,
    outline = Color(0x26FFFFFF),
    outlineVariant = Color(0x14FFFFFF),
    isDark = true
)

val LightSlooshColors = SlooshColors(
    primary = SlooshAccentLight,
    onPrimary = Color.White,
    background = RawBackgroundLight,
    onBackground = RawTextPrimaryLight,
    surface = RawSurfaceLight,
    onSurface = RawTextPrimaryLight,
    surfaceVariant = RawSurfaceVariantLight,
    onSurfaceVariant = RawTextSecondaryLight,
    textPrimary = RawTextPrimaryLight,
    textSecondary = RawTextSecondaryLight,
    textTertiary = RawTextTertiaryLight,
    textMuted = RawTextMutedLight,
    textPlaceholder = RawTextPlaceholderLight,
    liquidGlassSurface = LiquidGlassSurfaceLight,
    liquidGlassBorder = Color.Black.copy(alpha = 0.35f),
    outline = Color(0x1F000000),
    outlineVariant = Color(0x0F000000),
    isDark = false
)

val LocalSlooshColors = staticCompositionLocalOf { DarkSlooshColors }

object SlooshTheme {
    val colors: SlooshColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSlooshColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = SlooshAccentDark,
    onPrimary = Color.Black,
    background = RawBackgroundDark,
    onBackground = RawTextPrimaryDark,
    surface = RawSurfaceDark,
    onSurface = RawTextPrimaryDark,
    surfaceVariant = RawSurfaceVariantDark,
    onSurfaceVariant = RawTextSecondaryDark,
    outline = Color(0x26FFFFFF),
    outlineVariant = Color(0x14FFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = SlooshAccentLight,
    onPrimary = Color.White,
    background = RawBackgroundLight,
    onBackground = RawTextPrimaryLight,
    surface = RawSurfaceLight,
    onSurface = RawTextPrimaryLight,
    surfaceVariant = RawSurfaceVariantLight,
    onSurfaceVariant = RawTextSecondaryLight,
    outline = Color(0x1F000000),
    outlineVariant = Color(0x0F000000)
)

@Composable
fun SlooshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val slooshColors = if (darkTheme) DarkSlooshColors else LightSlooshColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val glassColors = remember(darkTheme, slooshColors) {
        GlassColors(
            isDark = darkTheme,
            accent = slooshColors.primary,
            content = slooshColors.textPrimary,
            secondaryContent = slooshColors.textSecondary,
            container = slooshColors.liquidGlassSurface,
            prominentContainer = if (darkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
            subtleStroke = slooshColors.liquidGlassBorder,
            groupedBackground = slooshColors.background,
            elevatedBackground = slooshColors.surface,
            separator = slooshColors.outlineVariant,
            tertiaryContent = slooshColors.textTertiary,
            destructive = RatingIosRed,
        )
    }
    val glassAppearance = remember {
        GlassAppearance(
            liquidGlassEnabled = true,
            reduceTransparency = false,
            adaptiveColorScheme = false
        )
    }
    val glassDimensions = remember {
        GlassDimensions()
    }

    CompositionLocalProvider(
        LocalIndication provides ripple(),
        LocalSlooshColors provides slooshColors,
        LocalGlassColors provides glassColors,
        LocalGlassAppearance provides glassAppearance,
        LocalGlassDimensions provides glassDimensions,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SlooshTypography
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides TextStyle(fontFamily = SlooshFontFamily),
                content = content
            )
        }
    }
}
