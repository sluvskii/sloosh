package com.slooshfilm.app.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slooshfilm.app.data.hdrezka.Comment
import com.slooshfilm.app.data.repository.HdRezkaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommentsState(
    val isLoading: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val error: String? = null,
    val page: Int = 1,
    val totalPages: Int = 1,
    val isLoadingNextPage: Boolean = false
) {
    val canLoadMore: Boolean get() = page < totalPages && !isLoadingNextPage && !isLoading
}

class CommentsViewModel(
    private val repository: HdRezkaRepository,
    private val postId: String
) : ViewModel() {

    private val _state = MutableStateFlow(CommentsState())
    val state: StateFlow<CommentsState> = _state.asStateFlow()

    init {
        loadComments(1)
    }

    fun loadComments(page: Int) {
        if (page == 1) {
            _state.value = CommentsState(isLoading = true)
        } else {
            _state.value = _state.value.copy(isLoadingNextPage = true)
        }

        viewModelScope.launch {
            try {
                val (newComments, totalPages) = repository.getComments(postId, page)
                val currentComments = if (page == 1) emptyList() else _state.value.comments
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoadingNextPage = false,
                    comments = currentComments + newComments,
                    page = page,
                    totalPages = totalPages,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoadingNextPage = false,
                    error = "Ошибка загрузки: ${e.message}"
                )
            }
        }
    }

    fun loadNextPage() {
        if (_state.value.canLoadMore) {
            loadComments(_state.value.page + 1)
        }
    }

    fun retry() {
        loadComments(1)
    }
}