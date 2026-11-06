package com.slooshfilm.app.ui.continue_watching

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.slooshfilm.app.ui.components.rememberCutoutHorizontalPadding
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import androidx.compose.ui.res.painterResource
import com.slooshfilm.app.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.slooshfilm.app.data.storage.PlaybackProgressStore
import com.slooshfilm.app.data.storage.WatchProgressItem
import com.slooshfilm.app.ui.components.EmptyStateView
import com.slooshfilm.app.ui.components.rememberNavBarBottomPadding
import androidx.compose.ui.graphics.RectangleShape
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.components.LocalBackdrop
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.ProgressiveBlurTokens
import com.slooshfilm.app.ui.components.iosProgressiveBlur
import com.slooshfilm.app.ui.theme.*
import java.util.concurrent.TimeUnit

@Composable
fun ContinueScreen(
    onPlayItem: (WatchProgressItem) -> Unit = {},
    onBackClick: () -> Unit = {},
    progressStore: PlaybackProgressStore = PlaybackProgressStore.getInstance(LocalContext.current),
    backdrop: com.slooshfilm.app.ui.backdrop.Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val progressItems by progressStore.itemsFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val authRepo = com.slooshfilm.app.data.repository.AuthRepository.getInstance(context)
        if (authRepo.isAuthenticated) {
            com.slooshfilm.app.data.repository.CloudSyncService.getInstance(context).syncAllData()
        }
    }

    // Одна карточка на тайтл: для сериала хранится прогресс нескольких серий,
    // но в «Продолжить» показываем только последнюю (записи идут от свежих к старым).
    val continueItems = remember(progressItems) { progressItems.distinctBy { it.mediaId } }

    val screenBg = MaterialTheme.colorScheme.background
    val contentBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }
    val sceneBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val rawStatusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarPadding = if (rawStatusBarPadding > 0.dp) rawStatusBarPadding else (if (isLandscape) 8.dp else 0.dp)
    val navBarBottomPadding = rememberNavBarBottomPadding()
    val topHeaderHeight = statusBarPadding + (if (isLandscape) 44.dp else 56.dp)
    val headerBlurHeight = statusBarPadding + (if (isLandscape) 52.dp else 70.dp)
    val bottomTotalPadding = if (isLandscape) navBarBottomPadding + 48.dp else navBarBottomPadding + 72.dp

    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    val headerFadeDistancePx = remember(density) { with(density) { 56.dp.toPx() } }
    val headerScrollFraction by remember(gridState, headerFadeDistancePx) {
        derivedStateOf {
            val offsetPx = if (gridState.firstVisibleItemIndex > 0) {
                headerFadeDistancePx
            } else {
                gridState.firstVisibleItemScrollOffset.toFloat()
            }
            (offsetPx / headerFadeDistancePx).coerceIn(0f, 1f)
        }
    }
    val headerAlpha by animateFloatAsState(
        targetValue = headerScrollFraction,
        animationSpec = tween(durationMillis = 200),
        label = "continueHeaderAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // 1. Edge-to-Edge Content and Progressive Blur Header captured in sceneBackdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(sceneBackdrop)
        ) {
            // Scrollable Content Layer captured in contentBackdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(contentBackdrop)
            ) {
                // Solid opaque background so cards and text blur completely
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(screenBg)
                )

                if (continueItems.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topHeaderHeight, bottom = bottomTotalPadding, start = 32.dp, end = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            iconRes = R.drawable.ic_sf_clock_arrow_circlepath,
                            title = "Вы пока ничего не смотрите",
                            message = "Начните просмотр любого фильма или сериала, и мы сохраним точный таймкод для быстрого продолжения."
                        )
                    }
                } else {
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val isTablet = configuration.screenWidthDp >= 600
                    val cutoutPadding = rememberCutoutHorizontalPadding()

                    // Adaptive Grid: 1 column in portrait, 2 columns in horizontal (landscape) orientation
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(if (isLandscape) 2 else 1),
                        contentPadding = PaddingValues(
                            start = (if (isTablet) 24.dp else 16.dp) + cutoutPadding,
                            end = (if (isTablet) 24.dp else 16.dp) + cutoutPadding,
                            top = topHeaderHeight + 8.dp,
                            bottom = bottomTotalPadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            continueItems,
                            key = { "${it.mediaId}_${it.season}_${it.episode}" }
                        ) { item ->
                            val isSeries = item.season != null && item.episode != null
                            val isEpisodeCompleted = isSeries && item.progress >= 0.90f
                            val effectiveItem = if (isEpisodeCompleted) {
                                item.copy(
                                    episode = (item.episode ?: 1) + 1,
                                    positionMs = 0L
                                )
                            } else {
                                item
                            }
                            ContinueWatchingCard(
                                item = effectiveItem,
                                isNextEpisode = isEpisodeCompleted,
                                onClick = { onPlayItem(effectiveItem) }
                            )
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

                // Ambient gradient overlay for typography contrast & seamless edge blending
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

        // 2. Floating Top Navigation Bar matching iOS SlooshTopBar 1:1
        CompositionLocalProvider(LocalBackdrop provides sceneBackdrop) {
            val cutoutPadding = rememberCutoutHorizontalPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarPadding)
                    .height(if (isLandscape) 44.dp else 56.dp)
                    .padding(horizontal = 16.dp + cutoutPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Продолжить",
                    style = if (isLandscape) SlooshTypography.titleMedium else SlooshTypography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    letterSpacing = (-0.3).sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * ContinueWatchingCard matching iOS ContinueWatchingCard 1:1.
 * Height: 190dp, cornerRadius: 24dp, border: white.opacity(0.08),
 * full-card backdrop artwork with 3-stop linear gradient scrim.
 */
@Composable
private fun ContinueWatchingCard(
    item: WatchProgressItem,
    isNextEpisode: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val cardHeight = if (isLandscape) 175.dp else 190.dp
    val imageUrl = item.backdropUrl?.takeIf { it.isNotBlank() } ?: item.posterUrl
    val isFinished = item.progress >= 0.95f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(
                elevation = 14.dp,
                shape = RoundedRectangle(24.dp),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedRectangle(24.dp))
            .background(SurfaceDark)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedRectangle(24.dp))
            .clickable(onClick = onClick)
    ) {
        // 1 & 2. Card Artwork and Dark Scrim Gradient
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Full-height Backdrop Image
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Dark Scrim Gradient matching iOS ContinueWatchingCard
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.10f),
                                Color.Black.copy(alpha = 0.28f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
        }

        // 3. Content Area at Bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Next episode badge if finished
            if (isFinished && item.season != null && item.episode != null) {
                Row(
                    modifier = Modifier
                        .clip(Capsule())
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sf_play_fill),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Следующая серия",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Title and Subtitle
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val subtitle = buildString {
                    val epLabel = item.episodeLabel
                    if (item.season != null && item.episode != null) {
                        append("Сезон ${item.season}, Серия ${item.episode}")
                    } else if (!epLabel.isNullOrBlank()) {
                        append(epLabel.replace(" • ", ", ").replace(" · ", ", "))
                    }
                }

                if (subtitle.isNotBlank() || isNextEpisode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isNextEpisode) {
                            Box(
                                modifier = Modifier
                                    .clip(Capsule())
                                    .background(SlooshAccent.copy(alpha = 0.22f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Следующая серия",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlooshAccent
                                )
                            }
                        }
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.74f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Progress Bar & Remaining Time (hidden if finished or next episode)
            if (!isFinished && !isNextEpisode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(Capsule())
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = item.progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(Capsule())
                            .background(Color.White)
                    )
                }

                // Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val posText = formatPlaybackTime(item.positionMs)
                    val durText = formatPlaybackTime(item.durationMs)
                    Text(
                        text = "$posText / $durText",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SlooshFontFamily,
                        letterSpacing = (-0.2).sp,
                        color = Color.White.copy(alpha = 0.92f)
                    )

                    val remaining = formatRemaining(item.positionMs, item.durationMs)
                    if (remaining.isNotBlank()) {
                        Text(
                            text = remaining,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SlooshFontFamily,
                            letterSpacing = (-0.2).sp,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatPlaybackTime(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun formatRemaining(posMs: Long, durMs: Long): String {
    if (durMs <= 0) return ""
    val diff = (durMs - posMs).coerceAtLeast(0)
    val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
    return if (mins > 60) {
        val hours = mins / 60
        val remMins = mins % 60
        "Осталось $hours ч $remMins мин"
    } else if (mins > 0) {
        "Осталось $mins мин"
    } else {
        ""
    }
}
