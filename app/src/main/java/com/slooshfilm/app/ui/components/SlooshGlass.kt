package com.slooshfilm.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import io.github.neoruaa.glasmio.core.GlassAppearance
import io.github.neoruaa.glasmio.glass.navigationGlassBackground
import io.github.neoruaa.glasmio.core.LocalGlassAppearance
import io.github.neoruaa.glasmio.glass.LocalBlurBackdrop
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.emptyBackdrop

/**
 * Оптические пресеты стекла sloosh.
 *
 * Исторически это были параметры самописного движка (`ui/components/LiquidGlass.kt`), который
 * удалён: теперь всё стекло рисует вендоренный движок glasmio (`io.github.neoruaa.glasmio.glass`).
 * Пресеты сохранены как **выразительный словарь** вызывающего кода: вызывающие места продолжают
 * говорить «здесь кнопка», «здесь компактная пилюля», «здесь диалог», а какой именно материал
 * glasmio использовать — решает общая точка (`Modifier.slooshGlassSurface` ниже).
 */
data class GlassPreset(
    val name: String,
    /** Ориентир глубины «мороза», dp. Отображается на материал glasmio. */
    val blurRadius: Dp = 20.dp,
    /** Ориентир плотности материала, 0..1. */
    val blurOpacity: Float = 0.72f,
    val highlightAlpha: Float = 0.68f,
    val shadowAlpha: Float = 0.0f,
)

/**
 * Каталог пресетов (прежний `SlooshGlassTokens`).
 * Значения — те же, что были у старого движка: это публичный словарь вызывающих мест.
 */
object SlooshGlassTokens {
    /** Панель навигации: 20dp, 72%. */
    val Bar = GlassPreset("bar", blurRadius = 20.dp, blurOpacity = 0.72f)

    /** Активная «капля»-ползунок. */
    val Slider = GlassPreset("slider", blurRadius = 4.dp, blurOpacity = 1f, highlightAlpha = 0.50f, shadowAlpha = 0.30f)

    /** Плавающая кнопка (поиск, медиа-контролы). */
    val Button = GlassPreset("button", blurRadius = 20.dp, blurOpacity = 0.72f, shadowAlpha = 0.25f)

    /** Кнопка в активном состоянии. */
    val ButtonActive = Button.copy(highlightAlpha = 0.85f, shadowAlpha = 0.50f)

    /** Карточка контента / шит. */
    val Surface = GlassPreset("surface", blurRadius = 20.dp, shadowAlpha = 0.25f)

    /** Компактная пилюля: чипы фильтров, капсулы плеера. */
    val CompactPill = GlassPreset("compact_pill", blurRadius = 12.dp, highlightAlpha = 0.35f)

    /** Контекстное меню. */
    val Menu = GlassPreset("menu", blurRadius = 10.dp, blurOpacity = 0.60f, highlightAlpha = 0.45f, shadowAlpha = 0.30f)

    /** Модальное окно: плотнее меню. */
    val Dialog = GlassPreset("dialog", blurRadius = 14.dp, blurOpacity = 0.78f, highlightAlpha = 0.45f, shadowAlpha = 0.34f)
}

/**
 * Настройки стекла, которые приложение выставляет из «Настроек» и читает в UI.
 *
 * Были объявлены в удалённом `LiquidGlass.kt`; теперь прокидываются прямо в
 * [GlassAppearance] движка glasmio, поэтому «Режим без стекла» и «Размытие стекла»
 * продолжают работать, но уже на его материале.
 */
val LocalGlassEnabled = staticCompositionLocalOf { true }
val LocalProgressiveBlur = staticCompositionLocalOf { true }
val LocalGlassBlurMultiplier = staticCompositionLocalOf { 0.70f }

/**
 * Бэкдроп для контента, который движок glasmio не прокидывает сам.
 *
 * Вендоренный `GlasmioGroupedList` (и обычные `Column`-секции) собирают строки настроек вне
 * `CompositionLocalProvider(LocalGlassBackdrop)`, поэтому `LocalGlassBackdrop.current` внутри
 * такой строки — это `staticCompositionLocalOf { error(...) }`, то есть падение.
 * Настройки кладут бэкдроп экрана в этот local, и все контролы строк берут его явно.
 */
val LocalSettingsBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Аварийный режим **управляющих линз** (`nativeControlLens`).
 *
 * Зачем: на части драйверов каскад `RuntimeShader` для линз переключателя, ползунка и
 * сегмент-контрола падает **нативно** (SIGSEGV в HWUI). Java-обработчик такое не видит,
 * поэтому приложение умирает молча — без отчёта и без диалога. Подтверждено на
 * Xiaomi 23043RP34G (Redmi Pad SE, Snapdragon 680 / Adreno 610, Android 14).
 *
 * При включённом флаге линзы не создаются вовсе, а **размытие стекла продолжает
 * работать**: навбар, кнопки и диалоги остаются стеклянными, без преломления на контролах.
 */
val LocalGlassOpticsDisabled = staticCompositionLocalOf { false }

/**
 * Размытие стекла разрешено (уровень деградации не «без оптики целиком»).
 * По умолчанию true: безопасный режим трогает только линзы, размытие остаётся.
 */
val LocalGlassBackdropDisabled = staticCompositionLocalOf { false }

/**
 * Живое состояние уровня деградации оптики (см. `GlassOptics`). Настройки пишут в него
 * напрямую, поэтому переключатель действует сразу, без перезапуска приложения.
 */
val LocalGlassOpticsLevelState = staticCompositionLocalOf<androidx.compose.runtime.MutableIntState?> { null }

/**
 * Провайдер настроек стекла: переводит настройки sloosh в термины движка glasmio
 * (`GlassAppearance.liquidGlassEnabled` / `reduceTransparency`), чтобы материал нaвбара,
 * кнопок и диалогов реагировал на «Режим без стекла» и «Размытие стекла».
 */
@Composable
fun SlooshGlassSettingsProvider(
    glassEnabled: Boolean,
    progressiveBlurEnabled: Boolean,
    blurMultiplier: Float,
    /** Управляющие линзы (`nativeControlLens`). false — только размытие, без преломления. */
    lensesEnabled: Boolean = false,
    /** Размытие и материалы стекла. false — оптика выключена целиком. */
    backdropEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val base = LocalGlassAppearance.current
    val appearance = GlassAppearance(
        // Ползунок «Размытие стекла» на 0% = полностью прозрачное стекло; ниже 8% считаем,
        // что оптику надо отключать целиком — так же вёл себя прежний движок.
        liquidGlassEnabled = glassEnabled && blurMultiplier > 0.08f,
        reduceTransparency = !glassEnabled || !backdropEnabled,
        adaptiveColorScheme = base.adaptiveColorScheme,
        // Линзы — единственный путь, который падал нативно на Adreno 610. Выключаются
        // отдельно от размытия: стекло остаётся, преломление на контролах уходит.
        controlLensesEnabled = glassEnabled && lensesEnabled && backdropEnabled,
    )
    CompositionLocalProvider(
        LocalGlassEnabled provides glassEnabled,
        LocalProgressiveBlur provides progressiveBlurEnabled,
        LocalGlassBlurMultiplier provides blurMultiplier,
        LocalGlassAppearance provides appearance,
        content = content
    )
}

/**
 * Провайдер настроек стекла + бэкдроп экрана для строк внутри `GlasmioGroupedList`.
 *
 * Нужен там, где контент экрана — это «сырые» строки настроек: вендоренный групповой список
 * не прокидывает `LocalGlassBackdrop`, а его дефолт — `error(...)`. Провайдер выставляет и
 * `LocalGlassBackdrop`, и `LocalBlurBackdrop` (его требуют модальные поверхности glasmio),
 * и `LocalSettingsBackdrop` для обёрток sloosh.
 */
@Composable
fun SlooshGlassSettingsProvider(
    backdrop: Backdrop,
    glassEnabled: Boolean,
    progressiveBlurEnabled: Boolean,
    blurMultiplier: Float,
    content: @Composable () -> Unit
) {
    SlooshGlassSettingsProvider(
        glassEnabled = glassEnabled,
        progressiveBlurEnabled = progressiveBlurEnabled,
        blurMultiplier = blurMultiplier,
    ) {
        CompositionLocalProvider(
            LocalGlassBackdrop provides backdrop,
            LocalBlurBackdrop provides backdrop,
            LocalSettingsBackdrop provides backdrop,
            content = content
        )
    }
}

/**
 * Токены прогрессивного блюра шапок (iOS `VariableBlurView`): пик 4dp, «хвост» 30dp.
 */
object ProgressiveBlurTokens {
    val MaxRadius = 4.dp
    val Extension = 30.dp
}

