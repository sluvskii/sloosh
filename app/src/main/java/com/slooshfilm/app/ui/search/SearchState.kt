package com.slooshfilm.app.ui.search

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.repository.MoviesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Stable
class SearchState(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val repository = MoviesRepository.getInstance()
    private val historyPrefs = context.getSharedPreferences("sloosh_search_history", Context.MODE_PRIVATE)

    var query by mutableStateOf("")
    var results by mutableStateOf<List<MediaDto>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isLoadingMore by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var currentPage by mutableIntStateOf(1)
    var canLoadMore by mutableStateOf(true)
    var isFocused by mutableStateOf(false)

    val focusRequester = FocusRequester()

    var searchHistory by mutableStateOf(
        historyPrefs.getStringSet("history", emptySet())?.toList() ?: emptyList()
    )
        private set

    private var searchJob: Job? = null
    private var requestGeneration = 0

    fun doSearch(q: String, page: Int = 1, isAppend: Boolean = false) {
        val trimmed = q.trim()
        if (trimmed.length < 2) {
            requestGeneration++
            searchJob?.cancel()
            searchJob = null
            results = emptyList()
            isLoading = false
            isLoadingMore = false
            errorMessage = null
            currentPage = 1
            canLoadMore = true
            return
        }

        if (isAppend && isLoadingMore) return

        if (!isAppend) {
            requestGeneration++
        }
        val generation = requestGeneration

        if (isAppend) {
            isLoadingMore = true
        } else {
            searchJob?.cancel()
            isLoading = true
            errorMessage = null
            currentPage = 1
            canLoadMore = true
        }

        searchJob = scope.launch {
            if (!isAppend) {
                delay(300)
            }
            try {
                val res = repository.searchMovies(trimmed, page = page)
                if (generation != requestGeneration) return@launch
                val newItems = res?.allItems ?: emptyList()

                if (isAppend) {
                    val existingIds = results.map { it.identifier }.toSet()
                    val unique = newItems.filter { it.identifier !in existingIds }
                    results = results + unique
                    canLoadMore = unique.isNotEmpty()
                    currentPage = page
                } else {
                    results = newItems
                    canLoadMore = newItems.isNotEmpty()
                    if (newItems.isNotEmpty()) {
                        addHistory(trimmed)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation != requestGeneration) return@launch
                if (!isAppend) {
                    errorMessage = e.localizedMessage ?: "Ошибка поиска"
                    results = emptyList()
                }
            } finally {
                if (generation == requestGeneration && currentCoroutineContext().isActive) {
                    isLoading = false
                    isLoadingMore = false
                }
            }
        }
    }

    fun clearQuery() {
        requestGeneration++
        searchJob?.cancel()
        searchJob = null
        query = ""
        results = emptyList()
        errorMessage = null
        isLoading = false
        isLoadingMore = false
    }

    fun addHistory(item: String) {
        val trimmed = item.trim()
        if (trimmed.length < 2) return
        val current = searchHistory.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val limited = current.take(10)
        searchHistory = limited
        historyPrefs.edit().putStringSet("history", limited.toSet()).apply()
    }

    fun removeHistory(item: String) {
        val current = searchHistory.toMutableList()
        current.remove(item)
        searchHistory = current
        historyPrefs.edit().putStringSet("history", current.toSet()).apply()
    }

    fun clearHistory() {
        searchHistory = emptyList()
        historyPrefs.edit().remove("history").apply()
    }
}

@Composable
fun rememberSearchState(
    context: Context = LocalContext.current,
    scope: CoroutineScope = rememberCoroutineScope()
): SearchState {
    return remember(context, scope) {
        SearchState(context, scope)
    }
}
