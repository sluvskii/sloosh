package com.slooshfilm.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slooshfilm.app.ui.sficons.SfIcon
import com.slooshfilm.app.ui.theme.SlooshAccent
import com.slooshfilm.app.ui.theme.SlooshFontFamily
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.GlassSurfaceStyle
import io.github.neoruaa.glasmio.glass.LocalBlurBackdrop
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import androidx.compose.ui.geometry.Rect
import com.kyant.shapes.Capsule
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * Диалоги и меню sloosh на движке glasmio.
 *
 * Заменяют удалённые самописные `LiquidGlassDialog` / `LiquidGlassAlertDialog` /
 * `LiquidGlassMenuOverlay` / `LiquidGlassPlayerDialog` самописного движка: поверхность, оптика и
 * кинематика берутся из вендоренного движка glasmio (`GlassSurface` с материалом навбара,
 * `GlasmioMenu`/`GlasmioMenuItem` для контекстных списков) — то же стекло, что у нижнего навбара.
 *
 * Публичные имена сохранены, чтобы не переписывать десятки мест вызова.
 */

// ---------------------------------------------------------------------------------------------
// Модальное окно (iOS UIAlertController — стеклянная карточка)
// ---------------------------------------------------------------------------------------------

/**
 * Модальная стеклянная карточка sloosh.
 *
 * Визуально 1:1 к прежнему диалогу sloosh: скрим, всплытие с пружиной, заголовок по центру,
 * кнопка закрытия. Рисуется glasmio-материалом (`GlassSurface`, style = Navigation).
 */
@Composable
fun SlooshGlassDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    showCloseButton: Boolean = true,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    val resolvedBackdrop = backdrop ?: LocalGlassBackdrop.current
    GlasmioScrimDialog(onDismissRequest = onDismissRequest) { dismiss ->
        Box(
            modifier = modifier
                .padding(horizontal = 32.dp)
                .widthInDialog(),
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                backdrop = resolvedBackdrop,
                shape = ContinuousRoundedRectangle(34.dp),
                style = GlassSurfaceStyle.Navigation,
                contentAlignment = Alignment.TopStart,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SlooshFontFamily,
                            letterSpacing = (-0.2).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        if (showCloseButton) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .clickable(interactionSource = null, indication = null) { dismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                SfIcon(
                                    systemName = "xmark",
                                    contentDescription = "Закрыть",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    size = 12.dp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    content(dismiss)
                }
            }
        }
    }
}

/** Перегрузка без параметра dismiss в content — для простых случаев. */
@Composable
fun SlooshGlassDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    showCloseButton: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) = SlooshGlassDialog(
    title = title,
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    backdrop = backdrop,
    showCloseButton = showCloseButton,
    content = { _ -> content() }
)

/**
 * Стеклянное подтверждение действия (iOS alert) на движке glasmio.
 */
