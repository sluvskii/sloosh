package com.slooshfilm.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.R
import com.slooshfilm.app.ui.components.LocalGlassBlurMultiplier
import com.slooshfilm.app.ui.components.LocalGlassEnabled
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.LocalSettingsBackdrop
import com.slooshfilm.app.ui.components.LocalSlooshMenuHostState
import com.slooshfilm.app.ui.components.ProgressiveBlurTokens
import com.slooshfilm.app.ui.components.SettingsMenuTrigger
import com.slooshfilm.app.ui.components.SlooshFloatingButton
import com.slooshfilm.app.ui.components.SlooshGlassDialog
import com.slooshfilm.app.ui.components.SlooshGlassMenuOverlay
import com.slooshfilm.app.ui.components.SlooshGlassSettingsProvider
import com.slooshfilm.app.ui.components.SlooshGroupedList
import com.slooshfilm.app.ui.components.SlooshMenuState
import com.slooshfilm.app.ui.components.SlooshMenuTokens
import com.slooshfilm.app.ui.components.SlooshToggle
import com.slooshfilm.app.ui.components.rememberCutoutHorizontalPadding
import com.slooshfilm.app.ui.components.rememberNavBarBottomPadding
import com.slooshfilm.app.ui.sficons.SfIcon
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import com.slooshfilm.app.ui.theme.SlooshAccent
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.GlassSurfaceStyle
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.progressiveBackdropBlur
import io.github.neoruaa.glasmio.glass.render.backdrops.LayerBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.layerBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.glass.render.drawBackdrop
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun SettingsScaffold(
    title: String,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    sceneBackdrop: LayerBackdrop? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val screenBg = MaterialTheme.colorScheme.background
    val effectiveSceneBackdrop = sceneBackdrop ?: rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }
    val contentBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }
    val menuState = remember { SlooshMenuState() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val rawStatusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarPadding = if (rawStatusBarPadding > 0.dp) rawStatusBarPadding else (if (isLandscape) 8.dp else 0.dp)
    val headerBlurHeight = statusBarPadding + (if (isLandscape) 48.dp else 64.dp)

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val headerFadeDistancePx = remember(density) { with(density) { 56.dp.toPx() } }
    val headerScrollFraction by remember(scrollState, headerFadeDistancePx) {
        derivedStateOf {
            (scrollState.value.toFloat() / headerFadeDistancePx).coerceIn(0f, 1f)
        }
    }
    val headerAlpha by animateFloatAsState(
        targetValue = headerScrollFraction,
        animationSpec = tween(durationMillis = 200),
        label = "settingsHeaderAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // 1. Edge-to-Edge Content and Progressive Blur Header captured in sceneBackdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(effectiveSceneBackdrop)
        ) {
            // Scrollable Content Layer captured in contentBackdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(contentBackdrop)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(screenBg)
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val cutoutPadding = rememberCutoutHorizontalPadding()
                    val navBarBottomPadding = rememberNavBarBottomPadding()
                    val bottomTotalPadding = navBarBottomPadding + (if (isLandscape) 48.dp else 70.dp)
                    Column(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .navigationBarsPadding()
                            .padding(horizontal = cutoutPadding)
                            .padding(top = headerBlurHeight + (if (isLandscape) 4.dp else 12.dp), bottom = bottomTotalPadding)
                    ) {
                        SlooshGlassSettingsProvider(
                            backdrop = contentBackdrop,
                            glassEnabled = LocalGlassEnabled.current,
                            progressiveBlurEnabled = LocalProgressiveBlur.current,
                            blurMultiplier = LocalGlassBlurMultiplier.current
                        ) {
                            CompositionLocalProvider(
                                LocalSlooshMenuHostState provides menuState
                            ) {
                                content()
                            }
                        }
                    }
                }
            }

            // Floating Progressive Blur Top Header (Apple TV+ Style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerBlurHeight)
                    .align(Alignment.TopCenter)
            ) {
                if (LocalProgressiveBlur.current) {
                    val isLightTheme = !LocalSlooshColors.current.isDark
                    // Progressive blur layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = headerAlpha }
                            .drawBackdrop(
                                backdrop = contentBackdrop,
                                shape = { RectangleShape },
                                effects = {
                                    val topOffsetPx = statusBarPadding.toPx()
                                    progressiveBackdropBlur(
                                maxRadius = ProgressiveBlurTokens.MaxRadius.toPx(),
                                startYFraction = ((topOffsetPx) / size.height.coerceAtLeast(1f)).coerceIn(0f, 1f),
                                endYFraction = (((topOffsetPx) + ((size.height - topOffsetPx).coerceAtLeast(1f))) / size.height.coerceAtLeast(1f)).coerceIn(0f, 1f)
                            )
                                },
                                highlight = null,
                                shadow = null
                            )
                    )
                }

                // Ambient gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = headerAlpha }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    screenBg,
                                    screenBg.copy(alpha = 0f)
                                )
                            )
                        )
                )
            }
        }

        // Floating TopBar with genuine Liquid Glass Back Button (refracting sceneBackdrop)
        SlooshGlassSettingsProvider(
            backdrop = effectiveSceneBackdrop,
            glassEnabled = LocalGlassEnabled.current,
            progressiveBlurEnabled = LocalProgressiveBlur.current,
            blurMultiplier = LocalGlassBlurMultiplier.current
        ) {
            val cutoutPadding = rememberCutoutHorizontalPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding)
                    .height(if (isLandscape) 44.dp else 56.dp)
                    .padding(horizontal = 16.dp + cutoutPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 60.dp)
                )
                onBackClick?.let {
                    SlooshFloatingButton(
                        onClick = it,
                        backdrop = effectiveSceneBackdrop,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(44.dp)
                    ) {
                        SfIcon(
                        systemName = "chevron.left",
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 24.dp,
                        weight = FontWeight.SemiBold,
                        modifier = Modifier.offset(x = (-1.5).dp)
                    )
                    }
                }
            }
        }

        // Topmost Liquid Glass Context Menu Overlay
        SlooshGlassMenuOverlay(
            state = menuState,
            backdrop = effectiveSceneBackdrop
        )
    }
}

