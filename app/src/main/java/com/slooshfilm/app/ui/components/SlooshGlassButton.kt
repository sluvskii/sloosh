package com.slooshfilm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.slooshfilm.app.ui.theme.SlooshFontFamily
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.GlassSurfaceStyle
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.render.Backdrop

/**
 * Кнопки sloosh на движке glasmio (iOS 27 Liquid Glass 1:1).
 *
 * Никакого собственного стекла здесь нет: поверхность и оптику рисует вендоренный
 * [GlassSurface] с материалом навбара (`GlassSurfaceStyle.Navigation`) — то же стекло,
 * что у нижней панели. Раньше это были кнопки самописного `ui/components/LiquidGlass.kt`
 * (файл удалён вместе с движком).
 *
 * [blurRadius] — «морозность» материала (апстримный `frostRadius`);
 * [borderAlpha] — интенсивность кромочного блика (`emphasis`: ≥ 0.75 → `Prominent`).
 */
@Composable
fun SlooshFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    shape: Shape = CircleShape,
    blurRadius: Dp = 20.dp,
    tint: Color? = null,
    borderAlpha: Float = 0.68f,
    elevation: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val emphasis = if (borderAlpha >= 0.75f) GlassEmphasis.Prominent else GlassEmphasis.Regular
    GlassSurface(
        modifier = modifier,
        backdrop = backdrop ?: LocalGlassBackdrop.current,
        shape = shape,
        style = GlassSurfaceStyle.Navigation,
        navigationSurfaceColor = tint,
        emphasis = emphasis,
        onClick = onClick,
        contentAlignment = Alignment.Center,
        frostRadius = blurRadius,
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            contentAlignment = Alignment.Center
        ) { content() }
    }
}

/** Капсульная форма sloosh. */
val SlooshCapsule: Shape = Capsule()

/**
 * Двойная стеклянная кнопка: две иконки в одной капсуле (загрузки + настройки).
 * Рисуется материалом glasmio через [SlooshFloatingButton].
 */
@Composable
fun SlooshDualButton(
    firstOnClick: () -> Unit,
    secondOnClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    shape: Shape = SlooshCapsule,
    blurRadius: Dp = 20.dp,
    tint: Color? = null,
    borderAlpha: Float = 0.68f,
    elevation: Dp = 4.dp,
    firstContent: @Composable BoxScope.() -> Unit,
    secondContent: @Composable BoxScope.() -> Unit
) {
    SlooshFloatingButton(
        onClick = firstOnClick,
        modifier = modifier,
        backdrop = backdrop,
        shape = shape,
        blurRadius = blurRadius,
        tint = tint,
        borderAlpha = borderAlpha,
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = firstOnClick
                    ),
                contentAlignment = Alignment.Center,
                content = firstContent
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = secondOnClick
                    ),
                contentAlignment = Alignment.Center,
                content = secondContent
            )
        }
    }
}

/**
 * Мульти-кнопка: три и более действия в одной непрерывной стеклянной капсуле.
 * Ряд содержимого передаётся вызывающим местом (у плеера — перемотка/пауза/эпизод).
 */
@Composable
fun SlooshMultiButton(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    shape: Shape = SlooshCapsule,
    blurRadius: Dp = 20.dp,
    tint: Color? = null,
    borderAlpha: Float = 0.68f,
    elevation: Dp = 4.dp,
    content: @Composable RowScope.() -> Unit
) {
    SlooshFloatingButton(
        onClick = {},
        modifier = modifier,
        backdrop = backdrop,
        shape = shape,
        blurRadius = blurRadius,
        tint = tint,
        borderAlpha = borderAlpha,
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Главная кнопка-действие iOS: плотная капсула без стекла (`.buttonStyle(.borderedProminent)`).
 *
 * Стекло здесь не нужно и не подделывается: на iOS главный CTA — заливка акцентным/белым цветом,
 * поэтому кнопка остаётся плотной, но живёт в одном месте и не расползается по экранам
 * собственными `ButtonDefaults`. Всё остальное (тулбары, второстепенные действия) — стекло glasmio
 * через [SlooshFloatingButton].
 */
@Composable
fun SlooshPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    containerColor: Color = Color.White,
    contentColor: Color = Color.Black,
    height: Dp = 50.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 32.dp),
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val shape = Capsule()
    Box(
        modifier = modifier
            .height(height)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.30f)
            )
            .clip(shape)
            .background(containerColor.copy(alpha = if (enabled) 1f else 0.40f))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            Row(verticalAlignment = Alignment.CenterVertically, content = content)
        } else {
            Text(
                text = text,
                color = contentColor.copy(alpha = if (enabled) 1f else 0.40f),
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = SlooshFontFamily,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

/**
 * Второстепенная кнопка iOS («Отмена»): стеклянная капсула на движке glasmio.
 *
 * Использует тот же материал навбара, что и остальное стекло, поэтому не появляется
 * «второго» визуального языка рядом с главными кнопками.
 */
@Composable
fun SlooshSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    backdrop: Backdrop? = null,
    height: Dp = 50.dp,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Medium
) {
    GlassSurface(
        modifier = modifier.height(height),
        backdrop = backdrop ?: LocalGlassBackdrop.current,
        shape = Capsule(),
        style = GlassSurfaceStyle.Navigation,
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = SlooshFontFamily,
            letterSpacing = (-0.2).sp
        )
    }
}