@Composable
fun SlooshAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String? = null,
    confirmButtonText: String = "Подтвердить",
    dismissButtonText: String = "Отмена",
    confirmButtonColor: Color = SlooshAccent,
    isDestructive: Boolean = false,
    iconSfName: String? = null,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    customContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val resolvedBackdrop = backdrop ?: LocalGlassBackdrop.current
    GlasmioScrimDialog(onDismissRequest = onDismissRequest) { dismiss ->
        Box(
            modifier = modifier
                .padding(horizontal = 32.dp)
                .widthInDialog(),
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                backdrop = resolvedBackdrop,
                shape = ContinuousRoundedRectangle(34.dp),
                style = GlassSurfaceStyle.Navigation,
                contentAlignment = Alignment.TopStart,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (iconSfName != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            SfIcon(
                                systemName = iconSfName,
                                contentDescription = null,
                                tint = if (isDestructive) Color(0xFFFF453A) else confirmButtonColor,
                                size = 24.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SlooshFontFamily,
                        letterSpacing = (-0.2).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    if (message != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontFamily = SlooshFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    customContent?.let {
                        Spacer(modifier = Modifier.height(14.dp))
                        it()
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SlooshGlassDialogButton(
                            text = dismissButtonText,
                            modifier = Modifier.weight(1f),
                            emphasis = GlassEmphasis.Regular,
                            onClick = {
                                dismiss()
                                onDismissRequest()
                            }
                        )
                        SlooshGlassDialogButton(
                            text = confirmButtonText,
                            modifier = Modifier.weight(1f),
                            emphasis = GlassEmphasis.Prominent,
                            tint = if (isDestructive) Color(0xFFFF453A) else confirmButtonColor,
                            onClick = {
                                dismiss()
                                onConfirm()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlooshGlassDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: GlassEmphasis = GlassEmphasis.Regular,
    tint: Color? = null,
) {
    GlassSurface(
        modifier = modifier.height(44.dp),
        backdrop = LocalGlassBackdrop.current,
        shape = Capsule(),
        style = GlassSurfaceStyle.Navigation,
        navigationSurfaceColor = tint,
        emphasis = emphasis,
        onClick = onClick,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SlooshFontFamily,
            letterSpacing = (-0.2).sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Контекстное меню (iOS pull-down / popup menu)
// ---------------------------------------------------------------------------------------------

/** Состояние открытого контекстного меню (прежний `LiquidMenuState`). */
@Stable
class SlooshMenuState {
    var isVisible by mutableStateOf(false)
        internal set

    var options by mutableStateOf<List<String>>(emptyList())
        internal set

    var itemIcons by mutableStateOf<List<Int?>?>(null)
        internal set

    var selectedIndex by mutableIntStateOf(-1)
        internal set

    var anchorBounds by mutableStateOf(Rect.Zero)
        internal set

    var customWidth by mutableStateOf<Dp?>(null)
        internal set

    var onSelectCallback by mutableStateOf<(Int) -> Unit>({})
        internal set

    fun show(
        options: List<String>,
        selectedIndex: Int,
        anchorBounds: Rect,
        customWidth: Dp? = null,
        itemIcons: List<Int?>? = null,
        onSelect: (Int) -> Unit
    ) {
        this.options = options
        this.itemIcons = itemIcons
        this.selectedIndex = selectedIndex
        this.anchorBounds = anchorBounds
        this.customWidth = customWidth
        this.onSelectCallback = onSelect
        this.isVisible = true
    }

    fun dismiss() {
        this.isVisible = false
    }
}

val LocalSlooshMenuHostState = staticCompositionLocalOf<SlooshMenuState?> { null }

/**
 * Корневой оверлей контекстного меню на движке glasmio.
 *
 * Рисуется `GlasmioMenu` + `GlasmioMenuItem`: тот же материал `GlassSurface` и та же
 * morph-анимация окна, что у навбара и остального стекла.
 */
@Composable
fun SlooshGlassMenuOverlay(
    state: SlooshMenuState,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return
    val resolvedBackdrop = backdrop ?: LocalBlurBackdrop.current
    val options = state.options
    val activeIndex = state.selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    GlasmioMenu(
        expanded = state.isVisible,
        onExpandedChange = { expanded -> if (!expanded) state.dismiss() },
        itemCount = options.size,
        modifier = modifier,
        backdrop = resolvedBackdrop,
        externalAnchorBounds = state.anchorBounds.takeIf { it != Rect.Zero },
        menuWidth = state.customWidth ?: 240.dp,
        anchor = { _ -> },
        content = { _, close ->
            options.forEachIndexed { index, option ->
                GlasmioMenuItem(
                    title = option,
                    onClick = {
                        state.onSelectCallback(index)
                        state.dismiss()
                        close()
                    },
                    systemName = null,
                    backdrop = resolvedBackdrop,
                    fontWeight = if (index == activeIndex) FontWeight.SemiBold else null,
                    icon = {
                        if (index == activeIndex) {
                            SfIcon(
                                systemName = "checkmark",
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                size = 16.dp
                            )
                        } else {
                            val iconResId = state.itemIcons?.getOrNull(index)
                            if (iconResId != null) {
                                Image(
                                    painter = painterResource(iconResId),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(22.dp).clip(CircleShape)
                                )
                            }
                        }
                    },
                )
            }
        }
    )
}

/**
 * Триггер выбора значения в строках настроек: текущее значение с двойным шевроном.
 * Открывает то же glasmio-меню.
 */
@Composable
fun SettingsMenuTrigger(
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val menuState = LocalSlooshMenuHostState.current
    val selectedIndex = remember(selectedValue, options) {
        options.indexOf(selectedValue).takeIf { it >= 0 } ?: 0
    }
    var expanded by remember { mutableStateOf(false) }

    GlasmioMenu(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        itemCount = options.size,
        modifier = modifier,
        backdrop = LocalBlurBackdrop.current,
        menuWidth = 220.dp,
        anchor = { openMenu ->
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clickable(interactionSource = null, indication = null) { openMenu() }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedValue,
                    color = SlooshAccent,
                    fontSize = 15.sp,
                    fontFamily = SlooshFontFamily,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                SfIcon(
                    systemName = "chevron.up.chevron.down",
                    contentDescription = null,
                    tint = SlooshAccent.copy(alpha = 0.90f),
                    size = 14.dp
                )
            }
        },
        content = { _, close ->
            options.forEachIndexed { index, option ->
                GlasmioMenuItem(
                    title = option,
                    onClick = {
                        onOptionSelected(option)
                        close()
                    },
                    backdrop = LocalBlurBackdrop.current,
                    fontWeight = if (index == selectedIndex) FontWeight.SemiBold else null,
                    icon = {
                        if (index == selectedIndex) {
                            SfIcon(
                                systemName = "checkmark",
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                size = 16.dp
                            )
                        }
                    },
                )
            }
        }
    )
}

/** Алиас для экрана плеера: тот же диалог glasmio. */
@Composable
fun SlooshGlassPlayerDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    showCloseButton: Boolean = true,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) = SlooshGlassDialog(
    title = title,
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    backdrop = backdrop,
    showCloseButton = showCloseButton,
    content = content
)

/** Ограничитель ширины модальной карточки: iOS UIAlertController — 300dp. */
private fun Modifier.widthInDialog(): Modifier = this.widthIn(max = 300.dp)

/**
 * Скрим + всплытие модального окна. Затемнение и «пружина» карточки — те же, что были
 * в прежнем диалоге sloosh; поверхность карточки теперь рисует glasmio.
 */
@Composable
private fun GlasmioScrimDialog(
    onDismissRequest: () -> Unit,
    content: @Composable (dismiss: () -> Unit) -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        val dismiss = { onDismissRequest() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(interactionSource = null, indication = null) { dismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.clickable(
                    interactionSource = null,
                    indication = null
                ) { /* поглощаем клик по карточке */ },
                contentAlignment = Alignment.Center
            ) {
                content(dismiss)
            }
        }
    }
}
