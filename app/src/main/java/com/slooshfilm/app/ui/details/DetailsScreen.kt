package com.slooshfilm.app.ui.details

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import androidx.compose.ui.res.painterResource
import com.slooshfilm.app.R
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlin.math.min
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.slooshfilm.app.data.alloha.AllohaApiResult
import com.slooshfilm.app.data.alloha.AllohaRepository
import com.slooshfilm.app.data.alloha.AllohaSessionHolder
import com.slooshfilm.app.data.api.MediaDetailsDto
import com.slooshfilm.app.data.api.StudioBrand
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.api.TrailerDto
import com.slooshfilm.app.data.api.CrewMemberDto
import com.slooshfilm.app.data.api.MovieCollectionDto
import com.slooshfilm.app.data.api.RelatedStudioResponseDto
import com.slooshfilm.app.data.api.TvSeasonDto
import com.slooshfilm.app.data.api.TvEpisodeDetailsDto
import com.slooshfilm.app.data.api.canonicalMediaType
import com.slooshfilm.app.data.api.formatCountryName
import com.slooshfilm.app.data.repository.MoviesRepository
import com.slooshfilm.app.data.storage.FavoritesStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.lerp
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberCanvasBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.components.ErrorStateView
import com.slooshfilm.app.ui.components.iosProgressiveBlur
import com.slooshfilm.app.ui.components.InteractiveHighlight
import com.slooshfilm.app.ui.components.LiquidGlassDialog
import com.slooshfilm.app.ui.components.LiquidGlassFloatingButton
import com.slooshfilm.app.ui.components.LiquidGlassTokens
import com.slooshfilm.app.ui.components.liquidGlassInteractive
import com.slooshfilm.app.ui.components.LocalBackdrop
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.ProgressiveBlurTokens
import com.slooshfilm.app.ui.components.LocalGlassEnabled
import com.slooshfilm.app.ui.components.MediaCard
import com.slooshfilm.app.ui.components.ShimmerPlaceholder
import com.slooshfilm.app.ui.components.liquidGlassSurface
import com.slooshfilm.app.ui.components.rememberCutoutHorizontalPadding
import com.slooshfilm.app.ui.components.rememberNavBarBottomPadding
import com.slooshfilm.app.ui.theme.*
import com.slooshfilm.app.ui.util.rememberAdaptiveAmbientColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

