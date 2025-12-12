package com.slooshfilm.app.ui.search

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.data.repository.HdRezkaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Content(val results: List<Movie>) : SearchState()
    data class Suggestions(val suggestions: List<String>) : SearchState()
    data class Error(val message: String) : SearchState()
    object EmptyResult : SearchState()
}

class SearchViewModel(private val repository: HdRezkaRepository) : ViewModel() {

    private var searchJob: Job? = null
    private var suggestionsJob: Job? = null
    private val _searchState = MutableLiveData<SearchState>()
    val searchState: LiveData<SearchState> = _searchState

    private var currentQuery: String = ""

    init {
        _searchState.value = SearchState.Idle
    }

    fun onSearchQueryChanged(query: String) {
        val newQuery = query.trim()
        if (newQuery == currentQuery && _searchState.value !is SearchState.Idle) {
            return // Query hasn't changed, do nothing unless we are in idle state.
        }
        currentQuery = newQuery

        searchJob?.cancel()
        suggestionsJob?.cancel()

        if (newQuery.isBlank()) {
            _searchState.value = SearchState.Idle
            return
        }

        // Don't show loader for suggestions
        // _searchState.value = SearchState.Loading

        suggestionsJob = viewModelScope.launch {
            delay(200) // Debounce for suggestions
            try {
                val suggestions = repository.getSearchSuggestions(newQuery)
                if (currentQuery == newQuery) {
                    _searchState.value = SearchState.Suggestions(suggestions)
                }
            } catch (e: CancellationException) {
                // Expected
            } catch (e: Exception) {
                // Don't show error for suggestions, just log it
                Log.e("SearchViewModel", "Failed to get search suggestions", e)
            }
        }
    }

    fun onSearchConfirmed(query: String) {
        val newQuery = query.trim()
        currentQuery = newQuery
        
        searchJob?.cancel()
        suggestionsJob?.cancel()

        if (newQuery.isBlank()) {
            _searchState.value = SearchState.Idle
            return
        }

        _searchState.value = SearchState.Loading

        searchJob = viewModelScope.launch {
            try {
                val results = repository.search(newQuery)
                if (currentQuery == newQuery) {
                    if (results.isNotEmpty()) {
                        _searchState.value = SearchState.Content(results)
                    } else {
                        _searchState.value = SearchState.EmptyResult
                    }
                }
            } catch (e: CancellationException) {
                // Expected
            } catch (e: Exception) {
                if (currentQuery == newQuery) {
                    _searchState.value = SearchState.Error(e.message ?: "Произошла неизвестная ошибка")
                }
            }
        }
    }
}
