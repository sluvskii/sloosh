/*
 * Vendored from Glasmio (https://github.com/NEORUAA/Glasmio), module glasmio-icons,
 * file GlasmioIcons.kt, commit 700a611 (v0.1.1 local snapshot). Apache-2.0.
 * Copyright 2026 NEORUAA. Package repackaged for sloosh.
 *
 * Local changes vs upstream:
 *  - The Material icon fallback (androidx.compose.material.icons) is removed entirely:
 *    AGENTS.md bans Google Material icons. Missing glyphs render the project's own
 *    vendored ic_sf_* SF Symbol vector drawables via SlooshIconFallback.
 *  - GlasmioIconProvider drops the pluggable vector resolver; the fallback source is
 *    the project drawable map.
 */
package com.slooshfilm.app.ui.sficons

import android.graphics.Typeface
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Optional host-owned symbol font. The library never ships or loads an Apple font. */
val LocalGlasmioSymbolTypeface = staticCompositionLocalOf<Typeface?> { null }

/** Evaluated at each icon call site so nested theme and row locals remain visible. */
val LocalGlasmioIconTint = staticCompositionLocalOf<@Composable () -> Color> {
    { LocalContentColor.current }
}

@Composable
fun GlasmioIconProvider(
    symbolTypeface: Typeface? = null,
    defaultTint: @Composable () -> Color = { LocalContentColor.current },
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalGlasmioSymbolTypeface provides symbolTypeface,
        LocalGlasmioIconTint provides defaultTint,
        content = content,
    )
}

/**
 * Fallback renderer for names the active font cannot display: draws the project's
 * vendored SF Symbol vector drawable (res/drawable/ic_sf_*.xml). If a name has no
 * drawable either, nothing is drawn — never a foreign icon set.
 */
@Composable
internal fun GlasmioResolvedIcon(
    systemName: String,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color,
    size: Dp,
    mirrored: Boolean,
) {
    val resId = SlooshIconFallback.resIdOrNull(systemName) ?: return
    Icon(
        painter = painterResource(resId),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .then(if (mirrored) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier),
        tint = tint,
    )
}

@Composable
fun GlasmioIcon(
    systemName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalGlasmioIconTint.current(),
    size: Dp = 24.dp,
) = SfIcon(systemName, contentDescription, modifier, tint, size)