@Composable
fun TopBar(title: String, onBackClick: (() -> Unit)? = null) {
    val cutoutPadding = rememberCutoutHorizontalPadding()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp + cutoutPadding, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        onBackClick?.let {
            SlooshFloatingButton(
                onClick = it,
                backdrop = LocalSettingsBackdrop.current,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
            ) {
                SfIcon(
                        systemName = "chevron.left",
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 24.dp,
                        weight = FontWeight.SemiBold,
                        modifier = Modifier.offset(x = (-1.5).dp)
                    )
            }
        }
    }
}

/**
 * Pure icon container for settings rows without background badge.
 */
@Composable
fun SettingsIconBadge(
    iconSfName: String,
    badgeColor: Color = Color.Transparent,
    iconTint: Color = SlooshAccent,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .then(
                if (badgeColor != Color.Transparent) {
                    Modifier
                        .clip(RoundedRectangle(8.dp))
                        .background(badgeColor)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        SfIcon(
            systemName = iconSfName,
            contentDescription = null,
            size = 22.dp,
            tint = iconTint
        )
    }
}

@Composable
fun SettingsDivider(
    modifier: Modifier = Modifier,
    startPadding: Dp = 54.dp,
    endPadding: Dp = 16.dp
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startPadding, end = endPadding),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp
    )
}

