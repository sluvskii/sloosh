package com.slooshfilm.app.ui.continue_watching

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.slooshfilm.app.data.storage.PlaybackProgressStore
import com.slooshfilm.app.data.storage.WatchProgressItem
import com.slooshfilm.app.ui.components.LiquidGlassAlertDialog
import com.slooshfilm.app.ui.components.LiquidGlassFloatingButton
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
    // Одна карточка на тайтл: для сериала хранится прогресс нескольких серий,
    // но в «Продолжить» показываем только последнюю (записи идут от свежих к старым).
    val continueItems = remember(progressItems) { progressItems.distinctBy { it.mediaId } }

    var showClearConfirm by remember { mutableStateOf(false) }

    val screenBg = MaterialTheme.colorScheme.background
    val contentBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }
    val sceneBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottomPadding = rememberNavBarBottomPadding()
    val topHeaderHeight = statusBarPadding + 68.dp
    val headerBlurHeight = statusBarPadding + 88.dp
    val bottomTotalPadding = navBarBottomPadding + 72.dp

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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.ic_sf_clock_arrow_circlepath),
                                contentDescription = null,
                                tint = TextTertiaryDark,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Вы пока ничего не смотрите",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Начните просмотр любого фильма или сериала, и мы сохраним точный таймкод для быстрого продолжения.",
                                fontSize = 14.sp,
                                color = TextSecondaryDark,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // List of cinematic 190dp cards
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = topHeaderHeight + 12.dp,
                            bottom = bottomTotalPadding
                        ),
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
                                onClick = { onPlayItem(effectiveItem) },
                                onRemove = {
                                    progressStore.deleteProgress(item.mediaId, item.season, item.episode)
                                }
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

        // 2. Floating Top Header with sceneBackdrop
        CompositionLocalProvider(LocalBackdrop provides sceneBackdrop) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Продолжить",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    letterSpacing = (-0.6).sp
                )
            }
        }
    }

    if (showClearConfirm) {
        LiquidGlassAlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = "Очистить историю?",
            message = "Все сохранённые таймкоды будут удалены.",
            confirmButtonText = "Очистить",
            dismissButtonText = "Отмена",
            isDestructive = true,
            onConfirm = {
                progressStore.clearAll()
            }
        )
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
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageUrl = item.backdropUrl?.takeIf { it.isNotBlank() } ?: item.posterUrl
    val isFinished = item.progress >= 0.95f
    val cardBackdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
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
        // 1 & 2. Card Artwork and Dark Scrim Gradient captured in cardBackdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(cardBackdrop)
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

        // 3. Top-Right Delete Button with Liquid Glass (refracting cardBackdrop)
        LiquidGlassFloatingButton(
            onClick = onRemove,
            backdrop = cardBackdrop,
            shape = CircleShape,
            tint = LiquidGlassSurfaceDark,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(30.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sf_xmark),
                contentDescription = "Удалить",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
        }

        // 4. Content Area at Bottom
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
                    if (item.season != null && item.episode != null) {
                        append("Сезон ${item.season} • Серия ${item.episode}")
                    } else if (item.episodeLabel != null) {
                        append(item.episodeLabel)
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
                            .background(SlooshAccent)
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
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.92f)
                    )

                    val remaining = formatRemaining(item.positionMs, item.durationMs)
                    if (remaining.isNotBlank()) {
                        Text(
                            text = remaining,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
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
