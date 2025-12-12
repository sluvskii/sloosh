package com.slooshfilm.app.ui.player.compose

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.slooshfilm.app.R
import com.slooshfilm.app.data.hdrezka.StreamInfo
import com.slooshfilm.app.data.hdrezka.Subtitle
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private val BrandYellowGreen = Color(0xFFD4E022)

@Composable
fun PlayerScreen(
    player: Player,
    streamInfo: StreamInfo,
    onQualitySelected: (String) -> Unit,
    onSubtitleSelected: (Subtitle?) -> Unit,
    onPipClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    hasNext: Boolean,
    hasPrevious: Boolean
) {
    var controlsVisible by remember { mutableStateOf(true) }

    val isPlaying by player.isPlayingAsState()
    val playbackState by player.playbackStateAsState()
    val currentPosition by player.currentPositionAsState()
    val duration by player.durationAsState()
    val title = player.mediaMetadata.title?.toString() ?: ""
    val subtitle = player.mediaMetadata.subtitle?.toString() ?: ""

    // Automatically hide controls after a delay when playing
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenWidth = maxWidth

        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false // We use our own Compose controls
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture detection for seeking and toggling controls
        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { offset ->
                        val third = screenWidth.toPx() / 3
                        when {
                            offset.x < third -> player.seekTo(player.currentPosition - 10000)
                            offset.x > third * 2 -> player.seekTo(player.currentPosition + 10000)
                            else -> controlsVisible = !controlsVisible // Double tap in center toggles controls too
                        }
                    }
                )
            }
        )

        // Custom Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerControls(
                modifier = Modifier.fillMaxSize(),
                title = title,
                subtitle = subtitle,
                isPlaying = isPlaying,
                playbackState = playbackState,
                onPlayPauseToggle = { if (player.isPlaying) player.pause() else player.play() },
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                currentPosition = currentPosition,
                duration = duration,
                onSeek = { newPosition -> player.seekTo(newPosition) },
                streamInfo = streamInfo,
                onQualitySelected = onQualitySelected,
                onSubtitleSelected = onSubtitleSelected,
                onSpeedSelected = { speed -> player.playbackParameters = androidx.media3.common.PlaybackParameters(speed) },
                onPipClick = onPipClick
            )
        }
    }
}

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    playbackState: Int,
    onPlayPauseToggle: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    streamInfo: StreamInfo,
    onQualitySelected: (String) -> Unit,
    onSubtitleSelected: (Subtitle?) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onPipClick: () -> Unit
) {
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSubtitlesDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var isSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentPosition) {
        if (!isSeeking) {
            sliderPosition = currentPosition.toFloat()
        }
    }

    if (showQualityDialog) {
        QualitySelectionDialog(streamInfo.streams, onQualitySelected) { showQualityDialog = false }
    }
    
    if (showSubtitlesDialog) {
        SubtitleSelectionDialog(streamInfo.subtitles, onSubtitleSelected) { showSubtitlesDialog = false }
    }

    if (showSpeedDialog) {
        SpeedSelectionDialog(onSpeedSelected) { showSpeedDialog = false }
    }

    Box(modifier = modifier.background(Color.Black.copy(alpha = 0.6f))) {

        // Top Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { (context as? Activity)?.finish() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                 Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row {
                IconButton(onClick = { showSpeedDialog = true }) {
                    Icon(painterResource(id = R.drawable.solar_playback_speed_linear), contentDescription = "Speed", tint = Color.White)
                }
                if (streamInfo.subtitles.isNotEmpty()) {
                    IconButton(onClick = { showSubtitlesDialog = true }) {
                        Icon(painterResource(id = R.drawable.solar_subtitles_linear), contentDescription = "Subtitles", tint = Color.White)
                    }
                }
                 IconButton(onClick = { showQualityDialog = true }) {
                    Icon(painterResource(id = R.drawable.solar_settings_linear), contentDescription = "Settings", tint = Color.White)
                }
                IconButton(onClick = onPipClick) {
                    Icon(painterResource(id = R.drawable.solar_to_pip_linear), contentDescription = "Picture-in-Picture", tint = Color.White)
                }
            }
        }

        // Center Controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(80.dp)
        ) {
            // Previous Button
            AnimatedVisibility(visible = hasPrevious, enter = fadeIn(), exit = fadeOut()) {
                 IconButton(onClick = onPreviousClick, modifier = Modifier.size(36.dp)) {
                    Icon(painterResource(id = R.drawable.solar_rewind_back_bold), contentDescription = "Previous", tint = Color.White, modifier = Modifier.fillMaxSize())
                }
            }

            // Play/Pause/Buffering
            Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                if (playbackState == Player.STATE_BUFFERING) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                } else {
                    IconButton(onClick = onPlayPauseToggle, modifier = Modifier.fillMaxSize()) {
                        val icon = if (isPlaying) R.drawable.solar_pause_bold else R.drawable.solar_play_bold
                        Icon(painterResource(id = icon), contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.fillMaxSize(0.8f))
                    }
                }
            }
            
            // Next Button
            AnimatedVisibility(visible = hasNext, enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = onNextClick, modifier = Modifier.size(36.dp)) {
                    Icon(painterResource(id = R.drawable.solar_rewind_forward_bold), contentDescription = "Next", tint = Color.White, modifier = Modifier.fillMaxSize())
                }
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(sliderPosition.toLong()), color = Color.White, fontSize = 14.sp)
                Text(formatDuration(duration), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
            Slider(
                value = sliderPosition,
                onValueChange = { 
                    isSeeking = true
                    sliderPosition = it 
                },
                onValueChangeFinished = {
                    onSeek(sliderPosition.toLong())
                    isSeeking = false
                },
                valueRange = 0f..(duration.takeIf { it > 0 }?.toFloat() ?: 0f),
                colors = SliderDefaults.colors(
                    thumbColor = BrandYellowGreen,
                    activeTrackColor = BrandYellowGreen,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun QualitySelectionDialog(streams: Map<String, String>, onQualitySelected: (String) -> Unit, onDismiss: () -> Unit) {
    val sortedQualities = streams.keys.sortedByDescending { quality ->
        when {
            quality.contains("4K", ignoreCase = true) -> 2160
            quality.contains("2K", ignoreCase = true) -> 1440
            quality.contains("1080p Ultra", ignoreCase = true) -> 1081
            else -> quality.filter { it.isDigit() }.toIntOrNull() ?: 0
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2F2F2F),
        title = { Text("Качество", color = Color.White) },
        text = {
            Column {
                sortedQualities.forEach { quality ->
                    Text(
                        text = quality,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                streams[quality]?.let(onQualitySelected)
                                onDismiss()
                            }
                            .padding(vertical = 14.dp)
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun SubtitleSelectionDialog(subtitles: List<Subtitle>, onSubtitleSelected: (Subtitle?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2F2F2F),
        title = { Text("Субтитры", color = Color.White) },
        text = {
            Column {
                Text(
                    text = "Отключить",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSubtitleSelected(null)
                            onDismiss()
                        }
                        .padding(vertical = 14.dp)
                )
                subtitles.forEach { subtitle ->
                    Text(
                        text = subtitle.name,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSubtitleSelected(subtitle)
                                onDismiss()
                            }
                            .padding(vertical = 14.dp)
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun SpeedSelectionDialog(onSpeedSelected: (Float) -> Unit, onDismiss: () -> Unit) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2F2F2F),
        title = { Text("Скорость", color = Color.White) },
        text = {
            Column {
                speeds.forEach { speed ->
                    Text(
                        text = "${speed}x",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSpeedSelected(speed)
                                onDismiss()
                            }
                            .padding(vertical = 14.dp)
                    )
                }
            }
        },
        confirmButton = {}
    )
}

// region Player State Composables

@Composable
private fun Player.isPlayingAsState(): State<Boolean> {
    val isPlaying = remember { mutableStateOf(this.isPlaying) }
    DisposableEffect(this) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying.value = playing
            }
        }
        addListener(listener)
        onDispose { removeListener(listener) }
    }
    return isPlaying
}

@Composable
private fun Player.playbackStateAsState(): State<Int> {
    val playbackState = remember { mutableStateOf(this.playbackState) }
    DisposableEffect(this) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState.value = state
            }
        }
        addListener(listener)
        onDispose { removeListener(listener) }
    }
    return playbackState
}

@Composable
private fun Player.currentPositionAsState(): State<Long> {
    val currentPosition = remember { mutableLongStateOf(this.currentPosition) }
    LaunchedEffect(this) {
        while (true) {
            if(this@currentPositionAsState.isPlaying) {
                 currentPosition.longValue = this@currentPositionAsState.currentPosition
            }
            delay(200)
        }
    }
    return currentPosition
}

@Composable
private fun Player.durationAsState(): State<Long> {
    val duration = remember { mutableLongStateOf(this.duration) }
    DisposableEffect(this) {
        val listener = object : Player.Listener {
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                if (!timeline.isEmpty) {
                    duration.longValue = timeline.getWindow(0, androidx.media3.common.Timeline.Window()).durationMs
                }
            }
        }
        addListener(listener)
        onDispose { removeListener(listener) }
    }
    return duration
}

private fun formatDuration(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

// endregion