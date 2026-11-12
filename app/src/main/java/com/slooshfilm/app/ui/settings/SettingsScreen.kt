package com.slooshfilm.app.ui.settings

import android.content.Intent
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.Coil
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.R
import com.slooshfilm.app.data.storage.LocalStorage
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.components.LiquidGlassAlertDialog
import com.slooshfilm.app.ui.components.LiquidSlider
import com.slooshfilm.app.ui.components.LocalGlassEnabled
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.theme.SlooshAccent
import com.slooshfilm.app.utils.AppUpdateInfo
import com.slooshfilm.app.utils.AppUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Flagship single-screen iOS Settings implementation for sloosh.
 * Consolidates Catalog View, Interface, Playback, Storage, and About into grouped iOS sections.
 */
@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    // Мгновенно применяем переключатели к состоянию приложения, не полагаясь
    // только на слушатель SharedPreferences: иначе настройки применялись
    // лишь после перезапуска.
    onGlassEnabledChange: (Boolean) -> Unit = {},
    onProgressiveBlurChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localStorage = remember { LocalStorage(context) }

    // --- State variables ---
    var currentThemeMode by remember { mutableIntStateOf(localStorage.getThemeMode()) }
    var liquidGlassBlur by remember { mutableFloatStateOf(localStorage.getLiquidGlassBlur()) }
    val globalGlassEnabled = LocalGlassEnabled.current
    var isGlassEnabled by remember(globalGlassEnabled) { mutableStateOf(globalGlassEnabled) }
    val globalProgressiveBlur = LocalProgressiveBlur.current
    var isProgressiveBlurEnabled by remember(globalProgressiveBlur) { mutableStateOf(globalProgressiveBlur) }
    var cardStyle by remember { mutableStateOf(localStorage.getCardStyle()) }
    var cardDensity by remember { mutableStateOf(localStorage.getCardDensity()) }
    var tabBarShowsLabels by remember { mutableStateOf(localStorage.getTabBarShowsLabels()) }
    var posterQuality by remember { mutableStateOf(localStorage.getString("poster_quality", "Высокое")) }
    var showOriginalTitle by remember { mutableStateOf(localStorage.getShowOriginalTitle()) }
    var autoAdvanceBackdrops by remember { mutableStateOf(localStorage.getAutoAdvanceBackdrops()) }
    var defaultQuality by remember { mutableStateOf(localStorage.getDefaultQuality()) }
    var autoNext by remember { mutableStateOf(localStorage.getAutoNextEnabled()) }
    var playerVolumeBrightnessGestures by remember { mutableStateOf(localStorage.getPlayerVolumeBrightnessGestures()) }
    var subtitleSize by remember { mutableStateOf(localStorage.getString("subtitle_size", "Стандартный")) }

    var cacheSizeFormatted by remember { mutableStateOf("0 КБ") }
    var isClearingCache by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    // Package info for version display
    val pm = context.packageManager
    val pi = if (Build.VERSION.SDK_INT >= 33) {
        pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, 0)
    }
    val versionName = pi.versionName ?: "2.0.0"
    val versionCode = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode.toInt() else @Suppress("DEPRECATION") pi.versionCode

    fun calculateCacheSize(): String {
        return try {
            var sizeBytes = 0L
            val coilCache = Coil.imageLoader(context).diskCache
            if (coilCache != null) {
                sizeBytes += coilCache.size
            }
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                sizeBytes += cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            }
            when {
                sizeBytes <= 0 -> "0 КБ"
                sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} КБ"
                else -> String.format(java.util.Locale.US, "%.1f МБ", sizeBytes / (1024.0 * 1024.0))
            }
        } catch (_: Exception) {
            "0 КБ"
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            cacheSizeFormatted = calculateCacheSize()
        }
    }

    fun clearCache() {
        isClearingCache = true
        scope.launch(Dispatchers.IO) {
            try {
                val imageLoader = Coil.imageLoader(context)
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()
                context.cacheDir.deleteRecursively()
                val newSize = calculateCacheSize()
                withContext(Dispatchers.Main) {
                    cacheSizeFormatted = newSize
                    Toast.makeText(context, "Кэш успешно очищен", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось очистить кэш", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isClearingCache = false
                }
            }
        }
    }

    val themeOptions = listOf("Системная", "Тёмная", "Светлая")
    val selectedThemeIndex = when (currentThemeMode) {
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> 0
        AppCompatDelegate.MODE_NIGHT_YES, LocalStorage.THEME_MODE_AMOLED -> 1
        else -> 2
    }

    val screenBg = MaterialTheme.colorScheme.background
    val sceneBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }

    SettingsScaffold(
        title = "Настройки",
        onBackClick = { navController.popBackStack() },
        modifier = modifier,
        sceneBackdrop = sceneBackdrop
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. ВИД КАТАЛОГА (iOS Section("Вид каталога"))
        SettingsSection(
            title = "Вид каталога"
        ) {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val isTablet = configuration.screenWidthDp >= 600

            // Fluid animated density and style dimensions (symmetrical 4-side iOS geometry)
            val isCompact = cardDensity == LocalStorage.CARD_DENSITY_COMPACT
            val previewCount = if (isTablet || isLandscape) 6 else 3

            val targetPadding = if (previewCount == 6) {
                if (isCompact) 10.dp else 14.dp
            } else {
                if (isCompact) 14.dp else 22.dp
            }
            val targetSpacing = if (previewCount == 6) {
                if (isCompact) 6.dp else 8.dp
            } else {
                if (isCompact) 8.dp else 14.dp
            }

            val animatedPadding by animateDpAsState(
                targetValue = targetPadding,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f),
                label = "previewPadding"
            )
            val animatedSpacing by animateDpAsState(
                targetValue = targetSpacing,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f),
                label = "previewSpacing"
            )

            // Аутентичный предварительный просмотр со строго одинаковыми отступами со всех 4 сторон
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(animatedPadding),
                contentAlignment = Alignment.Center
            ) {
                val effectivePreviewCount = if (this.maxWidth >= 500.dp || previewCount == 6) 6 else 3
                val cardWidth = (this.maxWidth - animatedSpacing * (effectivePreviewCount - 1)) / effectivePreviewCount.toFloat()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(animatedSpacing),
                    verticalAlignment = Alignment.Top
                ) {
                    val titleFills = listOf(0.80f, 0.65f, 0.85f, 0.70f, 0.82f, 0.60f)
                    val subtitleFills = listOf(0.50f, 0.40f, 0.52f, 0.45f, 0.50f, 0.38f)
                    for (i in 0 until effectivePreviewCount) {
                        PreviewMoviePosterCard(
                            style = cardStyle,
                            width = cardWidth,
                            titleFill = titleFills[i % titleFills.size],
                            subtitleFill = subtitleFills[i % subtitleFills.size]
                        )
                    }
                }
            }

            SettingsDivider(startPadding = 16.dp, endPadding = 16.dp)

            SettingsSegmentedRow(
                title = "Отображение названий",
                iconSfName = "rectangle.grid.1x2",
                options = listOf("Под постером", "Поверх постера"),
                selectedIndex = if (cardStyle == LocalStorage.CARD_STYLE_CLASSIC) 0 else 1,
                onOptionSelected = { index ->
                    val style = if (index == 0) LocalStorage.CARD_STYLE_CLASSIC else LocalStorage.CARD_STYLE_OVERLAY
                    localStorage.saveCardStyle(style)
                    cardStyle = style
                }
            )

            SettingsDivider()

            SettingsSegmentedRow(
                title = "Плотность сетки",
                iconSfName = "square.grid.2x2",
                options = listOf("Стандартная", "Компактная"),
                selectedIndex = if (cardDensity == LocalStorage.CARD_DENSITY_REGULAR) 0 else 1,
                onOptionSelected = { index ->
                    val density = if (index == 0) LocalStorage.CARD_DENSITY_REGULAR else LocalStorage.CARD_DENSITY_COMPACT
                    localStorage.saveCardDensity(density)
                    cardDensity = density
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. ИНТЕРФЕЙС (iOS Section("Интерфейс"))
        SettingsSection(title = "Интерфейс") {
            SettingsSegmentedRow(
                title = "Тема",
                iconSfName = "paintpalette.fill",
                options = themeOptions,
                selectedIndex = selectedThemeIndex,
                onOptionSelected = { index ->
                    val mode = when (index) {
                        0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        1 -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_NO
                    }
                    localStorage.saveThemeMode(mode)
                    currentThemeMode = mode
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            )

            SettingsDivider()

            SettingsSwitchItem(
                title = "Названия вкладок",
                iconSfName = "dock.rectangle",
                checked = tabBarShowsLabels,
                onCheckedChange = { checked ->
                    localStorage.saveTabBarShowsLabels(checked)
                    tabBarShowsLabels = checked
                }
            )

            SettingsDivider()

            SettingsMenuItem(
                title = "Качество постеров",
                selectedValue = posterQuality,
                options = listOf("Высокое", "Среднее", "Низкое"),
                iconSfName = "rectangle.stack.fill",
                onOptionSelected = { quality ->
                    localStorage.putString("poster_quality", quality)
                    posterQuality = quality
                }
            )

            SettingsDivider()

            SettingsSwitchItem(
                title = "Оригинальное название",
                iconSfName = "doc.text.fill",
                checked = showOriginalTitle,
                onCheckedChange = { checked ->
                    localStorage.saveShowOriginalTitle(checked)
                    showOriginalTitle = checked
                }
            )

            SettingsDivider()

            SettingsSwitchItem(
                title = "Автоперелистывание постеров",
                iconSfName = "rectangle.stack.fill",
                checked = autoAdvanceBackdrops,
                onCheckedChange = { checked ->
                    localStorage.saveAutoAdvanceBackdrops(checked)
                    autoAdvanceBackdrops = checked
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. ЖИДКОЕ СТЕКЛО (iOS Section("Жидкое стекло"))
        val blurPercentText = "${(liquidGlassBlur * 100).roundToInt()}%"
        val glassFooterText = if (isGlassEnabled) {
            "Плавная регулировка от абсолютно прозрачного стекла (0%) до сплошного непрозрачного размытия (100%). Блюр градиентов настраивается независимо."
        } else {
            "В режиме без стекла преломления и аберрации стекла отключены для максимальной плавности. Прогрессивный блюр градиентов может работать независимо."
        }

        SettingsSection(
            title = "Жидкое стекло",
            footer = glassFooterText
        ) {
            SettingsSwitchItem(
                title = "Режим без стекла",
                subtitle = "Отключает эффекты стекла для слабых устройств",
                iconSfName = "waveform",
                checked = !isGlassEnabled,
                onCheckedChange = { noGlass ->
                    val enabled = !noGlass
                    localStorage.saveGlassEnabled(enabled)
                    isGlassEnabled = enabled
                    onGlassEnabledChange(enabled)
                }
            )

            SettingsDivider()

            SettingsSwitchItem(
                title = "Прогрессивный блюр градиентов",
                subtitle = "Плавное оптическое размытие шапки под градиентами",
                iconSfName = "slider.horizontal.3",
                checked = isProgressiveBlurEnabled,
                onCheckedChange = { enabled ->
                    localStorage.saveProgressiveBlurEnabled(enabled)
                    isProgressiveBlurEnabled = enabled
                    onProgressiveBlurChange(enabled)
                }
            )

            AnimatedVisibility(
                visible = isGlassEnabled,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column {
                    SettingsDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SettingsIconBadge(
                                    iconSfName = "sparkles"
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Размытие стекла",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = blurPercentText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (liquidGlassBlur <= 0.01f) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LiquidSlider(
                            value = liquidGlassBlur,
                            onValueChange = { newBlur ->
                                liquidGlassBlur = newBlur
                                localStorage.saveLiquidGlassBlur(newBlur)
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. ВОСПРОИЗВЕДЕНИЕ (iOS Section("Воспроизведение"))
        SettingsSection(title = "Воспроизведение") {
            SettingsMenuItem(
                title = "Качество видео",
                selectedValue = defaultQuality,
                options = listOf("Спрашивать", "Авто", "4K (2160p)", "2K (1440p)", "1080p", "720p", "480p", "360p"),
                iconSfName = "film.fill",
                onOptionSelected = { quality ->
                    localStorage.saveDefaultQuality(quality)
                    defaultQuality = quality
                }
            )

            SettingsDivider()

            SettingsSwitchItem(
                title = "Автопереход к серии",
                iconSfName = "forward.end.fill",
                checked = autoNext,
                onCheckedChange = { checked ->
                    localStorage.saveAutoNextEnabled(checked)
                    autoNext = checked
                }
            )

            SettingsDivider()

            SettingsSwitchItem(
                title = "Жесты яркости и громкости",
                iconSfName = "slider.horizontal.3",
                checked = playerVolumeBrightnessGestures,
                onCheckedChange = { checked ->
                    localStorage.savePlayerVolumeBrightnessGestures(checked)
                    playerVolumeBrightnessGestures = checked
                }
            )

            SettingsDivider()

            SettingsMenuItem(
                title = "Размер субтитров",
                selectedValue = subtitleSize,
                options = listOf("Маленький", "Стандартный", "Крупный"),
                iconSfName = "captions.bubble.fill",
                onOptionSelected = { size ->
                    localStorage.putString("subtitle_size", size)
                    subtitleSize = size
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. ХРАНИЛИЩЕ (iOS Section("Хранилище"))
        SettingsSection(title = "Хранилище") {
            SettingsInfoRow(
                title = "Кэш приложения",
                value = cacheSizeFormatted,
                iconSfName = "internaldrive.fill"
            )

            SettingsDivider()

            SettingsActionItem(
                text = "Очистить кэш",
                iconSfName = "trash.fill",
                isLoading = isClearingCache,
                onClick = { showClearCacheDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. О ПРИЛОЖЕНИИ
        SettingsSection(title = "О приложении") {
            SettingsNavigationItem(
                text = "Telegram-канал",
                detailText = "@slooshapp",
                iconPainter = painterResource(id = R.drawable.ic_telegram)
            ) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/slooshapp"))
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show()
                }
            }

            SettingsDivider()

            SettingsNavigationItem(
                text = if (isCheckingUpdate) "Проверка..." else "Проверить обновление",
                detailText = if (isCheckingUpdate) "" else "v$versionName",
                iconSfName = "info.circle.fill"
            ) {
                if (isCheckingUpdate) return@SettingsNavigationItem
                isCheckingUpdate = true
                scope.launch {
                    try {
                        val updater = AppUpdater(context)
                        val info = updater.checkGithubLatest("sluvskii", "sloosh", versionName)
                        if (info != null) {
                            availableUpdate = info
                        } else {
                            Toast.makeText(context, "У вас установлена актуальная версия", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Не удалось проверить обновления. Проверьте интернет-соединение", Toast.LENGTH_SHORT).show()
                    } finally {
                        isCheckingUpdate = false
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 6. ДИАГНОСТИКА (iOS Section("Диагностика"))
        SettingsSection(title = "Диагностика") {
            SettingsNavigationItem(
                text = "Поделиться логами",
                iconSfName = "doc.text.fill",
                detailText = "",
                onClick = {
                    val report = com.slooshfilm.app.utils.Diagnostics.collectReport(context)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Логи sloosh")
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(shareIntent, "Поделиться логами"))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Sloosh Footer Branding
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_sloosh_logo),
                contentDescription = "sloosh",
                colorFilter = ColorFilter.tint(SlooshAccent),
                modifier = Modifier
                    .width(94.dp)
                    .height(26.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Версия $versionName ($versionCode)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }

    // --- Dialogs ---

    // Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        LiquidGlassAlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = "Очистить кэш?",
            message = "Временные файлы изображений и постеров ($cacheSizeFormatted) будут удалены. Ваши загрузки и избранное останутся нетронутыми.",
            confirmButtonText = "Очистить",
            dismissButtonText = "Отмена",
            isDestructive = true,
            iconSfName = "trash.fill",
            backdrop = sceneBackdrop,
            onConfirm = {
                clearCache()
            }
        )
    }

    // App Update Dialog
    availableUpdate?.let { info ->
        val updateMessage = buildString {
            append("Новая версия: ${info.versionCode}")
            if (!info.description.isNullOrBlank()) {
                append("\n\n")
                append(info.description)
            }
        }
        LiquidGlassAlertDialog(
            onDismissRequest = { availableUpdate = null },
            title = "Доступно обновление",
            message = updateMessage,
            confirmButtonText = "Скачать",
            dismissButtonText = "Позже",
            iconSfName = "arrow.down.circle.fill",
            backdrop = sceneBackdrop,
            onConfirm = {
                val url = info.url
                availableUpdate = null
                AppUpdater(context).downloadAndInstall(url)
            }
        )
    }
}

@Composable
private fun PreviewMoviePosterCard(
    style: String,
    width: Dp,
    titleFill: Float,
    subtitleFill: Float,
    modifier: Modifier = Modifier
) {
    val isClassic = style == LocalStorage.CARD_STYLE_CLASSIC

    // Fluid spring animations for all geometry and transparency
    val classicHeight by animateDpAsState(
        targetValue = if (isClassic) 34.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f),
        label = "previewClassicHeight"
    )
    val classicAlpha by animateFloatAsState(
        targetValue = if (isClassic) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f),
        label = "previewClassicAlpha"
    )
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isClassic) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f),
        label = "previewOverlayAlpha"
    )

    val posterHeight = width * 1.5f

    Column(
        modifier = modifier
            .width(width)
            .height(posterHeight + classicHeight),
        horizontalAlignment = Alignment.Start
    ) {
        // Authentic 2:3 Movie Poster Box (matches MediaCard RoundedRectangle(12.dp) 1:1 in skeleton form)
        Box(
            modifier = Modifier
                .width(width)
                .height(posterHeight)
                .clip(RoundedRectangle(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            // Overlay Mode Skeleton Typography & Gradient Scrim (inside poster, bottom)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .graphicsLayer {
                        alpha = overlayAlpha
                        translationY = (1f - overlayAlpha) * 4.dp.toPx()
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.14f)
                            )
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                // Title skeleton bar (matches title font size and proportions 1:1)
                Box(
                    modifier = Modifier
                        .height(11.dp)
                        .fillMaxWidth(titleFill)
                        .clip(RoundedRectangle(3.dp))
                        .background(Color.White.copy(alpha = 0.45f))
                )
                Spacer(modifier = Modifier.height(3.dp))
                // Subtitle skeleton bar (matches subtitle font size and proportions 1:1)
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .fillMaxWidth(subtitleFill)
                        .clip(RoundedRectangle(2.5.dp))
                        .background(Color.White.copy(alpha = 0.24f))
                )
            }
        }

        // Classic Mode Skeleton Typography (below poster, matches MediaCard 1:1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(classicHeight)
                .clipToBounds()
                .graphicsLayer {
                    alpha = classicAlpha
                    translationY = -(1f - classicAlpha) * 4.dp.toPx()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp, start = 2.dp, end = 2.dp)
            ) {
                // Title skeleton bar
                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .fillMaxWidth(titleFill)
                        .clip(RoundedRectangle(3.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f))
                )
                Spacer(modifier = Modifier.height(3.dp))
                // Subtitle skeleton bar
                Box(
                    modifier = Modifier
                        .height(9.dp)
                        .fillMaxWidth(subtitleFill)
                        .clip(RoundedRectangle(2.5.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f))
                )
            }
        }
    }
}


