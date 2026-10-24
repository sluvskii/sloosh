package com.slooshfilm.app.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.slooshfilm.app.R
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.repository.MoviesRepository
import com.slooshfilm.app.data.storage.LocalStorage
import com.slooshfilm.app.ui.backdrop.backdrops.rememberCanvasBackdrop
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
    val isCompact = LocalCardDensity.current == LocalStorage.CARD_DENSITY_COMPACT
    val horizontalPadding = if (isCompact) 12.dp else 16.dp
    val gridSpacing = if (isCompact) 8.dp else 16.dp
    val minCardWidth = if (isCompact) 95.dp else 105.dp
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
    val title = remember(kind, name) {
        if (kind == CATALOG_KIND_GENRE) displayGenreTitle(name) else name
    }
    val lastVisibleIndex by remember {
        derivedStateOf { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
    }

    LaunchedEffect(state) {
        state.loadInitial()
    }
    LaunchedEffect(lastVisibleIndex, state.items.size) {
        if (state.items.isNotEmpty() && lastVisibleIndex >= state.items.size - 4) {
            state.loadNextPage()
        }
    }

    // The back button floats over the flat screen background (no content scrolls
    // under the header row), so this canvas backdrop is the exact scene behind it.
    val glassBackdrop = rememberCanvasBackdrop {
        drawRect(BackgroundDark)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(44.dp)
        ) {
            LiquidGlassFloatingButton(
                onClick = onBack,
                backdrop = glassBackdrop,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sf_chevron_left),
                    contentDescription = "Назад",
                    tint = TextPrimaryDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = title,
            style = SlooshTypography.headlineLarge,
            color = TextPrimaryDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(bottom = 10.dp)
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { state.loadInitial(force = true) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minCardWidth),
                        contentPadding = PaddingValues(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            bottom = bottomPadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                        verticalArrangement = Arrangement.spacedBy(gridSpacing),
                        userScrollEnabled = false,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(9, contentType = { "poster-placeholder" }) {
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
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.ic_sf_film_fill),
                                contentDescription = null,
                                tint = TextTertiaryDark,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = if (state.errorMessage == null) "Ничего не найдено" else "Не удалось загрузить",
                                style = SlooshTypography.titleLarge,
                                color = TextPrimaryDark,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = state.errorMessage ?: if (kind == CATALOG_KIND_GENRE) {
                                    "В категории «$title» пока нет доступных фильмов"
                                } else {
                                    "В каталоге «$title» пока нет доступных релизов"
                                },
                                style = SlooshTypography.bodyMedium,
                                color = TextSecondaryDark,
                                textAlign = TextAlign.Center
                            )
                            if (state.errorMessage != null) {
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { state.loadInitial(force = true) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SlooshAccent,
                                        contentColor = Color.Black
                                    ),
                                    shape = Capsule()
                                ) {
                                    Text("Повторить")
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minCardWidth),
                        contentPadding = PaddingValues(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            bottom = bottomPadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                        verticalArrangement = Arrangement.spacedBy(gridSpacing),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = state.items,
                            key = { it.favoriteIdentity },
                            contentType = { "media-card" }
                        ) { media ->
                            MediaCard(
                                media = media,
                                onClick = { onMediaClick(media) }
                            )
                        }
                        if (state.isAppending) {
                            items(3, contentType = { "poster-placeholder" }) {
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
