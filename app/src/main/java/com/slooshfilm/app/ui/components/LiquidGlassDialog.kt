package com.slooshfilm.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import com.slooshfilm.app.R
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import com.slooshfilm.app.ui.theme.SlooshAccent
import com.slooshfilm.app.ui.theme.TextPrimary
import com.slooshfilm.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** Радиус скругления модальных окон (iOS modal sheet: 32pt). */
private val DialogCornerRadius = 32.dp

/** Плотность затемнения фона — ровно как у шторки (M3 BottomSheetDefaults.ScrimColor). */
private const val DialogScrimAlpha = 0.32f

/**
 * Максимальная упругая деформация окна при перетаскивании.
 * Окно крупное, поэтому тянется намеренно слабее кнопок (у них 0.08f).
 */
private const val DialogDragScale = 0.03f

/**
 * Насколько окно следует за пальцем при перетаскивании (1f — как у кнопок).
 * Окно крупное, поэтому сдвигается лишь слегка, а не «ездит» за пальцем.
 */
private const val DialogDragTranslation = 0.25f

/**
 * Universal Liquid Glass Modal Dialog:
 * The premier Apple TV+ and iOS Liquid Glass standard for all modal windows, bottom sheets, and selection dialogs.
 *
 * Kinematics & Design:
 * - Dynamic 3-pass compound optical glass (iosDualFrostedBlur, Vibrancy AGSL, iosLens, specular glints, hairline overlay border).
 * - Denser smoked tint ([LiquidGlassTokens.Dialog]): deeper frost (14dp + 28dp) and an extra ambient tint for legible text.
 * - Fluid non-linear spring expansion (stiffness = 330f, dampingRatio = 0.80f) with Hermite smoothstep alpha ($3p^2 - 2p^3$).
 * - Elastic liquid kinematics (liquidGlassInteractive) with tanh damping, interactive sheen, and spring recoil.
 * - Centered 17sp semibold title, uniform 20dp inner padding, and a 32dp iOS squircle geometry.
 */
@Composable
fun LiquidGlassDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    showCloseButton: Boolean = true,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    val expansionAnim = remember { Animatable(0f) }
    // Скрим живёт на собственной быстрой кривой: затемнение появляется сразу и не «догоняет» blooming окна.
    val scrimAnim = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    fun triggerDismiss() {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            launch { scrimAnim.animateTo(0f, tween()) }
            expansionAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    stiffness = 500f,
                    dampingRatio = 0.88f
                )
            )
            onDismissRequest()
        }
    }

    LaunchedEffect(Unit) {
        launch { scrimAnim.animateTo(1f, tween()) }
        expansionAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                stiffness = 330f,
                dampingRatio = 0.80f // Silky fluid flow with gentle ~2% liquid overshoot settling
            )
        )
    }

    Dialog(
        onDismissRequest = { triggerDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            // Окно рисуется edge-to-edge, поэтому скрим закрывает и статус-бар (как у шторки).
            decorFitsSystemWindows = false
        )
    ) {
        // Окно диалога (FloatingDialogWindowTheme) включает платформенный dim —
        // гасим его, иначе затемнение накладывается дважды и выходит темнее, чем у шторки.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.setDimAmount(0f)
        }

        val progress = expansionAnim.value
        val progressClamped = progress.coerceIn(0f, 1f)

        // Non-linear fluid curves:
        val rawAlpha = (progressClamped / 0.25f).coerceIn(0f, 1f)
        val alpha = rawAlpha * rawAlpha * (3f - 2f * rawAlpha) // Hermite smoothstep (zero pop on frame 0)

        val tPos = 1f - (1f - progressClamped) * (1f - progressClamped) * (1f - progressClamped)
        val liquidWave = sin(progressClamped * PI.toFloat()).coerceAtLeast(0f)
        val liquidSquish = liquidWave * 0.03f
        val bloomScale = lerp(0.85f, 1f, tPos)

        // Dynamic optical shimmer during blooming:
        val dynamicStyle = remember(progressClamped, liquidWave) {
            if (progressClamped >= 0.999f) {
                LiquidGlassTokens.Dialog
            } else {
                LiquidGlassTokens.Dialog.copy(
                    refractionAmount = 12.dp + (8.dp * progressClamped) + (5.dp * liquidWave),
                    chromaticAberration = 0f,
                    highlightAlpha = lerp(0.55f, 0.45f, progressClamped) + (0.10f * liquidWave)
                )
            }
        }

        // Glass button interactive kinematics for the dialog container:
        val interactiveHighlight = remember(scope) {
            InteractiveHighlight(
                animationScope = scope,
                washAlpha = 0.02f,
                spotAlpha = 0.05f
            )
        }

        val isGlassEnabled = LocalGlassEnabled.current
        val isLightTheme = !LocalSlooshColors.current.isDark
        // Окно не подчиняется настройке «Размытие стекла» (applyBlurSetting = false):
        // у него фиксированные оптика и дымчатый тинт. Тинт полупрозрачный — блюр и линза
        // должны читаться, но плотности хватает, чтобы текст не тонул в постере.
        // Цвет — iOS systemGray5 (тёмная тема), а не почти чёрный: Apple использует
        // серый тинт в том числе для кнопок, поэтому окно не должно уходить в чёрный.
        val resolvedBackdrop = backdrop ?: LocalBackdrop.current
        val dialogContainerColor = if (isGlassEnabled) {
            if (resolvedBackdrop != null) {
                if (isLightTheme) Color.White.copy(alpha = 0.80f) else Color(0xFF2C2C2E).copy(alpha = 0.65f)
            } else {
                if (isLightTheme) Color(0xFFF2F2F7).copy(alpha = 0.85f) else Color(0xFF2C2C2E).copy(alpha = 0.80f)
            }
        } else {
            if (isLightTheme) Color.White else Color(0xFF1E1E22)
        }
        val dialogTintOverlay = if (isLightTheme) {
            Color.Transparent
        } else {
            Color(0xFF1C1C1E).copy(alpha = 0.16f)
        }

        // Скрим 1:1 со шторкой: чёрный 32%, TweenSpec (300мс) — живет независимо от пружины окна.
        val scrimAlpha = scrimAnim.value * DialogScrimAlpha
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { triggerDismiss() }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Glass Modal Container
            Column(
                modifier = modifier
                    .widthIn(min = 292.dp, max = 420.dp)
                    .fillMaxWidth(0.82f)
                    .graphicsLayer {
                        scaleX = bloomScale
                        scaleY = bloomScale * (1f + liquidSquish)
                        this.alpha = alpha
                    }
                    .liquidGlassInteractive(
                        interactiveHighlight = interactiveHighlight,
                        style = dynamicStyle,
                        shape = RoundedRectangle(DialogCornerRadius),
                        containerColor = dialogContainerColor,
                        backdrop = backdrop ?: LocalBackdrop.current,
                        maxDragScale = DialogDragScale,
                        dragTranslationFactor = DialogDragTranslation,
                        applyBlurSetting = false,
                        onDrawExtraSurface = {
                            if (dialogTintOverlay.alpha > 0f) {
                                drawRect(dialogTintOverlay)
                            }
                        }
                    )
                    .clip(RoundedRectangle(DialogCornerRadius))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercept clicks inside modal
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Header: симметричный заголовок по центру (iOS alert: 17pt semibold, без «жирного» начертания).
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (showCloseButton) 36.dp else 0.dp)
                            .align(Alignment.Center)
                    )

                    if (showCloseButton) {
                        LiquidGlassFloatingButton(
                            onClick = { triggerDismiss() },
                            backdrop = backdrop,
                            shape = CircleShape,
                            tint = if (isLightTheme) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.10f),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_sf_xmark),
                                contentDescription = "Закрыть",
                                tint = TextPrimary.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                content { triggerDismiss() }
            }
        }
    }
}