// Shared hero fade used by both the real backdrop and its loading skeleton
private val detailsHeroFadeStops = arrayOf(
    0.00f to Color.Transparent,
    0.06f to Color.Black.copy(alpha = 0.40f),
    0.12f to Color.Black.copy(alpha = 0.85f),
    0.18f to Color.Black,
    0.35f to Color.Black,
    0.50f to Color.Black.copy(alpha = 0.80f),
    0.68f to Color.Black.copy(alpha = 0.45f),
    0.82f to Color.Black.copy(alpha = 0.20f),
    0.93f to Color.Black.copy(alpha = 0.06f),
    1.00f to Color.Transparent
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailsScreen(
    mediaId: String,
    mediaType: String,
    onBack: () -> Unit,
    onPlay: (season: Int?, episode: Int?, voice: String?, translationId: String?, iframeUrl: String?) -> Unit,
    onSimilarClick: (MediaDto) -> Unit,
    onGenreClick: (genre: String, mediaType: String?) -> Unit,
    onStudioClick: (studioId: String, studioName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { MoviesRepository.getInstance() }
    val allohaRepository = remember { AllohaRepository.getInstance(context) }
    val favoritesStore = remember { FavoritesStore.getInstance(context) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var details by remember { mutableStateOf<MediaDetailsDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detailsLoadAttempt by remember { mutableIntStateOf(0) }
    var forceRefreshAttempt by remember { mutableIntStateOf(-1) }
    var loadedDetailsIdentity by remember { mutableStateOf<Pair<String, String>?>(null) }
    val latestDetailsLoadIdentity by rememberUpdatedState(Triple(mediaId, mediaType, detailsLoadAttempt))

    fun requestDetailsRefresh() {
        val nextAttempt = detailsLoadAttempt + 1
        forceRefreshAttempt = nextAttempt
        detailsLoadAttempt = nextAttempt
    }

    // Favorites state
    val favorites by favoritesStore.favoritesFlow.collectAsStateWithLifecycle()
    val isFavorite = remember(favorites, details, mediaType) {
        val clean = details?.cleanId ?: mediaId.replace("tv_", "").replace("movie_", "")
        favoritesStore.isFavorite(clean, details?.type ?: mediaType)
    }

    // Alloha / Watch sheet state
    var showWatchSheet by remember { mutableStateOf(false) }
    var watchSheetSeason by remember { mutableStateOf<Int?>(null) }
    var watchSheetEpisode by remember { mutableStateOf<Int?>(null) }
    var allohaResult by remember { mutableStateOf<AllohaApiResult?>(null) }
    var isLoadingAlloha by remember { mutableStateOf(false) }
    var allohaError by remember { mutableStateOf<String?>(null) }

    // Offline download dialog state
    var showOfflineDownloadDialog by remember { mutableStateOf(false) }

    // Franchise collection state
    var movieCollection by remember { mutableStateOf<MovieCollectionDto?>(null) }

    // Related studio releases state
    var relatedStudio by remember { mutableStateOf<RelatedStudioResponseDto?>(null) }

    // TV Seasons and Episodes state
    var selectedSeasonNumber by remember { mutableIntStateOf(1) }
    var currentSeasonData by remember { mutableStateOf<TvSeasonDto?>(null) }
    var isLoadingSeasonEpisodes by remember { mutableStateOf(false) }
    val latestSeasonRequest by rememberUpdatedState(
        Pair(Triple(details?.id, details?.type, selectedSeasonNumber), detailsLoadAttempt)
    )

    // Description expandable state
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var canExpandDescription by remember { mutableStateOf(false) }
    val detailSideJobs = remember { mutableListOf<Job>() }

    LaunchedEffect(mediaId, mediaType, detailsLoadAttempt) {
        val requestedMediaId = mediaId
        val requestedMediaType = mediaType
        val requestedAttempt = detailsLoadAttempt
        val requestedIdentity = requestedMediaId to requestedMediaType
        val shouldPreserveCurrentDetails = loadedDetailsIdentity == requestedIdentity && details != null
        val shouldForceRefresh = requestedAttempt == forceRefreshAttempt
        detailSideJobs.forEach { it.cancel() }
        detailSideJobs.clear()
        if (!shouldPreserveCurrentDetails) {
            details = null
            loadedDetailsIdentity = null
            movieCollection = null
            relatedStudio = null
            allohaResult = null
            allohaError = null
            isLoadingAlloha = false
            currentSeasonData = null
            isLoadingSeasonEpisodes = false
            selectedSeasonNumber = 1
            isDescriptionExpanded = false
            canExpandDescription = false
            isLoading = true
            isRefreshing = false
        } else {
            isRefreshing = true
        }
        errorMessage = null
        try {
            val isTv = canonicalMediaType(mediaType) == "tv"
            val item = repository.getMediaDetails(mediaId, isTv, forceRefresh = shouldForceRefresh)
            currentCoroutineContext().ensureActive()
            if (item == null) {
                if (shouldPreserveCurrentDetails) {
                    Toast.makeText(context, "Не удалось обновить данные", Toast.LENGTH_SHORT).show()
                } else if (latestDetailsLoadIdentity == Triple(requestedMediaId, requestedMediaType, requestedAttempt)) {
                    errorMessage = "Не удалось загрузить данные. Проверьте соединение и попробуйте снова."
                }
                return@LaunchedEffect
            }
            details = item
            loadedDetailsIdentity = requestedIdentity
            val clean = item.cleanId
            fun isCurrentDetails(): Boolean =
                latestDetailsLoadIdentity == Triple(requestedMediaId, requestedMediaType, requestedAttempt) &&
                    details?.cleanId == clean

            // Load non-critical sections independently so they do not delay the main details screen.
            movieCollection = item.collection ?: if (shouldPreserveCurrentDetails) movieCollection else null
            if (!shouldPreserveCurrentDetails) {
                relatedStudio = null
                currentSeasonData = null
                allohaResult = null
                allohaError = null
            }
            isLoadingAlloha = item.isTvSeries && allohaResult == null

            if (!item.isTvSeries) {
                detailSideJobs += scope.launch {
                    val collection = repository.getMovieCollection(clean, forceRefresh = shouldForceRefresh) ?: item.collection
                    if (isCurrentDetails() && (collection != null || !shouldPreserveCurrentDetails)) {
                        movieCollection = collection
                    }
                }
            }

            val resolvedType = item.type ?: if (isTv) "tv" else "movie"
            detailSideJobs += scope.launch {
                val related = repository.getRelatedByStudio(resolvedType, clean, forceRefresh = shouldForceRefresh)
                if (isCurrentDetails() && (related != null || !shouldPreserveCurrentDetails)) {
                    relatedStudio = related
                }
            }

            // Fetch playback options independently so a slow stream lookup cannot hold the details screen on its skeleton.
            if (item.isTvSeries && allohaResult == null) {
                val kpId = item.kpId ?: if (mediaId.startsWith("kp_")) clean.toIntOrNull() else null
                val tmdbId = item.tmdbId ?: if (!mediaId.startsWith("kp_")) clean.toIntOrNull() else null
                val title = item.title ?: item.originalTitle ?: ""
                detailSideJobs += scope.launch {
                    try {
                        val result = allohaRepository.fetchAllohaData(
                            mediaId = clean,
                            explicitKpId = kpId,
                            tmdbId = tmdbId,
                            imdbId = item.imdbId,
                            title = title,
                            originalTitle = item.originalTitle,
                            year = item.year,
                            isTv = item.isTvSeries
                        )
                        currentCoroutineContext().ensureActive()
                        if (isCurrentDetails()) {
                            allohaResult = result
                            if (result == null) allohaError = "Видеопоток временно недоступен"
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        if (isCurrentDetails()) allohaError = "Видеопоток временно недоступен"
                    } finally {
                        if (isCurrentDetails()) isLoadingAlloha = false
                    }
                }
            } else {
                isLoadingAlloha = false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (latestDetailsLoadIdentity == Triple(requestedMediaId, requestedMediaType, requestedAttempt)) {
                if (shouldPreserveCurrentDetails) {
                    Toast.makeText(context, "Не удалось обновить данные", Toast.LENGTH_SHORT).show()
                } else {
                    errorMessage = "Не удалось загрузить данные. Проверьте соединение и попробуйте снова."
                }
            }
        } finally {
            if (latestDetailsLoadIdentity == Triple(requestedMediaId, requestedMediaType, requestedAttempt)) {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    // Load episodes for selected season
    LaunchedEffect(details?.id, details?.type, selectedSeasonNumber, detailsLoadAttempt) {
        val requestKey = Pair(Triple(details?.id, details?.type, selectedSeasonNumber), detailsLoadAttempt)
        val clean = details?.cleanId ?: mediaId.replace("tv_", "").replace("movie_", "")
        if (details?.isTvSeries == true && clean.isNotBlank()) {
            if (currentSeasonData?.seasonNumber != selectedSeasonNumber) currentSeasonData = null
            isLoadingSeasonEpisodes = true
            try {
                val season = repository.getSeason(
                    clean,
                    selectedSeasonNumber,
                    forceRefresh = detailsLoadAttempt == forceRefreshAttempt
                )
                currentCoroutineContext().ensureActive()
                currentSeasonData = season
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                currentSeasonData = null
            } finally {
                if (latestSeasonRequest == requestKey) isLoadingSeasonEpisodes = false
            }
        }
    }

    val allSeasonNumbers = remember(details, allohaResult) {
        val metaSeasons = details?.seasons?.mapNotNull { it.seasonNumber }?.filter { it > 0 } ?: emptyList()
        val streamSeasons = allohaResult?.seasons?.map { it.season }?.filter { it > 0 } ?: emptyList()
        val combined = (metaSeasons + streamSeasons).distinct().sorted()
        if (combined.isNotEmpty()) combined else listOf(1)
    }

    LaunchedEffect(allSeasonNumbers) {
        if (!allSeasonNumbers.contains(selectedSeasonNumber) && allSeasonNumbers.isNotEmpty()) {
            selectedSeasonNumber = allSeasonNumbers.first()
        }
    }

    fun loadAndShowWatchSelector(initialSeason: Int? = null, initialEpisode: Int? = null) {
        watchSheetSeason = initialSeason
        watchSheetEpisode = initialEpisode
        showWatchSheet = true
        if (allohaResult == null && !isLoadingAlloha) {
            val requestIdentity = Triple(mediaId, mediaType, detailsLoadAttempt)
            val item = details
            isLoadingAlloha = true
            allohaError = null
            detailSideJobs += scope.launch {
                try {
                    val kpId = item?.kpId ?: if (mediaId.startsWith("kp_")) (item?.cleanId ?: mediaId).removePrefix("kp_").toIntOrNull() else null
                    val tmdbId = item?.tmdbId ?: if (!mediaId.startsWith("kp_")) (item?.cleanId ?: mediaId).replace("tv_", "").replace("movie_", "").toIntOrNull() else null
                    val imdbId = item?.imdbId
                    val title = item?.title ?: item?.originalTitle ?: ""
                    val origTitle = item?.originalTitle
                    val year = item?.year
                    val res = allohaRepository.fetchAllohaData(
                        mediaId = item?.cleanId ?: mediaId,
                        explicitKpId = kpId,
                        tmdbId = tmdbId,
                        imdbId = imdbId,
                        title = title,
                        originalTitle = origTitle,
                        year = year,
                        isTv = item?.isTvSeries
                    )
                    currentCoroutineContext().ensureActive()
                    if (latestDetailsLoadIdentity == requestIdentity) {
                        allohaResult = res
                        if (res == null) {
                            allohaError = "Видеопоток временно недоступен"
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (latestDetailsLoadIdentity == requestIdentity) {
                        allohaError = "Ошибка подключения к источнику"
                    }
                } finally {
                    if (latestDetailsLoadIdentity == requestIdentity) {
                        isLoadingAlloha = false
                    }
                }
            }
        }
    }

    val localStorage = remember { com.slooshfilm.app.data.storage.LocalStorage(context) }
    var autoAdvanceBackdrops by remember { mutableStateOf(localStorage.getAutoAdvanceBackdrops()) }
    DisposableEffect(localStorage) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "auto_advance_backdrops") {
                autoAdvanceBackdrops = localStorage.getAutoAdvanceBackdrops()
            }
        }
        localStorage.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { localStorage.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val backdropUrls = remember(details) { details?.displayBackdropUrls ?: emptyList() }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { backdropUrls.size.coerceAtLeast(1) })
    val isAutoAdvance = autoAdvanceBackdrops && backdropUrls.size > 1 && !showWatchSheet && !showOfflineDownloadDialog
    val autoProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(details?.id) {
        pagerState.scrollToPage(0)
        autoProgress.snapTo(0f)
    }

    LaunchedEffect(isAutoAdvance, backdropUrls.size, lifecycleOwner) {
        if (!isAutoAdvance || backdropUrls.size <= 1) {
            autoProgress.snapTo(0f)
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                if (pagerState.isScrollInProgress) {
                    autoProgress.snapTo(0f)
                    snapshotFlow { pagerState.isScrollInProgress }.first { !it }
                    kotlinx.coroutines.delay(250)
                }

                autoProgress.snapTo(0f)
                val result = autoProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 4000, easing = androidx.compose.animation.core.LinearEasing)
                )

                if (result.endReason == androidx.compose.animation.core.AnimationEndReason.Finished && !pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.targetPage + 1) % backdropUrls.size
                    autoProgress.snapTo(0f)
                    pagerState.animateScrollToPage(
                        page = nextPage,
                        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                    )
                    kotlinx.coroutines.delay(150)
                }
            }
        }
    }
    val currentBackdrop = backdropUrls.getOrNull(pagerState.currentPage)
        ?: details?.getDisplayBackdropUrl(isLowQuality = false)
        ?: details?.getDisplayPosterUrl(isLowQuality = false)
    val ambientColor = rememberAdaptiveAmbientColor(currentBackdrop)

    val targetEffectiveBg = remember(ambientColor) {
        if (ambientColor != Color.Transparent) {
            lerp(Color.Black, ambientColor, 0.35f)
        } else {
            BackgroundDark
        }
    }
    val effectiveBg by animateColorAsState(
        targetValue = targetEffectiveBg,
        animationSpec = tween(durationMillis = 400),
        label = "effectiveBg"
    )

    val contentBackdrop = rememberLayerBackdrop {
        drawRect(effectiveBg)
        drawContent()
    }
    val sceneBackdrop = rememberLayerBackdrop {
        drawRect(effectiveBg)
        drawContent()
    }
    val descriptionBackdrop = rememberLayerBackdrop {
        drawRect(effectiveBg)
        drawContent()
    }
    // Flat page background behind the content-level glass elements (genres, studios,
    // seasons, download button). They live inside the content capture layer, so they
    // must sample a backdrop that does not contain themselves.
    val glassBackdrop = rememberCanvasBackdrop {
        drawRect(effectiveBg)
    }
    // Hero backdrop poster captured in its own layer so the pagination indicator pill
    // refracts the real backdrop image behind it instead of an empty backdrop.
    val heroBackdrop = rememberLayerBackdrop {
        drawRect(effectiveBg)
        drawContent()
    }

    val rawStatusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600
    val statusBarPadding = if (rawStatusBarPadding > 0.dp) rawStatusBarPadding else (if (isLandscape) 8.dp else 0.dp)
    val cutoutPadding = rememberCutoutHorizontalPadding()
    val detailsHorizontalPadding = 20.dp + cutoutPadding
    val heroHeight = if (isLandscape) 240.dp else if (isTablet) 320.dp else 365.dp
    val isProgressiveBlurEnabled = LocalProgressiveBlur.current
    val isGlassEnabled = LocalGlassEnabled.current
    val headerBlurHeight = statusBarPadding + (if (isLandscape) 100.dp else 160.dp)
    val navBarBottomPadding = rememberNavBarBottomPadding()
    val bottomContentPadding = if (isLandscape) navBarBottomPadding + 48.dp else navBarBottomPadding + 72.dp

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val scrollThresholdPx = remember(density) { with(density) { 370.dp.toPx() } }

    val isLogoAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > scrollThresholdPx
        }
    }

    val contentLogoAlpha by animateFloatAsState(
        targetValue = if (isLogoAtTop) 0f else 1f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "contentLogoAlpha"
    )

    val topLogoAlpha by animateFloatAsState(
        targetValue = if (isLogoAtTop) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "topLogoAlpha"
    )

    val topGradientAlpha by animateFloatAsState(
        targetValue = if (isLogoAtTop) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "topGradientAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(effectiveBg)
    ) {
        // 1. Scene Area captured in sceneBackdrop (contains scrollable content AND progressive blur header)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isGlassEnabled) Modifier.layerBackdrop(sceneBackdrop) else Modifier)
        ) {
            // Scrollable Content Area captured in contentBackdrop
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = ::requestDetailsRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isGlassEnabled) Modifier.layerBackdrop(contentBackdrop) else Modifier)
            ) {
            if (isLoading) {
                DetailsLoadingSkeleton(modifier = Modifier.fillMaxSize(), heroHeight = heroHeight)
            } else if (details != null) {
                val item = details!!

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomContentPadding)
            ) {
                // Header with Backdrop Carousel fading out to solid background (iOS 1:1)
                item(key = "details-backdrop-hero", contentType = "details-backdrop-hero") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroHeight)
                            .then(if (isGlassEnabled) Modifier.layerBackdrop(heroBackdrop) else Modifier)
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colorStops = detailsHeroFadeStops
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    ) {
                        if (backdropUrls.size > 1) {
                            androidx.compose.foundation.pager.HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                pageSpacing = 0.dp,
                                beyondViewportPageCount = 1,
                                key = { page -> backdropUrls.getOrNull(page) ?: page }
                            ) { page ->
                                val pageBackdrop = backdropUrls.getOrNull(page) ?: currentBackdrop
                                var isBackdropLoading by remember(pageBackdrop) {
                                    mutableStateOf(!pageBackdrop.isNullOrBlank())
                                }
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = pageBackdrop,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        onState = { state ->
                                            when (state) {
                                                is coil.compose.AsyncImagePainter.State.Loading -> isBackdropLoading = true
                                                is coil.compose.AsyncImagePainter.State.Success,
                                                is coil.compose.AsyncImagePainter.State.Error -> isBackdropLoading = false
                                                else -> Unit
                                            }
                                        }
                                    )
                                    if (isBackdropLoading) {
                                        ShimmerPlaceholder(modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp)
                                    }
                                }
                            }
                        } else {
                            var isBackdropLoading by remember(currentBackdrop) {
                                mutableStateOf(!currentBackdrop.isNullOrBlank())
                            }
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = currentBackdrop,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    onState = { state ->
                                        when (state) {
                                            is coil.compose.AsyncImagePainter.State.Loading -> isBackdropLoading = true
                                            is coil.compose.AsyncImagePainter.State.Success,
                                            is coil.compose.AsyncImagePainter.State.Error -> isBackdropLoading = false
                                            else -> Unit
                                        }
                                    }
                                )
                                if (isBackdropLoading) {
                                    ShimmerPlaceholder(modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp)
                                }
                            }
                        }
                    }
                }

                // Info Section
                item(key = "details-info-content", contentType = "details-info-content") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-30).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Details Header Content (padded 20.dp horizontally)
                        Column(
                            modifier = Modifier
                                .widthIn(max = if (isLandscape || isTablet) 580.dp else Dp.Unspecified)
                                .fillMaxWidth()
                                .padding(horizontal = detailsHorizontalPadding),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                        // 1. Backdrop Page Indicator (pills)
                        if (backdropUrls.size > 1) {
                            BackdropPageIndicator(
                                count = backdropUrls.size,
                                selectedIndex = pagerState.targetPage,
                                progress = autoProgress.value,
                                isAutoAdvance = isAutoAdvance,
                                onPageSelected = { page ->
                                    scope.launch {
                                        autoProgress.snapTo(0f)
                                        pagerState.animateScrollToPage(
                                            page = page,
                                            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                                        )
                                    }
                                },
                                modifier = Modifier.padding(bottom = 12.dp),
                                backdrop = heroBackdrop
                            )
                        }

                        // 2. Logo or Title (fades out and scales down as user scrolls towards top)
                        val logoUrl = item.displayLogoUrl
                        // Intrinsic size of the loaded logo: needed to fit it into the 300x110dp frame
                        // with mandatory scaling (including upscale) — iOS 1:1 (RemoteLogoView).
                        // Without this, Coil displays small logos in their pixel size, and they
                        // look "smaller" than large ones.
                        var logoIntrinsicSize by remember(logoUrl) { mutableStateOf<Size?>(null) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.graphicsLayer {
                                alpha = contentLogoAlpha
                                scaleX = 0.88f + 0.12f * contentLogoAlpha
                                scaleY = 0.88f + 0.12f * contentLogoAlpha
                            }
                        ) {
                            if (!logoUrl.isNullOrBlank()) {
                                val density = LocalDensity.current
                                val logoSize = logoIntrinsicSize?.let { intrinsic ->
                                    val widthDp = intrinsic.width / density.density
                                    val heightDp = intrinsic.height / density.density
                                    if (widthDp > 0f && heightDp > 0f) {
                                        val scale = min(300f / widthDp, 110f / heightDp)
                                        Pair((widthDp * scale).dp, (heightDp * scale).dp)
                                    } else {
                                        null
                                    }
                                }
                                AsyncImage(
                                    model = logoUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Fit,
                                    onSuccess = { state -> logoIntrinsicSize = state.painter.intrinsicSize },
                                    modifier = Modifier
                                        .then(
                                            if (logoSize != null) {
                                                Modifier.size(logoSize.first, logoSize.second)
                                            } else {
                                                Modifier.widthIn(max = 300.dp).heightIn(max = 110.dp)
                                            }
                                        )
                                        .padding(bottom = 8.dp)
                                )
                            } else {
                                Text(
                                    text = item.title ?: item.originalTitle ?: "Без названия",
                                    style = SlooshTypography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimaryDark,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }

                            val showOriginal = com.slooshfilm.app.ui.components.LocalShowOriginalTitle.current
                            if (showOriginal && !item.originalTitle.isNullOrBlank() && item.originalTitle != item.title) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.originalTitle,
                                    style = SlooshTypography.bodyMedium,
                                    color = TextMutedDark,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Primary Metadata Row: Rating, Age, Year, Country, Duration (iOS 1:1)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val rating = item.displayRating
                            if (rating > 0.0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedRectangle(6.dp))
                                        .background(ratingColor(rating))
                                        .padding(horizontal = 5.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f", rating),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.2).sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            val age = item.ageRating
                            if (!age.isNullOrBlank()) {
                                Text(
                                    text = age,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ageRatingColor(age)
                                )
                            }

                            if (item.year != null && item.year > 0) {
                                Text(
                                    text = item.year.toString(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondaryDark
                                )
                            }

                            item.countries?.firstOrNull { it.isNotBlank() }?.let { rawCountry ->
                                val country = formatCountryName(rawCountry)
                                Text(
                                    text = country,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondaryDark
                                )
                            }

                            item.duration?.takeIf { it > 0 }?.let { durationMin ->
                                Text(
                                    text = "$durationMin мин",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 4. Action row (iOS 1:1, DetailsHeroActionMetrics): download 52dp —
                        // play 72dp — favorite 52dp, spacing 22dp. The play button has no text:
                        // the triangle is cut out of the white disc (BlendMode.Clear inside an
                        // offscreen layer), so the real backdrop shows through it, as on iOS.
                        val playInteractionSource = remember { MutableInteractionSource() }
                        val playPressed by playInteractionSource.collectIsPressedAsState()
                        val playScale by animateFloatAsState(
                            targetValue = if (playPressed) 0.94f else 1f,
                            label = "playScale"
                        )
                        // The cutout is pure geometry, no blend modes or rasters: a single Path
                        // with PathFillType.EvenOdd — the disc contour plus the triangle contour
                        // transcribed from the ic_sf_play_fill path data (SF viewport 56, glyph
                        // scaled to 34dp as on iOS). The triangle becomes a hole in the fill, so
                        // the real backdrop shows through it on any GPU/driver.
                        val playDensity = LocalDensity.current
                        val playDiscPx = with(playDensity) { 72.dp.toPx() }
                        val playGlyphPx = with(playDensity) { 34.dp.toPx() }
                        val playDiscPath = remember(playDiscPx, playGlyphPx) {
                            val u = playGlyphPx / 56f
                            val o = (playDiscPx - playGlyphPx) / 2f
                            Path().apply {
                                fillType = PathFillType.EvenOdd
                                addOval(Rect(0f, 0f, playDiscPx, playDiscPx))
                                moveTo(o + 13.0937f * u, o + 47.8398f * u)
                                cubicTo(
                                    o + 14.0313f * u, o + 47.8398f * u,
                                    o + 14.8281f * u, o + 47.4648f * u,
                                    o + 15.7656f * u, o + 46.9258f * u
                                )
                                lineTo(o + 43.0937f * u, o + 31.1289f * u)
                                cubicTo(
                                    o + 45.0391f * u, o + 29.9805f * u,
                                    o + 45.7187f * u, o + 29.2305f * u,
                                    o + 45.7187f * u, o + 27.9883f * u
                                )
                                cubicTo(
                                    o + 45.7187f * u, o + 26.7461f * u,
                                    o + 45.0391f * u, o + 25.9961f * u,
                                    o + 43.0937f * u, o + 24.8711f * u
                                )
                                lineTo(o + 15.7656f * u, o + 9.0508f * u)
                                cubicTo(
                                    o + 14.8281f * u, o + 8.5118f * u,
                                    o + 14.0313f * u, o + 8.1602f * u,
                                    o + 13.0937f * u, o + 8.1602f * u
                                )
                                cubicTo(
                                    o + 11.3594f * u, o + 8.1602f * u,
                                    o + 10.2813f * u, o + 9.4727f * u,
                                    o + 10.2813f * u, o + 11.5118f * u
                                )
                                lineTo(o + 10.2813f * u, o + 44.4649f * u)
                                cubicTo(
                                    o + 10.2813f * u, o + 46.5039f * u,
                                    o + 11.3594f * u, o + 47.8398f * u,
                                    o + 13.0937f * u, o + 47.8398f * u
                                )
                                close()
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Download — to the left of play (iOS: DetailsHeroDownloadButton)
                            LiquidGlassFloatingButton(
                                onClick = { showOfflineDownloadDialog = true },
                                backdrop = glassBackdrop,
                                shape = CircleShape,
                                tint = LiquidGlassSurfaceDark,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sf_arrow_down_circle_fill),
                                    contentDescription = "Скачать",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Play — 72dp disc with a cut-out triangle (iOS: primaryDiameter 72, symbol 34pt)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .graphicsLayer {
                                        scaleX = playScale
                                        scaleY = playScale
                                    }
                                    .drawBehind {
                                        drawPath(
                                            path = playDiscPath,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                    .clickable(
                                        interactionSource = playInteractionSource,
                                        indication = null,
                                        onClick = { loadAndShowWatchSelector() }
                                    )
                                    .semantics { contentDescription = "Смотреть" }
                            )

                            // Favorite — to the right of play (iOS: favoriteButton in the hero row)
                            LiquidGlassFloatingButton(
                                onClick = {
                                    details?.let { mediaItem ->
                                        favoritesStore.toggleFavorite(mediaItem.toMediaDto())
                                    }
                                },
                                backdrop = glassBackdrop,
                                shape = CircleShape,
                                tint = LiquidGlassSurfaceDark,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    painter = painterResource(if (isFavorite) R.drawable.ic_sf_heart_fill else R.drawable.ic_sf_heart),
                                    contentDescription = if (isFavorite) "В избранном" else "В избранное",
                                    tint = if (isFavorite) SlooshAccent else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // 5. Genres Section (iOS 1:1 wrapping FlowRow)
                        val genres = item.genres
                            ?.map { it.trim() }
                            ?.filter { it.isNotEmpty() }
                            ?: emptyList()
                        if (genres.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(22.dp))
                            Text(
                                text = "Жанры",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                genres.forEach { genre ->
                                    LiquidGlassFloatingButton(
                                        onClick = {
                                            onGenreClick(genre, if (item.isTvSeries) "tv" else "movie")
                                        },
                                        backdrop = glassBackdrop,
                                        blurRadius = 8.dp,
                                        shape = Capsule(),
                                        tint = LiquidGlassSurfaceDark,
                                        borderAlpha = 0.50f,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // 6. Studios / Networks Section (iOS 1:1 wrapping FlowRow)
                        val networks = item.networks?.filter { !it.name.isNullOrBlank() } ?: emptyList()
                        val studios = (item.productionCompanies?.takeIf { it.isNotEmpty() } ?: item.studios.orEmpty())
                            .filter { !it.name.isNullOrBlank() }
                        val studioItems = (studios + networks).distinctBy { studio ->
                            studio.id?.takeIf { it.isNotBlank() } ?: studio.name.orEmpty()
                        }
                        val studioTitle = if (studioItems.size > 1) "Студии" else "Студия"

                        if (studioItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = studioTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                studioItems.forEach { studio ->
                                    val brand = StudioBrand.find(studio.name)
                                    val catalogId = brand?.id ?: studio.id
                                    val isEnabled = !catalogId.isNullOrBlank()
                                    LiquidGlassFloatingButton(
                                        onClick = {
                                            if (isEnabled) {
                                                catalogId?.let { id -> onStudioClick(id, brand?.name ?: studio.name.orEmpty()) }
                                            }
                                        },
                                        backdrop = glassBackdrop,
                                        blurRadius = 8.dp,
                                        shape = Capsule(),
                                        tint = LiquidGlassSurfaceDark,
                                        borderAlpha = 0.50f,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = studio.name ?: "",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // 7. Budget & Revenue Section (iOS 1:1 with section title)
                        val budgetStr = item.formattedBudget
                        val revenueStr = item.formattedRevenue
                        if (!budgetStr.isNullOrBlank() || !revenueStr.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = when {
                                    !budgetStr.isNullOrBlank() && !revenueStr.isNullOrBlank() -> "Бюджет и сборы"
                                    !budgetStr.isNullOrBlank() -> "Бюджет"
                                    else -> "Сборы"
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val rowText = buildString {
                                if (!budgetStr.isNullOrBlank()) append("Бюджет: $budgetStr")
                                if (!budgetStr.isNullOrBlank() && !revenueStr.isNullOrBlank()) append(" • ")
                                if (!revenueStr.isNullOrBlank()) append("Сборы: $revenueStr")
                            }
                            Text(
                                text = rowText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondaryDark,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 8. Description / Synopsis
                        if (!item.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Описание",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .then(if (isGlassEnabled) Modifier.layerBackdrop(descriptionBackdrop) else Modifier)
                                ) {
                                    Text(
                                        text = item.description,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = TextPrimaryDark.copy(alpha = 0.85f),
                                        lineHeight = 21.sp,
                                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4,
                                        overflow = TextOverflow.Ellipsis,
                                        onTextLayout = { layoutResult ->
                                            if (!isDescriptionExpanded) {
                                                canExpandDescription = layoutResult.hasVisualOverflow
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                            .drawWithContent {
                                                drawContent()
                                                if (canExpandDescription && !isDescriptionExpanded) {
                                                    drawRect(
                                                        brush = Brush.verticalGradient(
                                                            colorStops = arrayOf(
                                                                0f to Color.Black,
                                                                0.45f to Color.Black,
                                                                0.80f to Color.Black.copy(alpha = 0.45f),
                                                                1f to Color.Black.copy(alpha = 0.15f)
                                                            )
                                                        ),
                                                        blendMode = BlendMode.DstIn
                                                    )
                                                }
                                            }
                                    )
                                    if (canExpandDescription) {
                                        Spacer(modifier = Modifier.height(if (isDescriptionExpanded) 52.dp else 16.dp))
                                    }
                                }

                                if (canExpandDescription) {
                                    val chevronRotation by animateFloatAsState(
                                        targetValue = if (isDescriptionExpanded) 180f else 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "chevronRotation"
                                    )
                                    LiquidGlassFloatingButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isDescriptionExpanded = !isDescriptionExpanded
                                        },
                                        backdrop = descriptionBackdrop,
                                        blurRadius = 8.dp,
                                        shape = Capsule(),
                                        tint = LiquidGlassSurfaceDark,
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isDescriptionExpanded) "Свернуть" else "Читать далее",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Icon(
                                                painter = painterResource(R.drawable.ic_sf_chevron_down),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .graphicsLayer { rotationZ = chevronRotation }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }

                item(key = "details-cast", contentType = "details-cast") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    // 1. Cast horizontal list (iOS 1:1)
                    val cast = item.cast ?: emptyList()
                    if (cast.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Актёры",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = detailsHorizontalPadding)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = detailsHorizontalPadding),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(
                                items = cast,
                                key = { index, actor -> "cast-$index-${actor.id.orEmpty()}-${actor.name.orEmpty()}-${actor.character.orEmpty()}" },
                                contentType = { _, _ -> "cast-member" }
                            ) { _, actor ->
                                val photoUrl = actor.getDisplayPhotoUrl()
                                var isPhotoLoading by remember(photoUrl) {
                                    mutableStateOf(!photoUrl.isNullOrBlank())
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(76.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!photoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = photoUrl,
                                                contentDescription = actor.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                onState = { state ->
                                                    if (state is coil.compose.AsyncImagePainter.State.Success ||
                                                        state is coil.compose.AsyncImagePainter.State.Error
                                                    ) {
                                                        isPhotoLoading = false
                                                    } else if (state is coil.compose.AsyncImagePainter.State.Loading) {
                                                        isPhotoLoading = true
                                                    }
                                                }
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_sf_person_fill),
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.35f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        if (isPhotoLoading) {
                                            ShimmerPlaceholder(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape),
                                                cornerRadius = 38.dp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = actor.name ?: "",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark,
                                        maxLines = 2,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!actor.character.isNullOrBlank()) {
                                        Text(
                                            text = actor.character,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = TextSecondaryDark,
                                            maxLines = 2,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                }

                item(key = "details-crew", contentType = "details-crew") {
                    Column(
                        modifier = Modifier.fillMaxWidth().offset(y = (-30).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    // 2. Crew horizontal list (iOS 1:1)
                    val crew = item.crew ?: emptyList()
                    if (crew.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Создатели",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = detailsHorizontalPadding)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = detailsHorizontalPadding),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(
                                items = crew,
                                key = { index, member -> "crew-$index-${member.id.orEmpty()}-${member.name.orEmpty()}-${member.role.orEmpty()}" },
                                contentType = { _, _ -> "crew-member" }
                            ) { _, member ->
                                val photoUrl = member.getDisplayPhotoUrl()
                                var isPhotoLoading by remember(photoUrl) {
                                    mutableStateOf(!photoUrl.isNullOrBlank())
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(76.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!photoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = photoUrl,
                                                contentDescription = member.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                onState = { state ->
                                                    if (state is coil.compose.AsyncImagePainter.State.Success ||
                                                        state is coil.compose.AsyncImagePainter.State.Error
                                                    ) {
                                                        isPhotoLoading = false
                                                    } else if (state is coil.compose.AsyncImagePainter.State.Loading) {
                                                        isPhotoLoading = true
                                                    }
                                                }
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_sf_person_fill),
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.35f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        if (isPhotoLoading) {
                                            ShimmerPlaceholder(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape),
                                                cornerRadius = 38.dp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = member.name ?: "",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark,
                                        maxLines = 2,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!member.role.isNullOrBlank()) {
                                        Text(
                                            text = member.role,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = TextSecondaryDark,
                                            maxLines = 2,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                }

                item(key = "details-trailers", contentType = "details-trailers") {
                    Column(
                        modifier = Modifier.fillMaxWidth().offset(y = (-30).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    // 3. Trailers horizontal list (iOS 1:1)
                    val trailers = (item.trailers ?: emptyList()).filter { !it.youtubeKey.isNullOrBlank() }
                    if (trailers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Трейлеры",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = detailsHorizontalPadding)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = detailsHorizontalPadding),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(
                                items = trailers,
                                key = { index, trailer -> "trailer-$index-${trailer.id.orEmpty()}-${trailer.youtubeKey.orEmpty()}-${trailer.name.orEmpty()}" },
                                contentType = { _, _ -> "trailer-card" }
                            ) { _, trailer ->
                                Column(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            val key = trailer.youtubeKey
                                            if (!key.isNullOrBlank()) {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$key"))
                                                    context.startActivity(Intent.createChooser(intent, "Открыть трейлер"))
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "Не удалось открыть трейлер", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(220.dp)
                                            .height(124.dp)
                                            .clip(RoundedRectangle(14.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedRectangle(14.dp))
                                    ) {
                                        // Thumbnail captured in its own layer so the play badge
                                        // refracts the real poster behind it instead of an empty backdrop.
                                        val thumbBackdrop = rememberLayerBackdrop {
                                            drawRect(effectiveBg)
                                            drawContent()
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(if (isGlassEnabled) Modifier.layerBackdrop(thumbBackdrop) else Modifier)
                                        ) {
                                            val thumb = trailer.thumbnailUrl
                                            if (!thumb.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = thumb,
                                                    contentDescription = trailer.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_sf_film_fill),
                                                        contentDescription = null,
                                                        tint = Color.White.copy(alpha = 0.35f),
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }

                                            // Subtle bottom gradient
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                                                        )
                                                    )
                                            )
                                        }

                                        // Centered Liquid Glass Play Badge (42x42dp circle)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(42.dp)
                                                .liquidGlassSurface(
                                                    style = LiquidGlassTokens.Button,
                                                    shape = CircleShape,
                                                    containerColor = Color.Black.copy(alpha = 0.40f),
                                                    backdrop = thumbBackdrop
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_sf_play_fill),
                                                contentDescription = "Воспроизвести",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = trailer.name ?: "Трейлер",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    }
                }

                item(key = "details-seasons", contentType = "details-seasons") {
                    Column(
                        modifier = Modifier.fillMaxWidth().offset(y = (-30).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    // 4. TV Series: Seasons & Episodes (iOS 1:1)
                    if (item.isTvSeries) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Сезоны и серии",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = detailsHorizontalPadding)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Season selector chips
                        if (allSeasonNumbers.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = detailsHorizontalPadding),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(allSeasonNumbers, key = { it }, contentType = { "season-chip" }) { seasonNum ->
                                    val isSelected = seasonNum == selectedSeasonNumber
                                    LiquidGlassFloatingButton(
                                        onClick = {
                                            selectedSeasonNumber = seasonNum
                                        },
                                        backdrop = glassBackdrop,
                                        blurRadius = 8.dp,
                                        shape = Capsule(),
                                        tint = if (isSelected) Color.White else LiquidGlassSurfaceDark,
                                        borderAlpha = if (isSelected) 0.85f else 0.50f,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = "$seasonNum сезон",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) Color.Black else TextSecondaryDark
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Episodes horizontal carousel
                        if (isLoadingSeasonEpisodes && currentSeasonData?.episodes.isNullOrEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = detailsHorizontalPadding),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(4, key = { "episode-placeholder-$it" }, contentType = { "episode-placeholder" }) {
                                    Box(
                                        modifier = Modifier
                                            .width(180.dp)
                                            .height(104.dp)
                                            .clip(RoundedRectangle(12.dp))
                                            .background(SurfaceDark)
                                    )
                                }
                            }
                        } else {
                            val tmdbEpisodes = currentSeasonData?.episodes ?: emptyList()
                            val allohaEpisodes = allohaResult?.seasons?.firstOrNull { it.season == selectedSeasonNumber }?.episodes ?: emptyList()
                            val metaEpisodeCount = item.seasons?.firstOrNull { it.seasonNumber == selectedSeasonNumber }?.episodeCount ?: 0
                            val maxEp = maxOf(
                                tmdbEpisodes.maxOfOrNull { it.episodeNumber ?: 0 } ?: 0,
                                allohaEpisodes.maxOfOrNull { it.episode } ?: 0,
                                metaEpisodeCount
                            )

                            if (maxEp > 0) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = detailsHorizontalPadding),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(maxEp, key = { "episode-${it + 1}" }, contentType = { "episode-card" }) { index ->
                                        val epNumber = index + 1
                                        val epData = tmdbEpisodes.firstOrNull { it.episodeNumber == epNumber }
                                        val epTitle = epData?.name ?: "Серия $epNumber"
                                        val epAirDate = epData?.airDate
                                        val stillUrl = epData?.getDisplayStillUrl() ?: item.getDisplayBackdropUrl(isLowQuality = true)

                                        Column(
                                            modifier = Modifier
                                                .width(180.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    loadAndShowWatchSelector(
                                                        initialSeason = selectedSeasonNumber,
                                                        initialEpisode = epNumber
                                                    )
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(104.dp)
                                                    .clip(RoundedRectangle(12.dp))
                                                    .background(SurfaceDark)
                                            ) {
                                                // Still frame captured in its own layer so the play badge
                                                // refracts the real preview behind it instead of an empty backdrop.
                                                val stillBase = SurfaceDark
                                                val stillBackdrop = rememberLayerBackdrop {
                                                    drawRect(stillBase)
                                                    drawContent()
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .then(if (isGlassEnabled) Modifier.layerBackdrop(stillBackdrop) else Modifier)
                                                ) {
                                                    if (!stillUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = stillUrl,
                                                            contentDescription = epTitle,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                }
                                                // Play badge overlay
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.Center)
                                                        .size(36.dp)
                                                        .liquidGlassSurface(
                                                            style = LiquidGlassTokens.Button,
                                                            shape = CircleShape,
                                                            containerColor = Color.Black.copy(alpha = 0.50f),
                                                            backdrop = stillBackdrop
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_sf_play_fill),
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "$epNumber. $epTitle",
                                                style = SlooshTypography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimaryDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            val formattedDate = formatEpisodeAirDate(epAirDate)
                                            val subtitle = when {
                                                !formattedDate.isNullOrBlank() && epData?.duration != null && epData.duration > 0 -> "$formattedDate • ${epData.duration} мин"
                                                !formattedDate.isNullOrBlank() -> formattedDate
                                                epData?.duration != null && epData.duration > 0 -> "${epData.duration} мин"
                                                else -> null
                                            }
                                            if (!subtitle.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = subtitle,
                                                    style = SlooshTypography.labelSmall,
                                                    color = TextMutedDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Серии для этого сезона пока недоступны",
                                    fontSize = 14.sp,
                                    color = TextSecondaryDark,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }
                    }
                }

                item(key = "details-collection", contentType = "details-collection") {
                    Column(
                        modifier = Modifier.fillMaxWidth().offset(y = (-30).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    // 5. Franchise collection list (iOS 1:1)
                    val effectiveCollection = movieCollection ?: item.collection
                    val collectionParts = remember(effectiveCollection?.allParts) {
                        effectiveCollection?.allParts
                            ?.filter { !it.id.isNullOrBlank() }
                            ?.distinctBy { it.favoriteIdentity }
                            ?: emptyList()
                    }
                    val collectionName = effectiveCollection?.name
                    if (collectionParts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = detailsHorizontalPadding)
                        ) {
                            Text(
                                text = "Все части франшизы",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            if (!collectionName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = collectionName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = detailsHorizontalPadding),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(collectionParts, key = { it.favoriteIdentity }, contentType = { "media-card" }) { part ->
                                MediaCard(
                                    media = part,
                                    onClick = onSimilarClick,
                                    width = 110
                                )
                            }
                        }
                    }
                    }
                }

                item(key = "details-similar", contentType = "details-similar") {
                    Column(
                        modifier = Modifier.fillMaxWidth().offset(y = (-30).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    // 6. Similar media list (iOS 1:1)
                    val similarItems = remember(item.similar) {
                        item.similar?.distinctBy { it.favoriteIdentity } ?: emptyList()
                    }
                    if (similarItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (item.isTvSeries) "Похожие сериалы" else "Похожие фильмы",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = detailsHorizontalPadding)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = detailsHorizontalPadding),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(similarItems, key = { it.favoriteIdentity }, contentType = { "media-card" }) { similar ->
                                MediaCard(
                                    media = similar,
                                    onClick = onSimilarClick,
                                    width = 110
                                )
                            }
                        }
                    }
                    }
                }

                item(key = "details-studio-releases", contentType = "details-studio-releases") {
                    Column(
                        modifier = Modifier.fillMaxWidth().offset(y = (-30).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    // 7. Other releases by studio (iOS 1:1)
                    val studioReleases = remember(relatedStudio?.allItems, item.cleanId) {
                        relatedStudio?.allItems?.filter {
                            val cleanPartId = (it.id ?: "").replace("kp_", "").replace("movie_", "").replace("tv_", "")
                            cleanPartId != item.cleanId && !it.id.isNullOrBlank()
                        }?.distinctBy { it.favoriteIdentity } ?: emptyList()
                    }

                    if (studioReleases.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = detailsHorizontalPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Другие релизы",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            val studioLabel = relatedStudio?.label
                            if (!studioLabel.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .liquidGlassSurface(
                                            style = LiquidGlassTokens.CompactPill,
                                            shape = Capsule(),
                                            containerColor = LiquidGlassSurfaceDark,
                                            backdrop = glassBackdrop
                                        )
                                        .clip(Capsule())
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = studioLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = detailsHorizontalPadding),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(studioReleases, key = { it.favoriteIdentity }, contentType = { "media-card" }) { rel ->
                                MediaCard(
                                    media = rel,
                                    onClick = onSimilarClick,
                                    width = 110
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Premium Apple TV+ Error State
        ErrorStateView(
            title = "Не удалось загрузить",
            message = errorMessage ?: "Проверьте интернет-соединение и попробуйте снова.",
            onRetry = ::requestDetailsRefresh,
            iconRes = R.drawable.ic_sf_antenna_radiowaves,
            modifier = Modifier.fillMaxSize()
        )
    }
}

        // Floating Progressive Blur Top Header (Apple TV+ Style, appears when scrolled past threshold)
        if (topGradientAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerBlurHeight)
                    .align(Alignment.TopCenter)
                    .graphicsLayer { alpha = topGradientAlpha }
            ) {
                if (isProgressiveBlurEnabled) {
                    // Progressive blur layer: true physical variable radius blur (decreasing smoothly from 4dp to 0dp, zero masks)
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
                                        falloffDistance = (headerBlurHeight.toPx() - topOffsetPx).coerceAtLeast(1f)
                                    )
                                },
                                highlight = null,
                                shadow = null
                            )
                    )
                }

                // Top Dominant Gradient Bar for typography contrast & seamless edge blending
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    effectiveBg,
                                    effectiveBg.copy(alpha = 0f)
                                )
                            )
                        )
                )
            }
        }
    }

    // Floating Top Bar: Circular Glass Back Button (start) + Compact Logo / Title (center) + Trailing Actions (end)
    CompositionLocalProvider(LocalBackdrop provides sceneBackdrop) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .height(if (isLandscape) 44.dp else 56.dp)
                .padding(horizontal = 16.dp + cutoutPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                // Circular Back Button with Liquid Glass backdrop & touch stretch
                LiquidGlassFloatingButton(
                    onClick = onBack,
                    backdrop = sceneBackdrop,
                    shape = CircleShape,
                    tint = LiquidGlassSurfaceDark,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sf_chevron_left),
                        contentDescription = "Назад",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Compact Logo / Title in Top Bar (iOS 1:1, smoothly fades in when scrolled past threshold point)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .padding(horizontal = 10.dp)
                        .graphicsLayer {
                            alpha = topLogoAlpha
                            scaleX = 0.85f + 0.15f * topLogoAlpha
                            scaleY = 0.85f + 0.15f * topLogoAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val topLogoUrl = details?.displayLogoUrl
                    var topLogoIntrinsicSize by remember(topLogoUrl) { mutableStateOf<Size?>(null) }
                    if (!topLogoUrl.isNullOrBlank()) {
                        // iOS 1:1 (RemoteLogoView isTopBar): fixed height 32dp with proportional
                        // scaling, width capped by the center slot (160dp).
                        val density = LocalDensity.current
                        val topLogoSize = topLogoIntrinsicSize?.let { intrinsic ->
                            val widthDp = intrinsic.width / density.density
                            val heightDp = intrinsic.height / density.density
                            if (widthDp > 0f && heightDp > 0f) {
                                val scale = min(160f / widthDp, 32f / heightDp)
                                Pair((widthDp * scale).dp, (heightDp * scale).dp)
                            } else {
                                null
                            }
                        }
                        AsyncImage(
                            model = topLogoUrl,
                            contentDescription = details?.title,
                            contentScale = ContentScale.Fit,
                            onSuccess = { state -> topLogoIntrinsicSize = state.painter.intrinsicSize },
                            modifier = Modifier.then(
                                if (topLogoSize != null) {
                                    Modifier.size(topLogoSize.first, topLogoSize.second)
                                } else {
                                    Modifier.fillMaxHeight().widthIn(max = 160.dp)
                                }
                            )
                        )
                    } else {
                        Text(
                            text = details?.title ?: details?.originalTitle ?: "",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Trailing Action: circular Share button (iOS 1:1: square.and.arrow.up in the
                // navigation bar). Favorite has moved to the hero row, next to play, as on iOS.
                LiquidGlassFloatingButton(
                    onClick = {
                        val shareTitle = details?.title ?: details?.originalTitle
                        if (!shareTitle.isNullOrBlank()) {
                            val shareYear = details?.year?.takeIf { it > 0 }?.let { " ($it)" } ?: ""
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                                putExtra(Intent.EXTRA_TEXT, "$shareTitle$shareYear")
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(shareIntent, "Поделиться"))
                            }
                        }
                    },
                    backdrop = sceneBackdrop,
                    shape = CircleShape,
                    tint = LiquidGlassSurfaceDark,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sf_square_arrow_up),
                        contentDescription = "Поделиться",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Watch Selector Bottom Sheet
        if (showWatchSheet) {
            WatchSelectorSheet(
                allohaResult = allohaResult,
                isLoading = isLoadingAlloha,
                errorMessage = allohaError,
                initialSeason = watchSheetSeason,
                initialEpisode = watchSheetEpisode,
                title = details?.title ?: "Выбор озвучки",
                logoUrl = details?.displayLogoUrl,
                backdrop = sceneBackdrop,
                onDismiss = {
                    showWatchSheet = false
                    watchSheetSeason = null
                    watchSheetEpisode = null
                },
                onPlay = { iframe, translationName, translationId, season, episode ->
                    showWatchSheet = false
                    watchSheetSeason = null
                    watchSheetEpisode = null
                    AllohaSessionHolder.cachedApiResult = allohaResult
                    AllohaSessionHolder.activeTargetIframeUrl = iframe
                    AllohaSessionHolder.activeMediaId = details?.id ?: mediaId
                    AllohaSessionHolder.activeLogoUrl = details?.displayLogoUrl
                    AllohaSessionHolder.currentVoice = translationName
                    if (!iframe.isNullOrBlank()) {
                        AllohaSessionHolder.availableVoiceNames = listOf(translationName ?: "Русский", "Оригинал")
                    }
                    onPlay(season, episode, translationName, translationId, iframe)
                }
            )
        }

        // Offline Download Dialog (Coming Soon)
        if (showOfflineDownloadDialog) {
            LiquidGlassDialog(
                title = "Офлайн-просмотр",
                onDismissRequest = { showOfflineDownloadDialog = false },
                backdrop = sceneBackdrop,
                showCloseButton = false
            ) { dismiss ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_arrow_down_circle_fill),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Скоро будет доступно",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.60f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Сохранение фильмов и сериалов для просмотра без интернета появится в следующем обновлении.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Две строки действий: сверху основное, снизу закрытие (iOS alert).
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(Capsule())
                            .background(Color.White)
                            .clickable {
                                dismiss()
                                loadAndShowWatchSelector()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Смотреть онлайн",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(Capsule())
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            .clickable { dismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Понятно",
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backdrop carousel pagination indicator pill matching iOS BackdropPageIndicator 1:1.
 */
@Composable
private fun BackdropPageIndicator(
    count: Int,
    selectedIndex: Int,
    progress: Float = 0f,
    isAutoAdvance: Boolean = false,
    onPageSelected: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    if (count <= 1) return

    val activeWidth = 18.dp
    val inactiveWidth = 5.dp
    val pillHeight = 4.5.dp

    Box(
        modifier = modifier
            .liquidGlassSurface(
                style = LiquidGlassTokens.CompactPill.copy(highlightAlpha = 0.65f),
                shape = Capsule(),
                containerColor = Color.Black.copy(alpha = 0.35f),
                backdrop = backdrop
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until minOf(count, 8)) {
                val isSelected = i == selectedIndex
                val animatedWidth by animateDpAsState(
                    targetValue = if (isSelected) activeWidth else inactiveWidth,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f),
                    label = "indicatorWidth"
                )

                Box(
                    modifier = Modifier
                        .height(pillHeight)
                        .width(animatedWidth)
                        .clip(Capsule())
                        .background(Color.White.copy(alpha = 0.32f))
                        .then(
                            if (onPageSelected != null) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onPageSelected(i) }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isSelected) {
                        if (isAutoAdvance) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(activeWidth * progress.coerceIn(0f, 1f))
                                    .clip(Capsule())
                                    .background(Color.White)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(Capsule())
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsLoadingSkeleton(
    modifier: Modifier = Modifier,
    heroHeight: Dp = 365.dp
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item(key = "details-loading-skeleton", contentType = "details-loading-skeleton") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = detailsHeroFadeStops
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    ShimmerPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 0.dp
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-25).dp)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(3) { index ->
                            ShimmerPlaceholder(
                                modifier = Modifier
                                    .width(if (index == 0) 20.dp else 14.dp)
                                    .height(3.dp),
                                cornerRadius = 3.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    ShimmerPlaceholder(Modifier.width(230.dp).height(36.dp), cornerRadius = 8.dp)
                    Spacer(Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(32.dp, 26.dp, 38.dp, 64.dp, 56.dp).forEach { width ->
                            ShimmerPlaceholder(Modifier.width(width).height(18.dp), cornerRadius = 5.dp)
                        }
                    }
                    Spacer(Modifier.height(28.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerPlaceholder(Modifier.width(160.dp).height(50.dp), cornerRadius = 25.dp)
                        ShimmerPlaceholder(Modifier.size(50.dp), cornerRadius = 25.dp)
                    }
                    Spacer(Modifier.height(28.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        ShimmerPlaceholder(Modifier.width(88.dp).height(20.dp), cornerRadius = 4.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(88.dp, 108.dp, 76.dp).forEach { width ->
                                ShimmerPlaceholder(Modifier.width(width).height(32.dp), cornerRadius = 16.dp)
                            }
                        }
                        ShimmerPlaceholder(Modifier.width(95.dp).height(20.dp), cornerRadius = 4.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            repeat(3) {
                                ShimmerPlaceholder(Modifier.fillMaxWidth().height(15.dp), cornerRadius = 4.dp)
                            }
                            ShimmerPlaceholder(Modifier.width(220.dp).height(15.dp), cornerRadius = 4.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats a raw date string (e.g. "yyyy-MM-dd") into localized Russian format (e.g. "13 октября 2026").
 */
private fun formatEpisodeAirDate(dateStr: String?): String? {
    if (dateStr.isNullOrBlank()) return null
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val date = inputFormat.parse(dateStr) ?: return dateStr
        val outputFormat = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("ru"))
        outputFormat.format(date)
    } catch (_: Exception) {
        dateStr
    }
}
