package com.slooshfilm.app.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.data.repository.HdRezkaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WatchlistState {
    object Loading : WatchlistState()
    object Empty : WatchlistState()
    data class Content(val movies: List<Movie>, val page: Int, val totalPages: Int) : WatchlistState()
}

class WatchlistViewModel(private val repository: HdRezkaRepository) : ViewModel() {

    private val _watchlistState = MutableStateFlow<WatchlistState>(WatchlistState.Loading)
    val watchlistState = _watchlistState.asStateFlow()

    private var allMovies: MutableList<Movie> = mutableListOf()
    private var currentPage = 1
    private var totalPages = 1
    private var isLoading = false

    init {
        loadWatchlist(forceRefresh = true)
    }

    fun loadNextPage() {
        if (isLoading || currentPage >= totalPages) return
        loadWatchlist()
    }

    private fun loadWatchlist(forceRefresh: Boolean = false) {
        if (isLoading) return
        isLoading = true

        if (forceRefresh) {
            currentPage = 1
            allMovies.clear()
            _watchlistState.value = WatchlistState.Loading
        }

        viewModelScope.launch {
            try {
                val (movies, newTotalPages) = repository.getWatchLater(currentPage)
                totalPages = newTotalPages
                allMovies.addAll(movies)

                if (allMovies.isEmpty()) {
                    _watchlistState.value = WatchlistState.Empty
                } else {
                    _watchlistState.value = WatchlistState.Content(allMovies.toList(), currentPage, totalPages)
                }

                if (movies.isNotEmpty()) {
                    currentPage++
                }

            } catch (e: Exception) {
                if (allMovies.isEmpty()) {
                    _watchlistState.value = WatchlistState.Empty
                }
            } finally {
                isLoading = false
            }
        }
    }
}