/** Токены контекстного меню (прежний `SlooshMenuTokens`): размеры строк списка. */
object SlooshMenuTokens {
    var cornerRadius: Dp = 26.dp
    var itemHorizontalPadding: Dp = 6.dp
    var itemVerticalPadding: Dp = 2.dp
    val itemHeight: Dp = 44.dp
    val itemShape: Shape = com.kyant.capsule.ContinuousRoundedRectangle(20.dp)
    val itemHighlightColor: Color = Color.White.copy(alpha = 0.10f)
    val menuWidth: Dp = 240.dp
    val minMenuWidth: Dp = 200.dp
}

/**
 * `Modifier` плюс-освещения iOS (`.blendMode(.plusLighter)`): рисуем слой аддитивно.
 * В терминах Compose это тот же `BlendMode.Plus` поверх записанного слоя.
 */
@Composable
fun Modifier.plusBlendMode(): Modifier {
    val isLightTheme = !LocalSlooshColors.current.isDark
    if (isLightTheme) return this
    return this.drawWithContent {
        val paint = Paint().apply { blendMode = BlendMode.Plus }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(size.toRect(), paint)
            this@drawWithContent.drawContent()
            canvas.restore()
        }
    }
}

/**
 * Стеклянная подложка целого блока (`HomeScreen`): обычная glasmio-поверхность
 * без клика и без собственного контента.
 */
@Composable
fun Modifier.slooshGlass(
    backdrop: Backdrop? = null,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    blurRadius: Dp = 20.dp,
    tint: Color? = null,
    borderAlpha: Float = 0.68f,
    elevation: Dp = 4.dp
): Modifier = this.then(
    Modifier.navigationGlassBackground(
        backdrop = backdrop ?: LocalGlassBackdrop.current,
        shape = { shape },
        // containerColor — это ЦВЕТ материала внутри движка, а не заливка поверх стекла.
        // Дефолт берём нейтральный (surface как есть), а не полупрозрачную «плёнку»:
        // иначе стекло выглядит мутным и не совпадает с навбаром.
        containerColor = tint ?: MaterialTheme.colorScheme.surface,
        navigationMaterial = tint == null,
        innerShadows = null,
        isDark = LocalSlooshColors.current.isDark,
        blurRadius = blurRadius,
    )
)

/**
 * Стеклянная поверхность glasmio поверх бэкдропа.
 *
 * Это Modifier, а не `GlassSurface`, потому что вызывающие места навешивают его в цепочку
 * модификаторов вместе с `.padding(...)`; `GlassSurface` — composable-контейнер.
 */
@Composable
fun Modifier.slooshGlassSurface(
    style: GlassPreset = SlooshGlassTokens.Surface,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    containerColor: Color? = null,
    backdrop: Backdrop? = null,
    layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") onDrawExtraSurface: (androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit)? = null,
): Modifier {
    val isGlassEnabled = LocalGlassEnabled.current
    val appearance = LocalGlassAppearance.current
    return this.navigationGlassBackground(
        backdrop = if (isGlassEnabled) (backdrop ?: LocalGlassBackdrop.current) else emptyBackdrop(),
        shape = { shape },
        // См. комментарий в `slooshGlass`: это цвет материала glasmio, а не заливка.
        containerColor = containerColor ?: MaterialTheme.colorScheme.surface,
        navigationMaterial = true,
        innerShadows = null,
        isDark = LocalSlooshColors.current.isDark,
        blurRadius = style.blurRadius,
        appearance = appearance,
        layerBlock = layerBlock,
    )
}

/**
 * Интерактивный вариант поверхности. Глубина и подсветка берутся из пресета,
 * физика нажатия — из материала glasmio.
 */
@Composable
fun Modifier.slooshGlassInteractive(
    style: GlassPreset = SlooshGlassTokens.Button,
    shape: Shape = androidx.compose.foundation.shape.CircleShape,
    containerColor: Color? = null,
    backdrop: Backdrop? = null,
    /** Принимается для совместимости: глубину и подсветку задаёт пресет glasmio. */
    @Suppress("UNUSED_PARAMETER") interactiveHighlight: Any? = null,
    @Suppress("UNUSED_PARAMETER") maxDragScale: Float? = null,
    @Suppress("UNUSED_PARAMETER") dragTranslationFactor: Float = 1f,
    @Suppress("UNUSED_PARAMETER") applyBlurSetting: Boolean = true,
    @Suppress("UNUSED_PARAMETER") onDrawExtraSurface: (androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit)? = null,
): Modifier = this.slooshGlassSurface(
    style = style,
    shape = shape,
    containerColor = containerColor,
    backdrop = backdrop,
)
