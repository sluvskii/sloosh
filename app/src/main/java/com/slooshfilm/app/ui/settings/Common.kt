package com.slooshfilm.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import com.slooshfilm.app.ui.theme.LocalSlooshColors
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.R
import kotlin.math.abs
import kotlin.math.roundToInt
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberCanvasBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.backdrop.highlight.Highlight
import com.slooshfilm.app.ui.backdrop.shadow.Shadow
import com.slooshfilm.app.ui.components.ConcentratedHighlightStyle
import com.slooshfilm.app.ui.components.DampedDragAnimation
import com.slooshfilm.app.ui.components.LiquidBottomTab
import com.slooshfilm.app.ui.components.LiquidBottomTabs
import com.slooshfilm.app.ui.components.LiquidGlassFloatingButton
import com.slooshfilm.app.ui.components.LiquidToggle
import com.slooshfilm.app.ui.components.LocalBackdrop
import com.slooshfilm.app.ui.components.glassBorder
import com.slooshfilm.app.ui.components.iosFrostedBlur
import com.slooshfilm.app.ui.components.iosLens
import com.slooshfilm.app.ui.components.iosProgressiveBlur
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.ProgressiveBlurTokens
import com.slooshfilm.app.ui.components.iosVibrantGlass
import com.slooshfilm.app.ui.components.LiquidGlassDialog
import com.slooshfilm.app.ui.components.LiquidMenuTokens
import com.slooshfilm.app.ui.components.LiquidMenuState
import com.slooshfilm.app.ui.components.LocalLiquidMenuHostState
import com.slooshfilm.app.ui.components.LiquidGlassMenuOverlay
import com.slooshfilm.app.ui.components.SettingsMenuTrigger
import com.slooshfilm.app.ui.theme.BackgroundDark
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import com.slooshfilm.app.ui.theme.SlooshAccent
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SettingsScaffold(
    title: String,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val screenBg = MaterialTheme.colorScheme.background
    val contentBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }
    val sceneBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }
    val menuState = remember { LiquidMenuState() }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerBlurHeight = statusBarPadding + 64.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // 1. Edge-to-Edge Content and Progressive Blur Header captured in sceneBackdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(sceneBackdrop)
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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(top = headerBlurHeight + 12.dp, bottom = 96.dp)
                ) {
                    CompositionLocalProvider(LocalLiquidMenuHostState provides menuState) {
                        content()
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
                            .drawBackdrop(
                                backdrop = contentBackdrop,
                                shape = { RectangleShape },
                                effects = {
                                    val topOffsetPx = statusBarPadding.toPx()
                                    iosProgressiveBlur(
                                        maxRadius = ProgressiveBlurTokens.MaxRadius.toPx(),
                                        topOffset = topOffsetPx,
                                        falloffDistance = (size.height - topOffsetPx).coerceAtLeast(1f),
                                        isLight = isLightTheme
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
        CompositionLocalProvider(LocalBackdrop provides sceneBackdrop) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
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
                    LiquidGlassFloatingButton(
                        onClick = it,
                        backdrop = sceneBackdrop,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sf_chevron_left),
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Topmost Liquid Glass Context Menu Overlay
        LiquidGlassMenuOverlay(
            state = menuState,
            backdrop = contentBackdrop
        )
    }
}

@Composable
fun TopBar(title: String, onBackClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            LiquidGlassFloatingButton(
                onClick = it,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sf_chevron_left),
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
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
    icon: Painter,
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
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
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
            color = if (selected) SlooshAccent else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        AnimatedVisibility(
            visible = selected,
            enter = slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_sf_checkmark_circle_fill),
                contentDescription = "Выбрано",
                tint = SlooshAccent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SettingsNavigationItem(
    text: String,
    icon: Painter? = null,
    badgeColor: Color = Color.Transparent,
    iconTint: Color = SlooshAccent,
    textColor: Color = Color.Unspecified,
    detailText: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            SettingsIconBadge(
                icon = icon,
                badgeColor = badgeColor,
                iconTint = iconTint
            )
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
        Icon(
            painter = painterResource(id = R.drawable.ic_sf_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        )
    }
}

@Composable
fun SettingsInfoRow(
    title: String,
    value: String,
    icon: Painter? = null,
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
        if (icon != null) {
            SettingsIconBadge(
                icon = icon,
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
    icon: Painter? = null,
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
        if (icon != null) {
            SettingsIconBadge(
                icon = icon,
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
        Icon(
            painter = painterResource(id = R.drawable.ic_sf_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
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
    icon: Painter? = null,
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
        if (icon != null) {
            SettingsIconBadge(
                icon = icon,
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
            onOptionSelected = onOptionSelected
        )
    }
}

@Composable
fun SettingsSegmentedRow(
    title: String,
    icon: Painter? = null,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !LocalSlooshColors.current.isDark
    // Пилюля сегментов — стекло поверх трека, по которому она ездит, поэтому сэмплируем
    // именно цвет трека (а не фон экрана — он темнее и делал пилюлю чёрной).
    val trackUnderlay = if (isLightTheme) {
        lerpColor(MaterialTheme.colorScheme.surface, Color.Black, 0.08f)
    } else {
        Color(0xFF2C2C2E)
    }
    // Цвет читаем из состояния в момент отрисовки, а не из замыкания: иначе записанные
    // стеклянные слои (пилюля) остаются в цветах прошлой темы до первого взаимодействия.
    val trackUnderlayState = rememberUpdatedState(trackUnderlay)
    val glassBackdrop = rememberCanvasBackdrop {
        drawRect(trackUnderlayState.value)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                SettingsIconBadge(icon = icon)
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

        LiquidBottomTabs(
            selectedTabIndex = { selectedIndex },
            onTabSelected = onOptionSelected,
            backdrop = glassBackdrop,
            tabsCount = options.size,
            barHeight = 36.dp,
            pillHeight = 30.dp,
            pillExtraWidth = 0.dp,
            isStaticBar = true,
            accentColor = if (isLightTheme) Color.Black else Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, option ->
                LiquidBottomTab(
                    onClick = { onOptionSelected(index) },
                    clip = false
                ) {
                    Text(
                        text = option,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String = "",
    icon: Painter? = null,
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
        if (icon != null) {
            SettingsIconBadge(
                icon = icon,
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
        LiquidToggle(
            selected = { checked },
            onSelect = onCheckedChange
        )
    }
}

@Composable
fun SettingsActionItem(
    text: String,
    icon: Painter? = null,
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
        if (icon != null) {
            SettingsIconBadge(
                icon = icon,
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
    icon: Painter? = null,
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
            if (icon != null) {
                SettingsIconBadge(icon = icon)
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
                Icon(
                    painter = painterResource(id = R.drawable.ic_sf_minus),
                    contentDescription = "Уменьшить",
                    modifier = Modifier.size(16.dp)
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
                Icon(
                    painter = painterResource(id = R.drawable.ic_sf_plus),
                    contentDescription = "Увеличить",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun StepperButton(enabled: Boolean, onStep: () -> Unit, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && enabled) {
            delay(500)
            while (true) {
                onStep()
                delay(100)
            }
        }
    }

    val alpha by animateFloatAsState(targetValue = if (enabled) 1f else 0.4f, label = "alpha")
    val size by animateDpAsState(targetValue = if (enabled) 36.dp else 32.dp, label = "size")

    IconButton(
        onClick = { if (enabled) onStep() },
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(size)
            .alpha(alpha)
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

    LiquidGlassDialog(
        title = title,
        onDismissRequest = onDismiss
    ) { dismiss ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LiquidMenuTokens.itemHeight)
                        .clip(LiquidMenuTokens.itemShape)
                        .background(if (isSelected) LiquidMenuTokens.itemHighlightColor else Color.Transparent)
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
                        fontWeight = FontWeight.Normal,
                        color = if (isSelected) SlooshAccent else MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sf_checkmark),
                            contentDescription = "Выбрано",
                            tint = SlooshAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
