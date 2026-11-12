package com.slooshfilm.app.ui.search

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.CircleShape
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.ui.sficons.SfIcon
import com.slooshfilm.app.ui.components.EmptyStateView
import com.slooshfilm.app.ui.components.ErrorStateView
import com.slooshfilm.app.ui.components.InteractiveHighlight
import com.slooshfilm.app.ui.components.LiquidGlassFloatingButton
import com.slooshfilm.app.ui.components.LiquidGlassTokens
import com.slooshfilm.app.ui.components.liquidGlassInteractive
import com.slooshfilm.app.ui.components.rememberCutoutHorizontalPadding
import com.slooshfilm.app.ui.components.rememberNavBarBottomPadding
import androidx.compose.ui.graphics.RectangleShape
import com.slooshfilm.app.ui.backdrop.backdrops.LayerBackdrop
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberCanvasBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.components.LocalBackdrop
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.ProgressiveBlurTokens
import com.slooshfilm.app.ui.components.LocalGlassEnabled
import com.slooshfilm.app.ui.components.iosProgressiveBlur
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Velocity
import com.slooshfilm.app.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.repository.MoviesRepository
import com.slooshfilm.app.data.storage.LocalStorage
import com.slooshfilm.app.ui.components.LocalCardDensity
import com.slooshfilm.app.ui.components.LocalCardStyle
import com.slooshfilm.app.ui.components.MediaCard
import com.slooshfilm.app.ui.components.ShimmerPlaceholder
import com.slooshfilm.app.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onMediaClick: (MediaDto) -> Unit,
    onWatchClick: (MediaDto) -> Unit = onMediaClick,
    searchState: SearchState = rememberSearchState(),
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val screenBg = MaterialTheme.colorScheme.background

    val searchContentBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }
    val sceneBackdrop = rememberLayerBackdrop()
    val isGlassEnabled = LocalGlassEnabled.current

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val rawStatusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarPadding = if (rawStatusBarPadding > 0.dp) rawStatusBarPadding else (if (isLandscape) 8.dp else 0.dp)
    val navBarBottomPadding = rememberNavBarBottomPadding()
    val imeBottomDp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    // Smooth keyboard elevation for bottom padding so content scrolls above keyboard or bottom bar
    val animatedImeBottom by animateDpAsState(
        targetValue = imeBottomDp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "searchContentImeBottom"
    )

    val effectiveBottomPadding = if (animatedImeBottom > 0.dp) {
        animatedImeBottom + (if (isLandscape) 52.dp else 76.dp)
    } else {
        navBarBottomPadding + (if (isLandscape) 52.dp else 76.dp)
    }

    val headerBlurHeight = statusBarPadding + (if (isLandscape) 44.dp else 54.dp)
    val discoveryListState = rememberLazyListState()

    val density = LocalDensity.current
    val headerFadeDistancePx = remember(density) { with(density) { 56.dp.toPx() } }
    val headerScrollFraction by remember(searchState.query, discoveryListState, gridState, headerFadeDistancePx) {
        derivedStateOf {
            val isDiscovery = searchState.query.trim().isEmpty()
            val (firstIndex, offsetPx) = if (isDiscovery) {
                discoveryListState.firstVisibleItemIndex to discoveryListState.firstVisibleItemScrollOffset
            } else {
                gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
            }
            val effectiveOffset = if (firstIndex > 0) headerFadeDistancePx else offsetPx.toFloat()
            (effectiveOffset / headerFadeDistancePx).coerceIn(0f, 1f)
        }
    }

    // Track scroll events synchronously on the UI thread to reliably prevent accidental card opens
    val lastScrollTimestamp = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val scrollTrackingConnection = remember(gridState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                lastScrollTimestamp.set(android.os.SystemClock.uptimeMillis())
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                lastScrollTimestamp.set(android.os.SystemClock.uptimeMillis())
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                lastScrollTimestamp.set(android.os.SystemClock.uptimeMillis())
                return Velocity.Zero
            }
        }
    }

    val onMediaCardClick = remember<(MediaDto) -> Unit>(onMediaClick, gridState) {
        { media ->
            val now = android.os.SystemClock.uptimeMillis()
            val wasRecentlyScrolling = (now - lastScrollTimestamp.get()) < 350L
            if (!gridState.isScrollInProgress && !wasRecentlyScrolling) {
                onMediaClick(media)
            }
        }
    }

    // Detect reaching end for infinite scroll via derivedStateOf to prevent snapshot reader thrashing
    val shouldLoadMore by remember(gridState) {
        derivedStateOf {
            val total = searchState.results.size
            if (total == 0) return@derivedStateOf false
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            searchState.canLoadMore && !searchState.isLoading && !searchState.isLoadingMore && lastVisible >= total - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            searchState.doSearch(searchState.query, page = searchState.currentPage + 1, isAppend = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // 1. Edge-to-Edge Content and Progressive Blur Header captured in sceneBackdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isGlassEnabled) Modifier.layerBackdrop(sceneBackdrop) else Modifier)
        ) {
            // Scrollable Content Layer captured in searchContentBackdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isGlassEnabled) Modifier.layerBackdrop(searchContentBackdrop) else Modifier)
            ) {
                // Solid opaque background so posters and cards blur completely
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(screenBg)
                )

                if (searchState.query.trim().isEmpty()) {
                    // Discovery View
                    SearchDiscoveryView(
                        history = searchState.searchHistory,
                        topPadding = statusBarPadding + (if (isLandscape) 36.dp else 44.dp),
                        bottomPadding = effectiveBottomPadding,
                        onSelectHistory = { selected ->
                            searchState.query = selected
                            searchState.doSearch(selected, page = 1)
                        },
                        onRemoveHistory = { searchState.removeHistory(it) },
                        onClearHistory = { searchState.clearHistory() },
                        onSelectGenre = { genre ->
                            searchState.query = genre
                            searchState.doSearch(genre, page = 1)
                        },
                        onSelectStudio = { studio ->
                            searchState.query = studio
                            searchState.doSearch(studio, page = 1)
                        },
                        listState = discoveryListState
                    )
                } else if (searchState.isLoading && searchState.results.isEmpty()) {
                    val isTablet = configuration.screenWidthDp >= 600
                    val cardDensity = LocalCardDensity.current
                    val isCompact = cardDensity == LocalStorage.CARD_DENSITY_COMPACT
                    val cutoutPadding = rememberCutoutHorizontalPadding()
                    val gridSpacing = if (isTablet) 10.dp else if (isCompact) 6.dp else 8.dp
                    val gridSidePadding = (if (isTablet) 20.dp else if (isCompact) 10.dp else 14.dp) + cutoutPadding
                    val columnsCount = if (isTablet) 6 else if (isLandscape) 4 else 3

                    // Loading Skeleton (3 cols on phones, 6 in landscape, 7 on tablets)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnsCount),
                        contentPadding = PaddingValues(
                            top = statusBarPadding + (if (isLandscape) 8.dp else 14.dp),
                            bottom = effectiveBottomPadding,
                            start = gridSidePadding,
                            end = gridSidePadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                        verticalArrangement = Arrangement.spacedBy(gridSpacing),
                        userScrollEnabled = false,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(count = columnsCount * 3, contentType = { "poster-placeholder" }) {
                            SearchPosterPlaceholder()
                        }
                    }
                } else if (searchState.errorMessage != null && searchState.results.isEmpty()) {
                    // Premium Apple TV+ Error State
                    ErrorStateView(
                        title = "Не удалось выполнить поиск",
                        message = searchState.errorMessage ?: "Проверьте интернет-соединение и попробуйте снова.",
                        onRetry = { searchState.doSearch(searchState.query, page = 1) },
                        iconSfName = "antenna.radiowaves.left.and.right",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = statusBarPadding + (if (isLandscape) 8.dp else 14.dp), bottom = effectiveBottomPadding)
                    )
                } else if (searchState.results.isEmpty()) {
                    // Empty Results State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = statusBarPadding + (if (isLandscape) 8.dp else 14.dp), bottom = effectiveBottomPadding, start = 32.dp, end = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            iconSfName = "film.fill",
                            title = "Ничего не найдено",
                            message = "Попробуйте изменить поисковый запрос."
                        )
                    }
                } else {
                    val isTablet = configuration.screenWidthDp >= 600
                    val cardDensity = LocalCardDensity.current
                    val isCompact = cardDensity == LocalStorage.CARD_DENSITY_COMPACT
                    val cutoutPadding = rememberCutoutHorizontalPadding()
                    val gridSpacing = if (isTablet) 10.dp else if (isCompact) 6.dp else 8.dp
                    val gridSidePadding = (if (isTablet) 20.dp else if (isCompact) 10.dp else 14.dp) + cutoutPadding
                    val columnsCount = if (isTablet) 6 else if (isLandscape) 4 else 3

                    // Adaptive Results Grid (3 cols on phones, 6 in landscape, 7 on tablets)
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(columnsCount),
                        contentPadding = PaddingValues(
                            top = statusBarPadding + (if (isLandscape) 8.dp else 14.dp),
                            bottom = effectiveBottomPadding,
                            start = gridSidePadding,
                            end = gridSidePadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                        verticalArrangement = Arrangement.spacedBy(gridSpacing),
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollTrackingConnection)
                    ) {
                        items(
                            items = searchState.results,
                            key = { it.favoriteIdentity },
                            contentType = { "media-card" }
                        ) { media ->
                            MediaCard(
                                media = media,
                                onClick = onMediaCardClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (searchState.isLoadingMore) {
                            items(
                                count = columnsCount,
                                key = { "search_placeholder_$it" },
                                contentType = { "poster-placeholder" }
                            ) {
                                SearchPosterPlaceholder()
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
                SearchFloatingHeaderBackground(
                    searchContentBackdrop = searchContentBackdrop,
                    headerScrollFraction = headerScrollFraction,
                    screenBg = screenBg,
                    statusBarPadding = statusBarPadding
                )
            }
        }

        // Top right floating action button (Menu/Settings) in Discovery Mode
        if (searchState.query.trim().isEmpty()) {
            val cutoutPadding = rememberCutoutHorizontalPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarPadding)
                    .height(if (isLandscape) 44.dp else 56.dp)
                    .padding(horizontal = 16.dp + cutoutPadding),
                contentAlignment = Alignment.CenterEnd
            ) {
                LiquidGlassFloatingButton(
                    onClick = { onSettingsClick?.invoke() },
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                    backdrop = sceneBackdrop
                ) {
                    SfIcon(
                        systemName = "slider.horizontal.3",
                        contentDescription = "Меню",
                        tint = LocalContentColor.current,
                        size = 24.dp
                    )
                }
            }
        }

    }
}

