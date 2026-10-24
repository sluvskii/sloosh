package com.slooshfilm.app.ui.home

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
import com.slooshfilm.app.ui.components.liquidGlass
import com.slooshfilm.app.ui.components.plusBlendMode
import androidx.compose.ui.res.painterResource
import com.slooshfilm.app.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.ProgressiveBlurTokens
import com.slooshfilm.app.ui.components.iosProgressiveBlur
import androidx.compose.ui.text.font.FontWeight
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
import com.slooshfilm.app.ui.components.rememberNavBarBottomPadding
import com.slooshfilm.app.ui.theme.*

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

    // Edge-to-edge padding calculations for floating overlays
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottomPadding = rememberNavBarBottomPadding()

    val headerTotalHeight = statusBarPadding + 48.dp
    val bottomTotalPadding = navBarBottomPadding + 72.dp

    val cardDensity = LocalCardDensity.current
    val isCompact = cardDensity == LocalStorage.CARD_DENSITY_COMPACT
    val gridSpacing = if (isCompact) 6.dp else 8.dp
    val gridSidePadding = if (isCompact) 10.dp else 14.dp

    // Per-category scroll state preservation matching iOS 1:1 (scrollOffsets: [HomeCategory: CGFloat]).
    // rememberLazyGridState is backed by rememberSaveable: positions survive tab switches (the Home
    // entry is disposed while another tab is open) and process death. Fixed-order composition keeps
    // each call positionally memoized.
    val categoryGridStates = HomeCategory.values()
        .map { it to rememberLazyGridState() }
        .toMap()
    val activeGridState = categoryGridStates.getValue(uiState.selectedCategory)

    var longClickedMedia by remember { mutableStateOf<MediaDto?>(null) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Detect reaching near bottom for infinite scroll on active category grid
    val lastVisibleItemIndex by remember(activeGridState, uiState.items) {
        derivedStateOf {
            activeGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }
    }

    LaunchedEffect(lastVisibleItemIndex, uiState.items.size) {
        if (uiState.items.isNotEmpty() && lastVisibleItemIndex >= uiState.items.size - 4) {
            viewModel.loadMore()
        }
    }

    // Header blur & tint stay fully transparent at the top and fade in once the grid starts scrolling
    val density = androidx.compose.ui.platform.LocalDensity.current
    val headerFadeDistancePx = remember(density) { with(density) { 56.dp.toPx() } }
    val headerScrollFraction by remember(activeGridState, headerFadeDistancePx) {
        derivedStateOf {
            val offsetPx = if (activeGridState.firstVisibleItemIndex > 0) {
                headerFadeDistancePx
            } else {
                activeGridState.firstVisibleItemScrollOffset.toFloat()
            }
            (offsetPx / headerFadeDistancePx).coerceIn(0f, 1f)
        }
    }
    val headerAlpha by animateFloatAsState(
        targetValue = headerScrollFraction,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "homeHeaderAlpha"
    )

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
                // Initial Loading Placeholders
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
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
                    items(count = 12, contentType = { "poster-placeholder" }) {
                        PosterPlaceholderItem()
                    }
                }
            } else if (uiState.errorMessage != null && uiState.items.isEmpty()) {
                // Error State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = headerTotalHeight, bottom = bottomTotalPadding, start = 32.dp, end = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.errorMessage ?: "Ошибка загрузки",
                            color = TextSecondaryDark,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        val btnContainer = if (!LocalSlooshColors.current.isDark) Color.Black else Color.White
                        val btnContent = if (!LocalSlooshColors.current.isDark) Color.White else Color.Black
                        Button(
                            onClick = { viewModel.refresh() },
                            shape = Capsule(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = btnContainer,
                                contentColor = btnContent
                            )
                        ) {
                            Text(text = "Повторить", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else if (uiState.items.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = headerTotalHeight, bottom = bottomTotalPadding, start = 32.dp, end = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_film_fill),
                            contentDescription = null,
                            tint = TextTertiaryDark,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Фильмы не найдены",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Попробуйте выбрать другую вкладку или обновите ленту.",
                            fontSize = 14.sp,
                            color = TextSecondaryDark,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Flagship 3-Column Poster Grid
                LazyVerticalGrid(
                    state = activeGridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(
                        top = headerTotalHeight + 6.dp,
                        bottom = bottomTotalPadding,
                        start = gridSidePadding,
                        end = gridSidePadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.items,
                        key = { it.identifier },
                        contentType = { "media-card" }
                    ) { media ->
                        MediaCard(
                            media = media,
                            onClick = { onMediaClick(media) },
                            onLongClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                longClickedMedia = media
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (uiState.isLoadingMore) {
                        items(count = 3, contentType = { "poster-placeholder" }) {
                            PosterPlaceholderItem()
                        }
                    }
                }
            }
        }

        // 2. Floating Top Header (Apple TV+ Style)
        val headerBlurHeight = statusBarPadding + 140.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerBlurHeight)
                .align(Alignment.TopCenter)
        ) {
            if (isProgressiveBlurEnabled) {
                val isLightTheme = !LocalSlooshColors.current.isDark
                // Progressive blur layer: true physical variable radius blur (decreasing smoothly from 4dp to 0dp, zero masks)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = headerAlpha }
                        .drawBackdrop(
                            backdrop = homeBackdrop,
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

            // Category text tabs content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                CategoryTextTabs(
                    selectedCategory = uiState.selectedCategory,
                    onSelectCategory = { viewModel.selectCategory(it) }
                )
            }
        }

        // Direct Play long-press Action Sheet (matching iOS HomeDirectPlayWrapper 1:1)
        longClickedMedia?.let { media ->
            com.slooshfilm.app.ui.components.LiquidGlassDialog(
                title = media.displayTitle,
                onDismissRequest = { longClickedMedia = null },
                backdrop = homeBackdrop
            ) { dismiss ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedRectangle(12.dp))
                            .clickable {
                                dismiss()
                                onWatchClick(media)
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_play_fill),
                            contentDescription = null,
                            tint = SlooshAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Смотреть", fontSize = 16.sp, color = Color.White)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedRectangle(12.dp))
                            .clickable {
                                dismiss()
                                onMediaClick(media)
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_info_circle_fill),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Подробнее", fontSize = 16.sp, color = Color.White)
                    }
                }
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
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(HomeCategory.values()) { category ->
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
                    fontSize = 28.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = (-0.8).sp,
                    modifier = Modifier. plusBlendMode()
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
