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
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.ui.components.InteractiveHighlight
import com.slooshfilm.app.ui.components.LiquidGlassFloatingButton
import com.slooshfilm.app.ui.components.LiquidGlassTokens
import com.slooshfilm.app.ui.components.liquidGlassInteractive
import com.slooshfilm.app.ui.components.rememberNavBarBottomPadding
import androidx.compose.ui.graphics.RectangleShape
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberCanvasBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.components.LocalBackdrop
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.ProgressiveBlurTokens
import com.slooshfilm.app.ui.components.LocalGlassEnabled
import com.slooshfilm.app.ui.components.iosProgressiveBlur
import androidx.compose.ui.res.painterResource
import com.slooshfilm.app.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var longClickedMedia by remember { mutableStateOf<MediaDto?>(null) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val screenBg = MaterialTheme.colorScheme.background

    val searchContentBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }
    val sceneBackdrop = rememberLayerBackdrop()
    val isGlassEnabled = LocalGlassEnabled.current

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
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
        animatedImeBottom + 76.dp
    } else {
        navBarBottomPadding + 76.dp
    }

    val headerBlurHeight = statusBarPadding + 54.dp

    // Detect reaching end for infinite scroll
    val lastVisibleIndex by remember {
        derivedStateOf { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }

    LaunchedEffect(lastVisibleIndex, searchState.results.size) {
        if (searchState.results.isNotEmpty() && searchState.canLoadMore && !searchState.isLoading && !searchState.isLoadingMore && lastVisibleIndex >= searchState.results.size - 4) {
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
                        topPadding = statusBarPadding + 44.dp,
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
                        }
                    )
                } else if (searchState.isLoading && searchState.results.isEmpty()) {
                    // Loading Skeleton (3 columns)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(
                            top = statusBarPadding + 14.dp,
                            bottom = effectiveBottomPadding,
                            start = 14.dp,
                            end = 14.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(count = 12, contentType = { "poster-placeholder" }) {
                            SearchPosterPlaceholder()
                        }
                    }
                } else if (searchState.errorMessage != null && searchState.results.isEmpty()) {
                    // Error State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = statusBarPadding + 14.dp, bottom = effectiveBottomPadding, start = 32.dp, end = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = searchState.errorMessage ?: "Ошибка поиска",
                                color = TextSecondaryDark,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            val btnContainer = if (!LocalSlooshColors.current.isDark) Color.Black else Color.White
                            val btnContent = if (!LocalSlooshColors.current.isDark) Color.White else Color.Black
                            Button(
                                onClick = { searchState.doSearch(searchState.query, page = 1) },
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
                } else if (searchState.results.isEmpty()) {
                    // Empty Results State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = statusBarPadding + 14.dp, bottom = effectiveBottomPadding, start = 32.dp, end = 32.dp),
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
                                text = "Ничего не найдено",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Попробуйте изменить запрос",
                                fontSize = 14.sp,
                                color = TextSecondaryDark,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val cardDensity = LocalCardDensity.current
                    val isCompact = cardDensity == LocalStorage.CARD_DENSITY_COMPACT
                    val gridSpacing = if (isCompact) 6.dp else 8.dp
                    val gridSidePadding = if (isCompact) 10.dp else 14.dp

                    // 3-Column Results Grid
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(
                            top = statusBarPadding + 14.dp,
                            bottom = effectiveBottomPadding,
                            start = gridSidePadding,
                            end = gridSidePadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                        verticalArrangement = Arrangement.spacedBy(gridSpacing),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = searchState.results,
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

                        if (searchState.isLoadingMore) {
                            items(count = 3, contentType = { "poster-placeholder" }) {
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
                if (LocalProgressiveBlur.current) {
                    val isLightTheme = !LocalSlooshColors.current.isDark
                    // Progressive blur layer: true physical variable radius blur (decreasing smoothly from 4dp to 0dp)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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

                // Ambient gradient overlay for typography contrast & seamless edge blending
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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

        // Top right floating action button (Menu/Settings) in Discovery Mode
        if (searchState.query.trim().isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                LiquidGlassFloatingButton(
                    onClick = { onSettingsClick?.invoke() },
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                    backdrop = sceneBackdrop
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sf_slider_horizontal_3),
                        contentDescription = "Меню",
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Direct Play long-press Action Sheet (matching iOS SearchView 1:1)
        longClickedMedia?.let { media ->
            com.slooshfilm.app.ui.components.LiquidGlassDialog(
                title = media.displayTitle,
                onDismissRequest = { longClickedMedia = null },
                backdrop = sceneBackdrop
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
    onSelectStudio: (String) -> Unit
) {
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding)
    ) {
        // iOS Large Title "Поиск"
        item {
            Text(
                text = "Поиск",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 16.dp)
            )
        }

        // 1. History Section
        if (history.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
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
                                color = SlooshAccent
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        history.forEach { item ->
                            val chipHighlight = remember { InteractiveHighlight(animationScope = chipHighlightScope) }
                            Row(
                                modifier = Modifier
                                    .liquidGlassInteractive(
                                        interactiveHighlight = chipHighlight,
                                        style = LiquidGlassTokens.Button,
                                        shape = Capsule(),
                                        backdrop = chipBackdrop
                                    )
                                    .clip(Capsule())
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                        role = Role.Button
                                    ) { onSelectHistory(item) }
                                    .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
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
                                    Icon(
                                        painter = painterResource(R.drawable.ic_sf_xmark),
                                        contentDescription = "Удалить",
                                        tint = TextMutedDark,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Genres Section
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text(
                    text = "Жанры",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    color = TextPrimaryDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.forEach { genre ->
                        val chipHighlight = remember { InteractiveHighlight(animationScope = chipHighlightScope) }
                        Box(
                            modifier = Modifier
                                .liquidGlassInteractive(
                                    interactiveHighlight = chipHighlight,
                                    style = LiquidGlassTokens.Button,
                                    shape = Capsule(),
                                    backdrop = chipBackdrop
                                )
                                .clip(Capsule())
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                    role = Role.Button
                                ) { onSelectGenre(genre) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
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
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text(
                    text = "Студии и стриминги",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    color = TextPrimaryDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    studios.forEach { studio ->
                        val chipHighlight = remember { InteractiveHighlight(animationScope = chipHighlightScope) }
                        Box(
                            modifier = Modifier
                                .liquidGlassInteractive(
                                    interactiveHighlight = chipHighlight,
                                    style = LiquidGlassTokens.Button,
                                    shape = Capsule(),
                                    backdrop = chipBackdrop
                                )
                                .clip(Capsule())
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                    role = Role.Button
                                ) { onSelectStudio(studio) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
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

