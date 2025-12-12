
package com.slooshfilm.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slooshfilm.app.data.repository.HdRezkaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.slooshfilm.app.data.hdrezka.StreamInfo

class PlayerViewModel(
    private val hdRezkaRepository: HdRezkaRepository
) : ViewModel() {

    private val _streams = MutableStateFlow<StreamInfo?>(null)
    val streams = _streams.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun getStreams(postId: String, translatorId: String, season: String?, episode: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _streams.value = null
            try {
                _streams.value = hdRezkaRepository.getStreamsForPost(postId, translatorId, season, episode)
            } catch (e: Exception) {
                // Handle error or leave stream as null
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addWatchLater(postId: String) {
        viewModelScope.launch {
            try {
                hdRezkaRepository.addWatchLater(postId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateTime(postId: String, translatorId: String, season: String?, episode: String?, timeInSeconds: Long) {
        viewModelScope.launch {
            try {
                hdRezkaRepository.updateTime(postId, translatorId, season, episode, timeInSeconds)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
