package com.slooshfilm.app.ui.home

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.ui.components.EmptyStateView
import com.slooshfilm.app.ui.components.ErrorStateView
import com.slooshfilm.app.ui.components.liquidGlass
import com.slooshfilm.app.ui.components.plusBlendMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Velocity
import com.slooshfilm.app.R
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import com.slooshfilm.app.ui.backdrop.backdrops.LayerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.ProgressiveBlurTokens
import com.slooshfilm.app.ui.components.iosProgressiveBlur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.ui.components.MediaCard
import com.slooshfilm.app.ui.components.ShimmerPlaceholder
import com.slooshfilm.app.data.storage.LocalStorage
import com.slooshfilm.app.ui.components.LocalCardDensity
import com.slooshfilm.app.ui.components.LocalCardStyle
import com.slooshfilm.app.ui.components.rememberCutoutHorizontalPadding
import com.slooshfilm.app.ui.components.rememberNavBarBottomPadding
import androidx.compose.ui.platform.LocalConfiguration
import com.slooshfilm.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediaClick: (MediaDto) -> Unit,
    onWatchClick: (MediaDto) -> Unit = onMediaClick,
    viewModel: HomeViewModel = viewModel(),
    gridState: LazyGridState = rememberLazyGridState(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isProgressiveBlurEnabled = LocalProgressiveBlur.current
    val screenBg = MaterialTheme.colorScheme.background
    val homeBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600

    // In landscape, camera cutout is on the side, but the status bar icons (clock, battery)
    // occupy the system top inset. Adapt dynamically to the device's real status bar height.
    val rawStatusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarPadding = if (rawStatusBarPadding > 0.dp) rawStatusBarPadding else (if (isLandscape) 8.dp else 0.dp)
    val navBarBottomPadding = rememberNavBarBottomPadding()

    val headerTotalHeight = statusBarPadding + (if (isLandscape) 38.dp else 48.dp)
    val bottomTotalPadding = if (isLandscape) navBarBottomPadding + 48.dp else navBarBottomPadding + 72.dp

    val cardDensity = LocalCardDensity.current
    val isCompact = cardDensity == LocalStorage.CARD_DENSITY_COMPACT
    val cutoutPadding = rememberCutoutHorizontalPadding()
    val gridSpacing = if (isTablet) 10.dp else if (isCompact) 6.dp else 8.dp
    val gridSidePadding = (if (isTablet) 20.dp else if (isCompact) 10.dp else 14.dp) + cutoutPadding
    val columnsCount = if (isTablet) 6 else if (isLandscape) 4 else 3

    // Per-category scroll state preservation matching iOS 1:1 (scrollOffsets: [HomeCategory: CGFloat]).
    // rememberLazyGridState is backed by rememberSaveable: positions survive tab switches (the Home
    // entry is disposed while another tab is open) and process death. Fixed-order composition keeps
    // each call positionally memoized.
    val allGridState = rememberLazyGridState()
    val moviesGridState = rememberLazyGridState()
    val seriesGridState = rememberLazyGridState()
    val cartoonsGridState = rememberLazyGridState()
    val animeGridState = rememberLazyGridState()
    val activeGridState = when (uiState.selectedCategory) {
        HomeCategory.ALL -> allGridState
        HomeCategory.MOVIES -> moviesGridState
        HomeCategory.SERIES -> seriesGridState
        HomeCategory.CARTOONS -> cartoonsGridState
        HomeCategory.ANIME -> animeGridState
    }

    // Synchronous scroll & fling tracker on the UI thread to reliably prevent accidental card opens when touching to stop a fling
    val lastScrollTimestamp = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val scrollTrackingConnection = remember(activeGridState) {
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

    // Memoized stable item click callback guarded against active scroll and fling-interruption gestures
    val onMediaCardClick = remember<(MediaDto) -> Unit>(onMediaClick, activeGridState) {
        { media ->
            val now = android.os.SystemClock.uptimeMillis()
            val wasRecentlyScrolling = (now - lastScrollTimestamp.get()) < 350L
            if (!activeGridState.isScrollInProgress && !wasRecentlyScrolling) {
                onMediaClick(media)
            }
        }
    }

    // Detect reaching near bottom for infinite scroll via derivedStateOf to prevent snapshot reader thrashing
    val prefetchThreshold = remember(columnsCount) { columnsCount * 3 }
    val shouldLoadMore by remember(activeGridState, prefetchThreshold) {
        derivedStateOf {
            val layoutInfo = activeGridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - prefetchThreshold
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // 1. Edge-to-edge Scrollable Content Layer (optionally captured in homeBackdrop for header progressive blur)
        val contentModifier = if (isProgressiveBlurEnabled) {
            Modifier
                .fillMaxSize()
                .layerBackdrop(homeBackdrop)
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = contentModifier) {
            // Solid opaque background captured directly in homeBackdrop so text and card borders blur fully
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(screenBg)
            )

            if (uiState.isLoading && uiState.items.isEmpty()) {
                // Initial Loading Placeholders (3 cols on phones, 6 in landscape, 7 on tablets)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    contentPadding = PaddingValues(
                        top = headerTotalHeight + 8.dp,
                        bottom = bottomTotalPadding,
                        start = gridSidePadding,
                        end = gridSidePadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = columnsCount * 3,
                        key = { "init_placeholder_$it" },
                        contentType = { "poster-placeholder" }
                    ) {
                        PosterPlaceholderItem()
                    }
                }
            } else if (uiState.errorMessage != null && uiState.items.isEmpty()) {
                // Premium Apple TV+ Error State
                ErrorStateView(
                    title = "Не удалось загрузить",
                    message = uiState.errorMessage ?: "Проверьте интернет-соединение и попробуйте обновить ленту.",
                    onRetry = { viewModel.refresh() },
                    iconSfName = "antenna.radiowaves.left.and.right",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = headerTotalHeight, bottom = bottomTotalPadding)
                )
            } else if (uiState.items.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = headerTotalHeight, bottom = bottomTotalPadding, start = 32.dp, end = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        iconSfName = "film.fill",
                        title = "Фильмы не найдены",
                        message = "Попробуйте выбрать другую вкладку или обновите ленту."
                    )
                }
            } else {
                // Adaptive Poster Grid with smooth PullToRefresh (3 cols on phones, 6 in landscape, 7 on tablets)
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyVerticalGrid(
                        state = activeGridState,
                        columns = GridCells.Fixed(columnsCount),
                        contentPadding = PaddingValues(
                            top = headerTotalHeight + 6.dp,
                            bottom = bottomTotalPadding,
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
                            items = uiState.items,
                            key = { it.favoriteIdentity },
                            contentType = { "media-card" }
                        ) { media ->
                            MediaCard(
                                media = media,
                                onClick = onMediaCardClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (uiState.isLoadingMore) {
                            items(
                                count = columnsCount,
                                key = { "footer_placeholder_$it" },
                                contentType = { "poster-placeholder" }
                            ) {
                                PosterPlaceholderItem()
                            }
                        }
                    }
                }
            }
        }

        // 2. Floating Top Header (Apple TV+ Style)
        val headerBlurHeight = statusBarPadding + (if (isLandscape) 80.dp else 140.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerBlurHeight)
                .align(Alignment.TopCenter)
        ) {
            HomeFloatingHeaderBackground(
                backdrop = homeBackdrop,
                isProgressiveBlurEnabled = isProgressiveBlurEnabled,
                isScrolledPastTop = {
                    activeGridState.firstVisibleItemIndex > 0 || activeGridState.firstVisibleItemScrollOffset > 30
                },
                screenBg = screenBg,
                statusBarPadding = statusBarPadding,
                headerBlurHeight = headerBlurHeight
            )

            // Category text tabs content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding)
            ) {
                CategoryTextTabs(
                    selectedCategory = uiState.selectedCategory,
                    onSelectCategory = { viewModel.selectCategory(it) }
                )
            }
        }
    }
}

