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

    private fun loadHistory(): List<String> {
        val raw = historyPrefs.getString("history_ordered_v2", null)
        if (!raw.isNullOrBlank()) {
            try {
                val list = com.google.gson.Gson().fromJson<List<String>>(
                    raw,
                    object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                )
                if (!list.isNullOrEmpty()) return list
            } catch (_: Exception) {}
        }
        return historyPrefs.getStringSet("history", emptySet())?.toList() ?: emptyList()
    }

    var searchHistory by mutableStateOf(loadHistory())
        private set

    private fun persistHistory(list: List<String>) {
        searchHistory = list
        val json = com.google.gson.Gson().toJson(list)
        historyPrefs.edit()
            .putString("history_ordered_v2", json)
            .putStringSet("history", list.toSet())
            .apply()
    }

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
                    errorMessage = when {
                        e is java.net.UnknownHostException ||
                        e is java.net.ConnectException ||
                        e is java.net.SocketTimeoutException ||
                        e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                        e.message?.contains("Failed to connect", ignoreCase = true) == true ||
                        e.message?.contains("api.sloosh", ignoreCase = true) == true ||
                        e.message?.contains("workers.dev", ignoreCase = true) == true -> {
                            "Не удалось подключиться к серверу. Проверьте интернет-соединение."
                        }
                        else -> {
                            "Не удалось выполнить поиск. Попробуйте повторить запрос."
                        }
                    }
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
        val limited = current.take(15)
        persistHistory(limited)
    }

    fun removeHistory(item: String) {
        val current = searchHistory.toMutableList()
        current.remove(item)
        persistHistory(current)
    }

    fun clearHistory() {
        searchHistory = emptyList()
        historyPrefs.edit()
            .remove("history")
            .remove("history_ordered_v2")
            .apply()
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
