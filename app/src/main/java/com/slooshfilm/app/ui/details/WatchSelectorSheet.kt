package com.slooshfilm.app.ui.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kyant.shapes.Capsule
import com.kyant.shapes.UnevenRoundedRectangle
import com.slooshfilm.app.R
import com.slooshfilm.app.data.alloha.AllohaApiResult
import com.slooshfilm.app.data.alloha.AllohaTranslationMatcher
import com.slooshfilm.app.data.alloha.DubbingStudio
import com.slooshfilm.app.data.alloha.LanguageFlag
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import java.util.Locale
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.components.EmptyStateView
import com.slooshfilm.app.ui.components.LiquidGlassFloatingButton
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.ProgressiveBlurTokens
import com.slooshfilm.app.ui.components.ShimmerPlaceholder
import com.slooshfilm.app.ui.components.rememberAdaptiveSheetHeight
import com.slooshfilm.app.ui.components.iosProgressiveBlur
import com.slooshfilm.app.ui.theme.*

/** Радиус верхних углов шторки. */
private val SheetCornerRadius = 32.dp

/**
 * Аутентичная шторка выбора озвучки и серий, портированная 1:1 из iOS SourceSelectionView.swift.
 *
 * - Высота шторки зафиксирована на 62% экрана (`skipPartiallyExpanded = true`), поэтому она всегда
 *   остаётся свёрнутой и никогда не разворачивается на весь экран (iOS: `.presentationDetents([.fraction(0.62)])`).
 * - Верхние углы 32dp. Серый драг-индикатор рисуется ПОВЕРХ контента и не занимает места в разметке,
 *   поэтому кнопка закрытия стоит ровно 16dp от верхнего и 16dp от левого края (отступы симметричны).
 * - Верхняя шапка (кнопка закрытия + логотип) без фона — «летающая», с прогрессивным блюром контента
 *   под ней, как на главном экране и в настройках.
 * - Кнопка «Смотреть» — «летающая» белая капсула по ширине контента, закреплена снизу и видна всегда.
 * - Порядок секций строго как на iOS: Озвучка -> Сезон -> Серия. Чипы — 32dp капсулы в FlowRow.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WatchSelectorSheet(
    allohaResult: AllohaApiResult?,
    isLoading: Boolean,
    errorMessage: String? = null,
    initialSeason: Int? = null,
    initialEpisode: Int? = null,
    title: String? = null,
    logoUrl: String? = null,
    backdrop: com.slooshfilm.app.ui.backdrop.Backdrop? = null,
    onDismiss: () -> Unit,
    onPlay: (
        iframeUrl: String,
        translationName: String,
        translationId: String?,
        season: Int?,
        episode: Int?
    ) -> Unit
) {
    // Шторка всегда тёмная, поэтому прогрессивный блюр шапки считается как для тёмной темы.
    val sheetBackground = SurfaceDark

    // Слои для «летающей» шапки с progressive blur (тот же паттерн, что в HomeScreen / SettingsScaffold):
    // sheetContentBackdrop — прокручиваемый контент шторки, sheetSceneBackdrop — контент + размытие шапки.
    val sheetContentBackdrop = rememberLayerBackdrop {
        drawRect(sheetBackground)
        drawContent()
    }
    val sheetSceneBackdrop = rememberLayerBackdrop {
        drawRect(sheetBackground)
        drawContent()
    }

    val isProgressiveBlurEnabled = LocalProgressiveBlur.current
    // Единственная позиция -> шторка всегда свёрнута на фиксированной высоте.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Шторка закрывается свайпом вниз только из начала списка.
    // Material3 в onPostFling всегда отдаёт шторке «хвост» скорости прокрутки (settle(available)),
    // поэтому инерция списка, домотанного до начала, закрывала шторку сама по себе:
    // velocityThreshold = 125 dp/s, флик — это ~1000+ dp/s, а якорей всего два.
    // Перехватываем этот импульс раньше Surface шторки (post-* идут изнутри наружу) и не отдаём
    // его вниз. Дальше settle(0) выбирает якорь по позиции, поэтому для закрытия нужно реально
    // вытянуть шторку из начала списка (positionalThreshold = 56dp), а не «долистать» список.
    val sheetDragGate: NestedScrollConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return if (available.y > 0f) available else Velocity.Zero
            }
        }
    }

    // iOS: .presentationDetents([.fraction(0.62)]).
    // В ландшафте 62% высоты экрана — это ~220dp, из которых шапка (68dp) и нижняя капсула
    // с отступами (~98dp) съедают почти всё: список сезонов/серий не помещается. Поэтому
    // в горизонтали шторка поднимается на 92% высоты, а в портрете остаётся 1:1 с iOS.
    val sheetHeight = rememberAdaptiveSheetHeight(portraitFraction = 0.62f)

    // iOS: .safeAreaInset(edge: .top) -> padding horizontal 16 / top 16 / bottom 8 вокруг строки 44dp.
    val headerTopPadding = 16.dp
    val headerRowHeight = 44.dp
    val headerBottomPadding = 8.dp
    val headerBlurHeight = headerTopPadding + headerRowHeight + headerBottomPadding

    // iOS: VariableBlurView(...).padding(.bottom, -30) — блюр выходит на 30dp ниже шапки,
    // поэтому зона затухания = высота шапки + ProgressiveBlurTokens.Extension (общая для всех экранов).
    val headerBlurLayerHeight = headerBlurHeight + ProgressiveBlurTokens.Extension

    // iOS: контент скролла имеет .contentMargins(top 16) поверх инсета шапки.
    val contentTopPadding = headerBlurHeight + 16.dp

    // iOS: .safeAreaInset(edge: .bottom) с капсулой 50dp + .contentMargins(bottom 28).
    // Отступ снизу = боковым отступам контента (20dp) — капсула «парит», а не липнет к нижнему краю
    // (навбар-инсет добавляет сама шторка, т.к. диалог edge-to-edge).
    val actionButtonHeight = 50.dp
    val actionButtonBottomPadding = 20.dp

    val result = allohaResult
    val hasContent = result != null && (result.movie != null || result.seasons.isNotEmpty())
    val showActionButton = !isLoading && hasContent

    // Итоговый отступ контента снизу: под «летающую» кнопку + дыхание iOS contentMargins(bottom 28).
    val contentBottomPadding = if (showActionButton) {
        actionButtonHeight + actionButtonBottomPadding + 28.dp
    } else {
        28.dp
    }

    // ----- Состояние выбора поднято на уровень шторки, чтобы нижнюю кнопку можно было закрепить. -----

    // Сериалы: сезон -> серия -> озвучка.
    val allSeasons = result?.seasons.orEmpty()
    var selectedSeasonNum by remember(result, initialSeason) {
        val defaultSeason = initialSeason ?: allSeasons.firstOrNull()?.season ?: 1
        mutableIntStateOf(
            if (allSeasons.any { it.season == defaultSeason }) defaultSeason
            else allSeasons.firstOrNull()?.season ?: 1
        )
    }
    val currentSeason = remember(result, selectedSeasonNum) {
        allSeasons.firstOrNull { it.season == selectedSeasonNum } ?: allSeasons.firstOrNull()
    }
    var selectedEpisodeNum by remember(selectedSeasonNum, currentSeason, initialEpisode) {
        val defaultEpisode = initialEpisode ?: currentSeason?.episodes?.firstOrNull()?.episode ?: 1
        mutableIntStateOf(
            if (currentSeason?.episodes?.any { it.episode == defaultEpisode } == true) defaultEpisode
            else currentSeason?.episodes?.firstOrNull()?.episode ?: 1
        )
    }
    val currentEpisode = remember(currentSeason, selectedEpisodeNum) {
        currentSeason?.episodes?.firstOrNull { it.episode == selectedEpisodeNum }
            ?: currentSeason?.episodes?.firstOrNull()
    }

    val allAvailableTranslations = remember(result) {
        allSeasons.flatMap { s -> s.episodes.flatMap { it.translations } }
            .map { it.name }
            .filter { name ->
                val lower = name.lowercase(Locale.ROOT)
                !lower.contains("субтитр") && !lower.contains("subtitle")
            }
            .distinct()
            .sortedBy { AllohaTranslationMatcher.translationRank(it) }
    }
    val currentEpisodeTranslations = remember(currentEpisode) {
        currentEpisode?.translations?.sortedBy { AllohaTranslationMatcher.translationRank(it.name) } ?: emptyList()
    }
    var selectedTranslationName by remember(allAvailableTranslations) {
        val best = AllohaTranslationMatcher.bestTranslation(currentEpisodeTranslations)?.name
            ?: currentEpisodeTranslations.firstOrNull()?.name
            ?: allAvailableTranslations.firstOrNull()
            ?: ""
        mutableStateOf(best)
    }

    // Фильмы: только озвучка. Выбор в шторке идёт по имени, поэтому одинаковые имена
    // (дубли API) схлопываем — иначе чипы неотличимы. Субтитры отсекаем (они доступны в плеере).
    val sortedMovieTranslations = remember(result?.movie) {
        result?.movie?.translations
            ?.filter { t ->
                val lower = t.name.lowercase(Locale.ROOT)
                !lower.contains("субтитр") && !lower.contains("subtitle")
            }
            ?.sortedBy { AllohaTranslationMatcher.translationRank(it.name) }
            ?.distinctBy { it.name }
            ?: emptyList()
    }

    var selectedMovieTranslationName by remember(sortedMovieTranslations) {
        val best = AllohaTranslationMatcher.bestTranslation(sortedMovieTranslations)?.name
            ?: sortedMovieTranslations.firstOrNull()?.name
            ?: ""
        mutableStateOf(best)
    }

    // Разрешение выбранной пары озвучка/серия в реальный iframe для нижней кнопки.
    val resolvedTranslationName: String
    val resolvedTranslationId: String?
    val targetIframe: String?
    if (result?.isSerial == true) {
        val resolved = currentEpisodeTranslations.firstOrNull { it.name == selectedTranslationName }
            ?: currentEpisodeTranslations.firstOrNull {
                AllohaTranslationMatcher.matches(it.name, selectedTranslationName, exactOnly = true)
            }
            ?: currentEpisodeTranslations.firstOrNull {
                AllohaTranslationMatcher.matches(it.name, selectedTranslationName)
            }
            ?: currentEpisodeTranslations.firstOrNull()
        resolvedTranslationName = resolved?.name ?: "По умолчанию"
        resolvedTranslationId = resolved?.id
        targetIframe = resolved?.iframeUrl ?: currentEpisode?.translations?.firstOrNull()?.iframeUrl
    } else {
        val resolved = sortedMovieTranslations.firstOrNull { it.name == selectedMovieTranslationName }
            ?: sortedMovieTranslations.firstOrNull {
                AllohaTranslationMatcher.matches(it.name, selectedMovieTranslationName, exactOnly = true)
            }
            ?: sortedMovieTranslations.firstOrNull {
                AllohaTranslationMatcher.matches(it.name, selectedMovieTranslationName)
            }
            ?: sortedMovieTranslations.firstOrNull()
        resolvedTranslationName = resolved?.name ?: "По умолчанию"
        resolvedTranslationId = resolved?.id
        targetIframe = resolved?.iframeUrl ?: result?.movie?.iframeUrl
    }
    val isReadyToPlay = !targetIframe.isNullOrBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBackground,
        dragHandle = {},
        shape = UnevenRoundedRectangle(
            topStart = SheetCornerRadius,
            topEnd = SheetCornerRadius
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
            ) {

            // 1. Контент и размытие шапки, захваченные в sheetSceneBackdrop.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(sheetSceneBackdrop)
            ) {
                // 1.1 Прокручиваемый контент, захваченный в sheetContentBackdrop.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(sheetContentBackdrop)
                        .nestedScroll(sheetDragGate)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = contentTopPadding,
                                bottom = contentBottomPadding
                            ),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (isLoading) {
                            // Skeleton matching iOS SourceSelectionSkeletonSection
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Озвучка",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(96.dp, 128.dp, 108.dp, 116.dp, 92.dp).forEach { width ->
                                        ShimmerPlaceholder(
                                            modifier = Modifier
                                                .width(width)
                                                .height(32.dp),
                                            cornerRadius = 16.dp
                                        )
                                    }
                                }
                            }
                        } else if (result == null || (result.movie == null && result.seasons.isEmpty())) {
                            // Empty state matching unified EmptyStateView
                            EmptyStateView(
                                iconRes = R.drawable.ic_sf_rectangle_stack_fill,
                                title = "Видео пока недоступно",
                                message = errorMessage ?: "Этот проект пока отсутствует в источниках стриминга или еще не вышел в релиз.",
                                modifier = Modifier.padding(vertical = 48.dp)
                            )
                        } else if (result != null && result.isSerial) {
                            // TV Series Selector: Sections in iOS order: 1. Озвучка, 2. Сезон, 3. Серия

                            // 1. Озвучка (FlowRow, iOS 1:1)
                            if (allAvailableTranslations.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Озвучка",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        allAvailableTranslations.forEachIndexed { index, rawName ->
                                            // iOS: выделение строго по исходному имени (никакого fuzzy-сравнения),
                                            // иначе «Дубляж» и «Дубляж HDrezka Studio» подсвечиваются одновременно.
                                            val isSelected = selectedTranslationName == rawName
                                            val isAvailableInCurrentEp = currentEpisodeTranslations.any {
                                                it.name == rawName ||
                                                    AllohaTranslationMatcher.matches(it.name, rawName, exactOnly = true)
                                            }
                                            val displayName = DubbingStudio.displayTranslationName(
                                                rawName,
                                                index,
                                                allAvailableTranslations
                                            )
                                            val studio = DubbingStudio.detect(rawName)
                                            val flag = LanguageFlag.detect("$rawName $displayName")

                                            WatchSelectorChip(
                                                title = displayName,
                                                isSelected = isSelected,
                                                isAvailable = isAvailableInCurrentEp,
                                                studio = studio,
                                                flag = flag,
                                                onClick = {
                                                    selectedTranslationName = rawName
                                                    // If not available in current ep, find first season/episode with this translation
                                                    if (!isAvailableInCurrentEp) {
                                                        allSeasons.forEach { s ->
                                                            s.episodes.forEach { ep ->
                                                                if (ep.translations.any {
                                                                        it.name == rawName ||
                                                                            AllohaTranslationMatcher.matches(
                                                                                it.name,
                                                                                rawName,
                                                                                exactOnly = true
                                                                            )
                                                                    }
                                                                ) {
                                                                    selectedSeasonNum = s.season
                                                                    selectedEpisodeNum = ep.episode
                                                                    return@WatchSelectorChip
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Сезон (FlowRow, iOS 1:1)
                            if (allSeasons.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Сезон",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        allSeasons.forEach { s ->
                                            val isSelected = s.season == selectedSeasonNum
                                            val isAvailableForTranslation = s.episodes.any { ep ->
                                                ep.translations.any {
                                                    isSameTranslationName(it.name, selectedTranslationName)
                                                }
                                            }
                                            WatchSelectorChip(
                                                title = "${s.season} сезон",
                                                isSelected = isSelected,
                                                isAvailable = isAvailableForTranslation,
                                                onClick = {
                                                    selectedSeasonNum = s.season
                                                    val eps = s.episodes
                                                    if (!eps.any { it.episode == selectedEpisodeNum }) {
                                                        selectedEpisodeNum = eps.firstOrNull()?.episode ?: 1
                                                    }
                                                    val targetEpisode = eps.firstOrNull { it.episode == selectedEpisodeNum }
                                                    val selectedTranslationIsAvailable = targetEpisode?.translations?.any {
                                                        isSameTranslationName(it.name, selectedTranslationName)
                                                    } == true
                                                    if (!selectedTranslationIsAvailable) {
                                                        selectedTranslationName = AllohaTranslationMatcher
                                                            .bestTranslation(targetEpisode?.translations.orEmpty(), selectedTranslationName)
                                                            ?.name
                                                            ?: targetEpisode?.translations?.firstOrNull()?.name
                                                            ?: selectedTranslationName
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Серия (FlowRow, iOS 1:1)
                            val episodes = currentSeason?.episodes ?: emptyList()
                            if (episodes.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Серия",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        episodes.forEach { ep ->
                                            val isSelected = ep.episode == selectedEpisodeNum
                                            val isAvailableForTranslation = ep.translations.any {
                                                isSameTranslationName(it.name, selectedTranslationName)
                                            }
                                            WatchSelectorChip(
                                                title = "${ep.episode} серия",
                                                isSelected = isSelected,
                                                isAvailable = isAvailableForTranslation,
                                                onClick = {
                                                    selectedEpisodeNum = ep.episode
                                                    if (!isAvailableForTranslation) {
                                                        selectedTranslationName = AllohaTranslationMatcher
                                                            .bestTranslation(ep.translations, selectedTranslationName)
                                                            ?.name
                                                            ?: ep.translations.firstOrNull()?.name
                                                            ?: selectedTranslationName
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Movie Selector: только озвучка (FlowRow)
                            if (sortedMovieTranslations.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Озвучка",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        sortedMovieTranslations.forEachIndexed { index, t ->
                                            // iOS: строгое равенство исходного имени.
                                            val isSelected = selectedMovieTranslationName == t.name
                                            val displayName = DubbingStudio.displayTranslationName(
                                                t.name,
                                                index,
                                                sortedMovieTranslations.map { it.name }
                                            )
                                            val studio = DubbingStudio.detect(t.name)
                                            val flag = LanguageFlag.detect("${t.name} $displayName")

                                            WatchSelectorChip(
                                                title = displayName,
                                                isSelected = isSelected,
                                                isAvailable = true,
                                                studio = studio,
                                                flag = flag,
                                                onClick = {
                                                    selectedMovieTranslationName = t.name
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 1.2 «Летающая» шапка без фона: прогрессивный блюр контента + амбиентный градиент.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerBlurHeight)
                        .align(Alignment.TopCenter)
                ) {
                    if (isProgressiveBlurEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(headerBlurLayerHeight)
                                .drawBackdrop(
                                    backdrop = sheetContentBackdrop,
                                    shape = { RectangleShape },
                                    effects = {
                                        iosProgressiveBlur(
                                            maxRadius = ProgressiveBlurTokens.MaxRadius.toPx(),
                                            topOffset = 0f,
                                            falloffDistance = size.height.coerceAtLeast(1f),
                                            isLight = false
                                        )
                                    },
                                    highlight = null,
                                    shadow = null
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        sheetBackground,
                                        sheetBackground.copy(alpha = 0f)
                                    )
                                )
                            )
                    )
                }
            }

            // 2. Шапка поверх размытия (вне capture-слоя): кнопка закрытия + логотип/заголовок.
            //    Отступы 16dp слева/справа и 16dp сверху — серый драг-индикатор места не занимает.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(start = 16.dp, end = 16.dp, top = headerTopPadding)
                    .height(headerRowHeight)
            ) {
                // Centered Logo or Title
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!logoUrl.isNullOrBlank()) {
                        val sheetContext = LocalContext.current
                        val sheetImageRequest = remember(logoUrl) {
                            ImageRequest.Builder(sheetContext)
                                .data(logoUrl)
                                .size(coil.size.Size.ORIGINAL)
                                .crossfade(true)
                                .build()
                        }
                        AsyncImage(
                            model = sheetImageRequest,
                            contentDescription = title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .heightIn(max = 28.dp)
                                .widthIn(max = 220.dp)
                        )
                    } else {
                        Text(
                            text = title?.ifBlank { "Выбор озвучки" } ?: "Выбор озвучки",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Left Close Button (Liquid Glass 44x44 circular button matching iOS)
                LiquidGlassFloatingButton(
                    onClick = onDismiss,
                    backdrop = sheetSceneBackdrop,
                    shape = CircleShape,
                    tint = LiquidGlassSurfaceDark,
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sf_xmark),
                        contentDescription = "Закрыть",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 3. «Летающая» кнопка действия: плотная белая капсула по ширине контента, закреплена снизу,
            //    видна всегда (iOS: .safeAreaInset(edge: .bottom)).
            if (showActionButton) {
                Button(
                    onClick = {
                        val iframe = targetIframe
                        if (!iframe.isNullOrBlank()) {
                            onPlay(
                                iframe,
                                resolvedTranslationName,
                                resolvedTranslationId,
                                if (result?.isSerial == true) selectedSeasonNum else null,
                                if (result?.isSerial == true) selectedEpisodeNum else null
                            )
                        }
                    },
                    enabled = isReadyToPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White,
                        disabledContentColor = Color.Black
                    ),
                    shape = Capsule(),
                    contentPadding = PaddingValues(horizontal = 26.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = actionButtonBottomPadding)
                        .graphicsLayer { alpha = if (isReadyToPlay) 1f else 0.40f }
                        .defaultMinSize(minHeight = actionButtonHeight)
                        .shadow(
                            elevation = 10.dp,
                            shape = Capsule(),
                            spotColor = Color.Black.copy(alpha = 0.22f)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_play_fill),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Смотреть",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }

            // 4. Серый драг-индикатор поверх контента (места в разметке не занимает, iOS 1:1).
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TextTertiaryDark)
            )
        }
        }
    }
}

/**
 * 32dp Капсула выбора (WatchSelectorChip) — аутентичный чип iOS 1:1.
 */
