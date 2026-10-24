package com.slooshfilm.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import com.slooshfilm.app.R
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.data.alloha.AllohaEpisode
import com.slooshfilm.app.data.alloha.AllohaSeason
import com.slooshfilm.app.data.alloha.AllohaTranslation
import com.slooshfilm.app.data.alloha.AllohaTranslationMatcher
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.components.LiquidGlassDialog
import com.slooshfilm.app.ui.components.LiquidMenuTokens
import com.slooshfilm.app.ui.theme.SlooshAccent

/**
 * Universal Liquid Glass Modal Dialog for video player settings and selection sheets.
 * Follows Apple TV+ glass aesthetic and sloosh design language.
 */
@Composable
fun LiquidGlassPlayerDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    showCloseButton: Boolean = true,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    LiquidGlassDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        backdrop = backdrop,
        showCloseButton = showCloseButton,
        content = content
    )
}

/**
 * Playback Speed Selection Dialog
 */
@Composable
fun PlayerSpeedGlassDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    val haptic = LocalHapticFeedback.current
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    LiquidGlassPlayerDialog(
        title = "Скорость воспроизведения",
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        backdrop = backdrop
    ) { dismiss ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            speeds.forEach { speed ->
                val isSelected = currentSpeed == speed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LiquidMenuTokens.itemHeight)
                        .clip(LiquidMenuTokens.itemShape)
                        .background(if (isSelected) LiquidMenuTokens.itemHighlightColor else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectSpeed(speed)
                            dismiss()
                        }
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${speed}x",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.3).sp
                    )

                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_checkmark),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Voiceover Selection Dialog with iOS-level translation ranking and matching
 */
@Composable
fun PlayerVoiceoverGlassDialog(
    currentVoice: String?,
    availableTranslations: List<AllohaTranslation>,
    onSelectVoice: (AllohaTranslation) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    val haptic = LocalHapticFeedback.current
    // Sort translations by viewer priority (Dubbing -> Top Studios -> Professional -> Others)
    val sortedTranslations = remember(availableTranslations) {
        availableTranslations.sortedBy { AllohaTranslationMatcher.translationRank(it.name) }
    }

    LiquidGlassPlayerDialog(
        title = "Выбор озвучки",
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        backdrop = backdrop
    ) { dismiss ->
        LazyColumn(
            modifier = Modifier.heightIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(sortedTranslations) { trans ->
                val isSelected = remember(trans.name, currentVoice) {
                    AllohaTranslationMatcher.matches(trans.name, currentVoice)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = LiquidMenuTokens.itemHeight)
                        .clip(LiquidMenuTokens.itemShape)
                        .background(if (isSelected) LiquidMenuTokens.itemHighlightColor else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectVoice(trans)
                            dismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trans.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.2).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )

                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_checkmark),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Video Quality Selection Dialog
 */
@Composable
fun PlayerQualityGlassDialog(
    currentQuality: String,
    qualityMap: Map<String, String>,
    onSelectQuality: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    val haptic = LocalHapticFeedback.current
    val available = remember(qualityMap) {
        val list = mutableListOf("Авто")
        val sortedKeys = qualityMap.keys.filter { it != "Авто" }.sortedWith { a, b ->
            val numA = a.replace("p", "").replace("K", "000").toIntOrNull() ?: 0
            val numB = b.replace("p", "").replace("K", "000").toIntOrNull() ?: 0
            numB.compareTo(numA) // Descending
        }
        if (sortedKeys.isNotEmpty()) {
            list.addAll(sortedKeys)
        } else {
            list.addAll(listOf("1080p", "720p", "480p", "360p"))
        }
        list
    }

    LiquidGlassPlayerDialog(
        title = "Качество видео",
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        backdrop = backdrop
    ) { dismiss ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            available.forEach { quality ->
                val isSelected = currentQuality == quality
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LiquidMenuTokens.itemHeight)
                        .clip(LiquidMenuTokens.itemShape)
                        .background(if (isSelected) LiquidMenuTokens.itemHighlightColor else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectQuality(quality)
                            dismiss()
                        }
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quality,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.3).sp
                    )

                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_checkmark),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Season & Episode Selection Dialog for TV Series
 */
@Composable
fun PlayerEpisodesGlassDialog(
    currentSeason: Int?,
    currentEpisode: Int?,
    seasons: List<AllohaSeason>,
    onSelectEpisode: (season: Int, episode: Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    val haptic = LocalHapticFeedback.current
    var selectedSeasonTab by remember { mutableIntStateOf(currentSeason ?: seasons.firstOrNull()?.season ?: 1) }

    val activeEpisodes = remember(seasons, selectedSeasonTab) {
        seasons.firstOrNull { it.season == selectedSeasonTab }?.episodes ?: emptyList()
    }

    LiquidGlassPlayerDialog(
        title = "Выбор серии",
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        backdrop = backdrop
    ) { dismiss ->
        // Season Tabs
        if (seasons.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(seasons) { s ->
                    val isSelected = s.season == selectedSeasonTab
                    Box(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(if (isSelected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedSeasonTab = s.season
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Сезон ${s.season}",
                            color = Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Episode List
        LazyColumn(
            modifier = Modifier.heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(activeEpisodes) { ep ->
                val isCurrent = (currentSeason ?: 1) == selectedSeasonTab && (currentEpisode ?: 1) == ep.episode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LiquidMenuTokens.itemHeight)
                        .clip(LiquidMenuTokens.itemShape)
                        .background(if (isCurrent) LiquidMenuTokens.itemHighlightColor else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectEpisode(selectedSeasonTab, ep.episode)
                            dismiss()
                        }
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Серия ${ep.episode}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.2).sp
                    )

                    if (isCurrent) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_play_fill),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
