package com.slooshfilm.app.ui.details

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.slooshfilm.app.ui.sficons.SfIcon
import com.slooshfilm.app.ui.components.rememberCutoutHorizontalPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.slooshfilm.app.R
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.repository.MoviesRepository
import com.slooshfilm.app.data.storage.LocalStorage
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.slooshfilm.app.ui.backdrop.backdrops.LayerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.components.LocalBackdrop
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.ProgressiveBlurTokens
import com.slooshfilm.app.ui.components.iosProgressiveBlur
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import com.slooshfilm.app.ui.backdrop.backdrops.rememberCanvasBackdrop
import com.slooshfilm.app.ui.components.EmptyStateView
import com.slooshfilm.app.ui.components.ErrorStateView
import com.slooshfilm.app.ui.components.LiquidGlassFloatingButton
import com.slooshfilm.app.ui.components.LocalCardDensity
import com.slooshfilm.app.ui.components.MediaCard
import com.slooshfilm.app.ui.components.ShimmerPlaceholder
import com.slooshfilm.app.ui.theme.BackgroundDark
import com.slooshfilm.app.ui.theme.SlooshAccent
import com.slooshfilm.app.ui.theme.SlooshTypography
import com.slooshfilm.app.ui.theme.TextPrimaryDark
import com.slooshfilm.app.ui.theme.TextSecondaryDark
import com.slooshfilm.app.ui.theme.TextTertiaryDark
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCatalogScreen(
    kind: String,
    id: String,
    name: String,
    mediaType: String?,
    onBack: () -> Unit,
    onMediaClick: (MediaDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val state = remember(kind, id, name, mediaType, scope) {
        MediaCatalogState(
            kind = kind,
            id = id,
            name = name,
            mediaType = mediaType,
            repository = MoviesRepository.getInstance(),
            scope = scope
        )
    }
    val gridState = rememberLazyGridState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600
    val isCompact = LocalCardDensity.current == LocalStorage.CARD_DENSITY_COMPACT
    val cutoutPadding = rememberCutoutHorizontalPadding()
    val horizontalPadding = (if (isTablet) 20.dp else if (isCompact) 10.dp else 14.dp) + cutoutPadding
    val gridSpacing = if (isTablet) 10.dp else if (isCompact) 6.dp else 8.dp
    val columnsCount = if (isTablet) 6 else if (isLandscape) 4 else 3

    val title = remember(kind, name) {
        if (kind == CATALOG_KIND_GENRE) displayGenreTitle(name) else name
    }

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

    LaunchedEffect(state) {
        state.loadInitial()
    }

    val shouldLoadMore by remember(gridState) {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val total = layoutInfo.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= total - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            state.loadNextPage()
        }
    }

    val screenBg = BackgroundDark
    val isProgressiveBlurEnabled = LocalProgressiveBlur.current
    val contentBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }
    val sceneBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }

    val rawStatusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarPadding = if (rawStatusBarPadding > 0.dp) rawStatusBarPadding else (if (isLandscape) 8.dp else 0.dp)
    val navBarBottomPadding = com.slooshfilm.app.ui.components.rememberNavBarBottomPadding()
    val topHeaderHeight = statusBarPadding + (if (isLandscape) 44.dp else 56.dp)
    val headerBlurHeight = statusBarPadding + (if (isLandscape) 52.dp else 70.dp)
    val bottomTotalPadding = if (isLandscape) navBarBottomPadding + 48.dp else navBarBottomPadding + 24.dp

    val isScrolledPastTop by remember(gridState) {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 30
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
                .then(if (isProgressiveBlurEnabled) Modifier.layerBackdrop(sceneBackdrop) else Modifier)
        ) {
            // Scrollable Content Layer captured in contentBackdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isProgressiveBlurEnabled) Modifier.layerBackdrop(contentBackdrop) else Modifier)
            ) {
                // Solid opaque background so posters and cards blur completely
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(screenBg)
                )

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { state.loadInitial(force = true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        state.isLoading && state.items.isEmpty() -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columnsCount),
                                contentPadding = PaddingValues(
                                    top = topHeaderHeight + 8.dp,
                                    bottom = bottomTotalPadding,
                                    start = horizontalPadding,
                                    end = horizontalPadding
                                ),
                                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                                userScrollEnabled = false,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(columnsCount * 3, contentType = { "poster-placeholder" }) {
                                    Column {
                                        ShimmerPlaceholder(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(2f / 3f),
                                            cornerRadius = 12.dp
                                        )
                                        Spacer(Modifier.height(7.dp))
                                        ShimmerPlaceholder(Modifier.fillMaxWidth(0.82f).height(14.dp), cornerRadius = 4.dp)
                                        Spacer(Modifier.height(5.dp))
                                        ShimmerPlaceholder(Modifier.fillMaxWidth(0.55f).height(11.dp), cornerRadius = 4.dp)
                                    }
                                }
                            }
                        }

                        state.items.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        top = topHeaderHeight,
                                        bottom = bottomTotalPadding,
                                        start = 32.dp,
                                        end = 32.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.errorMessage != null) {
                                    ErrorStateView(
                                        title = "Не удалось загрузить",
                                        message = state.errorMessage ?: "Проверьте интернет-соединение и попробуйте снова.",
                                        onRetry = { state.loadInitial(force = true) },
                                        iconSfName = "antenna.radiowaves.left.and.right",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    EmptyStateView(
                                        iconSfName = "film.fill",
                                        title = "Ничего не найдено",
                                        message = if (kind == CATALOG_KIND_GENRE) {
                                            "В категории «$title» пока нет доступных фильмов"
                                        } else {
                                            "В каталоге «$title» пока нет доступных релизов"
                                        }
                                    )
                                }
                            }
                        }

                        else -> {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(columnsCount),
                                contentPadding = PaddingValues(
                                    top = topHeaderHeight + 8.dp,
                                    bottom = bottomTotalPadding,
                                    start = horizontalPadding,
                                    end = horizontalPadding
                                ),
                                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollTrackingConnection)
                            ) {
                                items(
                                    items = state.items,
                                    key = { it.favoriteIdentity },
                                    contentType = { "media-card" }
                                ) { media ->
                                    MediaCard(
                                        media = media,
                                        onClick = onMediaCardClick,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (state.isAppending) {
                                    items(
                                        count = columnsCount,
                                        key = { "cat_placeholder_$it" },
                                        contentType = { "poster-placeholder" }
                                    ) {
                                        Column {
                                            ShimmerPlaceholder(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(2f / 3f),
                                                cornerRadius = 12.dp
                                            )
                                            Spacer(Modifier.height(7.dp))
                                            ShimmerPlaceholder(Modifier.fillMaxWidth(0.82f).height(14.dp), cornerRadius = 4.dp)
                                        }
                                    }
                                }
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
                CatalogFloatingHeaderBackground(
                    contentBackdrop = contentBackdrop,
                    isProgressiveBlurEnabled = isProgressiveBlurEnabled,
                    isScrolledPastTop = isScrolledPastTop,
                    screenBg = screenBg,
                    statusBarPadding = statusBarPadding
                )
            }
        }

        // 2. Floating Top Navigation Bar (refracting sceneBackdrop)
        CompositionLocalProvider(LocalBackdrop provides sceneBackdrop) {
            val cutoutPadding = rememberCutoutHorizontalPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding)
                    .height(if (isLandscape) 44.dp else 56.dp)
                    .padding(horizontal = 16.dp + cutoutPadding),
                contentAlignment = Alignment.Center
            ) {
                // Centered Title matching iOS SlooshTopBar 1:1
                Text(
                    text = title,
                    style = if (isLandscape) SlooshTypography.titleMedium else SlooshTypography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 56.dp)
                )

                // Circular Glass Back Button
                LiquidGlassFloatingButton(
                    onClick = onBack,
                    backdrop = sceneBackdrop,
                    shape = CircleShape,
                    tint = com.slooshfilm.app.ui.theme.LiquidGlassSurfaceDark,
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.CenterStart)
                ) {
                    SfIcon(
                        systemName = "chevron.left",
                        contentDescription = "Назад",
                        tint = Color.White,
                        size = 24.dp,
                        weight = FontWeight.SemiBold,
                        modifier = Modifier.offset(x = (-1.5).dp)
                    )
                }
            }
        }
    }
}

