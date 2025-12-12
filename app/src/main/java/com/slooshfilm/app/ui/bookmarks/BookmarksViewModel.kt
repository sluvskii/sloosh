package com.slooshfilm.app.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.data.models.Bookmark
import com.slooshfilm.app.data.repository.HdRezkaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookmarksViewState(
    val isLoading: Boolean = true,
    val bookmarks: List<Bookmark> = emptyList(),
    val filmsByBookmarkId: Map<String, List<Movie>> = emptyMap(),
    val selectedBookmarkId: String? = null,
    val loadingBookmarkId: String? = null,
    val error: String? = null
)

class BookmarksViewModel(private val repository: HdRezkaRepository) : ViewModel() {

    private val _viewState = MutableStateFlow(BookmarksViewState())
    val viewState = _viewState.asStateFlow()

    fun getBookmarks() {
        _viewState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val bookmarksPage = repository.getBookmarksPage()
                val bookmarks = bookmarksPage.categories
                _viewState.update {
                    it.copy(isLoading = false, bookmarks = bookmarks)
                }
                // Выбираем и загружаем первую закладку, если она есть
                bookmarks.firstOrNull()?.let { selectBookmark(it) }
            } catch (e: Exception) {
                _viewState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectBookmark(bookmark: Bookmark) {
        _viewState.update { it.copy(selectedBookmarkId = bookmark.id) }
        // Загружаем фильмы для закладки, если их еще нет
        if (_viewState.value.filmsByBookmarkId[bookmark.id].isNullOrEmpty()) {
            loadFilmsForBookmark(bookmark)
        }
    }

    private fun loadFilmsForBookmark(bookmark: Bookmark) {
        _viewState.update { it.copy(loadingBookmarkId = bookmark.id) }
        viewModelScope.launch {
            try {
                val films = repository.getFilmsForBookmark(bookmark)
                _viewState.update { state ->
                    val newMap = state.filmsByBookmarkId.toMutableMap()
                    newMap[bookmark.id] = films
                    state.copy(filmsByBookmarkId = newMap, loadingBookmarkId = null)
                }
            } catch (e: Exception) {
                _viewState.update { it.copy(loadingBookmarkId = null, error = e.message) }
            }
        }
    }

    fun createBookmarkCategory(name: String) {
        viewModelScope.launch {
            val success = repository.createBookmarkCategory(name)
            if (success) {
                getBookmarks() // Перезагружаем все, чтобы увидеть новую категорию
            }
        }
    }
}
