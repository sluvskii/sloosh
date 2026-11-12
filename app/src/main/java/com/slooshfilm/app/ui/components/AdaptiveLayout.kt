package com.slooshfilm.app.ui.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Единый источник правды адаптивной раскладки sloosh: телефон / планшет / ландшафт.
 *
 * Все метрики выводятся из текущей конфигурации через [LocalConfiguration], поэтому
 * раскладка перестраивается реактивно. Это обязательно: MainActivity объявляет
 * `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|smallestScreenSize"`,
 * то есть при повороте активити НЕ пересоздаётся — новую конфигурацию Compose получает
 * через `AndroidComposeView.onConfigurationChanged`, и метрики пересчитываются сами,
 * без потери состояния экранов и позиции скролла.
 *
 * Классы устройства соответствуют брейкпоинтам Material 3 Window Size Class
 * (Compact < 600dp, Medium 600..839dp, Expanded >= 840dp по ширине), а порог
 * [SLOOSH_TABLET_MIN_WIDTH_DP] совпадает с проверкой `screenWidthDp >= 600`, которая
 * исторически используется экранами sloosh.
 */
enum class SlooshDeviceClass { Compact, Medium, Expanded }

/** Порог планшета/складного устройства в раскрытом состоянии (Material 3 Medium). */
const val SLOOSH_TABLET_MIN_WIDTH_DP = 600

/** Порог Expanded-класса (Material 3): крупные планшеты, складные устройства, desktop mode. */
const val SLOOSH_EXPANDED_MIN_WIDTH_DP = 840

/**
 * Высота экрана, ниже которой вертикальный контент телефона в горизонтали не помещается
 * и требует прокрутки (ландшафт телефона: обычно 320..430dp).
 */
const val SLOOSH_SHORT_VIEWPORT_MAX_HEIGHT_DP = 600

@Composable
fun rememberSlooshDeviceClass(): SlooshDeviceClass {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return when {
        screenWidthDp >= SLOOSH_EXPANDED_MIN_WIDTH_DP -> SlooshDeviceClass.Expanded
        screenWidthDp >= SLOOSH_TABLET_MIN_WIDTH_DP -> SlooshDeviceClass.Medium
        else -> SlooshDeviceClass.Compact
    }
}

/** Текущая ориентация — горизонтальная. */
@Composable
fun rememberIsLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

/**
 * Планшет/складное устройство (`screenWidthDp >= 600`).
 * Семантика совпадает с локальными проверками `isTablet` в экранах sloosh.
 */
@Composable
fun rememberIsTablet(): Boolean =
    LocalConfiguration.current.screenWidthDp >= SLOOSH_TABLET_MIN_WIDTH_DP

/**
 * Короткий вьюпорт: телефон в горизонтальной ориентации.
 *
 * В этом режиме вертикального места (~320..430dp) не хватает на полноэкранные формы
 * (обязательный вход, привязка ТВ), поэтому такие экраны переходят на прокрутку вместо
 * центрирования через `Spacer(weight(1f))` — иначе кнопка действия уезжает за край экрана
 * и добраться до неё нельзя. На планшете в ландшафте (высота >= 600dp) и в портрете
 * возвращается false, и раскладка остаётся прежней.
 */
@Composable
fun rememberIsShortViewport(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.screenHeightDp < SLOOSH_SHORT_VIEWPORT_MAX_HEIGHT_DP
}

/**
 * Адаптивные метрики плавающего нижнего навбара.
 *
 * @param barHeight высота стеклянной капсулы с табами.
 * @param buttonSize размер круглой кнопки поиска справа (совпадает с высотой бара).
 * @param horizontalPadding боковые отступы бара с учётом выреза камеры в ландшафте.
 * @param maxBarWidth ограничение ширины; `null` — бар занимает всю доступную ширину.
 * @param compactTabsWidth ширина капсулы табов в ландшафте/на планшете; `null` — занимает всё доступное место слева.
 */
@Immutable
data class AdaptiveBarMetrics(
    val barHeight: Dp,
    val buttonSize: Dp,
    val horizontalPadding: Dp,
    val maxBarWidth: Dp? = null,
    val compactTabsWidth: Dp? = null
)

/**
 * Метрики навбара под текущую ориентацию и класс устройства.
 *
 * Портрет телефона не меняется вообще: 54dp бар во всю ширину с отступами 24dp.
 * В горизонтали и на планшете капсула табов остаётся компактной слева (360dp, высота 44dp),
 * а круглая кнопка поиска (44dp) прижата справа под правый большой палец, освобождая
 * середину экрана для удобного двухручного хвата и предотвращая наезд на контент.
 *
 * Значения 54dp/44dp и 24dp — существующие токены sloosh, новых чисел не вводится.
 * `pressedScale = 60f / 44f` активной капли при этом не затрагивается: это отношение
 * применяется к высоте пилюли (`barHeight - 8.dp`), поэтому капля масштабируется
 * пропорционально автоматически.
 */
@Composable
fun rememberAdaptiveBarMetrics(): AdaptiveBarMetrics {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= SLOOSH_TABLET_MIN_WIDTH_DP
    val cutoutPadding = rememberCutoutHorizontalPadding()

    return AdaptiveBarMetrics(
        barHeight = if (isLandscape) 44.dp else 54.dp,
        buttonSize = if (isLandscape) 44.dp else 54.dp,
        // В портрете rememberCutoutHorizontalPadding() == 0.dp, поэтому отступ остаётся 24.dp.
        // В ландшафте берём большее из базового отступа и реального инсета выреза камеры.
        horizontalPadding = maxOf(24.dp, cutoutPadding),
        maxBarWidth = null,
        compactTabsWidth = if (isLandscape || isTablet) 360.dp else null
    )
}

/**
 * Высота модальной шторки под ориентацию экрана.
 *
 * В портрете — доля [portraitFraction] от высоты экрана (1:1 с iOS
 * `.presentationDetents([.fraction(0.62)])`). В ландшафте эта же доля даёт ~220dp,
 * из которых шапка и нижняя капсула съедают ~150dp, поэтому шторка поднимается
 * почти на всю высоту — [landscapeFraction].
 *
 * Превысить доступную высоту шторки невозможно: `Modifier.height()` ограничивается
 * входящими constraints от ModalBottomSheet.
 */
@Composable
fun rememberAdaptiveSheetHeight(
    portraitFraction: Float = 0.62f,
    landscapeFraction: Float = 0.92f
): Dp {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenHeight = configuration.screenHeightDp.dp
    return screenHeight * (if (isLandscape) landscapeFraction else portraitFraction)
}