@Stable
private class MediaCatalogState(
    private val kind: String,
    private val id: String,
    private val name: String,
    private val mediaType: String?,
    private val repository: MoviesRepository,
    private val scope: CoroutineScope
) {
    var items by mutableStateOf<List<MediaDto>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var isAppending by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var page by mutableIntStateOf(1)
    private var canLoadMore by mutableStateOf(true)
    private var generation = 0
    private var initialJob: Job? = null
    private var appendJob: Job? = null

    fun loadInitial(force: Boolean = false) {
        if (isLoading || isRefreshing) return
        generation++
        val requestGeneration = generation
        initialJob?.cancel()
        appendJob?.cancel()
        appendJob = null
        isAppending = false
        if (force && items.isNotEmpty()) {
            isRefreshing = true
        } else {
            isLoading = true
            items = emptyList()
        }
        errorMessage = null
        page = 1
        canLoadMore = true

        initialJob = scope.launch {
            try {
                val response = loadPage(1)
                if (requestGeneration != generation) return@launch
                if (response == null) {
                    errorMessage = "Не удалось загрузить каталог"
                    canLoadMore = false
                } else {
                    val rawItems = response.allItems
                    items = filterValidItems(rawItems)
                    canLoadMore = rawItems.isNotEmpty() && (kind == CATALOG_KIND_GENRE || page < response.effectiveTotalPages)
                    page = 1
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (requestGeneration == generation) {
                    errorMessage = "Не удалось загрузить каталог"
                    if (!force) items = emptyList()
                    canLoadMore = false
                }
            } finally {
                if (requestGeneration == generation) {
                    isLoading = false
                    isRefreshing = false
                }
            }
        }
    }

    fun loadNextPage() {
        if (isLoading || isRefreshing || isAppending || !canLoadMore || items.isEmpty()) return
        val requestGeneration = generation
        val nextPage = page + 1
        isAppending = true
        appendJob = scope.launch {
            try {
                val response = loadPage(nextPage)
                if (requestGeneration != generation) return@launch
                val rawItems = response?.allItems.orEmpty()
                if (rawItems.isEmpty()) {
                    canLoadMore = false
                    return@launch
                }
                val existing = items.mapTo(HashSet()) { it.favoriteIdentity }
                val nextItems = filterValidItems(rawItems).filter { it.favoriteIdentity !in existing }
                items = items + nextItems
                page = nextPage
                val hasMorePages = kind == CATALOG_KIND_GENRE ||
                    nextPage < (response?.effectiveTotalPages ?: nextPage)
                canLoadMore = nextItems.isNotEmpty() && hasMorePages
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (requestGeneration == generation) errorMessage = "Не удалось загрузить каталог"
            } finally {
                if (requestGeneration == generation) isAppending = false
            }
        }
    }

    private suspend fun loadPage(page: Int) = when (kind) {
        CATALOG_KIND_GENRE -> repository.getGenreCatalog(name, mediaType, page)
        CATALOG_KIND_STUDIO -> repository.getStudioCatalog(id, page)
        else -> null
    }

    private fun filterValidItems(items: List<MediaDto>): List<MediaDto> = items
        .filter { media ->
            val poster = media.getDisplayPosterUrl().orEmpty()
            poster.isNotBlank() && !poster.contains("no-poster", ignoreCase = true) &&
                media.displayTitle.isNotBlank()
        }
        .distinctBy { it.favoriteIdentity }
}

private const val CATALOG_KIND_GENRE = "genre"
private const val CATALOG_KIND_STUDIO = "studio"

private fun displayGenreTitle(genre: String): String {
    val normalized = genre.trim()
    return if (normalized.equals("НФ и фэнтези", ignoreCase = true) ||
        normalized.equals("Научная фантастика и фэнтези", ignoreCase = true)
    ) {
        "НФ и фэнтези"
    } else {
        normalized.replaceFirstChar { char -> char.titlecase() }
    }
}

@Composable
private fun CatalogFloatingHeaderBackground(
    contentBackdrop: LayerBackdrop,
    isProgressiveBlurEnabled: Boolean,
    isScrolledPastTop: Boolean,
    screenBg: Color,
    statusBarPadding: Dp
) {
    val headerAlpha by animateFloatAsState(
        targetValue = if (isScrolledPastTop) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "catalogHeaderAlpha"
    )

    if (isProgressiveBlurEnabled && (isScrolledPastTop || headerAlpha > 0.001f)) {
        val isLightTheme = !LocalSlooshColors.current.isDark
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = headerAlpha }
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

    if (isScrolledPastTop || headerAlpha > 0.001f) {
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
