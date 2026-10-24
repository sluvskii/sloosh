package com.slooshfilm.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Raw static constants
val RawBackgroundDark = Color(0xFF050505) // Deep flagship near-black, richer than pure black
val RawBackgroundLight = Color(0xFFF2F2F7) // Authentic Apple iOS System Grouped Background
val RawSurfaceDark = Color(0xFF111111)
val RawSurfaceLight = Color(0xFFFFFFFF)
val RawSurfaceVariantDark = Color(0xFF1C1C1E)
val RawSurfaceVariantLight = Color(0xFFE5E5EA)

val RawTextPrimaryDark = Color(0xFFFFFFFF)
val RawTextPrimaryLight = Color(0xFF000000)
val RawTextSecondaryDark = Color(0xB3FFFFFF)     // 70% opacity white
val RawTextSecondaryLight = Color(0x99000000)   // 60% opacity black
val RawTextTertiaryDark = Color(0x80FFFFFF)      // 50% opacity white
val RawTextTertiaryLight = Color(0x61000000)     // 38% opacity black
val RawTextMutedDark = Color(0x66FFFFFF)          // 40% opacity white
val RawTextMutedLight = Color(0x40000000)         // 25% opacity black
val RawTextPlaceholderDark = Color(0x40FFFFFF)    // 25% opacity white
val RawTextPlaceholderLight = Color(0x29000000)   // 16% opacity black

// Static background constants (available for DrawScope, canvas, and backdrops)
val BackgroundDark = RawBackgroundDark
val BackgroundLight = RawBackgroundLight

// Accent (Official iOS slooshAccent: dark = rgb(0.70, 1.0, 0.0) -> #B2FF00, light = rgb(0.45, 0.80, 0.0) -> #73CC00)
val RawSlooshAccentDark = Color(0xFFB2FF00)
val RawSlooshAccentLight = Color(red = 0.45f, green = 0.80f, blue = 0.0f, alpha = 1.0f) // Authentic iOS slooshAccent for light mode (#73CC00)
val SlooshAccentDark = RawSlooshAccentDark
val SlooshAccentLight = RawSlooshAccentLight

// Liquid Glass Surfaces (iOS 27 authentic vibrant smoked glass)
val LiquidGlassSurfaceDark = Color.White.copy(alpha = 0.05f)       // Authentic Apple Liquid Glass crystalline vibrant tint
val LiquidGlassSurfaceLight = Color.White.copy(alpha = 0.82f)     // Authentic Apple Liquid Glass crystalline white tint for light mode
val GlassSurfaceDark = Color(0xCC1A1A1A)       // 80% opacity dark glass
val GlassSurfaceElevated = Color(0xE6222222)   // 90% opacity elevated glass
val GlassBorderDark = Color(0x26FFFFFF)         // 15% opacity white border
val GlassBorderHighlight = Color(0x40FFFFFF)    // 25% opacity white border for focus/active

// Rating colors matching the iOS thresholds and palette.
val RatingIosGreen = Color(0xFF1FBA4D)
val RatingIosGray = Color(0xFF808C99)
val RatingIosRed = Color(0xFFE62E2E)

fun ratingColor(rating: Double): Color = when {
    rating >= 7.5 -> RatingIosGreen
    rating >= 5.0 -> RatingIosGray
    else -> RatingIosRed
}

@Composable
@ReadOnlyComposable
fun ageRatingColor(rating: String): Color {
    val clean = rating.trim().lowercase()
    return when {
        clean.contains("18") || clean == "r" || clean == "nc-17" -> Color(0xFFF23D3D)
        clean.contains("16") || clean == "tv-ma" -> Color(0xFFFF8C00)
        clean.contains("12") || clean == "pg-13" || clean == "tv-14" -> Color(0xFFFFC208)
        clean.contains("6") || clean == "pg" || clean == "tv-pg" -> Color(0xFF2699FA)
        clean.contains("0") || clean == "g" || clean == "tv-g" || clean == "tv-y" -> Color(0xFF2ECC70)
        else -> TextSecondaryDark
    }
}

// =========================================================================
// Semantic Dynamic Design Tokens (iOS 1:1, auto-switching based on active theme)
// =========================================================================

val SlooshAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.primary

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.textSecondary

val TextTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.textTertiary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.textMuted

val TextPlaceholder: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.textPlaceholder

val Background: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.background

val Surface: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.surface

val SurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.surfaceVariant

// Backward-compatible dynamic aliases (redirects all existing call sites seamlessly)
val TextPrimaryDark: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.textPrimary

val TextSecondaryDark: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.textSecondary

val TextTertiaryDark: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.textTertiary

val TextMutedDark: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.textMuted

val TextPlaceholderDark: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.textPlaceholder

val SurfaceDark: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.surface

val SurfaceVariantDark: Color
    @Composable
    @ReadOnlyComposable
    get() = SlooshTheme.colors.surfaceVariant