/**
 * SearchDiscoveryView matching iOS SearchDiscoveryView 1:1.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchDiscoveryView(
    history: List<String>,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSelectHistory: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSelectGenre: (String) -> Unit,
    onSelectStudio: (String) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val cutoutPadding = rememberCutoutHorizontalPadding()
    val horizontalPadding = 16.dp + cutoutPadding

    val chipHighlightScope = rememberCoroutineScope()
    val screenBg = MaterialTheme.colorScheme.background
    // Flat discovery background behind the interactive capsules (history, genres, studios).
    // The chips live inside the content capture layer, so they must sample a backdrop
    // that does not contain themselves.
    val chipBackdrop = rememberCanvasBackdrop {
        drawRect(screenBg)
    }
    val genres = remember {
        listOf(
            "Боевик", "Комедия", "Драма", "Фантастика", "Триллер",
            "Ужасы", "Детектив", "Мелодрама", "Приключения", "Фэнтези",
            "Мультфильм", "Криминал", "Семейный", "Аниме"
        )
    }

    val studios = remember {
        listOf(
            "Marvel", "DC", "A24", "Pixar", "Disney", "Warner Bros.",
            "Universal", "Paramount", "20th Century", "Sony Pictures",
            "DreamWorks", "Netflix", "HBO", "Apple TV+", "Prime Video",
            "Cartoon Network", "Adult Swim"
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding)
    ) {
        // iOS Large Title "Поиск"
        item {
            Text(
                text = "Поиск",
                fontSize = if (isLandscape) 26.sp else 34.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(start = horizontalPadding, end = horizontalPadding, top = 2.dp, bottom = if (isLandscape) 8.dp else 16.dp)
            )
        }

        // 1. History Section
        if (history.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = if (isLandscape) 14.dp else 22.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Недавние запросы",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondaryDark
                        )
                        TextButton(
                            onClick = onClearHistory,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Очистить",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        history.forEach { item ->
                            LiquidGlassFloatingButton(
                                onClick = { onSelectHistory(item) },
                                backdrop = chipBackdrop,
                                blurRadius = 8.dp,
                                shape = Capsule(),
                                tint = LiquidGlassSurfaceDark,
                                borderAlpha = 0.50f,
                                contentPadding = PaddingValues(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { onRemoveHistory(item) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SfIcon(
                                            systemName = "xmark",
                                            contentDescription = "Удалить",
                                            tint = TextMutedDark,
                                            size = 11.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Genres Section
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = if (isLandscape) 16.dp else 24.dp)) {
                Text(
                    text = "Жанры",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    color = TextPrimaryDark,
                    modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 6.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.forEach { genre ->
                        LiquidGlassFloatingButton(
                            onClick = { onSelectGenre(genre) },
                            backdrop = chipBackdrop,
                            blurRadius = 8.dp,
                            shape = Capsule(),
                            tint = LiquidGlassSurfaceDark,
                            borderAlpha = 0.50f,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = genre,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }
            }
        }

        // 3. Studios & Networks Section
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = if (isLandscape) 16.dp else 24.dp)) {
                Text(
                    text = "Студии и стриминги",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    color = TextPrimaryDark,
                    modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 6.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    studios.forEach { studio ->
                        LiquidGlassFloatingButton(
                            onClick = { onSelectStudio(studio) },
                            backdrop = chipBackdrop,
                            blurRadius = 8.dp,
                            shape = Capsule(),
                            tint = LiquidGlassSurfaceDark,
                            borderAlpha = 0.50f,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = studio,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPosterPlaceholder() {
    val isOverlay = LocalCardStyle.current == LocalStorage.CARD_STYLE_OVERLAY
    Column(modifier = Modifier.fillMaxWidth()) {
        ShimmerPlaceholder(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            cornerRadius = 12.dp
        )
        if (!isOverlay) {
            Spacer(modifier = Modifier.height(5.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            ) {
                ShimmerPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp),
                    cornerRadius = 6.dp
                )
                Spacer(modifier = Modifier.height(2.dp))
                ShimmerPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(11.dp),
                    cornerRadius = 4.dp
                )
            }
        }
    }
}

@Composable
private fun SearchFloatingHeaderBackground(
    searchContentBackdrop: LayerBackdrop,
    headerScrollFraction: Float,
    screenBg: Color,
    statusBarPadding: Dp
) {
    val headerAlpha by animateFloatAsState(
        targetValue = headerScrollFraction,
        animationSpec = tween(durationMillis = 200),
        label = "searchHeaderAlpha"
    )

    if (LocalProgressiveBlur.current && headerAlpha > 0.001f) {
        val isLightTheme = !LocalSlooshColors.current.isDark
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = headerAlpha }
                .drawBackdrop(
                    backdrop = searchContentBackdrop,
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

    if (headerAlpha > 0.001f) {
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

