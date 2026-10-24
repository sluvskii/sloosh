package com.slooshfilm.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.repository.MoviesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HomeCategory(val title: String) {
    ALL("Все"),
    MOVIES("Фильмы"),
    SERIES("Сериалы"),
    CARTOONS("Мульты"),
    ANIME("Аниме")
}

enum class HomeFilter(val title: String) {
    POPULAR("Смотрят сейчас"),
    TOP_RATED("По рейтингу")
}

private data class CacheKey(
    val category: HomeCategory,
    val filter: HomeFilter
)

data class HomeUiState(
    val selectedCategory: HomeCategory = HomeCategory.ALL,
    val selectedFilter: HomeFilter = HomeFilter.POPULAR,
    val items: List<MediaDto> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: MoviesRepository = MoviesRepository.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val cache = mutableMapOf<CacheKey, MutableList<MediaDto>>()
    private val pageMap = mutableMapOf<CacheKey, Int>()
    private val canLoadMoreMap = mutableMapOf<CacheKey, Boolean>()

    private var currentLoadJob: Job? = null

    init {
        loadData(HomeCategory.ALL, HomeFilter.POPULAR)
    }

    fun selectCategory(category: HomeCategory) {
        if (_uiState.value.selectedCategory == category) return
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        displayCachedOrLoad(category, _uiState.value.selectedFilter)
    }

    fun selectFilter(filter: HomeFilter) {
        if (_uiState.value.selectedFilter == filter) return
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        displayCachedOrLoad(_uiState.value.selectedCategory, filter)
    }

    fun refresh() {
        val cat = _uiState.value.selectedCategory
        val flt = _uiState.value.selectedFilter
        val key = CacheKey(cat, flt)
        cache.remove(key)
        pageMap.remove(key)
        canLoadMoreMap[key] = true
        loadData(cat, flt, page = 1, forceRefresh = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore) return
        val key = CacheKey(state.selectedCategory, state.selectedFilter)
        if (canLoadMoreMap[key] == false) return

        val nextPage = (pageMap[key] ?: 1) + 1
        loadData(state.selectedCategory, state.selectedFilter, page = nextPage, isLoadMore = true)
    }

    private fun displayCachedOrLoad(category: HomeCategory, filter: HomeFilter) {
        val key = CacheKey(category, filter)
        val cached = cache[key]
        if (!cached.isNullOrEmpty()) {
            currentLoadJob?.cancel()
            _uiState.value = _uiState.value.copy(
                items = cached.toList(),
                isLoading = false,
                isLoadingMore = false,
                errorMessage = null
            )
        } else {
            loadData(category, filter, page = 1)
        }
    }

    private fun loadData(
        category: HomeCategory,
        filter: HomeFilter,
        page: Int = 1,
        forceRefresh: Boolean = false,
        isLoadMore: Boolean = false
    ) {
        val key = CacheKey(category, filter)
        currentLoadJob?.cancel()

        currentLoadJob = viewModelScope.launch {
            if (isLoadMore) {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            }

            try {
                val fetched = fetchForCategory(category, filter, page, forceRefresh)
                currentCoroutineContext().ensureActive()
                val validItems = filterValid(fetched)
                currentCoroutineContext().ensureActive()

                val list = cache.getOrPut(key) { mutableListOf() }
                if (page == 1) {
                    list.clear()
                }

                val existingIds = list.map { it.identifier }.toSet()
                val newUnique = validItems.filter { it.identifier !in existingIds }
                list.addAll(newUnique)

                pageMap[key] = page
                canLoadMoreMap[key] = validItems.isNotEmpty()

                if (_uiState.value.selectedCategory == category && _uiState.value.selectedFilter == filter) {
                    _uiState.value = _uiState.value.copy(
                        items = list.toList(),
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_uiState.value.selectedCategory == category && _uiState.value.selectedFilter == filter) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = if (isLoadMore) null else (e.localizedMessage ?: "Ошибка загрузки")
                    )
                }
            }
        }
    }

    private suspend fun fetchForCategory(
        category: HomeCategory,
        filter: HomeFilter,
        page: Int,
        forceRefresh: Boolean
    ): List<MediaDto> {
        return when (category) {
            HomeCategory.ALL -> {
                when (filter) {
                    HomeFilter.POPULAR -> {
                        val (trending, movies, tv, cartoons, anime) = coroutineScope {
                            val trendingRequest = async { fetchOrEmpty { repository.getTrending(page, forceRefresh = forceRefresh) } }
                            val moviesRequest = async { fetchOrEmpty { repository.getPopularMovies(page, forceRefresh) } }
                            val tvRequest = async { fetchOrEmpty { repository.getPopularTv(page, forceRefresh) } }
                            val cartoonsRequest = async { fetchOrEmpty { repository.getCartoons(page, forceRefresh) } }
                            val animeRequest = async { fetchOrEmpty { repository.getAnime(page, forceRefresh, order = "popular") } }
                            listOf(
                                trendingRequest.await(),
                                moviesRequest.await(),
                                tvRequest.await(),
                                cartoonsRequest.await(),
                                animeRequest.await()
                            )
                        }
                        val trendingMovies = trending.filter { it.isMovie }
                        val trendingTv = trending.filter { it.isTvSeries && !it.isCartoon && !it.isTalkShowOrReality }
                        val trendingCartoons = trending.filter { it.isCartoon && !it.isAnime }
                        val trendingAnime = trending.filter { it.isAnime }
                        interleaveBalancedMedia(
                            movies = interleaveLists(trendingMovies, movies.filter { it.isMovie }),
                            tv = interleaveLists(trendingTv, tv.filter { it.isTvSeries && !it.isCartoon && !it.isTalkShowOrReality }),
                            cartoons = interleaveLists(trendingCartoons, cartoons.filter { it.isCartoon && !it.isAnime }),
                            anime = interleaveLists(trendingAnime, anime.filter { it.isAnime })
                        ).ifEmpty {
                            repository.getPopularMovies(page, forceRefresh)
                        }
                    }
                    HomeFilter.TOP_RATED -> {
                        val (movies, tv, cartoons, anime) = coroutineScope {
                            val moviesRequest = async { fetchOrEmpty { repository.getTopMovies(page, forceRefresh) } }
                            val tvRequest = async { fetchOrEmpty { repository.getTopTv(page, forceRefresh) } }
                            val cartoonsRequest = async { fetchOrEmpty { repository.getCartoons(page, forceRefresh) } }
                            val animeRequest = async { fetchOrEmpty { repository.getAnime(page, forceRefresh, order = "top") } }
                            listOf(moviesRequest.await(), tvRequest.await(), cartoonsRequest.await(), animeRequest.await())
                        }
                        interleaveBalancedMedia(
                            movies = movies.filter { it.isMovie && !it.isCartoon },
                            tv = tv.filter { it.isTvSeries && !it.isCartoon && !it.isTalkShowOrReality },
                            cartoons = cartoons.filter { it.isCartoon && !it.isAnime },
                            anime = anime.filter { it.isAnime }
                        ).ifEmpty {
                            repository.getTopMovies(page, forceRefresh)
                        }
                    }
                }
            }
            HomeCategory.MOVIES -> {
                when (filter) {
                    HomeFilter.POPULAR -> repository.getPopularMovies(page, forceRefresh)
                    HomeFilter.TOP_RATED -> repository.getTopMovies(page, forceRefresh)
                }
            }
            HomeCategory.SERIES -> {
                when (filter) {
                    HomeFilter.POPULAR -> repository.getPopularTv(page, forceRefresh)
                    HomeFilter.TOP_RATED -> repository.getTopTv(page, forceRefresh)
                }.filter { !it.isTalkShowOrReality }
            }
            HomeCategory.CARTOONS -> {
                repository.getCartoons(page, forceRefresh).filter { it.isCartoon && !it.isAnime }
            }
            HomeCategory.ANIME -> {
                val order = if (filter == HomeFilter.TOP_RATED) "top" else "popular"
                repository.getAnime(page, forceRefresh, order).filter { it.isAnime }
            }
        }
    }

    private fun interleaveLists(primary: List<MediaDto>, secondary: List<MediaDto>): List<MediaDto> {
        val result = mutableListOf<MediaDto>()
        val seen = mutableSetOf<String>()
        var primaryIndex = 0
        var secondaryIndex = 0
        while (primaryIndex < primary.size || secondaryIndex < secondary.size) {
            if (primaryIndex < primary.size) {
                val item = primary[primaryIndex++]
                if (item.identifier.isNotEmpty() && seen.add(item.identifier)) result.add(item)
            }
            if (secondaryIndex < secondary.size) {
                val item = secondary[secondaryIndex++]
                if (item.identifier.isNotEmpty() && seen.add(item.identifier)) result.add(item)
            }
        }
        return result
    }

    private fun interleaveBalancedMedia(
        movies: List<MediaDto>,
        tv: List<MediaDto>,
        cartoons: List<MediaDto>,
        anime: List<MediaDto>
    ): List<MediaDto> {
        val result = mutableListOf<MediaDto>()
        val seen = mutableSetOf<String>()
        var moviesIndex = 0
        var tvIndex = 0
        var cartoonsIndex = 0
        var animeIndex = 0
        var itemsSinceLastAnime = 10
        var step = 0
        val maxSteps = movies.size + tv.size + cartoons.size + anime.size

        fun appendFrom(source: List<MediaDto>, startIndex: Int): Int {
            var index = startIndex
            while (index < source.size) {
                val item = source[index++]
                val id = item.identifier
                if (id.isBlank() || id in seen || item.isTalkShowOrReality) continue
                if (item.isAnime && itemsSinceLastAnime < 4) continue

                seen.add(id)
                result.add(item)
                itemsSinceLastAnime = if (item.isAnime) 0 else itemsSinceLastAnime + 1
                break
            }
            return index
        }

        while ((moviesIndex < movies.size || tvIndex < tv.size || cartoonsIndex < cartoons.size || animeIndex < anime.size) && step < maxSteps) {
            step++
            moviesIndex = appendFrom(movies, moviesIndex)
            tvIndex = appendFrom(tv, tvIndex)
            moviesIndex = appendFrom(movies, moviesIndex)
            tvIndex = appendFrom(tv, tvIndex)

            if (step % 2 == 1) {
                val oldSize = result.size
                cartoonsIndex = appendFrom(cartoons, cartoonsIndex)
                if (result.size == oldSize) animeIndex = appendFrom(anime, animeIndex)
            } else {
                val oldSize = result.size
                animeIndex = appendFrom(anime, animeIndex)
                if (result.size == oldSize) cartoonsIndex = appendFrom(cartoons, cartoonsIndex)
            }
        }
        return result
    }

    private fun filterValid(items: List<MediaDto>): List<MediaDto> {
        return items.filter { item ->
            val poster = item.getDisplayPosterUrl()
            val hasPoster = !poster.isNullOrBlank() && !poster.lowercase().contains("no-poster")
            val hasTitle = item.displayTitle.isNotBlank()
            hasPoster && hasTitle
        }
    }

    private suspend fun fetchOrEmpty(fetch: suspend () -> List<MediaDto>): List<MediaDto> = try {
        fetch()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyList()
    }
}