/**
 * Convenience overload of [LiquidGlassDialog] accepting standard ColumnScope content.
 */
@Composable
fun LiquidGlassDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    showCloseButton: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) = LiquidGlassDialog(
    title = title,
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    backdrop = backdrop,
    showCloseButton = showCloseButton,
    content = { _ -> content() }
)

/**
 * Universal Liquid Glass Alert / Confirmation Dialog for actions (Clear Cache, Delete Chat, Clear History, Updates).
 * Features authentic Apple Smoked Grey glass, elastic kinematics, and tactile spring action buttons.
 */
@Composable
fun LiquidGlassAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String? = null,
    confirmButtonText: String = "Подтвердить",
    dismissButtonText: String = "Отмена",
    confirmButtonColor: Color = SlooshAccent,
    isDestructive: Boolean = false,
    icon: Painter? = null,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    customContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    val isLightTheme = !LocalSlooshColors.current.isDark

    LiquidGlassDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        backdrop = backdrop,
        showCloseButton = false
    ) { dismiss ->
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isDestructive) Color(0xFFFF453A).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.08f))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = if (isDestructive) Color(0xFFFF453A) else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                letterSpacing = (-0.2).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )
        }

        customContent?.invoke(this)

        // Две строки кнопок: сверху основное действие, снизу — отмена (iOS alert).
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Confirm button (Apple solid white capsule or destructive red capsule, matching DetailsScreen)
            val (confirmBg, confirmTextColor) = if (isDestructive) {
                Color(0xFFFF453A) to Color.White
            } else {
                Color.White to Color.Black
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(Capsule())
                    .background(confirmBg)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onConfirm()
                        dismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = confirmButtonText,
                    color = confirmTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
            }

            // Dismiss button (Subtle translucent Apple capsule)
            val (dismissBg, dismissTextColor) = if (isLightTheme) {
                Color.Black.copy(alpha = 0.06f) to Color.Black
            } else {
                Color.White.copy(alpha = 0.08f) to Color.White
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(Capsule())
                    .background(dismissBg)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        dismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dismissButtonText,
                    color = dismissTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.2).sp
                )
            }
        }
    }
}