/**
 * Top large typography text tabs matching iOS HomeCategoryTextTabs 1:1 with authentic blend mode overlay.
 */
@Composable
private fun CategoryTextTabs(
    selectedCategory: HomeCategory,
    onSelectCategory: (HomeCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600
    val cutoutPadding = rememberCutoutHorizontalPadding()
    val tabSpacing = if (isLandscape) 10.dp else if (isTablet) 18.dp else 12.dp
    val edgePadding = (if (isTablet) 20.dp else 16.dp) + cutoutPadding
    val titleFontSize = if (isLandscape) 22.sp else if (isTablet) 32.sp else 28.sp

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        contentPadding = PaddingValues(horizontal = edgePadding),
        horizontalArrangement = Arrangement.spacedBy(tabSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(HomeCategory.values(), key = { it.name }) { category ->
            val isSelected = selectedCategory == category
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val textColor by animateColorAsState(
                targetValue = if (isSelected) TextPrimary.copy(alpha = 0.90f) else TextPrimary.copy(alpha = 0.45f),
                animationSpec = spring(),
                label = "categoryTabColor"
            )

            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.94f else 1.0f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                label = "tabScale"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        onSelectCategory(category)
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.title,
                    color = textColor,
                    fontSize = titleFontSize,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = (-0.8).sp
                )
            }
        }
    }
}

@Composable
private fun PosterPlaceholderItem() {
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

/**
 * Isolated floating header background composable.
 * Scoping headerAlpha animation to this leaf avoids recomposing the entire HomeScreen
 * during scroll-triggered fade transitions, preserving 120 FPS on all devices.
 */
@Composable
private fun HomeFloatingHeaderBackground(
    backdrop: LayerBackdrop,
    isProgressiveBlurEnabled: Boolean,
    isScrolledPastTop: () -> Boolean,
    screenBg: Color,
    statusBarPadding: Dp,
    headerBlurHeight: Dp
) {
    val isScrolled = isScrolledPastTop()
    val headerAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "homeHeaderAlpha"
    )

    if (isScrolled || headerAlpha > 0.001f) {
        if (isProgressiveBlurEnabled) {
            val isLightTheme = !LocalSlooshColors.current.isDark
            // Progressive blur layer: true physical variable radius blur (decreasing smoothly from 4dp to 0dp, zero masks)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = headerAlpha }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RectangleShape },
                        effects = {
                            val topOffsetPx = statusBarPadding.toPx()
                            iosProgressiveBlur(
                                maxRadius = ProgressiveBlurTokens.MaxRadius.toPx(),
                                topOffset = topOffsetPx,
                                falloffDistance = (headerBlurHeight.toPx() - topOffsetPx).coerceAtLeast(1f),
                                isLight = isLightTheme
                            )
                        },
                        highlight = null,
                        shadow = null
                    )
            )
        }

        // Ambient dark gradient overlay for typography contrast & seamless edge blending
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