@Composable
fun SettingsSection(
    title: String,
    footer: String = "",
    shape: Shape = RoundedRectangle(22.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (title.isNotEmpty()) {
            Text(
                text = title.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
        SlooshGroupedList(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
        if (footer.isNotEmpty()) {
            Text(
                text = footer,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun SettingsRadioItem(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        AnimatedVisibility(
            visible = selected,
            enter = slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut()
        ) {
            SfIcon(
                systemName = "checkmark.circle.fill",
                contentDescription = "Выбрано",
                tint = Color.White,
                size = 24.dp
            )
        }
    }
}

@Composable
fun SettingsNavigationItem(
    text: String,
    iconSfName: String? = null,
    badgeColor: Color = Color.Transparent,
    iconTint: Color = SlooshAccent,
    textColor: Color = Color.Unspecified,
    detailText: String = "",
    // Исключение для брендовых логотипов (ic_telegram, ic_google) — не для SF Symbols.
    iconPainter: Painter? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconSfName != null) {
            SettingsIconBadge(
                iconSfName = iconSfName,
                badgeColor = badgeColor,
                iconTint = iconTint
            )
            Spacer(modifier = Modifier.width(14.dp))
        } else if (iconPainter != null) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = iconTint
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(
            text = text,
            fontSize = 16.sp,
            color = if (textColor != Color.Unspecified) textColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (detailText.isNotEmpty()) {
            Text(
                text = detailText,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        SfIcon(
            systemName = "chevron.right",
            contentDescription = null,
            size = 14.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        )
    }
}

@Composable
fun SettingsInfoRow(
    title: String,
    value: String,
    iconSfName: String? = null,
    badgeColor: Color = Color.Transparent,
    iconTint: Color = SlooshAccent,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconSfName != null) {
            SettingsIconBadge(
                iconSfName = iconSfName,
                badgeColor = badgeColor,
                iconTint = iconTint
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(
            text = title,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsValueItem(
    title: String,
    value: String,
    iconSfName: String? = null,
    subtitle: String = "",
    badgeColor: Color = Color.Transparent,
    iconTint: Color = SlooshAccent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconSfName != null) {
            SettingsIconBadge(
                iconSfName = iconSfName,
                badgeColor = badgeColor,
                iconTint = iconTint
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        SfIcon(
            systemName = "chevron.right",
            contentDescription = null,
            size = 14.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        )
    }
}

/**
 * Settings item with authentic iOS pull-down Liquid Glass context menu.
 * Displays the current value in SlooshAccent with double chevron (↕) on the right,
 * which morphs smoothly into a glass dropdown menu on tap.
 */
@Composable
fun SettingsMenuItem(
    title: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    iconSfName: String? = null,
    subtitle: String = "",
    badgeColor: Color = Color.Transparent,
    iconTint: Color = SlooshAccent,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconSfName != null) {
            SettingsIconBadge(
                iconSfName = iconSfName,
                badgeColor = badgeColor,
                iconTint = iconTint
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        SettingsMenuTrigger(
            selectedValue = selectedValue,
            options = options,
            onOptionSelected = onOptionSelected,
            backdrop = LocalSettingsBackdrop.current
        )
    }
}

@Composable
fun SettingsSegmentedRow(
    title: String,
    iconSfName: String? = null,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (iconSfName != null) {
                SettingsIconBadge(iconSfName = iconSfName)
                Spacer(modifier = Modifier.width(14.dp))
            }
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Сегмент-контрол рисует движок glasmio: дорожка, стеклянный индикатор с линзовым
        // преломлением и физика перетаскивания приходят из вендоренного glasmio-ui.
        val segmentedItems = remember(options) { options.mapIndexed { index, label -> index to label } }
        GlasmioSegmentedControl(
            items = segmentedItems,
            selected = selectedIndex,
            onSelected = onOptionSelected,
            modifier = Modifier.fillMaxWidth(),
            height = 36.dp,
            // У движка параметр не-null: если настройки ещё не поставили свой бэкдроп,
            // берём унаследованный (внутри провайдера он всегда задан).
            backdrop = LocalSettingsBackdrop.current ?: LocalGlassBackdrop.current,
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String = "",
    iconSfName: String? = null,
    badgeColor: Color = Color.Transparent,
    iconTint: Color = SlooshAccent,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (iconSfName != null) {
            SettingsIconBadge(
                iconSfName = iconSfName,
                badgeColor = badgeColor,
                iconTint = iconTint
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        SlooshToggle(
            selected = { checked },
            onSelect = onCheckedChange,
            // Строка настройки не наследует бэкдроп: вендоренный GlasmioGroupedList его не
            // прокидывает, поэтому передаём бэкдроп экрана явно.
            backdrop = LocalSettingsBackdrop.current
        )
    }
}

@Composable
fun SettingsActionItem(
    text: String,
    iconSfName: String? = null,
    iconTint: Color = Color(0xFFFF453A),
    badgeColor: Color = Color.Transparent,
    textColor: Color = Color(0xFFFF453A),
    detailText: String = "",
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconSfName != null) {
            SettingsIconBadge(
                iconSfName = iconSfName,
                badgeColor = badgeColor,
                iconTint = iconTint
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(
            text = text,
            fontSize = 16.sp,
            color = textColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = SlooshAccent,
                strokeWidth = 2.dp
            )
        } else if (detailText.isNotEmpty()) {
            Text(
                text = detailText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun IntervalStepper(
    label: String,
    value: Int,
    iconSfName: String? = null,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (iconSfName != null) {
                SettingsIconBadge(iconSfName = iconSfName)
                Spacer(modifier = Modifier.width(14.dp))
            }
            Text(
                text = label,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton(enabled = value > 5, onStep = { onValueChange(-5) }) {
                SfIcon(
                    systemName = "minus",
                    contentDescription = "Уменьшить",
                    size = 16.dp
                )
            }
            Text(
                text = "$value сек",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            StepperButton(enabled = value < 60, onStep = { onValueChange(5) }) {
                SfIcon(
                    systemName = "plus",
                    contentDescription = "Увеличить",
                    size = 16.dp
                )
            }
        }
    }
}

@Composable
fun StepperButton(enabled: Boolean, onStep: () -> Unit, content: @Composable () -> Unit) {
    val alpha by animateFloatAsState(targetValue = if (enabled) 1f else 0.4f, label = "alpha")
    val size by animateDpAsState(targetValue = if (enabled) 36.dp else 32.dp, label = "size")
    // `onPress` — suspend-лямбда в `PressGestureScope`, поэтому корутину повтора запускаем
    // из внешнего scope экрана, а не из него самого.
    val pressScope = rememberCoroutineScope()

    // Ступпер строки настроек: та же стеклянная капсула glasmio, что и у остальных кнопок
    // (минус/плюс рядом с числом), вместо прозрачной Material `IconButton`.
    GlassSurface(
        modifier = Modifier
            .size(size)
            .alpha(alpha)
            // Удержание ступпера продолжает шагать значение (как раньше через press-events
            // Material-кнопки): 500 мс ожидания, затем шаг каждые 100 мс.
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        val repeat = pressScope.launch {
                            delay(500)
                            while (isActive) {
                                onStep()
                                delay(100)
                            }
                        }
                        tryAwaitRelease()
                        repeat.cancel()
                    }
                )
            },
        backdrop = LocalSettingsBackdrop.current ?: LocalGlassBackdrop.current,
        shape = CircleShape,
        style = GlassSurfaceStyle.Navigation,
        enabled = enabled,
        onClick = { if (enabled) onStep() },
    ) {
        val contentColor = if (enabled) SlooshAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * Authentic iOS-styled modal choice dialog with radio checkmarks.
 */
@Composable
fun SettingsChoiceDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    SlooshGlassDialog(
        title = title,
        onDismissRequest = onDismiss
    ) { dismiss ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SlooshMenuTokens.itemHeight)
                        .clip(SlooshMenuTokens.itemShape)
                        .background(if (isSelected) Color.White.copy(alpha = 0.10f) else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOptionSelected(option)
                            dismiss()
                        }
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        SfIcon(
                            systemName = "checkmark",
                            contentDescription = "Выбрано",
                            tint = Color.White,
                            size = 18.dp
                        )
                    }
                }
            }
        }
    }
}
