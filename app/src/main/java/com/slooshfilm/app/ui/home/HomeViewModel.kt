package com.slooshfilm.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.data.repository.ContentCategory
import com.slooshfilm.app.data.repository.ContentType
import com.slooshfilm.app.data.repository.HdRezkaRepository
import com.slooshfilm.app.data.repository.WatchHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class UiContentType { FILMS, SERIES }

data class HomeViewState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val contentType: UiContentType = UiContentType.FILMS,
    val category: ContentCategory = ContentCategory.WATCHING_NOW,
    val filmsList: List<Movie> = emptyList(),
    val seriesList: List<Movie> = emptyList(),
    val watchHistory: List<Movie> = emptyList(),
    val error: String? = null,
    val filmsCurrentPage: Int = 1,
    val seriesCurrentPage: Int = 1,
    val canLoadMoreFilms: Boolean = true,
    val canLoadMoreSeries: Boolean = true,
    val topInset: Int = 0,
    val bottomInset: Int = 0
)

class HomeViewModel(
    private val hdRezkaRepository: HdRezkaRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {

    private val _viewState = MutableStateFlow(HomeViewState())
    val viewState = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                watchHistoryRepository.getWatchHistoryWithDetails(),
                _viewState
            ) { watchHistory, viewState ->
                val movies = watchHistory.mapNotNull { it.movie }
                _viewState.value = viewState.copy(watchHistory = movies)
            }.collect { }
        }
        loadContent(isRefresh = true)
    }

    fun setContentType(contentType: UiContentType) {
        if (contentType != _viewState.value.contentType) {
            _viewState.value = _viewState.value.copy(contentType = contentType)
            // Load content only if the list for the new type is empty
            val list = if (contentType == UiContentType.FILMS) _viewState.value.filmsList else _viewState.value.seriesList
            if (list.isEmpty()) {
                loadContent(isRefresh = true)
            }
        }
    }

    fun setCategory(category: ContentCategory) {
        if (category != _viewState.value.category) {
            // When category changes, we need to refresh both lists
            _viewState.value = _viewState.value.copy(
                category = category,
                filmsList = emptyList(),
                seriesList = emptyList(),
                filmsCurrentPage = 1,
                seriesCurrentPage = 1
            )
            loadContent(isRefresh = true)
        }
    }

    fun loadMore() {
        val state = _viewState.value
        val isLoading = state.isLoading || state.isLoadingMore
        val canLoadMore = if (state.contentType == UiContentType.FILMS) state.canLoadMoreFilms else state.canLoadMoreSeries

        if (isLoading || !canLoadMore) return

        loadContent(isRefresh = false)
    }

    fun loadContent(isRefresh: Boolean = true) {
        val state = _viewState.value
        val contentType = state.contentType
        val pageToLoad = when {
            isRefresh && contentType == UiContentType.FILMS -> 1
            isRefresh && contentType == UiContentType.SERIES -> 1
            contentType == UiContentType.FILMS -> state.filmsCurrentPage + 1
            else -> state.seriesCurrentPage + 1
        }

        _viewState.value = state.copy(
            isLoading = if (isRefresh && getListForType(contentType).isEmpty()) true else false,
            isLoadingMore = !isRefresh,
            error = null
        )

        viewModelScope.launch {
            try {
                val repoContentType = if (contentType == UiContentType.FILMS) ContentType.FILMS else ContentType.SERIES
                val (newContent, totalPages) = hdRezkaRepository.getContent(repoContentType, state.category, pageToLoad)

                if (contentType == UiContentType.FILMS) {
                    val currentList = if (isRefresh) newContent else state.filmsList + newContent
                    _viewState.value = _viewState.value.copy(
                        filmsList = currentList.distinctBy { it.url },
                        filmsCurrentPage = pageToLoad,
                        canLoadMoreFilms = newContent.isNotEmpty() && pageToLoad < totalPages
                    )
                } else {
                    val currentList = if (isRefresh) newContent else state.seriesList + newContent
                    _viewState.value = _viewState.value.copy(
                        seriesList = currentList.distinctBy { it.url },
                        seriesCurrentPage = pageToLoad,
                        canLoadMoreSeries = newContent.isNotEmpty() && pageToLoad < totalPages
                    )
                }

            } catch (e: Exception) {
                _viewState.value = _viewState.value.copy(error = e.message)
            } finally {
                _viewState.value = _viewState.value.copy(isLoading = false, isLoadingMore = false)
            }
        }
    }
    
    private fun getListForType(contentType: UiContentType): List<Movie> {
        return if (contentType == UiContentType.FILMS) _viewState.value.filmsList else _viewState.value.seriesList
    }

    fun setInsets(top: Int, bottom: Int) {
        _viewState.value = _viewState.value.copy(topInset = top, bottomInset = bottom)
    }
}