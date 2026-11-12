package com.slooshfilm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.ui.sficons.SfIcon
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import com.slooshfilm.app.ui.theme.SlooshAccent
import com.slooshfilm.app.ui.theme.SurfaceDark
import com.slooshfilm.app.ui.theme.TextPrimary
import com.slooshfilm.app.ui.theme.TextSecondary
import com.slooshfilm.app.ui.theme.TextTertiary

/**
 * Аутентичная карточка сгруппированного списка в стиле iOS 27 / Apple TV+ Settings.
 */
@Composable
fun LiquidGroupedList(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isGlassEnabled = LocalGlassEnabled.current
    val isDark = LocalSlooshColors.current.isDark
    val shape = RoundedRectangle(22.dp)
    val containerColor = if (isDark) SurfaceDark else Color.White

    val baseModifier = modifier
        .fillMaxWidth()
        .clip(shape)

    val surfaceModifier = if (isGlassEnabled && backdrop != null) {
        baseModifier.liquidGlassSurface(
            style = LiquidGlassTokens.Surface,
            shape = shape,
            containerColor = containerColor.copy(alpha = if (isDark) 0.85f else 0.92f),
            backdrop = backdrop,
        )
    } else {
        baseModifier.background(containerColor, shape)
    }

    Column(modifier = surfaceModifier) {
        content()
    }
}

/**
 * Заголовок секции списка настроек в стиле iOS (13sp, uppercase или естественный регистр с отступом).
 */
@Composable
fun LiquidSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(top = 8.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Строка сгруппированного списка iOS (52dp по умолчанию, 64dp при наличии подзаголовка).
 * Разделитель автоматически отступает слева, пропуская иконку, строго как в iOS Settings.
 */
@Composable
fun LiquidListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    detail: String? = null,
    iconSfName: String? = null,
    iconTint: Color = SlooshAccent,
    iconBackground: Color? = null,
    showTopSeparator: Boolean = false,
    enabled: Boolean = true,
    showDisclosure: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSlooshColors.current.isDark
    val separatorColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val hasLeadingIcon = iconSfName != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
    ) {
        if (showTopSeparator) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (hasLeadingIcon) 54.dp else 16.dp),
                thickness = 0.5.dp,
                color = separatorColor
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = if (subtitle == null) 52.dp else 64.dp)
                .then(
                    if (onClick != null && enabled) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClick()
                            }
                        )
                    } else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconSfName != null) {
                if (iconBackground != null) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedRectangle(7.dp))
                            .background(iconBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        SfIcon(
                            systemName = iconSfName,
                            contentDescription = null,
                            tint = iconTint,
                            size = 18.dp
                        )
                    }
                } else {
                    SfIcon(
                        systemName = iconSfName,
                        contentDescription = null,
                        tint = iconTint,
                        size = 22.dp
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (detail != null) {
                Text(
                    text = detail,
                    fontSize = 15.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            } else if (showDisclosure || onClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                SfIcon(
                    systemName = "chevron.right",
                    contentDescription = null,
                    tint = TextTertiary,
                    size = 14.dp
                )
            }
        }
    }
}

/**
 * Строка настройки со встроенным жидким переключателем (LiquidToggle) в стиле iOS.
 */
@Composable
fun LiquidListSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconSfName: String? = null,
    iconTint: Color = SlooshAccent,
    iconBackground: Color? = null,
    showTopSeparator: Boolean = false,
    enabled: Boolean = true
) {
    LiquidListRow(
        title = title,
        subtitle = subtitle,
        iconSfName = iconSfName,
        iconTint = iconTint,
        iconBackground = iconBackground,
        showTopSeparator = showTopSeparator,
        enabled = enabled,
        showDisclosure = false,
        onClick = if (enabled) { { onCheckedChange(!checked) } } else null,
        trailing = {
            LiquidToggle(
                selected = { checked },
                onSelect = onCheckedChange
            )
        },
        modifier = modifier
    )
}