@Composable
fun WatchSelectorChip(
    title: String,
    isSelected: Boolean,
    isAvailable: Boolean = true,
    badge: String? = null,
    studio: DubbingStudio? = null,
    flag: LanguageFlag? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSlooshColors.current.isDark

    val activeIconResId = studio?.iconResId ?: flag?.iconResId
    val displayTitle = if (activeIconResId != null) {
        LanguageFlag.stripLeadingEmoji(title)
    } else {
        title
    }

    val backgroundColor = if (isSelected) {
        if (isDark) Color.White else Color.Black
    } else {
        if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.07f)
    }

    val textColor = if (isSelected) {
        if (isDark) Color.Black else Color.White
    } else {
        TextPrimary
    }

    val leadingPadding = when {
        activeIconResId != null -> 6.dp
        !badge.isNullOrEmpty() -> 8.dp
        else -> 12.dp
    }

    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 32.dp)
            .clip(Capsule())
            .background(backgroundColor)
            .graphicsLayer {
                alpha = if (isSelected || isAvailable) 1.0f else 0.40f
            }
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(start = leadingPadding, end = 11.dp, top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.5.dp)
        ) {
            if (activeIconResId != null) {
                Image(
                    painter = painterResource(activeIconResId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            }
            if (!badge.isNullOrEmpty()) {
                Text(
                    text = badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Text(
                text = displayTitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Одно и то же ли это название озвучки. Как iOS: строгое имя либо точное совпадение
 * (без fuzzy-эвристик, иначе «Дубляж» и «Дубляж HDrezka Studio» считаются одной дорожкой).
 */
private fun isSameTranslationName(candidate: String?, selected: String?): Boolean {
    if (candidate.isNullOrBlank() || selected.isNullOrBlank()) return false
    return candidate == selected ||
        AllohaTranslationMatcher.matches(candidate, selected, exactOnly = true)
}
