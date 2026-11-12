package com.slooshfilm.app.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.slooshfilm.app.data.alloha.LanguageFlag
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.ui.components.InteractiveHighlight
import com.slooshfilm.app.ui.components.LiquidGlassFloatingButton
import com.slooshfilm.app.ui.components.LiquidGlassMultiButton
import com.slooshfilm.app.ui.components.LiquidGlassMenuOverlay
import com.slooshfilm.app.ui.components.LiquidGlassTokens
import com.slooshfilm.app.ui.components.LiquidMenuState
import com.slooshfilm.app.ui.components.LocalBackdrop
import com.slooshfilm.app.ui.components.liquidGlassInteractive
import com.slooshfilm.app.ui.components.liquidGlassSurface
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.slooshfilm.app.R
import com.slooshfilm.app.data.alloha.*
import com.slooshfilm.app.data.repository.MoviesRepository
import java.util.Locale
import com.slooshfilm.app.data.storage.LocalStorage
import com.slooshfilm.app.data.storage.PlaybackProgressStore
import com.slooshfilm.app.data.storage.WatchProgressItem
import com.slooshfilm.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    mediaId: String,
    mediaType: String,
    title: String = "",
    posterUrl: String? = null,
    backdropUrl: String? = null,
    initialSeason: Int? = null,
    initialEpisode: Int? = null,
    initialVoice: String? = null,
    initialTranslationId: String? = null,
    /** Конкретный iframe, выбранный в шторке «Смотреть» — исключает матчинг по имени. */
    initialIframeUrl: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val progressStore = remember { PlaybackProgressStore.getInstance(context) }
    val localStorage = remember { LocalStorage(context) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val scope = rememberCoroutineScope()

    var controlsVisible by remember { mutableStateOf(true) }
    var isInterfaceLocked by remember { mutableStateOf(false) }
    var showLockOverlay by remember { mutableStateOf(false) }
    var streamUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingStream by remember { mutableStateOf(true) }
    var streamErrorMessage by remember { mutableStateOf<String?>(null) }
    var isRecoveringStream by remember { mutableStateOf(false) }
    var silentRetryCount by remember { mutableIntStateOf(0) }
    var activeIframeUrl by remember { mutableStateOf<String?>(null) }

    var currentSeason by remember { mutableStateOf(initialSeason) }
    var currentEpisode by remember { mutableStateOf(initialEpisode) }
    var currentVoice by remember {
        mutableStateOf(
            initialVoice?.takeIf { it.isNotBlank() }
                ?: localStorage.getLastVoice(mediaId)
                ?: AllohaSessionHolder.currentTranslation.takeIf { it.isNotBlank() }
        )
    }
    // id/iframe, выбранные в шторке «Смотреть»: применяем один раз, при первом резолве.
    var currentTranslationId by remember { mutableStateOf(initialTranslationId) }
    var pendingVoice by remember { mutableStateOf<String?>(null) }
    var pendingTranslation by remember { mutableStateOf<AllohaTranslation?>(null) }
    var preparedVoice by remember { mutableStateOf(currentVoice) }
    var requiresNativeVoiceTrack by remember { mutableStateOf(false) }
    var initialIframeConsumed by remember { mutableStateOf(false) }
    // Резервные URL выбранного потока: при ошибке идём к следующему, а не долбим тот же.
    var playableCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var candidateIndex by remember { mutableIntStateOf(0) }
    var qualityPreferenceApplied by remember { mutableStateOf(false) }
    // Явный запрос перезагрузки потока (смена озвучки в плеере). Не сбрасывается
    // при очистке pending*-значений, поэтому коммит выбора не вызывает повторный резолв.
    var reloadToken by remember { mutableIntStateOf(0) }
    val requestedVoice = pendingTranslation?.name ?: pendingVoice ?: currentVoice
    var resolvedAudioVariants by remember { mutableStateOf(AllohaSessionHolder.audioVariants) }
    val latestRequestedVoice by rememberUpdatedState(requestedVoice)

    LaunchedEffect(mediaId) {
        pendingTranslation = null
        pendingVoice = null
    }

    // Заголовок/постер для карточки «Продолжить просмотр» берём из сохранённой записи тайтла.
    val savedProgress = remember(mediaId) { progressStore.getProgress(mediaId) }

    var activeTitle by remember { mutableStateOf(title.ifBlank { savedProgress?.title ?: "" }) }
    var activePosterUrl by remember { mutableStateOf(posterUrl ?: savedProgress?.posterUrl) }
    var activeBackdropUrl by remember { mutableStateOf(backdropUrl ?: savedProgress?.backdropUrl) }
    var activeLogoUrl by remember { mutableStateOf(AllohaSessionHolder.activeLogoUrl) }
    var origTitle by remember { mutableStateOf<String?>(null) }
    var mediaYear by remember { mutableStateOf<Int?>(null) }

    // Stream Session Resolution states
    var sessionReloadKey by remember { mutableIntStateOf(0) }
    var preservePositionMs by remember { mutableLongStateOf(0L) }
    var activeSubtitleConfigs by remember { mutableStateOf<List<MediaItem.SubtitleConfiguration>>(emptyList()) }

    // Cached Alloha Data
    var allohaData by remember { mutableStateOf(AllohaSessionHolder.cachedApiResult) }

    // Available translations for the active video/episode (combining API translations with parsed audioVariants)
    val availableTranslations = remember(allohaData, currentSeason, currentEpisode, resolvedAudioVariants, activeIframeUrl) {
        val apiTranslations = if (allohaData?.isSerial == true) {
            val s = allohaData?.seasons?.firstOrNull { it.season == (currentSeason ?: 1) } ?: allohaData?.seasons?.firstOrNull()
            val ep = s?.episodes?.firstOrNull { it.episode == (currentEpisode ?: 1) } ?: s?.episodes?.firstOrNull()
            ep?.translations ?: emptyList()
        } else {
            allohaData?.movie?.translations ?: emptyList()
        }

        val list = if (apiTranslations.size > 1) {
            apiTranslations
        } else if (resolvedAudioVariants.isNotEmpty()) {
            val mapped = resolvedAudioVariants.mapNotNull { v ->
                val t = (v["title"] as? String)?.trim() ?: return@mapNotNull null
                if (t.isBlank() || t.contains("субтитр", ignoreCase = true) || t.contains("subtitle", ignoreCase = true)) return@mapNotNull null
                val id = (v["id"] as? String) ?: t
                AllohaTranslation(
                    id = id,
                    name = t,
                    iframeUrl = activeIframeUrl ?: ""
                )
            }
            if (mapped.isNotEmpty()) mapped else apiTranslations
        } else {
            apiTranslations
        }
        list.asSequence()
            .filter { it.name.isNotBlank() }
            .filterNot { it.name.contains("субтитр", ignoreCase = true) || it.name.contains("subtitle", ignoreCase = true) }
            .distinctBy { "${it.id}|${it.iframeUrl}" }
            .sortedBy { AllohaTranslationMatcher.translationRank(it.name) }
            .toList()
    }
    val latestAvailableTranslations by rememberUpdatedState(availableTranslations)

    // Gestures state
    var isHudVisible by remember { mutableStateOf(false) }
    // Ссылки на сами state-холдеры нужны, чтобы HUD читал их в своей изолированной
    // области композиции (PlayerGestureHud), а не в корне плеера.
    val hudTypeState = remember { mutableStateOf(GestureHudType.VOLUME) }
    var hudType by hudTypeState
    val hudProgressState = remember { mutableFloatStateOf(0.5f) }
    var hudProgress by hudProgressState
    val hudVisibilityAnim = remember { Animatable(0f) }
    LaunchedEffect(isHudVisible) {
        if (isHudVisible) {
            hudVisibilityAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            hudVisibilityAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 320,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }
    var isScrubbingTimeline by remember { mutableStateOf(false) }
    var seekRippleText by remember { mutableStateOf<String?>(null) }
    var isZoomedToFill by remember { mutableStateOf(false) }
    var videoSize by remember { mutableStateOf(VideoSize.UNKNOWN) }
    val videoScale = remember { Animatable(1.0f) }
    val videoAspect = remember(videoSize) {
        val w = videoSize.width
        val h = videoSize.height
        if (w > 0 && h > 0) w.toFloat() / h.toFloat() else 16f / 9f
    }

    val haptic = LocalHapticFeedback.current
    val playerMenuState = remember { LiquidMenuState() }
    var multiCapsuleBounds by remember { mutableStateOf(Rect.Zero) }
    var voiceCapsuleBounds by remember { mutableStateOf(Rect.Zero) }
    var episodePillBounds by remember { mutableStateOf(Rect.Zero) }
    // Прозрачность капсул-триггеров при открытом меню.
    // Animatable + snapshotFlow вместо animateFloatAsState: состояние меню читается вне
    // композиции, поэтому открытие меню не пересобирает корень плеера в первом кадре морфа
    // (альфа капсул применяется в graphicsLayer, то есть на GPU).
    val multiCapsuleAlpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        snapshotFlow { playerMenuState.isVisible }.collectLatest { visible ->
            multiCapsuleAlpha.animateTo(if (visible) 0f else 1f, tween(150))
        }
    }

    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentQualityKey by remember { mutableStateOf(AllohaSessionHolder.currentQuality) }

    val isTvSeries = remember(mediaType, allohaData) {
        mediaType.lowercase().contains("tv") ||
        mediaType.lowercase().contains("serial") ||
        (allohaData?.isSerial == true)
    }

    // Dynamic HTTP Data Source Factory with automatic token recovery.
    // tokenRefreshState[0] = timestamp последнего резолва: когда CDN отклоняет даже свежеподписанный
    // URL, немедленный повторный резолв возвращает ту же устаревшую подпись (edge_hash обновляется
    // по WebSocket с задержкой). Кулдаун ~2с между последовательными recovery даёт сессии время
    // обновить креды; выполняется на loading-потоке ExoPlayer под tokenRecoveryLock — UI не блокируется.
    val tokenRefreshState = remember { longArrayOf(0L) }
    val httpDataSourceFactory = remember {
        DynamicHttpDataSourceFactory(
            headerProvider = {
                AllohaSessionHolder.activeHeaders
            },
            iframeUrlProvider = { activeIframeUrl },
            userAgentProvider = {
                AllohaSessionHolder.activeUserAgent
            },
            tokenRefresher = { iframe ->
                if (iframe.isBlank()) null
                else {
                    try {
                        val startedAt = System.currentTimeMillis()
                        val sinceLast = startedAt - tokenRefreshState[0]
                        if (sinceLast in 0 until 2_000L) {
                            return@DynamicHttpDataSourceFactory null
                        }
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            return@DynamicHttpDataSourceFactory null
                        }
                        tokenRefreshState[0] = System.currentTimeMillis()
                        android.util.Log.d("PlayerScreen", "DynamicHttpDataSource requested token refresh for $iframe")
                        AllohaRuntimeResolver.invalidateCache(iframe)
                        val resolver = AllohaRuntimeResolver(context)
                        AllohaSessionHolder.activeUserAgent = resolver.activeUserAgent
                        kotlinx.coroutines.runBlocking {
                            withTimeoutOrNull(8_000L) {
                                val resolved = resolver.resolve(iframe)
                                @Suppress("UNCHECKED_CAST")
                                val hdrs = (resolved["headers"] as? Map<String, String>) ?: emptyMap()
                                if (hdrs.isNotEmpty()) AllohaSessionHolder.activeHeaders = hdrs

                                @Suppress("UNCHECKED_CAST")
                                val av = (resolved["audioVariants"] as? List<Map<String, Any>>) ?: emptyList()
                                @Suppress("UNCHECKED_CAST")
                                val qv = (resolved["qualityVariants"] as? List<Map<String, Any>>) ?: emptyList()
                                val isDedicated = iframe.isNotBlank() || latestAvailableTranslations.mapNotNull { it.iframeUrl.takeIf(String::isNotBlank) }.distinct().size > 1
                                val enrichedAv = enrichAudioVariants(av, latestAvailableTranslations, isDedicatedIframe = isDedicated)
                                if (enrichedAv.isNotEmpty()) AllohaSessionHolder.audioVariants = enrichedAv
                                if (qv.isNotEmpty()) AllohaSessionHolder.qualityVariants = qv

                                val matchingAudio = AllohaTranslationMatcher.findMatchingAudioVariant(
                                    enrichedAv,
                                    latestRequestedVoice,
                                    isDedicatedIframe = true
                                )
                                val targetAudio = matchingAudio ?: if (latestRequestedVoice.isNullOrBlank()) enrichedAv.firstOrNull() else null
                                val audioQualityVariants = (targetAudio?.get("qualityVariants") as? List<*>)?.mapNotNull { it as? Map<*, *> }
                                    ?: qv.mapNotNull { it as? Map<*, *> }

                                val qMap = linkedMapOf<String, String>()
                                for (variant in audioQualityVariants) {
                                    val label = variant["label"] as? String ?: continue
                                    val u = variant["url"] as? String ?: continue
                                    qMap[label] = u
                                }
                                val fallback = (targetAudio?.get("url") as? String) ?: (resolved["url"] as? String).orEmpty()
                                val freshUrl = selectBestPlayableUrl(qMap, currentQualityKey, fallback)
                                freshUrl.ifBlank { fallback }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PlayerScreen", "tokenRefresher failed: ${e.message}")
                        null
                    }
                }
            },
            // Сессионные cookie из WebView: без них часть CDN отдаёт 403 на сегменты.
            cookieProvider = { url ->
                runCatching { android.webkit.CookieManager.getInstance().getCookie(url) }.getOrNull()
            }
        )
    }

    // Retry policy: fail fast on 401/403 so self-healing takes over immediately rather than stalling for 50s
    val loadErrorHandlingPolicy = remember {
        object : DefaultLoadErrorHandlingPolicy(1) {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                val exception = loadErrorInfo.exception
                if (exception is HttpDataSource.InvalidResponseCodeException) {
                    if (exception.responseCode == 403 || exception.responseCode == 401) {
                        return C.TIME_UNSET
                    }
                }
                return 400L
            }

            override fun getMinimumLoadableRetryCount(dataType: Int): Int = 1
        }
    }

    val seekForwardSec = remember { localStorage.getSeekForwardInterval() }
    val seekBackwardSec = remember { localStorage.getSeekBackwardInterval() }
    val seekForwardMs = remember(seekForwardSec) { seekForwardSec * 1000L }
    val seekBackwardMs = remember(seekBackwardSec) { seekBackwardSec * 1000L }
    val volumeBrightnessGesturesEnabled = remember { localStorage.getPlayerVolumeBrightnessGestures() }

    // Initialize ExoPlayer with decoder fallback, TS extractors & custom retry policy
    val exoPlayer = remember {
        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
            .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 10_000,
                /* retainBackBufferFromKeyframe = */ true
            )
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setPreferredAudioLanguages("ru", "rus", "ru-RU")
                .build()
        }

        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(httpDataSourceFactory, extractorsFactory)
                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
            )
            .setSeekForwardIncrementMs(seekForwardMs)
            .setSeekBackIncrementMs(seekBackwardMs)
            .build()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val saveCurrentProgress by rememberUpdatedState(newValue = {
        val positionMs = exoPlayer.currentPosition
        val durationMs = exoPlayer.duration
        if (positionMs > 5_000L && durationMs > 0L) {
            progressStore.saveProgress(
                WatchProgressItem(
                    mediaId = mediaId,
                    title = activeTitle,
                    posterUrl = activePosterUrl,
                    backdropUrl = activeBackdropUrl,
                    type = mediaType,
                    season = currentSeason,
                    episode = currentEpisode,
                    voice = currentVoice,
                    positionMs = positionMs,
                    durationMs = durationMs
                )
            )
        }
    })
    val pendingVoiceForPlayback by rememberUpdatedState(pendingVoice)
    val preparedVoiceForPlayback by rememberUpdatedState(preparedVoice)
    val requiresNativeVoiceTrackForPlayback by rememberUpdatedState(requiresNativeVoiceTrack)
    val commitVoiceSelection by rememberUpdatedState(newValue = { voice: String ->
        currentVoice = voice
        pendingTranslation?.let { currentTranslationId = it.id }
        AllohaSessionHolder.currentTranslation = voice
        // 1:1 с iOS: запоминаем озвучку для тайтла и глобально.
        localStorage.saveLastVoice(mediaId, voice)
        pendingVoice = null
        pendingTranslation = null
        progressStore.saveProgress(
            WatchProgressItem(
                mediaId = mediaId,
                title = activeTitle,
                posterUrl = activePosterUrl,
                backdropUrl = activeBackdropUrl,
                type = mediaType,
                season = currentSeason,
                episode = currentEpisode,
                voice = voice,
                positionMs = exoPlayer.currentPosition,
                durationMs = exoPlayer.duration
            )
        )
    })

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> saveCurrentProgress()
                Lifecycle.Event.ON_RESUME -> {
                    if (exoPlayer.playbackState == Player.STATE_IDLE && streamErrorMessage != null) {
                        streamErrorMessage = null
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val mainHandler = Handler(Looper.getMainLooper())
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                mainHandler.post {
                    if (exoPlayer.playbackState == Player.STATE_IDLE && streamErrorMessage != null) {
                        android.util.Log.d("PlayerScreen", "Network re-established, auto-recovering playback...")
                        streamErrorMessage = null
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                }
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { connectivityManager?.registerNetworkCallback(request, networkCallback) }
        onDispose {
            runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
        }
    }

    // Playback state tracking.
    // ВАЖНО: isPlaying/playbackState намеренно НЕ читаются в корне через `by` — иначе
    // каждый play/pause и каждая буферизация пересобирали бы весь экран (сотни узлов
    // и все стеклянные поверхности). Их читают только те, кому они нужны: кнопка
    // play/pause, seekbar и корутины через snapshotFlow.
    val isPlayingState = exoPlayer.isPlayingAsState()
    val duration by exoPlayer.durationAsState()

    // iOS пишет прогресс каждые 5 секунд — иначе при убийстве процесса таймкод теряется.
    LaunchedEffect(mediaId, currentSeason, currentEpisode) {
        snapshotFlow { isPlayingState.value }.collectLatest { playing ->
            if (!playing) return@collectLatest
            while (currentCoroutineContext().isActive) {
                delay(5_000L)
                saveCurrentProgress()
            }
        }
    }

    // Next / Previous Episode calculations
    val hasNextEpisode = remember(allohaData, currentSeason, currentEpisode) {
        if (allohaData?.isSerial != true) false
        else {
            val curS = currentSeason ?: 1
            val curEp = currentEpisode ?: 1
            val seasonObj = allohaData?.seasons?.firstOrNull { it.season == curS }
            val nextInSeason = seasonObj?.episodes?.any { it.episode > curEp } == true
            val nextSeasonExists = allohaData?.seasons?.any { it.season > curS } == true
            nextInSeason || nextSeasonExists
        }
    }

    val hasPreviousEpisode = remember(allohaData, currentSeason, currentEpisode) {
        if (allohaData?.isSerial != true) false
        else {
            val curS = currentSeason ?: 1
            val curEp = currentEpisode ?: 1
            val seasonObj = allohaData?.seasons?.firstOrNull { it.season == curS }
            val prevInSeason = seasonObj?.episodes?.any { it.episode < curEp } == true
            val prevSeasonExists = allohaData?.seasons?.any { it.season < curS } == true
            prevInSeason || prevSeasonExists
        }
    }

    fun preferredTranslationForEpisode(translations: List<AllohaTranslation>): AllohaTranslation? =
        translations.firstOrNull { it.id == currentTranslationId }
            ?: AllohaTranslationMatcher.bestTranslation(translations, requestedVoice)

    fun playNextEpisode() {
        val curS = currentSeason ?: 1
        val curEp = currentEpisode ?: 1
        val seasonObj = allohaData?.seasons?.firstOrNull { it.season == curS }
        val nextEpInSeason = seasonObj?.episodes?.filter { it.episode > curEp }?.minByOrNull { it.episode }
        val (nextS, nextEp) = if (nextEpInSeason != null) {
            curS to nextEpInSeason.episode
        } else {
            val nextSeasonObj = allohaData?.seasons?.filter { it.season > curS }?.minByOrNull { it.season }
            if (nextSeasonObj != null) {
                nextSeasonObj.season to (nextSeasonObj.episodes.minByOrNull { it.episode }?.episode ?: 1)
            } else return
        }

        val curPos = exoPlayer.currentPosition
        val curDur = exoPlayer.duration
        if (curPos > 5000L && curDur > 0L) {
            progressStore.saveProgress(
                WatchProgressItem(
                    mediaId = mediaId,
                    title = activeTitle,
                    posterUrl = activePosterUrl,
                    backdropUrl = activeBackdropUrl,
                    type = mediaType,
                    season = currentSeason,
                    episode = currentEpisode,
                    voice = currentVoice,
                    positionMs = curPos,
                    durationMs = curDur
                )
            )
        }

        // Match translation in the new episode preserving user's preference
        val epTranslations = allohaData?.seasons
            ?.firstOrNull { it.season == nextS }
            ?.episodes?.firstOrNull { it.episode == nextEp }
            ?.translations ?: emptyList()
        val bestTrans = preferredTranslationForEpisode(epTranslations)
        if (bestTrans != null) {
            pendingVoice = bestTrans.name
            pendingTranslation = bestTrans
        }

        preservePositionMs = 0L
        silentRetryCount = 0
        currentSeason = nextS
        currentEpisode = nextEp
    }

    fun playPreviousEpisode() {
        val curS = currentSeason ?: 1
        val curEp = currentEpisode ?: 1
        val seasonObj = allohaData?.seasons?.firstOrNull { it.season == curS }
        val prevEpInSeason = seasonObj?.episodes?.filter { it.episode < curEp }?.maxByOrNull { it.episode }
        val (prevS, prevEp) = if (prevEpInSeason != null) {
            curS to prevEpInSeason.episode
        } else {
            val prevSeasonObj = allohaData?.seasons?.filter { it.season < curS }?.maxByOrNull { it.season }
            if (prevSeasonObj != null) {
                prevSeasonObj.season to (prevSeasonObj.episodes.maxByOrNull { it.episode }?.episode ?: 1)
            } else return
        }

        val curPos = exoPlayer.currentPosition
        val curDur = exoPlayer.duration
        if (curPos > 5000L && curDur > 0L) {
            progressStore.saveProgress(
                WatchProgressItem(
                    mediaId = mediaId,
                    title = activeTitle,
                    posterUrl = activePosterUrl,
                    backdropUrl = activeBackdropUrl,
                    type = mediaType,
                    season = currentSeason,
                    episode = currentEpisode,
                    voice = currentVoice,
                    positionMs = curPos,
                    durationMs = curDur
                )
            )
        }

        val epTranslations = allohaData?.seasons
            ?.firstOrNull { it.season == prevS }
            ?.episodes?.firstOrNull { it.episode == prevEp }
            ?.translations ?: emptyList()
        val bestTrans = preferredTranslationForEpisode(epTranslations)
        if (bestTrans != null) {
            pendingVoice = bestTrans.name
            pendingTranslation = bestTrans
        }

        preservePositionMs = 0L
        silentRetryCount = 0
        currentSeason = prevS
        currentEpisode = prevEp
    }

    fun selectEpisode(newS: Int, newEp: Int) {
        if (newS != currentSeason || newEp != currentEpisode) {
            val curPos = exoPlayer.currentPosition
            val curDur = exoPlayer.duration
            if (curPos > 5000L && curDur > 0L) {
                progressStore.saveProgress(
                    WatchProgressItem(
                        mediaId = mediaId,
                        title = activeTitle,
                        posterUrl = activePosterUrl,
                        backdropUrl = activeBackdropUrl,
                        type = mediaType,
                        season = currentSeason,
                        episode = currentEpisode,
                        voice = currentVoice,
                        positionMs = curPos,
                        durationMs = curDur
                    )
                )
            }

            val epTranslations = allohaData?.seasons
                ?.firstOrNull { it.season == newS }
                ?.episodes?.firstOrNull { it.episode == newEp }
                ?.translations ?: emptyList()
            val bestTrans = preferredTranslationForEpisode(epTranslations)
            if (bestTrans != null) {
                pendingVoice = bestTrans.name
                pendingTranslation = bestTrans
            }

            preservePositionMs = 0L
            silentRetryCount = 0
            currentSeason = newS
            currentEpisode = newEp
        }
    }

    // Attach ExoPlayer Listener for self-healing error recovery & auto-next episode
    DisposableEffect(exoPlayer, hasNextEpisode, activeIframeUrl) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (hasNextEpisode && localStorage.getAutoNextEnabled()) {
                        playNextEpisode()
                    }
                } else if (playbackState == Player.STATE_READY) {
                    isRecoveringStream = false
                    val targetVoice = pendingVoiceForPlayback ?: preparedVoiceForPlayback ?: currentVoice
                    if (!targetVoice.isNullOrBlank()) {
                        val authenticVoiceNames = AllohaSessionHolder.availableVoiceNames
                        val explicitIdx = authenticVoiceNames.indexOfFirst {
                            AllohaTranslationMatcher.matches(it, targetVoice, exactOnly = true)
                        }.takeIf { it >= 0 } ?: authenticVoiceNames.indexOfFirst {
                            AllohaTranslationMatcher.matches(it, targetVoice, exactOnly = false)
                        }.takeIf { it >= 0 }

                        val audioGroups = exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
                        val audioTrackCount = audioGroups.sumOf { it.length }

                        if (audioTrackCount > 1) {
                            when (exoPlayer.selectAudioTrackForVoice(targetVoice, authenticVoiceNames, explicitIdx)) {
                                true -> commitVoiceSelection(targetVoice)
                                false -> Unit // Override применится, onTracksChanged подтвердит выбор.
                                null -> commitVoiceSelection(targetVoice)
                            }
                        } else {
                            commitVoiceSelection(targetVoice)
                        }
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val targetVoice = pendingVoiceForPlayback ?: preparedVoiceForPlayback ?: currentVoice
                if (!targetVoice.isNullOrBlank()) {
                    val authenticVoiceNames = AllohaSessionHolder.availableVoiceNames
                    val explicitIdx = authenticVoiceNames.indexOfFirst {
                        AllohaTranslationMatcher.matches(it, targetVoice, exactOnly = true)
                    }.takeIf { it >= 0 } ?: authenticVoiceNames.indexOfFirst {
                        AllohaTranslationMatcher.matches(it, targetVoice, exactOnly = false)
                    }.takeIf { it >= 0 }

                    if (exoPlayer.selectAudioTrackForVoice(targetVoice, authenticVoiceNames, explicitIdx) == true) {
                        commitVoiceSelection(targetVoice)
                    }
                }
            }

            override fun onVideoSizeChanged(newVideoSize: VideoSize) {
                videoSize = newVideoSize
            }

            override fun onPlayerError(error: PlaybackException) {
                val lastPos = exoPlayer.currentPosition
                android.util.Log.e("PlayerScreen", "ExoPlayer error (${error.errorCodeName}): ${error.message} at ${lastPos}ms. Initiating self-healing retry $silentRetryCount/3...")

                // Сначала переходим к следующему резервному URL этого же потока (другое качество
                // или master-playlist), не дёргая тяжёлый повторный резолв iframe.
                val nextCandidate = playableCandidates.getOrNull(candidateIndex + 1)
                if (nextCandidate != null && silentRetryCount < 3) {
                    silentRetryCount++
                    candidateIndex += 1
                    preservePositionMs = if (lastPos > 0) lastPos else preservePositionMs
                    isRecoveringStream = true
                    AllohaSessionHolder.registerStreamUrlUpdate(streamUrl, nextCandidate)
                    streamUrl = nextCandidate
                    exoPlayer.setMediaItem(createPlayableMediaItem(nextCandidate, activeSubtitleConfigs), preservePositionMs)
                    exoPlayer.prepare()
                    exoPlayer.play()
                    return
                }

                if (silentRetryCount < 3) {
                    silentRetryCount++
                    preservePositionMs = if (lastPos > 0) lastPos else preservePositionMs
                    isRecoveringStream = true

                    scope.launch {
                        try {
                            val iframe = activeIframeUrl
                            if (!iframe.isNullOrBlank()) {
                                android.util.Log.d("PlayerScreen", "Self-healing: re-resolving stream tokens for $iframe at ${preservePositionMs}ms...")
                                AllohaRuntimeResolver.invalidateCache(iframe)
                                val resolver = AllohaRuntimeResolver(context)
                                AllohaSessionHolder.activeUserAgent = resolver.activeUserAgent
                                val resolved = resolver.resolve(iframe)
                                currentCoroutineContext().ensureActive()

                                @Suppress("UNCHECKED_CAST")
                                val hdrs = (resolved["headers"] as? Map<String, String>) ?: emptyMap()
                                @Suppress("UNCHECKED_CAST")
                                val av = (resolved["audioVariants"] as? List<Map<String, Any>>) ?: emptyList()
                                @Suppress("UNCHECKED_CAST")
                                val qv = (resolved["qualityVariants"] as? List<Map<String, Any>>) ?: emptyList()

                                AllohaSessionHolder.activeHeaders = hdrs
                                val isDedicated = !iframe.isNullOrBlank() || latestAvailableTranslations.mapNotNull { it.iframeUrl.takeIf(String::isNotBlank) }.distinct().size > 1
                                val enrichedAv = enrichAudioVariants(av, latestAvailableTranslations, isDedicatedIframe = isDedicated)
                                AllohaSessionHolder.audioVariants = enrichedAv
                                AllohaSessionHolder.qualityVariants = qv
                                resolvedAudioVariants = enrichedAv

                                // Find matching audio variant or pick first
                                val matchingAudio = AllohaTranslationMatcher.findMatchingAudioVariant(
                                    enrichedAv,
                                    latestRequestedVoice,
                                    isDedicatedIframe = true
                                )
                                val targetAudio = matchingAudio ?: if (latestRequestedVoice.isNullOrBlank()) enrichedAv.firstOrNull() else null

                                // Build quality map from the chosen audio variant (or global qualityVariants)
                                val audioQualityVariants = (targetAudio?.get("qualityVariants") as? List<*>)?.mapNotNull { it as? Map<*, *> }
                                    ?: qv.mapNotNull { it as? Map<*, *> }

                                val qMap = linkedMapOf<String, String>()
                                for (variant in audioQualityVariants) {
                                    val label = variant["label"] as? String ?: continue
                                    val u = variant["url"] as? String ?: continue
                                    qMap[label] = u
                                }
                                val fallback = (targetAudio?.get("url") as? String)
                                    ?: (resolved["url"] as? String).orEmpty()

                                // If a specific quality was selected and failed repeatedly, auto-fallback to Авто
                                val targetQuality = if (silentRetryCount >= 2 && currentQualityKey != "Авто") {
                                    "Авто"
                                } else {
                                    currentQualityKey
                                }
                                val freshUrl = selectBestPlayableUrl(qMap, targetQuality, fallback)
                                qMap["Авто"] = freshUrl
                                AllohaSessionHolder.qualityMap = qMap
                                playableCandidates = buildPlayableCandidates(qMap, targetQuality, fallback)
                                    .ifEmpty { listOf(freshUrl) }
                                candidateIndex = 0

                                if (freshUrl.isNotBlank()) {
                                    AllohaSessionHolder.registerStreamUrlUpdate(streamUrl, freshUrl)
                                    streamUrl = freshUrl
                                    AllohaSessionHolder.latestMasterUrl = freshUrl
                                    isLoadingStream = false

                                    if (exoPlayer.applicationLooper.thread.isAlive) {
                                        val resumePos = if (preservePositionMs > 0L) preservePositionMs else 0L
                                        preparedVoice = latestRequestedVoice
                                        exoPlayer.setMediaItem(createPlayableMediaItem(freshUrl, activeSubtitleConfigs), resumePos)
                                        exoPlayer.prepare()
                                        exoPlayer.play()
                                    }
                                    return@launch
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            android.util.Log.e("PlayerScreen", "Error during self-healing: ${e.message}")
                        }
                        sessionReloadKey++
                    }
                } else {
                    pendingVoice = null
                    pendingTranslation = null
                    streamErrorMessage = "Не удалось воспроизвести видеопоток. Пожалуйста, попробуйте другую озвучку."
                    isLoadingStream = false
                    isRecoveringStream = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { isPlayingState.value to isRecoveringStream }.collectLatest { (playing, recovering) ->
            if (!playing || recovering) return@collectLatest
            delay(30_000L)
            if (isPlayingState.value && !isRecoveringStream && exoPlayer.playerError == null) {
                silentRetryCount = 0
            }
        }
    }

    // Proactive background token refresh: Alloha/VKVideo tokens expire every ~400s (6.6 mins).
    // Refresh every 240s (4 mins) while playing so tokens never expire and segments never fail with 403.
    LaunchedEffect(activeIframeUrl) {
        val iframe = activeIframeUrl ?: return@LaunchedEffect
        while (isActive) {
            // Ждём начала воспроизведения, не читая isPlaying в композиции.
            snapshotFlow { isPlayingState.value }.first { it }
            delay(240_000L)
            if (!isActive || !isPlayingState.value) continue
            try {
                android.util.Log.d("PlayerScreen", "Proactive background token refresh starting at position ${exoPlayer.currentPosition}ms...")
                val resolver = AllohaRuntimeResolver(context)
                AllohaSessionHolder.activeUserAgent = resolver.activeUserAgent
                val resolved = resolver.resolve(iframe)
                currentCoroutineContext().ensureActive()

                @Suppress("UNCHECKED_CAST")
                val hdrs = (resolved["headers"] as? Map<String, String>) ?: emptyMap()
                if (hdrs.isNotEmpty()) AllohaSessionHolder.activeHeaders = hdrs

                @Suppress("UNCHECKED_CAST")
                val av = (resolved["audioVariants"] as? List<Map<String, Any>>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val qv = (resolved["qualityVariants"] as? List<Map<String, Any>>) ?: emptyList()
                val isDedicated = iframe.isNotBlank() || latestAvailableTranslations.mapNotNull { it.iframeUrl.takeIf(String::isNotBlank) }.distinct().size > 1
                val enrichedAv = enrichAudioVariants(av, latestAvailableTranslations, isDedicatedIframe = isDedicated)
                if (enrichedAv.isNotEmpty()) AllohaSessionHolder.audioVariants = enrichedAv
                if (qv.isNotEmpty()) AllohaSessionHolder.qualityVariants = qv
                resolvedAudioVariants = enrichedAv

                val matchingAudio = AllohaTranslationMatcher.findMatchingAudioVariant(
                    enrichedAv,
                    latestRequestedVoice,
                    isDedicatedIframe = true
                )
                val targetAudio = matchingAudio ?: if (latestRequestedVoice.isNullOrBlank()) enrichedAv.firstOrNull() else null
                val audioQualityVariants = (targetAudio?.get("qualityVariants") as? List<*>)?.mapNotNull { it as? Map<*, *> }
                    ?: qv.mapNotNull { it as? Map<*, *> }

                val qMap = linkedMapOf<String, String>()
                for (variant in audioQualityVariants) {
                    val label = variant["label"] as? String ?: continue
                    val u = variant["url"] as? String ?: continue
                    qMap[label] = u
                }
                val fallback = (targetAudio?.get("url") as? String) ?: (resolved["url"] as? String).orEmpty()
                val freshUrl = selectBestPlayableUrl(qMap, currentQualityKey, fallback)
                val freshMaster = (targetAudio?.get("masterUrl") as? String)
                    ?: (resolved["masterUrl"] as? String)
                    ?: (resolved["url"] as? String)?.takeIf { it.contains("master.m3u8", ignoreCase = true) }
                    ?: freshUrl

                if (freshMaster.isNotBlank()) {
                    val oldMaster = AllohaSessionHolder.latestMasterUrl ?: streamUrl
                    AllohaSessionHolder.registerStreamUrlUpdate(oldMaster, freshMaster)
                    AllohaSessionHolder.latestMasterUrl = freshMaster
                    android.util.Log.d("PlayerScreen", "Proactive background token refresh completed successfully. New base registered.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("PlayerScreen", "Proactive token refresh error (handled gracefully): ${e.message}")
            }
        }
    }

    // Auto-rotate, immersive edge-to-edge mode without camera notch black bar
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        val originalCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity?.window?.attributes?.layoutInDisplayCutoutMode
        } else null

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Allow drawing behind camera notch / cutout so there is NO black bar on the side
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity?.window?.let { win ->
                val params = win.attributes
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                win.attributes = params
            }
        }

        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && originalCutoutMode != null) {
                activity?.window?.let { win ->
                    val params = win.attributes
                    params.layoutInDisplayCutoutMode = originalCutoutMode
                    win.attributes = params
                }
            }

            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }

            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (pos > 5000L && dur > 0L) {
                progressStore.saveProgress(
                    WatchProgressItem(
                        mediaId = mediaId,
                        title = activeTitle,
                        posterUrl = activePosterUrl,
                        backdropUrl = activeBackdropUrl,
                        type = mediaType,
                        season = currentSeason,
                        episode = currentEpisode,
                        voice = currentVoice,
                        positionMs = pos,
                        durationMs = dur
                    )
                )
            }
            exoPlayer.release()
            AllohaSessionHolder.clear()
        }
    }

    // Ключи намеренно не содержат currentVoice: нативное переключение встроенной дорожки
    // не должно пересоздавать поток. Перезагрузку инициирует только reloadToken.
    LaunchedEffect(
        mediaId,
        currentSeason,
        currentEpisode,
        reloadToken,
        sessionReloadKey
    ) {
        isLoadingStream = true
        streamErrorMessage = null

        val moviesRepo = MoviesRepository.getInstance()

        // Fetch details if needed for title or logo
        if (activeTitle.isBlank() || activeLogoUrl.isNullOrBlank()) {
            try {
                val isTv = mediaType.lowercase().contains("tv") || mediaType.lowercase().contains("serial")
                val clean = mediaId.replace("tv_", "").replace("movie_", "")
                val fetchedDetails = moviesRepo.getMediaDetails(clean, isTv)
                if (fetchedDetails != null) {
                    if (activeTitle.isBlank()) activeTitle = fetchedDetails.title ?: fetchedDetails.originalTitle ?: ""
                    origTitle = fetchedDetails.originalTitle
                    mediaYear = fetchedDetails.year
                    if (activePosterUrl.isNullOrBlank()) activePosterUrl = fetchedDetails.getDisplayPosterUrl()
                    if (activeBackdropUrl.isNullOrBlank()) activeBackdropUrl = fetchedDetails.getDisplayBackdropUrl()
                    if (activeLogoUrl.isNullOrBlank()) {
                        activeLogoUrl = fetchedDetails.displayLogoUrl
                        AllohaSessionHolder.activeLogoUrl = fetchedDetails.displayLogoUrl
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }

        // Fetch or reuse AllohaApiResult
        var currentAlloha = allohaData
        if (currentAlloha == null || AllohaSessionHolder.activeMediaId != mediaId) {
            try {
                val allohaRepo = AllohaRepository.getInstance(context)
                val isExplicitKp = mediaId.startsWith("kp_")
                val explicitKp = if (isExplicitKp) mediaId.removePrefix("kp_").toIntOrNull() else null
                val explicitTmdb = if (!isExplicitKp) mediaId.replace("tv_", "").replace("movie_", "").toIntOrNull() else null
                currentAlloha = allohaRepo.fetchAllohaData(
                    mediaId = mediaId,
                    explicitKpId = explicitKp,
                    tmdbId = explicitTmdb,
                    title = activeTitle,
                    originalTitle = origTitle,
                    year = mediaYear,
                    isTv = mediaType == "tv" || mediaId.startsWith("tv_")
                )
                currentCoroutineContext().ensureActive()
                allohaData = currentAlloha
                AllohaSessionHolder.cachedApiResult = currentAlloha
                AllohaSessionHolder.activeMediaId = mediaId
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PlayerScreen", "Error loading playback options: ${e.message}", e)
                pendingVoice = null
                pendingTranslation = null
                streamErrorMessage = "Не удалось найти источник воспроизведения"
                isLoadingStream = false
                return@LaunchedEffect
            }
        }

        if (currentAlloha == null) {
            pendingVoice = null
            pendingTranslation = null
            streamErrorMessage = "Не удалось найти источник воспроизведения"
            isLoadingStream = false
            return@LaunchedEffect
        }

        // Сериал без явно переданного сезона/серии: фиксируем реально выбранные значения,
        // иначе прогресс сохраняется под (null, null) и возобновление ломается.
        if (currentAlloha.isSerial) {
            val seasonObj = currentAlloha.seasons.firstOrNull { it.season == (currentSeason ?: 1) }
                ?: currentAlloha.seasons.firstOrNull()
            val episodeObj = seasonObj?.episodes?.firstOrNull { it.episode == (currentEpisode ?: 1) }
                ?: seasonObj?.episodes?.firstOrNull()
            if (seasonObj != null && currentSeason == null) currentSeason = seasonObj.season
            if (episodeObj != null && currentEpisode == null) currentEpisode = episodeObj.episode
        }

        // Resolve the selected translation first so the iframe and the displayed voice stay in sync.
        val targetTranslations = if (currentAlloha.isSerial) {
            val season = currentAlloha.seasons.firstOrNull { it.season == (currentSeason ?: 1) }
                ?: currentAlloha.seasons.firstOrNull()
            val ep = season?.episodes?.firstOrNull { it.episode == (currentEpisode ?: 1) }
                ?: season?.episodes?.firstOrNull()
            ep?.translations.orEmpty()
        } else {
            currentAlloha.movie?.translations.orEmpty()
        }
        val targetTranslation = pendingTranslation?.takeIf { pending ->
            targetTranslations.any { it.id == pending.id && it.iframeUrl == pending.iframeUrl }
        } ?: targetTranslations.firstOrNull { pending ->
            // Явный iframe из шторки «Смотреть» — приоритетнее любого матчинга по имени.
            !initialIframeConsumed && !initialIframeUrl.isNullOrBlank() && pending.iframeUrl == initialIframeUrl
        } ?: targetTranslations.firstOrNull {
            !initialIframeConsumed && !currentTranslationId.isNullOrBlank() && it.id == currentTranslationId
        } ?: targetTranslations.firstOrNull {
            AllohaTranslationMatcher.matches(it.name, requestedVoice, exactOnly = true)
        } ?: targetTranslations.firstOrNull {
            AllohaTranslationMatcher.matches(it.name, requestedVoice)
        } ?: targetTranslations.firstOrNull()
        val targetIframe = targetTranslation?.iframeUrl?.takeIf { it.isNotBlank() }
            ?: currentAlloha.movie?.iframeUrl?.takeIf { !currentAlloha.isSerial && it.isNotBlank() }

        activeIframeUrl = targetIframe
        initialIframeConsumed = true

        if (targetIframe.isNullOrBlank()) {
            pendingVoice = null
            pendingTranslation = null
            streamErrorMessage = "Ссылка на видеопоток пуста"
            isLoadingStream = false
            return@LaunchedEffect
        }

        try {
            if (sessionReloadKey > 0) AllohaRuntimeResolver.invalidateCache(targetIframe)
            android.util.Log.d("PlayerScreen", "Resolving stream with AllohaRuntimeResolver for $targetIframe")
            val resolver = AllohaRuntimeResolver(context)
            AllohaSessionHolder.activeUserAgent = resolver.activeUserAgent
            val resolved = resolver.resolve(targetIframe)
            currentCoroutineContext().ensureActive()

            @Suppress("UNCHECKED_CAST")
            val av = (resolved["audioVariants"] as? List<Map<String, Any>>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val qv = (resolved["qualityVariants"] as? List<Map<String, Any>>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val hdrs = (resolved["headers"] as? Map<String, String>) ?: emptyMap()

            AllohaSessionHolder.activeHeaders = hdrs
            val hasDistinctIframes = latestAvailableTranslations.mapNotNull { it.iframeUrl.takeIf(String::isNotBlank) }.distinct().size > 1
            val isDedicatedIframe = !targetIframe.isNullOrBlank() || hasDistinctIframes
            val enrichedAv = enrichAudioVariants(av, latestAvailableTranslations, isDedicatedIframe = isDedicatedIframe)
            AllohaSessionHolder.audioVariants = enrichedAv
            AllohaSessionHolder.qualityVariants = qv
            resolvedAudioVariants = enrichedAv

            // Parse sidecar subtitles from resolved stream (selection flag 0: disabled by default)
            val parsedSubtitles = (resolved["subtitles"] as? List<*>)?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val u = map["url"] as? String ?: return@mapNotNull null
                val name = (map["name"] as? String)?.takeIf { it.isNotBlank() } ?: "Субтитры"
                val lang = (map["language"] as? String)?.takeIf { it.isNotBlank() } ?: "ru"
                val mime = if (u.contains(".vtt", ignoreCase = true)) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(u))
                    .setMimeType(mime)
                    .setLabel(name)
                    .setLanguage(lang)
                    .setSelectionFlags(0)
                    .build()
            }.orEmpty()
            activeSubtitleConfigs = parsedSubtitles

            // Extract skip intro / outro ranges from resolved stream
            val introMap = (resolved["introRange"] as? Map<*, *>)
            val outroMap = (resolved["outroRange"] as? Map<*, *>)
            val skips = mutableListOf<LongRange>()
            if (introMap != null) {
                val s = (introMap["start"] as? Number)?.toLong()
                val e = (introMap["end"] as? Number)?.toLong()
                if (s != null && e != null && e > s) skips.add(s..e)
            }
            if (outroMap != null) {
                val s = (outroMap["start"] as? Number)?.toLong()
                val e = (outroMap["end"] as? Number)?.toLong()
                if (s != null && e != null && e > s) skips.add(s..e)
            }
            AllohaSessionHolder.skipRanges = skips

            // Find matching audio variant or pick first
            val matchingAudio = AllohaTranslationMatcher.findMatchingAudioVariant(
                enrichedAv,
                requestedVoice,
                isDedicatedIframe = true
            )
            val targetAudio = matchingAudio ?: if (requestedVoice.isNullOrBlank()) enrichedAv.firstOrNull() else null

            // Build quality map from the chosen audio variant (or global qualityVariants)
            val audioQualityVariants = (targetAudio?.get("qualityVariants") as? List<*>)?.mapNotNull { it as? Map<*, *> }
                ?: qv.mapNotNull { it as? Map<*, *> }

            val qMap = linkedMapOf<String, String>()
            for (variant in audioQualityVariants) {
                val label = variant["label"] as? String ?: continue
                val u = variant["url"] as? String ?: continue
                qMap[label] = u
            }

            val defaultUrl = (targetAudio?.get("url") as? String)
                ?: (resolved["url"] as? String)
                ?: ""

            // Настройка «Качество по умолчанию» применяется один раз за сессию плеера
            // (iOS restoreOrApplyQuality). «Спрашивать» пока трактуем как «Авто».
            if (!qualityPreferenceApplied) {
                qualityPreferenceApplied = true
                val pref = localStorage.getDefaultQuality()
                val wanted = if (pref.isBlank() || pref == "Спрашивать") "Авто" else pref
                // Метка в настройках может быть «1080p Ultra» — сопоставляем по высоте кадра.
                val matchingKey = when {
                    wanted == "Авто" -> null
                    !qMap[wanted].isNullOrBlank() -> wanted
                    else -> qMap.keys.firstOrNull { key ->
                        key != "Авто" &&
                            qualityHeight(key) > 0 &&
                            qualityHeight(key) == qualityHeight(wanted)
                    }
                }
                if (matchingKey != null) {
                    currentQualityKey = matchingKey
                    AllohaSessionHolder.currentQuality = matchingKey
                    AllohaSessionHolder.isAutoQuality = false
                } else {
                    currentQualityKey = "Авто"
                    AllohaSessionHolder.currentQuality = "Авто"
                    AllohaSessionHolder.isAutoQuality = true
                }
            }

            val currentVoiceName = targetTranslation?.name ?: requestedVoice.takeIf { !it.isNullOrBlank() } ?: "Русский"
            val streamVoices = if (isDedicatedIframe) {
                listOf(currentVoiceName, "Оригинал")
            } else if (enrichedAv.isNotEmpty() && enrichedAv.any {
                val t = (it["title"] as? String).orEmpty()
                !t.startsWith("Озвучка", ignoreCase = true) &&
                    !t.startsWith("Дорожка", ignoreCase = true) &&
                    !t.startsWith("Track", ignoreCase = true) &&
                    !t.equals("Unknown", ignoreCase = true)
            }) {
                enrichedAv.mapNotNull { (it["title"] as? String)?.takeIf(String::isNotBlank) }
            } else {
                latestAvailableTranslations.map { it.name }.ifEmpty { listOf(currentVoiceName, "Оригинал") }
            }
            AllohaSessionHolder.availableVoiceNames = streamVoices
            AllohaSessionHolder.currentVoice = currentVoiceName

            val bestUrl = selectBestPlayableUrl(qMap, currentQualityKey, defaultUrl)
            qMap["Авто"] = bestUrl
            AllohaSessionHolder.qualityMap = qMap

            // 1:1 with iOS PlaybackHlsRewriter: For multi-audio HLS streams, the root URL passed
            // to ExoPlayer must ALWAYS be the master playlist (master.m3u8), not a single video
            // stream variant (e.g. index-v1.m3u8). DynamicHttpDataSource rewrites the master playlist
            // using AllohaHlsRewriter so that all authentic voice names are present and the chosen
            // voice is marked DEFAULT=YES.
            val masterUrl = (targetAudio?.get("masterUrl") as? String)
                ?: (resolved["masterUrl"] as? String)
                ?: defaultUrl.takeIf { it.contains("master.m3u8", ignoreCase = true) }
                ?: (resolved["url"] as? String)?.takeIf { it.contains("master.m3u8", ignoreCase = true) }

            val directMp4Url = qMap[currentQualityKey]?.takeIf {
                it.contains(".mp4", ignoreCase = true) && !it.contains(".m3u8", ignoreCase = true)
            }

            val chosenRootUrl = directMp4Url
                ?: masterUrl
                ?: if (currentQualityKey == "Авто") bestUrl else (qMap[currentQualityKey] ?: bestUrl)

            if (chosenRootUrl.isBlank() || !chosenRootUrl.startsWith("http") || chosenRootUrl.contains("<") || chosenRootUrl.contains("\n")) {
                throw Exception("Не удалось извлечь корректный URL потока из плеера")
            }
            val finalUrl = chosenRootUrl.trim()

            // Резервная цепочка: выбранное качество -> остальные варианты -> master/прямой URL.
            val candidates = buildPlayableCandidates(qMap, currentQualityKey, defaultUrl)
                .ifEmpty { listOf(finalUrl) }
            playableCandidates = candidates
            candidateIndex = candidates.indexOf(finalUrl).coerceAtLeast(0)

            android.util.Log.d("PlayerScreen", "Alloha stream resolved: url=$finalUrl av=${av.size} qv=${qv.size} skips=${skips.size} headers=${hdrs.keys}")

            AllohaSessionHolder.registerStreamUrlUpdate(null, finalUrl)
            AllohaSessionHolder.latestMasterUrl = masterUrl ?: finalUrl
            streamUrl = finalUrl
            isLoadingStream = false
            isRecoveringStream = false
            currentTranslationId = targetTranslation?.id
            val seekTarget = if (preservePositionMs > 0L) {
                val p = preservePositionMs
                preservePositionMs = 0L
                p
            } else {
                // Возобновление строго по текущей серии (iOS: mediaId c суффиксом _sN_eM).
                progressStore.resumePositionMs(
                    mediaId,
                    if (currentAlloha.isSerial) currentSeason else null,
                    if (currentAlloha.isSerial) currentEpisode else null
                )
            }

            val mediaItem = createPlayableMediaItem(finalUrl, parsedSubtitles)
            preparedVoice = requestedVoice
            val matchingAudioQualityVariants = (matchingAudio?.get("qualityVariants") as? List<*>)
                ?.mapNotNull { it as? Map<*, *> }
                .orEmpty()
            val matchingAudioHasPlayableUrl = !(matchingAudio?.get("url") as? String).isNullOrBlank() ||
                matchingAudioQualityVariants.any { !(it["url"] as? String).isNullOrBlank() }
            // Если все озвучки живут в одном HLS (общий URL), дорожку переключаем нативно,
            // а не пересоздаём MediaItem с тем же адресом.
            val distinctVariantUrls = enrichedAv.mapNotNull { (it["url"] as? String)?.takeIf(String::isNotBlank) }.distinct()
            requiresNativeVoiceTrack = !requestedVoice.isNullOrBlank() &&
                (distinctVariantUrls.size <= 1 || !matchingAudioHasPlayableUrl)

            val maxRes = when {
                currentQualityKey.contains("4K", ignoreCase = true) || currentQualityKey.contains("2160") -> 3840 to 2160
                currentQualityKey.contains("2K", ignoreCase = true) || currentQualityKey.contains("1440") -> 2560 to 1440
                currentQualityKey.contains("1080") -> 1920 to 1080
                currentQualityKey.contains("720") -> 1280 to 720
                currentQualityKey.contains("480") -> 854 to 480
                currentQualityKey.contains("360") -> 640 to 360
                else -> Int.MAX_VALUE to Int.MAX_VALUE
            }

            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(maxRes.first, maxRes.second)
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED)
                .setPreferredAudioLanguages("ru", "rus", "ru-RU")
                .setPreferredTextLanguage(null)
                .build()
            exoPlayer.setMediaItem(mediaItem, seekTarget)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PlayerScreen", "Error resolving Alloha stream: ${e.message}", e)
            pendingVoice = null
            pendingTranslation = null
            streamErrorMessage = "Не удалось воспроизвести видеопоток. Пожалуйста, попробуйте другую озвучку."
            isLoadingStream = false
            isRecoveringStream = false
        }
    }

    // Auto-hide controls timer (strictly paused when any menu sheet is visible or when actively scrubbing timeline)
    LaunchedEffect(Unit) {
        snapshotFlow {
            controlsVisible && isPlayingState.value && !playerMenuState.isVisible && !isScrubbingTimeline
        }.collectLatest { shouldAutoHide ->
            if (shouldAutoHide) {
                delay(4000)
                controlsVisible = false
            }
        }
    }


    // Skip intro / outro state tracking (isolated coroutine check to avoid 250ms root recompositions)
    var activeSkipRange by remember { mutableStateOf<LongRange?>(null) }
    LaunchedEffect(exoPlayer) {
        snapshotFlow { isPlayingState.value }.collectLatest { playing ->
            if (!playing) {
                activeSkipRange = null
                return@collectLatest
            }
            while (currentCoroutineContext().isActive) {
                val curSec = exoPlayer.currentPosition / 1000L
                val ranges = AllohaSessionHolder.skipRanges
                val matched = ranges.firstOrNull { curSec in it }
                if (activeSkipRange != matched) {
                    activeSkipRange = matched
                }
                delay(500)
            }
        }
    }

    // Backdrop for real-time video refraction in Liquid Glass buttons and capsules
    val playerBackdrop = rememberLayerBackdrop {
        drawRect(Color.Black)
        drawContent()
    }

    // Keep the visibility branch stable during the fade; the layer alpha is updated by the GPU.
    val shouldShowControls = controlsVisible && !isLoadingStream && streamErrorMessage.isNullOrBlank() && !isInterfaceLocked
    val controlsTransition = updateTransition(targetState = shouldShowControls, label = "playerControlsVisibility")
    val controlsAlpha by controlsTransition.animateFloat(
        transitionSpec = { tween(220, easing = FastOutSlowInEasing) },
        label = "controlsAlpha"
    ) { visible -> if (visible) 1f else 0f }

    // Safe cutout insets
    val cutoutInsets = WindowInsets.displayCutout.asPaddingValues()
    val safeCutoutStart = cutoutInsets.calculateStartPadding(LayoutDirection.Ltr)
    val safeCutoutEnd = cutoutInsets.calculateEndPadding(LayoutDirection.Ltr)
    val horizontalPadding = maxOf(24.dp, maxOf(safeCutoutStart, safeCutoutEnd) + 12.dp)

    // Screen Layout
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CompositionLocalProvider(LocalSlooshColors provides DarkSlooshColors) {
            val screenWidth = maxWidth
        val screenHeight = maxHeight
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val screenAspect = if (screenHeightPx > 0f) screenWidthPx / screenHeightPx else (16f / 9f)
        val rawFillScale = if (screenAspect > videoAspect) (screenAspect / videoAspect) else (videoAspect / screenAspect)
        val fillScale = maxOf(1.05f, minOf(rawFillScale, 2.5f))

        LaunchedEffect(fillScale, isZoomedToFill) {
            if (!videoScale.isRunning) {
                val target = if (isZoomedToFill) fillScale else 1.0f
                if (kotlin.math.abs(videoScale.value - target) > 0.01f) {
                    videoScale.animateTo(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
        }

        // 1. ExoPlayer Video Surface with TextureView + Gradient Vignette inside Backdrop Capture
        // Placing the gradient overlay INSIDE playerBackdrop ensures all glass lenses refract
        // the live video WITH the darkened cinematic vignette gradient smoothly and seamlessly!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(playerBackdrop)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = videoScale.value
                        scaleY = videoScale.value
                        clip = true
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        val view = LayoutInflater.from(ctx).inflate(R.layout.view_sloosh_player, null, false) as PlayerView
                        view.player = exoPlayer
                        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

                        // Apply Apple TV / AVKit style captions (translucent dark plates wrapping each line)
                        val captionStyle = CaptionStyleCompat(
                            android.graphics.Color.WHITE,
                            android.graphics.Color.argb(184, 0, 0, 0),
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_NONE,
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Typeface.DEFAULT_BOLD
                        )
                        view.subtitleView?.setStyle(captionStyle)
                        view.subtitleView?.setApplyEmbeddedStyles(false)
                        view.subtitleView?.setApplyEmbeddedFontSizes(false)

                        val subtitleSizeStr = localStorage.getString("subtitle_size", "Стандартный")
                        val subtitleSp = when (subtitleSizeStr) {
                            "Маленький" -> 15f
                            "Крупный" -> 24f
                            else -> 19f
                        }
                        view.subtitleView?.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, subtitleSp)
                        view
                    },
                    update = { _ -> },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Cinematic gradient vignette inside the backdrop capture layer (GPU layer alpha)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = controlsAlpha }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
            )
        }

        // 2. Multi-touch Gesture Detection Overlay (Pinch to Zoom, Double Tap Seek, Drag Brightness/Volume)
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Smooth YouTube-style Pinch to Zoom detector
                .pointerInput(fillScale) {
                    var accumulatedScale = 1.0f
                    detectSmoothPinchToZoom(
                        onPinchStart = {
                            accumulatedScale = videoScale.value
                        },
                        onPinch = { zoomMultiplier ->
                            accumulatedScale *= zoomMultiplier
                            val clamped = accumulatedScale.coerceIn(0.80f, fillScale * 1.35f)
                            scope.launch {
                                videoScale.snapTo(clamped)
                            }
                        },
                        onPinchEnd = {
                            scope.launch {
                                val currentVal = videoScale.value
                                val threshold = (1.0f + fillScale) * 0.48f
                                val shouldFill = if (isZoomedToFill) {
                                    currentVal > (1.0f + fillScale) * 0.42f
                                } else {
                                    currentVal > threshold
                                }

                                val targetScale = if (shouldFill) fillScale else 1.0f
                                val stateChanged = isZoomedToFill != shouldFill
                                isZoomedToFill = shouldFill

                                if (stateChanged) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }

                                videoScale.animateTo(
                                    targetValue = targetScale,
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    )
                }
                // Tap and Double Tap detector
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (isInterfaceLocked) {
                                showLockOverlay = !showLockOverlay
                            } else if (playerMenuState.isVisible) {
                                playerMenuState.dismiss()
                            } else {
                                controlsVisible = !controlsVisible
                            }
                        },
                        onDoubleTap = { offset ->
                            if (isInterfaceLocked) return@detectTapGestures
                            val widthPx = screenWidth.toPx()
                            if (offset.x < widthPx * 0.35f) {
                                val target = (exoPlayer.currentPosition - seekBackwardMs).coerceAtLeast(0)
                                exoPlayer.seekTo(target)
                                seekRippleText = "-$seekBackwardSec сек"
                            } else if (offset.x > widthPx * 0.65f) {
                                val target = (exoPlayer.currentPosition + seekForwardMs).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(target)
                                seekRippleText = "+$seekForwardSec сек"
                            } else {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                        }
                    )
                }
                // Vertical Drag detector for Brightness and Volume with Non-linear Velocity Acceleration & Momentum Fling
                .pointerInput(Unit) {
                    var currentProgress = 0.5f
                    var isLeftZone = false
                    var lastUptime = 0L
                    val velocityTracker = VelocityTracker()
                    var flingJob: kotlinx.coroutines.Job? = null
                    var dismissJob: kotlinx.coroutines.Job? = null

                    // Троттлинг системных вызовов: setStreamVolume и запись window.attributes — это
                    // IPC в system_server. На каждом кадре драга они давали реальные подтормаживания,
                    // а визуально шаг 0.5% яркости / один шаг громкости всё равно незаметен.
                    var maxVolumeCache = 0
                    var appliedBrightness = -1f
                    var appliedVolume = -1

                    fun applyBrightness(value: Float) {
                        val v = value.coerceIn(0.01f, 1.0f)
                        if (appliedBrightness >= 0f && kotlin.math.abs(v - appliedBrightness) < 0.005f) return
                        appliedBrightness = v
                        val win = activity?.window ?: return
                        val params = win.attributes
                        params.screenBrightness = v
                        win.attributes = params
                    }

                    fun applyVolume(value: Float) {
                        if (maxVolumeCache <= 0) {
                            maxVolumeCache = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        }
                        val target = (value * maxVolumeCache).roundToInt().coerceIn(0, maxVolumeCache)
                        if (target == appliedVolume) return
                        appliedVolume = target
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                    }

                    detectDragGestures(
                        onDragStart = { offset ->
                            if (isInterfaceLocked || !volumeBrightnessGesturesEnabled) return@detectDragGestures
                            dismissJob?.cancel()
                            flingJob?.cancel()
                            velocityTracker.resetTracking()
                            lastUptime = 0L

                            val widthPx = screenWidth.toPx()
                            isLeftZone = offset.x < widthPx / 2
                            if (isLeftZone) {
                                val win = activity?.window
                                val params = win?.attributes
                                val current = if (params != null && params.screenBrightness >= 0) params.screenBrightness else 0.5f
                                appliedBrightness = current
                                currentProgress = current
                                hudProgress = current
                                hudType = GestureHudType.BRIGHTNESS
                                isHudVisible = true
                            } else {
                                maxVolumeCache = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                appliedVolume = curVol
                                val volFraction = curVol.toFloat() / maxVolumeCache.toFloat()
                                currentProgress = volFraction
                                hudProgress = volFraction
                                hudType = GestureHudType.VOLUME
                                isHudVisible = true
                            }
                        },
                        onDragEnd = {
                            dismissJob?.cancel()
                            flingJob?.cancel()

                            val velocityY = velocityTracker.calculateVelocity().y
                            val screenH = maxOf(screenHeight.toPx(), 1f)

                            // If user swiped with notable speed, glide smoothly with momentum
                            if (kotlin.math.abs(velocityY) > 350f) {
                                val flingDelta = (-velocityY / (screenH * 1.8f)).coerceIn(-0.35f, 0.35f)
                                val targetProgress = (currentProgress + flingDelta).coerceIn(0f, 1f)
                                val startVal = currentProgress
                                val duration = (kotlin.math.abs(flingDelta) * 550f).toInt().coerceIn(160, 300)

                                flingJob = scope.launch {
                                    val anim = Animatable(startVal)
                                    anim.animateTo(
                                        targetValue = targetProgress,
                                        animationSpec = tween(
                                            durationMillis = duration,
                                            easing = FastOutSlowInEasing
                                        )
                                    ) {
                                        currentProgress = value
                                        hudProgress = value
                                        if (isLeftZone) applyBrightness(value) else applyVolume(value)
                                    }
                                    delay(850)
                                    isHudVisible = false
                                }
                            } else {
                                dismissJob = scope.launch {
                                    delay(850)
                                    isHudVisible = false
                                }
                            }
                        },
                        onDragCancel = {
                            dismissJob?.cancel()
                            flingJob?.cancel()
                            dismissJob = scope.launch {
                                delay(850)
                                isHudVisible = false
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (!volumeBrightnessGesturesEnabled || change.isConsumed) return@detectDragGestures
                            dismissJob?.cancel()
                            flingJob?.cancel()

                            velocityTracker.addPosition(change.uptimeMillis, change.position)

                            val dt = if (lastUptime > 0L) (change.uptimeMillis - lastUptime).coerceIn(4L, 50L) else 16L
                            lastUptime = change.uptimeMillis

                            val dy = -dragAmount.y
                            val speed = kotlin.math.abs(dy) / dt.toFloat()

                            // Non-linear velocity multiplier:
                            // Slow precision movement (< 0.15 px/ms) -> 0.65x for fine control
                            // Fast swiping (> 1.0 px/ms) -> smoothly scales up to 3.5x
                            val velocityMultiplier = (0.65f + speed * 0.90f).coerceIn(0.65f, 3.50f)

                            val dragRange = maxOf(screenHeight.toPx() * 0.70f, 1f)
                            val step = (dy / dragRange) * velocityMultiplier

                            currentProgress = (currentProgress + step).coerceIn(0f, 1f)
                            hudProgress = currentProgress

                            if (isLeftZone) {
                                applyBrightness(currentProgress)
                                hudType = GestureHudType.BRIGHTNESS
                                isHudVisible = true
                            } else {
                                applyVolume(currentProgress)
                                hudType = GestureHudType.VOLUME
                                isHudVisible = true
                            }
                        }
                    )
                }
        )

        // Provide Live Video Backdrop to all Liquid Glass components
        CompositionLocalProvider(LocalBackdrop provides playerBackdrop) {

            // Loading Overlay: Minimalist pure white indicator without any text, matching native iOS
            if (isLoadingStream && !isRecoveringStream) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.70f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.85f),
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(44.dp)
                    )
                }
            } else if (!streamErrorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_exclamationmark_triangle_fill),
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = streamErrorMessage!!,
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LiquidGlassFloatingButton(
                                onClick = {
                                    silentRetryCount = 0
                                    sessionReloadKey++
                                },
                                shape = Capsule(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text("Повторить", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            LiquidGlassFloatingButton(
                                onClick = onBack,
                                shape = Capsule(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text("Вернуться назад", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // TOP iOS-STYLE GLASS VOLUME & BRIGHTNESS HUD
            // Читаем анимацию видимости и прогресс ТОЛЬКО внутри PlayerGestureHud:
            // иначе каждый кадр драга громкости/яркости пересобирал бы весь слой
            // контролов (десятки стеклянных кнопок) и ронял кадры видео.
            PlayerGestureHud(
                visibility = hudVisibilityAnim,
                type = hudTypeState,
                progress = hudProgressState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
            )

            // Double Tap Ripple Text (+10s / -10s)
            LaunchedEffect(seekRippleText) {
                if (seekRippleText != null) {
                    delay(800)
                    seekRippleText = null
                }
            }
            if (seekRippleText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .liquidGlassSurface(
                            style = LiquidGlassTokens.CompactPill,
                            shape = Capsule(),
                            containerColor = LiquidGlassSurfaceDark
                        )
                        .clip(Capsule())
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(text = seekRippleText!!, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }


            // Controls Layer (GPU compositor animated, 0 CPU recomposition overhead during playback)
            if (controlsTransition.currentState || controlsTransition.targetState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = controlsAlpha }
                ) {
                    // TOP BAR (iOS TopBarView 1:1):
                    // Ось кнопок фиксированной высоты 44dp, капсула сезон/серия — ниже неё,
                    // поэтому её появление больше не сдвигает ни логотип, ни кнопки.
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(
                                start = horizontalPadding,
                                end = horizontalPadding,
                                top = 24.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 1. Centered Title or Logo (строго на оси кнопок)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .graphicsLayer { alpha = (1f - hudVisibilityAnim.value).coerceIn(0f, 1f) }
                                    .padding(horizontal = 112.dp)
                            ) {
                                val logo = activeLogoUrl
                                if (!logo.isNullOrBlank()) {
                                    val context = LocalContext.current
                                    val playerLogoRequest = remember(logo) {
                                        ImageRequest.Builder(context)
                                            .data(logo)
                                            .size(coil.size.Size.ORIGINAL)
                                            .crossfade(true)
                                            .build()
                                    }
                                    AsyncImage(
                                        model = playerLogoRequest,
                                        contentDescription = activeTitle,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .heightIn(max = 36.dp)
                                            .widthIn(max = 240.dp)
                                    )
                                } else {
                                    Text(
                                        text = activeTitle,
                                        color = Color.White.copy(alpha = 0.92f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 2. Left Action Buttons (Close, PiP)
                            Row(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LiquidGlassFloatingButton(
                                    onClick = onBack,
                                    shape = CircleShape,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_sf_xmark),
                                        contentDescription = "Закрыть",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    LiquidGlassFloatingButton(
                                        onClick = {
                                            try {
                                                val aspectRatio = Rational(16, 9)
                                                val params = PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
                                                activity?.enterPictureInPictureMode(params)
                                            } catch (_: Exception) {}
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_sf_dock_rectangle),
                                            contentDescription = "PiP",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // 3. Right Action Button (Screen Lock matching iOS 1:1)
                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LiquidGlassFloatingButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isInterfaceLocked = true
                                        controlsVisible = false
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_sf_lock_open),
                                        contentDescription = "Заблокировать экран",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // 4. Compact Season & Episode button под осью кнопок (iOS 1:1 TopBarView.swift)
                        if (isTvSeries && currentSeason != null && currentEpisode != null) {
                            val episodePillHighlight = remember { InteractiveHighlight(animationScope = scope) }
                            Box(
                                modifier = Modifier
                                    .height(26.dp)
                                    .graphicsLayer { alpha = (1f - hudVisibilityAnim.value).coerceIn(0f, 1f) }
                                    .onGloballyPositioned { coords ->
                                        episodePillBounds = coords.boundsInRoot()
                                    }
                                    .liquidGlassInteractive(
                                        interactiveHighlight = episodePillHighlight,
                                        style = LiquidGlassTokens.CompactPill,
                                        shape = Capsule(),
                                        containerColor = LiquidGlassSurfaceDark
                                    )
                                    .clip(Capsule())
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                        role = Role.Button
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val seasonsList = allohaData?.seasons ?: emptyList()
                                        val isMultiSeason = seasonsList.size > 1
                                        val episodeEntries = mutableListOf<Triple<String, Int, Int>>()
                                        if (isMultiSeason) {
                                            seasonsList.forEach { s ->
                                                s.episodes.forEach { ep ->
                                                    episodeEntries.add(Triple("С${s.season}, Серия ${ep.episode}", s.season, ep.episode))
                                                }
                                            }
                                        } else {
                                            val firstSeasonEps = seasonsList.firstOrNull()?.episodes ?: emptyList()
                                            firstSeasonEps.forEach { ep ->
                                                episodeEntries.add(Triple("Серия ${ep.episode}", ep.season, ep.episode))
                                            }
                                        }
                                        val options = episodeEntries.map { it.first }
                                        val selIdx = episodeEntries.indexOfFirst {
                                            it.second == (currentSeason ?: 1) && it.third == (currentEpisode ?: 1)
                                        }.coerceAtLeast(0)

                                        playerMenuState.show(
                                            options = options,
                                            selectedIndex = selIdx,
                                            anchorBounds = episodePillBounds,
                                            customWidth = 240.dp
                                        ) { index ->
                                            if (index in episodeEntries.indices) {
                                                val entry = episodeEntries[index]
                                                selectEpisode(entry.second, entry.third)
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.5.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_sf_rectangle_stack_fill),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "$currentSeason сезон, $currentEpisode серия",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.92f)
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.ic_sf_chevron_up_chevron_down),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.50f),
                                        modifier = Modifier
                                            .width(10.dp)
                                            .height(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // CENTER CONTROLS:
                    // For Series: Previous Episode / Play-Pause / Next Episode
                    // For Movies: ONLY Play-Pause button strictly matching iOS!
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (isTvSeries) {
                            LiquidGlassFloatingButton(
                                onClick = { if (hasPreviousEpisode) playPreviousEpisode() },
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sf_backward_end_fill),
                                    contentDescription = "Предыдущая серия",
                                    tint = if (hasPreviousEpisode) Color.White else Color.White.copy(alpha = 0.35f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        PlayerPlayPauseButton(
                            player = exoPlayer,
                            isRecoveringStream = isRecoveringStream
                        )

                        if (isTvSeries) {
                            LiquidGlassFloatingButton(
                                onClick = { if (hasNextEpisode) playNextEpisode() },
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sf_forward_end_fill),
                                    contentDescription = "Следующая серия",
                                    tint = if (hasNextEpisode) Color.White else Color.White.copy(alpha = 0.35f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // BOTTOM CONTROLS & TIMELINE (iOS-style Liquid Glass panels)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(
                                start = horizontalPadding,
                                end = horizontalPadding,
                                bottom = 24.dp
                            )
                    ) {
                        // Строка над полосой перемотки: слева капсула озвучки,
                        // справа — «Пропустить» и капсула [скорость | качество] (iOS 1:1).
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Left: капсула текущей озвучки (iOS 1:1 PlayerTitleInfoView).
                            // Показывает название дорожки и открывает выбор озвучки.
                            // Всё считаем в remember: иначе нормализация имён, матчинг и
                            // форматирование подписи выполнялись бы на каждую пересборку плеера.
                            val voiceChip = remember(availableTranslations, pendingTranslation, requestedVoice) {
                                val name = (
                                    pendingTranslation?.name
                                        ?: requestedVoice
                                        ?: availableTranslations.firstOrNull()?.name
                                    ).orEmpty()
                                if (availableTranslations.isEmpty() || name.isBlank()) {
                                    null
                                } else {
                                    val names = availableTranslations.map { it.name }
                                    val index = availableTranslations.indexOfFirst { it.name == name }
                                        .takeIf { it >= 0 }
                                        ?: availableTranslations.indexOfFirst {
                                            AllohaTranslationMatcher.matches(it.name, name, exactOnly = true)
                                        }.takeIf { it >= 0 }
                                        ?: 0
                                    val rawName = name
                                    val formatted = DubbingStudio.displayTranslationName(rawName, index, names)
                                    val studio = DubbingStudio.detect(rawName)
                                    val flag = LanguageFlag.detect("$rawName $formatted")
                                    val iconResId = studio?.iconResId ?: flag.iconResId
                                    val displayTitle = if (iconResId != null) LanguageFlag.stripLeadingEmoji(formatted) else formatted
                                    PlayerVoiceChip(
                                        names = names,
                                        index = index,
                                        label = displayTitle,
                                        canSwitch = availableTranslations.size > 1,
                                        iconResId = iconResId
                                    )
                                }
                            }
                            voiceChip?.let { chip ->
                                val voiceCapsuleHighlight = remember { InteractiveHighlight(animationScope = scope) }

                                Box(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .widthIn(max = 280.dp)
                                        .graphicsLayer { alpha = multiCapsuleAlpha.value }
                                        .onGloballyPositioned { coords ->
                                            voiceCapsuleBounds = coords.boundsInRoot()
                                        }
                                        .liquidGlassInteractive(
                                            interactiveHighlight = voiceCapsuleHighlight,
                                            style = LiquidGlassTokens.Button,
                                            shape = Capsule(),
                                            containerColor = LiquidGlassSurfaceDark
                                        )
                                        .clip(Capsule())
                                        .clickable(
                                            enabled = chip.canSwitch,
                                            interactionSource = null,
                                            indication = null,
                                            role = Role.Button
                                        ) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val sortedTranslations = availableTranslations
                                            // 1:1 с iOS: подпись чипа — очищенное название с сохранением
                                            // слова «Дубляж» и студии, «#N» только при реальных дублях.
                                            val voiceOptions = sortedTranslations.mapIndexed { index, translation ->
                                                val formatted = DubbingStudio.displayTranslationName(
                                                    translation.name,
                                                    index,
                                                    chip.names
                                                )
                                                val studio = DubbingStudio.detect(translation.name)
                                                val flag = LanguageFlag.detect("${translation.name} $formatted")
                                                val iconRes = studio?.iconResId ?: flag.iconResId
                                                if (iconRes != null) LanguageFlag.stripLeadingEmoji(formatted) else formatted
                                            }
                                            val voiceIcons = sortedTranslations.mapIndexed { index, translation ->
                                                val formatted = DubbingStudio.displayTranslationName(
                                                    translation.name,
                                                    index,
                                                    chip.names
                                                )
                                                val studio = DubbingStudio.detect(translation.name)
                                                val flag = LanguageFlag.detect("${translation.name} $formatted")
                                                studio?.iconResId ?: flag.iconResId
                                            }
                                            val selectedTranslationId = pendingTranslation?.id ?: currentTranslationId
                                            val selIdx = sortedTranslations.indexOfFirst { it.id == selectedTranslationId }
                                                .takeIf { it >= 0 } ?: sortedTranslations.indexOfFirst {
                                                it.name == requestedVoice
                                            }.takeIf { it >= 0 } ?: sortedTranslations.indexOfFirst {
                                                AllohaTranslationMatcher.matches(it.name, requestedVoice, exactOnly = true)
                                            }.takeIf { it >= 0 } ?: sortedTranslations.indexOfFirst {
                                                AllohaTranslationMatcher.matches(it.name, requestedVoice)
                                            }.takeIf { it >= 0 } ?: if (requestedVoice.isNullOrBlank()) 0 else -1
                                            playerMenuState.show(
                                                options = voiceOptions,
                                                selectedIndex = selIdx,
                                                anchorBounds = voiceCapsuleBounds,
                                                customWidth = 280.dp,
                                                itemIcons = voiceIcons
                                            ) { index ->
                                                val trans = sortedTranslations[index]
                                                val pos = exoPlayer.currentPosition
                                                preservePositionMs = if (pos > 0L) pos else preservePositionMs
                                                silentRetryCount = 0
                                                candidateIndex = 0

                                                // 1:1 с iOS PlayerView.swift (lines 1956-1965):
                                                // 1. Если для озвучки задан отдельный iframeUrl и он отличается от текущего —
                                                // инвалидируем кэш и перезагружаем iframe с сохранённой позицией!
                                                val targetIframe = trans.iframeUrl.takeIf { it.isNotBlank() }
                                                if (targetIframe != null && targetIframe != activeIframeUrl) {
                                                    AllohaSessionHolder.currentVoice = trans.name
                                                    AllohaSessionHolder.availableVoiceNames = listOf(trans.name, "Оригинал")
                                                    AllohaRuntimeResolver.invalidateCache(targetIframe)
                                                    currentTranslationId = trans.id
                                                    activeIframeUrl = targetIframe
                                                    pendingVoice = trans.name
                                                    pendingTranslation = trans
                                                    preparedVoice = trans.name
                                                    reloadToken += 1
                                                    sessionReloadKey += 1
                                                    return@show
                                                }

                                                // 2. Быстрое переключение через resolvedAudioVariants (прямые HLS ссылки внутри текущего потока)
                                                val matchingVariant = resolvedAudioVariants.firstOrNull {
                                                    AllohaTranslationMatcher.matches(it["title"] as? String, trans.name, exactOnly = true)
                                                } ?: resolvedAudioVariants.firstOrNull {
                                                    AllohaTranslationMatcher.matches(it["title"] as? String, trans.name)
                                                }
                                                val targetVariantUrl = matchingVariant?.get("url") as? String

                                                if (!targetVariantUrl.isNullOrBlank() && targetVariantUrl != streamUrl) {
                                                    val currentPos = exoPlayer.currentPosition
                                                    preservePositionMs = currentPos
                                                    streamUrl = targetVariantUrl
                                                    currentTranslationId = trans.id
                                                    commitVoiceSelection(trans.name)
                                                    val freshMediaItem = createPlayableMediaItem(targetVariantUrl, activeSubtitleConfigs)
                                                    exoPlayer.setMediaItem(freshMediaItem, currentPos)
                                                    exoPlayer.prepare()
                                                    exoPlayer.play()
                                                    return@show
                                                }

                                                val audioTrackCount = exoPlayer.currentTracks.groups
                                                    .filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
                                                    .sumOf { it.length }
                                                val hasMultipleAudioTracks = audioTrackCount > 1

                                                // 1:1 с iOS: сначала пытаемся переключить встроенную дорожку HLS без перезагрузки
                                                val switched = if (hasMultipleAudioTracks) {
                                                    exoPlayer.selectAudioTrackForVoice(
                                                        voice = trans.name,
                                                        voices = AllohaSessionHolder.availableVoiceNames.ifEmpty { chip.names },
                                                        explicitIndex = index
                                                    )
                                                } else null

                                                if (switched != null) {
                                                    currentTranslationId = trans.id
                                                    commitVoiceSelection(trans.name)
                                                } else {
                                                    val fallbackIframe = targetIframe ?: activeIframeUrl
                                                    if (!fallbackIframe.isNullOrBlank()) {
                                                        AllohaRuntimeResolver.invalidateCache(fallbackIframe)
                                                    }
                                                    sessionReloadKey += 1
                                                    pendingVoice = trans.name
                                                    pendingTranslation = trans
                                                    preparedVoice = trans.name
                                                    reloadToken += 1
                                                }
                                            }
                                        }
                                        .padding(
                                            start = if (chip.iconResId != null) 10.dp else 13.dp,
                                            end = if (chip.canSwitch) 11.dp else 13.dp
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        if (chip.iconResId != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                            ) {
                                                Image(
                                                    painter = painterResource(chip.iconResId),
                                                    contentDescription = "Озвучка",
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .border(
                                                            width = 0.5.dp,
                                                            color = Color.White.copy(alpha = 0.14f),
                                                            shape = CircleShape
                                                        )
                                                )
                                            }
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_sf_waveform),
                                                contentDescription = "Озвучка",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Text(
                                            text = chip.label,
                                            color = Color.White.copy(alpha = 0.92f),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (chip.canSwitch) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_sf_chevron_up_chevron_down),
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.45f),
                                                modifier = Modifier
                                                    .width(12.dp)
                                                    .height(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Right: «Пропустить» и капсула [скорость | качество]
                            Column(horizontalAlignment = Alignment.End) {
                                activeSkipRange?.let { range ->
                                    val isOutro = duration > 0L && (range.first * 1000L) > (duration * 0.70f)
                                    val skipLabel = if (isOutro) "Пропустить титры" else "Пропустить заставку"
                                    LiquidGlassFloatingButton(
                                        onClick = { exoPlayer.seekTo((range.last + 1) * 1000L) },
                                        shape = Capsule(),
                                        modifier = Modifier
                                            .padding(bottom = 8.dp)
                                            .height(44.dp),
                                        contentPadding = PaddingValues(horizontal = 18.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_sf_forward_end_fill),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = skipLabel,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Нижняя капсула справа: Скорость | Качество (iOS BottomRowView)
                                LiquidGlassMultiButton(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .graphicsLayer { alpha = multiCapsuleAlpha.value }
                                        .onGloballyPositioned { coords ->
                                            multiCapsuleBounds = coords.boundsInRoot()
                                        },
                                    shape = Capsule()
                                ) {
                                    // Speed Button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .clip(Capsule())
                                            .clickable(
                                                interactionSource = null,
                                                indication = null
                                            ) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val speeds = listOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x")
                                                val selIdx = speeds.indexOf("${currentSpeed}x").takeIf { it >= 0 } ?: 2
                                                playerMenuState.show(
                                                    options = speeds,
                                                    selectedIndex = selIdx,
                                                    anchorBounds = multiCapsuleBounds
                                                ) { index ->
                                                    val speedVal = speeds[index].replace("x", "").toFloatOrNull() ?: 1.0f
                                                    currentSpeed = speedVal
                                                    exoPlayer.playbackParameters = PlaybackParameters(speedVal)
                                                }
                                            }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_sf_gauge_with_needle),
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.92f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "${currentSpeed}x",
                                                color = Color.White.copy(alpha = 0.92f),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // Quality Button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .clip(Capsule())
                                            .clickable(
                                                interactionSource = null,
                                                indication = null
                                            ) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val availableQualities = run {
                                                    val list = mutableListOf("Авто")
                                                    val sortedKeys = AllohaSessionHolder.qualityMap.keys.filter { it != "Авто" }.sortedWith { a, b ->
                                                        qualityHeight(b).compareTo(qualityHeight(a))
                                                    }
                                                    if (sortedKeys.isNotEmpty()) list.addAll(sortedKeys)
                                                    list
                                                }
                                                val selIdx = availableQualities.indexOf(currentQualityKey).takeIf { it >= 0 } ?: 0
                                                playerMenuState.show(
                                                    options = availableQualities,
                                                    selectedIndex = selIdx,
                                                    anchorBounds = multiCapsuleBounds
                                                ) { index ->
                                                    val q = availableQualities[index]
                                                    currentQualityKey = q
                                                    AllohaSessionHolder.currentQuality = q
                                                    AllohaSessionHolder.isAutoQuality = (q == "Авто")

                                                    val isCurrentMp4 = streamUrl?.contains(".mp4", ignoreCase = true) == true && streamUrl?.contains(".m3u8", ignoreCase = true) != true
                                                    val isTargetMp4 = AllohaSessionHolder.qualityMap[q]?.contains(".mp4", ignoreCase = true) == true &&
                                                        AllohaSessionHolder.qualityMap[q]?.contains(".m3u8", ignoreCase = true) != true

                                                    val maxRes = when {
                                                        q.contains("4K", ignoreCase = true) || q.contains("2160") -> 3840 to 2160
                                                        q.contains("2K", ignoreCase = true) || q.contains("1440") -> 2560 to 1440
                                                        q.contains("1080") -> 1920 to 1080
                                                        q.contains("720") -> 1280 to 720
                                                        q.contains("480") -> 854 to 480
                                                        q.contains("360") -> 640 to 360
                                                        else -> Int.MAX_VALUE to Int.MAX_VALUE
                                                    }
                                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                        .buildUpon()
                                                        .setMaxVideoSize(maxRes.first, maxRes.second)
                                                        .build()

                                                    if (isTargetMp4 || isCurrentMp4) {
                                                        val targetUrl = if (isTargetMp4) {
                                                            AllohaSessionHolder.qualityMap[q]
                                                        } else {
                                                            AllohaSessionHolder.latestMasterUrl ?: streamUrl
                                                        }
                                                        if (!targetUrl.isNullOrBlank() && targetUrl != streamUrl) {
                                                            val currentPos = exoPlayer.currentPosition
                                                            AllohaSessionHolder.registerStreamUrlUpdate(streamUrl, targetUrl)
                                                            streamUrl = targetUrl
                                                            playableCandidates = listOf(targetUrl)
                                                            candidateIndex = 0
                                                            exoPlayer.setMediaItem(createPlayableMediaItem(targetUrl, activeSubtitleConfigs), currentPos)
                                                            exoPlayer.prepare()
                                                            exoPlayer.play()
                                                        }
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_sf_gearshape),
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.92f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = currentQualityKey,
                                                color = Color.White.copy(alpha = 0.92f),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Ultra-clean continuous iOS-style Seek Bar (zero bulky knobs)
                        SlooshPlayerSeekBar(
                            player = exoPlayer,
                            isPlaying = isPlayingState,
                            preservePositionMs = preservePositionMs,
                            isRecoveringStream = isRecoveringStream,
                            durationMs = duration,
                            onScrubbingChanged = { isScrubbing ->
                                isScrubbingTimeline = isScrubbing
                            },
                            onSeek = { targetMs ->
                                exoPlayer.seekTo(targetMs)
                            }
                        )
                    }
                }
            }

            // Lock overlay (when screen is locked, shows unlock button on tap)
            if (isInterfaceLocked) {
                LaunchedEffect(showLockOverlay) {
                    if (showLockOverlay) {
                        delay(3500)
                        showLockOverlay = false
                    }
                }
                val lockAlpha by animateFloatAsState(
                    targetValue = if (showLockOverlay) 1f else 0f,
                    animationSpec = tween(220),
                    label = "lockAlpha"
                )
                if (lockAlpha > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = lockAlpha }
                            .padding(
                                start = horizontalPadding,
                                end = horizontalPadding,
                                top = 16.dp
                            )
                    ) {
                        LiquidGlassFloatingButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isInterfaceLocked = false
                                showLockOverlay = false
                                controlsVisible = true
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_sf_lock_fill),
                                contentDescription = "Разблокировать экран",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // LIQUID GLASS MENU OVERLAY (Speed, Quality, Voiceover, Episodes)
            LiquidGlassMenuOverlay(
                state = playerMenuState
            )
        }
    }
}
}

/**
 * Ultra-sleek continuous iOS-style Seek Bar:
 * - Pure continuous bar with NO bulky circular knobs or sliders.
 * - Smoothly expands from 3dp to 5dp on touch/drag.
 * - Seamless tap-to-seek and horizontal drag gestures.
 * - Monospaced elapsed and negative remaining time display.
 */
@Composable
private fun SlooshPlayerSeekBar(
    player: Player,
    isPlaying: State<Boolean>,
    preservePositionMs: Long,
    isRecoveringStream: Boolean,
    durationMs: Long,
    onScrubbingChanged: (Boolean) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var pendingSeekMs by remember { mutableStateOf<Long?>(null) }

    val livePosition by player.currentPositionAsState(isPlaying.value)
    val totalDuration = maxOf(durationMs, 1L)

    val currentDuration by rememberUpdatedState(totalDuration)
    val currentPreservePositionMs by rememberUpdatedState(preservePositionMs)
    val currentIsRecoveringStream by rememberUpdatedState(isRecoveringStream)
    val currentOnScrubbingChanged by rememberUpdatedState(onScrubbingChanged)
    val currentOnSeek by rememberUpdatedState(onSeek)

    // Clear optimistic seek lock once ExoPlayer has caught up to the seek target
    LaunchedEffect(livePosition, pendingSeekMs) {
        val target = pendingSeekMs ?: return@LaunchedEffect
        if (kotlin.math.abs(livePosition - target) < 1200L) {
            pendingSeekMs = null
        }
    }

    // Safety fallback timeout to release seek lock after 2.5s
    LaunchedEffect(pendingSeekMs) {
        if (pendingSeekMs != null) {
            delay(2500L)
            pendingSeekMs = null
        }
    }

    val displayPositionMs = when {
        isDragging -> (dragProgress * totalDuration).toLong().coerceIn(0L, totalDuration)
        pendingSeekMs != null -> pendingSeekMs!!.coerceIn(0L, totalDuration)
        isRecoveringStream && preservePositionMs > 0L -> preservePositionMs.coerceIn(0L, totalDuration)
        else -> livePosition.coerceIn(0L, totalDuration)
    }

    val progress = when {
        isDragging -> dragProgress
        pendingSeekMs != null -> (pendingSeekMs!!.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
        isRecoveringStream && preservePositionMs > 0L -> (preservePositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
        else -> (livePosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    }

    // Animate track thickness: 5.5dp resting, expands to 8dp on touch/drag
    val animatedTrackHeight by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 5.5.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "trackHeight"
    )

    var capsuleWidthPx by remember { mutableFloatStateOf(0f) }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .liquidGlassSurface(
                style = LiquidGlassTokens.Bar,
                shape = Capsule(),
                containerColor = LiquidGlassSurfaceDark
            )
            .clip(Capsule())
            .onSizeChanged { capsuleWidthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                var lastUptime = 0L
                var isFirstDrag = true
                val velocityTracker = VelocityTracker()
                var flingJob: kotlinx.coroutines.Job? = null

                detectDragGestures(
                    onDragStart = { _ ->
                        isDragging = true
                        currentOnScrubbingChanged(true)
                        flingJob?.cancel()
                        velocityTracker.resetTracking()
                        lastUptime = 0L
                        isFirstDrag = true

                        val currentTotalDur = currentDuration
                        val currentStartingProgress = when {
                            pendingSeekMs != null -> (pendingSeekMs!!.toFloat() / currentTotalDur.toFloat()).coerceIn(0f, 1f)
                            currentIsRecoveringStream && currentPreservePositionMs > 0L -> (currentPreservePositionMs.toFloat() / currentTotalDur.toFloat()).coerceIn(0f, 1f)
                            else -> (player.currentPosition.toFloat() / currentTotalDur.toFloat()).coerceIn(0f, 1f)
                        }
                        dragProgress = currentStartingProgress
                    },
                    onDragEnd = {
                        flingJob?.cancel()
                        val velocityX = velocityTracker.calculateVelocity().x
                        val safeTrackW = if (trackWidthPx > 0f) trackWidthPx else maxOf(capsuleWidthPx * 0.65f, 1f)
                        val currentTotalDur = currentDuration

                        if (kotlin.math.abs(velocityX) > 400f) {
                            val flingDelta = (velocityX / (safeTrackW * 1.8f)).coerceIn(-0.35f, 0.35f)
                            val targetProgress = (dragProgress + flingDelta).coerceIn(0f, 1f)
                            val startVal = dragProgress
                            val duration = (kotlin.math.abs(flingDelta) * 550f).toInt().coerceIn(160, 300)

                            flingJob = coroutineScope.launch {
                                val anim = Animatable(startVal)
                                anim.animateTo(
                                    targetValue = targetProgress,
                                    animationSpec = tween(
                                        durationMillis = duration,
                                        easing = FastOutSlowInEasing
                                    )
                                ) {
                                    dragProgress = value
                                }
                                val targetMs = (targetProgress * currentTotalDur).toLong().coerceIn(0L, currentTotalDur)
                                pendingSeekMs = targetMs
                                isDragging = false
                                currentOnScrubbingChanged(false)
                                currentOnSeek(targetMs)
                            }
                        } else {
                            val targetMs = (dragProgress * currentTotalDur).toLong().coerceIn(0L, currentTotalDur)
                            pendingSeekMs = targetMs
                            isDragging = false
                            currentOnScrubbingChanged(false)
                            currentOnSeek(targetMs)
                        }
                    },
                    onDragCancel = {
                        flingJob?.cancel()
                        isDragging = false
                        currentOnScrubbingChanged(false)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        flingJob?.cancel()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)

                        if (isFirstDrag) {
                            isFirstDrag = false
                            lastUptime = change.uptimeMillis
                            return@detectDragGestures
                        }

                        val dt = if (lastUptime > 0L) (change.uptimeMillis - lastUptime).coerceIn(8L, 50L) else 16L
                        lastUptime = change.uptimeMillis

                        val dx = dragAmount.x
                        val speed = kotlin.math.abs(dx) / dt.toFloat()

                        // Non-linear velocity multiplier for relative scrubbing:
                        // Slow precision scrubbing (< 0.15 px/ms) -> 0.65x for fine control
                        // Fast scrubbing (> 1.0 px/ms) -> smoothly scales up to 3.0x
                        val velocityMultiplier = (0.65f + speed * 0.90f).coerceIn(0.65f, 3.00f)

                        val safeTrackW = if (trackWidthPx > 0f) trackWidthPx else maxOf(capsuleWidthPx * 0.65f, 1f)
                        val step = (dx / safeTrackW) * velocityMultiplier

                        dragProgress = (dragProgress + step).coerceIn(0f, 1f)
                    }
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Current Elapsed Time
        Text(
            text = formatDuration(displayPositionMs),
            color = Color.White.copy(alpha = 0.90f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Continuous iOS bar without any circular knob
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .onSizeChanged { trackWidthPx = it.width.toFloat() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedTrackHeight)
            ) {
                val centerY = size.height / 2f
                val strokeH = size.height
                val totalW = size.width

                // 1. Inactive background bar (soft translucent white)
                drawLine(
                    color = Color.White.copy(alpha = 0.24f),
                    start = Offset(0f, centerY),
                    end = Offset(totalW, centerY),
                    strokeWidth = strokeH,
                    cap = StrokeCap.Round
                )

                // 2. Active progress bar (clean pure white, zero knob)
                val activeW = (totalW * progress).coerceIn(0f, totalW)
                if (activeW > 0f) {
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, centerY),
                        end = Offset(activeW, centerY),
                        strokeWidth = strokeH,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Remaining Negative Time (e.g. -45:20)
        val remainingMs = maxOf(0L, totalDuration - displayPositionMs)
        Text(
            text = "-${formatDuration(remainingMs)}",
            color = Color.White.copy(alpha = 0.90f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

enum class GestureHudType {
    BRIGHTNESS,
    VOLUME
}

/** Подготовленная подпись капсулы озвучки — считается один раз, а не на каждую пересборку. */
private data class PlayerVoiceChip(
    val names: List<String>,
    val index: Int,
    val label: String,
    val canSwitch: Boolean,
    val iconResId: Int? = null
)

/**
 * Изолированный хост HUD громкости/яркости.
 *
 * Анимация видимости и прогресс читаются ЗДЕСЬ, а не в корне плеера: иначе каждое
 * движение пальца по экрану и каждый кадр появления/скрытия HUD пересобирали бы
 * весь слой контролов (кнопки, капсулы, стеклянные поверхности) и роняли кадры видео.
 */
@Composable
private fun PlayerGestureHud(
    visibility: Animatable<Float, AnimationVector1D>,
    type: State<GestureHudType>,
    progress: State<Float>,
    modifier: Modifier = Modifier
) {
    if (visibility.value <= 0.001f) return

    Box(
        modifier = modifier.graphicsLayer {
            val v = visibility.value.coerceIn(0f, 1f)
            alpha = v
            scaleX = androidx.compose.ui.util.lerp(0.70f, 1.0f, v)
            scaleY = androidx.compose.ui.util.lerp(0.70f, 1.0f, v)
            translationY = androidx.compose.ui.util.lerp(-46.dp.toPx(), 0f, v)
            transformOrigin = TransformOrigin(0.5f, 0f)
        }
    ) {
        IosTopGestureHud(
            type = type.value,
            progress = progress.value
        )
    }
}

@Composable
private fun IosTopGestureHud(
    type: GestureHudType,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hudProgressAnimation"
    )

    val iconRes = when (type) {
        GestureHudType.BRIGHTNESS -> when {
            clampedProgress < 0.35f -> R.drawable.ic_sf_sun_min_fill
            clampedProgress < 0.75f -> R.drawable.ic_sf_sun_max_fill
            else -> R.drawable.ic_sf_sun_max_fill
        }
        GestureHudType.VOLUME -> when {
            clampedProgress <= 0.001f -> R.drawable.ic_sf_speaker_slash_fill
            clampedProgress < 0.40f -> R.drawable.ic_sf_speaker_wave_1_fill
            else -> R.drawable.ic_sf_speaker_wave_3_fill
        }
    }

    val label = if (type == GestureHudType.BRIGHTNESS) "Яркость" else "Громкость"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .width(230.dp)
            .height(38.dp)
            .liquidGlassSurface(
                style = LiquidGlassTokens.Bar,
                shape = Capsule(),
                containerColor = LiquidGlassSurfaceDark
            )
            .clip(Capsule())
            .padding(horizontal = 14.dp)
    ) {
        AnimatedContent(
            targetState = iconRes,
            transitionSpec = {
                fadeIn(tween(90)).togetherWith(fadeOut(tween(90)))
            },
            label = "hudIconTransition"
        ) { currentIconRes ->
            Icon(
                painter = painterResource(currentIconRes),
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Horizontal continuous progress track with solid white fill and spring animation
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.5.dp)
                .clip(Capsule())
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .clip(Capsule())
                    .background(Color.White)
            )
        }
    }
}

/**
 * Центральная кнопка Play/Pause.
 *
 * Читает isPlaying/playbackState внутри собственной области композиции: буферизация,
 * пауза и старт воспроизведения больше не пересобирают весь экран плеера.
 */
@Composable
private fun PlayerPlayPauseButton(
    player: Player,
    isRecoveringStream: Boolean
) {
    val isPlaying by player.isPlayingAsState()
    val playbackState by player.playbackStateAsState()

    LiquidGlassFloatingButton(
        onClick = { if (player.isPlaying) player.pause() else player.play() },
        shape = CircleShape,
        modifier = Modifier.size(80.dp)
    ) {
        if (playbackState == Player.STATE_BUFFERING || isRecoveringStream) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(38.dp))
        } else {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (scaleIn(
                        initialScale = 0.70f,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(180)))
                        .togetherWith(
                            scaleOut(
                                targetScale = 0.70f,
                                animationSpec = tween(140)
                            ) + fadeOut(animationSpec = tween(140))
                        )
                },
                label = "PlayPauseTransition"
            ) { playing ->
                if (playing) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sf_pause_fill),
                        contentDescription = "Пауза",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sf_play_fill),
                        contentDescription = "Воспроизвести",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Player.isPlayingAsState(): State<Boolean> {
    val state = remember { mutableStateOf(isPlaying) }
    DisposableEffect(this) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                state.value = playing
            }
        }
        addListener(listener)
        onDispose { removeListener(listener) }
    }
    return state
}

@Composable
private fun Player.playbackStateAsState(): State<Int> {
    val state = remember { mutableIntStateOf(playbackState) }
    DisposableEffect(this) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(s: Int) {
                state.intValue = s
            }
        }
        addListener(listener)
        onDispose { removeListener(listener) }
    }
    return state
}

@Composable
private fun Player.currentPositionAsState(isPlaying: Boolean): State<Long> {
    val pos = remember { mutableLongStateOf(currentPosition) }
    DisposableEffect(this) {
        val listener = object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                pos.longValue = newPosition.positionMs
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                pos.longValue = currentPosition
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                pos.longValue = currentPosition
            }
        }
        addListener(listener)
        onDispose { removeListener(listener) }
    }
    LaunchedEffect(this, isPlaying) {
        if (!isPlaying) {
            pos.longValue = this@currentPositionAsState.currentPosition
            return@LaunchedEffect
        }
        while (isActive) {
            val now = this@currentPositionAsState.currentPosition
            if (pos.longValue != now) pos.longValue = now
            delay(200)
        }
    }
    return pos
}

@Composable
private fun Player.durationAsState(): State<Long> {
    val dur = remember { mutableLongStateOf(duration) }
    DisposableEffect(this) {
        val listener = object : Player.Listener {
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                if (!timeline.isEmpty) {
                    dur.longValue = timeline.getWindow(0, androidx.media3.common.Timeline.Window()).durationMs
                }
            }
        }
        addListener(listener)
        onDispose { removeListener(listener) }
    }
    return dur
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    // Без String.format: он заметно дороже и аллоцирует на каждый вызов.
    val mm = if (minutes < 10L) "0$minutes" else minutes.toString()
    val ss = if (seconds < 10L) "0$seconds" else seconds.toString()
    return if (hours > 0) {
        val hh = if (hours < 10L) "0$hours" else hours.toString()
        "$hh:$mm:$ss"
    } else {
        "$mm:$ss"
    }
}

/**
 * Intelligently selects the best playable video URL from available quality variants.
 * 1. If preferredQuality is specified and available in qualityMap, use it.
 * 2. If 'Авто', select the optimal resolution up to 1080p (1080p -> 720p -> 480p -> 360p).
 * 3. Fallback to any available quality variant or direct URL.
 */
private fun selectBestPlayableUrl(
    qualityMap: Map<String, String>,
    preferredQuality: String,
    fallbackUrl: String? = null
): String {
    // 1. If user selected a specific non-Auto quality, find exact or height-matched variant
    if (preferredQuality != "Авто" && preferredQuality != "Спрашивать") {
        val direct = qualityMap[preferredQuality]
        if (!direct.isNullOrBlank()) {
            return direct
        }
        val targetHeight = qualityHeight(preferredQuality)
        if (targetHeight > 0) {
            val matchingKey = qualityMap.keys.firstOrNull { k ->
                k != "Авто" && qualityHeight(k) == targetHeight && !qualityMap[k].isNullOrBlank()
            }
            if (matchingKey != null) {
                return qualityMap[matchingKey]!!
            }
        }
    }

    // 2. For 'Авто', select highest resolution up to 1080p (mobile standard)
    val orderedKeys = listOf("1080p", "1080", "720p", "720", "480p", "480", "360p", "360")
    for (key in orderedKeys) {
        val u = qualityMap[key]
        if (!u.isNullOrBlank()) {
            return u
        }
    }

    // 3. Any non-Auto quality
    val nonAuto = qualityMap.entries.firstOrNull {
        it.key != "Авто" && it.value.isNotBlank()
    }?.value
    if (!nonAuto.isNullOrBlank()) {
        return nonAuto
    }

    // 4. Any quality (including 4K/2K)
    val anyQuality = qualityMap.values.firstOrNull { it.isNotBlank() }
    if (!anyQuality.isNullOrBlank()) {
        return anyQuality
    }

    // 5. Fallback URL
    if (!fallbackUrl.isNullOrBlank()) {
        return fallbackUrl
    }

    // 6. Absolute last resort
    return ""
}

/**
 * Собирает упорядоченную цепочку резервных URL одного потока:
 * выбранное качество -> остальные варианты (по убыванию) -> master/прямой URL.
 * Нужна, чтобы при ошибке плеер переходил на следующий вариант, а не повторял тот же.
 */
private fun buildPlayableCandidates(
    qualityMap: Map<String, String>,
    preferredQuality: String,
    fallbackUrl: String?
): List<String> {
    val ordered = linkedSetOf<String>()

    fun add(url: String?) {
        val clean = url?.trim().orEmpty()
        if (clean.startsWith("http") && !clean.contains("\n") && !clean.contains("<")) {
            ordered.add(clean)
        }
    }

    add(selectBestPlayableUrl(qualityMap, preferredQuality, fallbackUrl))

    val qualityEntries = qualityMap.entries
        .filter { it.key != "Авто" && it.value.isNotBlank() }
        .sortedByDescending { qualityHeight(it.key) }
    qualityEntries.forEach { add(it.value) }

    add(fallbackUrl)
    return ordered.toList()
}

private fun qualityHeight(label: String): Int {
    val normalized = label.uppercase(Locale.ROOT)
    return when {
        normalized.contains("4K") || normalized.contains("2160") -> 2160
        normalized.contains("2K") || normalized.contains("1440") -> 1440
        else -> normalized.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
    }
}

private data class AudioTrackCandidate(
    val group: Tracks.Group,
    val trackIndex: Int,
    val overallIndex: Int,
    val format: Format,
    val label: String,
    val language: String,
    val idStr: String,
    val isRussian: Boolean,
    val isEnglishOrOriginal: Boolean
)

private fun extractAudioIndex(raw: String?, isUri: Boolean = false): Int? {
    if (raw.isNullOrBlank()) return null
    if (isUri) {
        val uriRegex = Regex("""index-a(\d+)""", RegexOption.IGNORE_CASE)
        val match = uriRegex.find(raw)
        if (match != null) {
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num >= 1) return num - 1
        }
    }
    val pattern = Regex("""(?:^|[^a-zA-Z0-9])(?:[a-zA-Z]{2,4}|audio[_-]?|track[_-]?)(\d+)(?:$|[^a-zA-Z0-9])""", RegexOption.IGNORE_CASE)
    val match = pattern.find(raw)
    if (match != null) {
        val num = match.groupValues[1].toIntOrNull()
        if (num != null) return num
    }
    return null
}

private fun enrichAudioVariants(
    variants: List<Map<String, Any>>,
    translations: List<AllohaTranslation>,
    isDedicatedIframe: Boolean = false
): List<Map<String, Any>> {
    if (variants.isEmpty() || isDedicatedIframe) return variants
    val hasDistinctIframes = translations.mapNotNull { it.iframeUrl.takeIf(String::isNotBlank) }.distinct().size > 1
    if (hasDistinctIframes) return variants
    return variants.mapIndexed { index, variant ->
        val rawTitle = (variant["title"] as? String)?.trim().orEmpty()
        val isGeneric = rawTitle.isBlank() ||
                rawTitle.startsWith("Дорожка", ignoreCase = true) ||
                rawTitle.startsWith("Озвучка", ignoreCase = true) ||
                rawTitle.startsWith("Track", ignoreCase = true) ||
                rawTitle.equals("Unknown", ignoreCase = true)
        if (isGeneric && index < translations.size) {
            val trans = translations[index]
            val mutable = HashMap(variant)
            mutable["title"] = trans.name
            mutable["translationId"] = trans.id
            mutable
        } else {
            variant
        }
    }
}

/**
 * Intelligently selects the exact embedded audio track matching the requested translation.
 * Achieves 1:1 behavioral parity with iOS `selectAudioTrackInPlayer` and `PlaybackHlsRewriter`.
 */
@OptIn(UnstableApi::class)
private fun ExoPlayer.selectAudioTrackForVoice(
    voice: String,
    voices: List<String> = emptyList(),
    explicitIndex: Int? = null
): Boolean? {
    val groups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
    var runningIndex = 0
    val candidates = groups.flatMap { group ->
        (0 until group.length)
            .filter { group.isTrackSupported(it) }
            .map { trackIndex ->
                val format = group.getTrackFormat(trackIndex)
                val label = format.label.orEmpty().trim()
                val lang = format.language.orEmpty().trim().lowercase(Locale.ROOT)
                val idStr = format.id.orEmpty().trim()
                val fullMeta = "$label $lang $idStr".trim()
                val isEng = AllohaTranslationMatcher.isOriginalOrEnglishTranslation(fullMeta) ||
                        lang.startsWith("en") || label.contains("eng", ignoreCase = true) ||
                        label.contains("orig", ignoreCase = true) || label.contains("англ", ignoreCase = true)
                val isRus = lang.startsWith("ru") || label.contains("rus", ignoreCase = true) ||
                        label.contains("рус", ignoreCase = true) || label.contains("дубл", ignoreCase = true) ||
                        AllohaTranslationMatcher.detectLanguageTag(fullMeta) == "rus"
                AudioTrackCandidate(
                    group = group,
                    trackIndex = trackIndex,
                    overallIndex = runningIndex++,
                    format = format,
                    label = label,
                    language = lang,
                    idStr = idStr,
                    isRussian = isRus,
                    isEnglishOrOriginal = isEng
                )
            }
    }
    if (candidates.isEmpty()) return null

    val targetVoiceIndex: Int? = explicitIndex
        ?: voices.indexOfFirst { AllohaTranslationMatcher.matches(it, voice, exactOnly = true) }.takeIf { it >= 0 }
        ?: voices.indexOfFirst { AllohaTranslationMatcher.matches(it, voice, exactOnly = false) }.takeIf { it >= 0 }
        ?: extractAudioIndex(voice)

    // 1. Exact / fuzzy match by authentic track label or id in HLS metadata
    var match: AudioTrackCandidate? = candidates.firstOrNull {
        AllohaTranslationMatcher.matches(it.label, voice, exactOnly = true) ||
                AllohaTranslationMatcher.matches(it.idStr, voice, exactOnly = true)
    } ?: candidates.firstOrNull {
        AllohaTranslationMatcher.matches(it.label, voice, exactOnly = false) ||
                AllohaTranslationMatcher.matches(it.idStr, voice, exactOnly = false)
    }

    // 2. Match by embedded index in label/URI/ID (e.g. rus0, audio0, index-a1)
    if (match == null && targetVoiceIndex != null) {
        match = candidates.firstOrNull { cand ->
            val idx = extractAudioIndex(cand.label)
                ?: extractAudioIndex(cand.idStr, isUri = true)
                ?: extractAudioIndex(cand.language)
            idx == targetVoiceIndex
        }
    }

    // 3. Match by position in candidates list (accounting for English original track 0)
    if (match == null && targetVoiceIndex != null) {
        val isLookingForRussian = AllohaTranslationMatcher.isRussianTranslation(voice)
        val candidate0IsEnglish = candidates.isNotEmpty() && candidates[0].isEnglishOrOriginal
        if (isLookingForRussian && candidate0IsEnglish) {
            val nonEnglishCandidates = candidates.filter { !it.isEnglishOrOriginal }
            if (targetVoiceIndex in nonEnglishCandidates.indices) {
                match = nonEnglishCandidates[targetVoiceIndex]
            } else if (targetVoiceIndex + 1 in candidates.indices) {
                match = candidates[targetVoiceIndex + 1]
            }
        } else if (targetVoiceIndex in candidates.indices) {
            match = candidates[targetVoiceIndex]
        }
    }

    // 4. Language tag match if unambiguous (iOS Step 6)
    if (match == null) {
        if (AllohaTranslationMatcher.isOriginalOrEnglishTranslation(voice)) {
            match = candidates.firstOrNull { it.isEnglishOrOriginal }
                ?: if (candidates.size > 1) candidates[1] else null
        } else if (AllohaTranslationMatcher.isRussianTranslation(voice)) {
            match = candidates.firstOrNull { it.isRussian }
                ?: candidates.firstOrNull { !it.isEnglishOrOriginal }
                ?: candidates.firstOrNull()
        }
    }

    // 5. Fallback for Russian target: if candidate 0 is English and there are multiple tracks, pick candidate 1 (iOS Step 7)
    if (match == null && AllohaTranslationMatcher.isRussianTranslation(voice) && candidates.size > 1 && candidates[0].isEnglishOrOriginal) {
        match = candidates[1]
    }

    // 6. Guaranteed fallback: select candidate 0
    val resolved = match ?: candidates.firstOrNull() ?: return null

    android.util.Log.d(
        "PlayerScreen",
        "selectAudioTrackForVoice: target='$voice', total=${candidates.size}, chosen='${resolved.label}' (overall=${resolved.overallIndex}, track=${resolved.trackIndex}, lang=${resolved.language}, isRus=${resolved.isRussian}, isEng=${resolved.isEnglishOrOriginal})"
    )

    val selectedTrackCount = groups.sumOf { group ->
        (0 until group.length).count(group::isTrackSelected)
    }
    if (resolved.group.isTrackSelected(resolved.trackIndex) && selectedTrackCount == 1) return true

    trackSelectionParameters = trackSelectionParameters
        .buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        .setOverrideForType(TrackSelectionOverride(resolved.group.mediaTrackGroup, resolved.trackIndex))
        .setPreferredAudioLanguages("ru", "rus", "ru-RU")
        .build()
    return false
}

/**
 * Creates a MediaItem configured with explicit HLS/MP4/MPD mime types.
 * Ensures that ExoPlayer always uses HlsMediaSource even for CDN tokens lacking an .m3u8 extension.
 */
private fun createPlayableMediaItem(
    url: String,
    subtitles: List<MediaItem.SubtitleConfiguration> = emptyList()
): MediaItem {
    val builder = MediaItem.Builder().setUri(url)
    val lower = url.lowercase(Locale.ROOT)
    when {
        lower.contains(".mp4") -> builder.setMimeType(MimeTypes.VIDEO_MP4)
        lower.contains(".mpd") -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
        else -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
    }
    if (subtitles.isNotEmpty()) {
        builder.setSubtitleConfigurations(subtitles)
    }
    return builder.build()
}

/**
 * Detects smooth YouTube/iOS style two-finger pinch-to-zoom gestures.
 * Consumes events only when 2+ fingers are actively pinching, allowing single-finger
 * taps and vertical drags (brightness/volume) to pass through without conflict.
 */
private suspend fun PointerInputScope.detectSmoothPinchToZoom(
    onPinchStart: () -> Unit,
    onPinch: (zoomMultiplier: Float) -> Unit,
    onPinchEnd: () -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var isPinching = false

        do {
            val event = awaitPointerEvent()
            val downPointers = event.changes.filter { it.pressed }

            if (downPointers.size >= 2) {
                if (!isPinching) {
                    isPinching = true
                    onPinchStart()
                }

                val p0 = downPointers[0]
                val p1 = downPointers[1]

                val currentDistance = (p0.position - p1.position).getDistance()
                val previousDistance = (p0.previousPosition - p1.previousPosition).getDistance()

                if (previousDistance > 0f && currentDistance > 0f) {
                    val zoomMultiplier = currentDistance / previousDistance
                    p0.consume()
                    p1.consume()
                    onPinch(zoomMultiplier)
                }
            } else if (isPinching) {
                break
            }
        } while (event.changes.any { it.pressed })

        if (isPinching) {
            onPinchEnd()
        }
    }
}


