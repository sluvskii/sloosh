package com.slooshfilm.app.data.messenger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class MessengerRepository {
    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SlooshUser>>(emptyList())
    val searchResults: StateFlow<List<SlooshUser>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun searchUsers(query: String): List<SlooshUser> = withContext(Dispatchers.Default) {
        val q = query.trim().lowercase().removePrefix("@")
        if (q.isBlank()) {
            _searchResults.value = emptyList()
            return@withContext emptyList()
        }
        // Real user search will be populated when connected to backend database
        _searchResults.value = emptyList()
        emptyList()
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun deleteConversation(chatId: String) {
        _conversations.value = _conversations.value.filter { it.chatId != chatId }
    }

    companion object {
        @Volatile
        private var INSTANCE: MessengerRepository? = null

        fun getInstance(): MessengerRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MessengerRepository().also { INSTANCE = it }
            }
        }
    }
}
