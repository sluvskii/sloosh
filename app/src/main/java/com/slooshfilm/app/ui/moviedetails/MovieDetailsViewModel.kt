package com.slooshfilm.app.ui.moviedetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.slooshfilm.app.data.model.Episode
import com.slooshfilm.app.data.model.MovieDetails
import com.slooshfilm.app.data.repository.HdRezkaRepository
import com.slooshfilm.app.data.hdrezka.StreamInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

data class MovieDetailsViewState(
    val isLoading: Boolean = true,
    val movieDetails: MovieDetails? = null,
    val isBookmarked: Boolean = false,
    val error: String? = null
)

class MovieDetailsViewModel(private val repository: HdRezkaRepository) : ViewModel() {

    private val _viewState = MutableLiveData<MovieDetailsViewState>()
    val viewState: LiveData<MovieDetailsViewState> = _viewState

    private var loadDetailsJob: Job? = null

    fun loadMovieDetails(url: String) {
        loadDetailsJob?.cancel()
        _viewState.value = MovieDetailsViewState(isLoading = true)

        loadDetailsJob = viewModelScope.launch {
            try {
                val details = repository.getMovieDetails(url)

                details?.postId?.let { postId ->
                    val isBookmarkedDeferred = async { repository.isBookmarked(postId) }

                    var detailsWithSeasons = details
                    if (details.isSerial && details.translators.isNotEmpty() && details.seasons.isEmpty()) {
                        try {
                            val translatorList = details.translators.values.toList()
                            Log.d("MovieDetailsViewModel", "Fetching episodes for ${translatorList.size} translators in parallel")
                            
                            val episodeFetches = translatorList.map { translator ->
                                async {
                                    try {
                                        val tid = translator.id ?: return@async null to emptyMap<Int, List<Episode>>()
                                        Log.d("MovieDetailsViewModel", "Fetching episodes for translator: ${translator.name} (id=$tid)")
                                        val seasons = repository.getEpisodes(postId, tid, details.pageUrl)
                                        Log.d("MovieDetailsViewModel", "Translator ${translator.name}: got ${seasons.values.sumOf { it.size }} episodes")
                                        tid to seasons
                                    } catch (e: Exception) {
                                        Log.w("MovieDetailsViewModel", "Failed to fetch episodes for translator", e)
                                        null to emptyMap<Int, List<Episode>>()
                                    }
                                }
                            }
                            
                            val results = episodeFetches.map { it.await() }
                            
                            var bestSeasons: Map<Int, List<Episode>> = emptyMap()
                            var bestTranslatorId: String? = null
                            
                            for ((tid, seasons) in results) {
                                if (tid != null && seasons.isNotEmpty()) {
                                    val count = seasons.values.sumOf { it.size }
                                    val bestCount = bestSeasons.values.sumOf { it.size }
                                    if (count > bestCount) {
                                        bestSeasons = seasons
                                        bestTranslatorId = tid
                                        Log.d("MovieDetailsViewModel", "New best translator: $tid with $count episodes")
                                    }
                                }
                            }

                            if (bestSeasons.isNotEmpty()) {
                                detailsWithSeasons = details.copy(seasons = bestSeasons, selectedTranslatorId = bestTranslatorId)
                                Log.d("MovieDetailsViewModel", "Selected translator for episodes: $bestTranslatorId with ${bestSeasons.values.sumOf { it.size }} total episodes across all seasons")
                            } else {
                                Log.w("MovieDetailsViewModel", "No episodes found for any translator")
                            }
                        } catch (ex: Exception) {
                            Log.w("MovieDetailsViewModel", "Failed to fetch episodes for translators", ex)
                        }
                    }

                    _viewState.postValue(
                        MovieDetailsViewState(
                            isLoading = false,
                            movieDetails = detailsWithSeasons,
                            isBookmarked = false
                        )
                    )

                    val isBookmarked = isBookmarkedDeferred.await()
                    _viewState.value?.let { currentState ->
                        if (currentState.movieDetails?.postId == postId) {
                            _viewState.postValue(currentState.copy(isBookmarked = isBookmarked))
                        }
                    }
                } ?: run {
                    _viewState.postValue(MovieDetailsViewState(isLoading = false, error = "Не удалось загрузить детали фильма."))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _viewState.postValue(MovieDetailsViewState(isLoading = false, error = e.message))
            }
        }
    }

    fun toggleBookmark(categoryId: String? = null) {
        val currentDetails = _viewState.value?.movieDetails ?: return
        val postId = currentDetails.postId ?: return
        val isCurrentlyBookmarked = _viewState.value?.isBookmarked ?: false

        viewModelScope.launch {
            val success = if (isCurrentlyBookmarked) {
                repository.removeBookmark(postId)
            } else {
                if (categoryId != null) {
                    repository.addBookmark(postId, categoryId)
                } else {
                    repository.addBookmark(postId)
                }
            }
            if (success) {
                _viewState.postValue(
                    _viewState.value?.copy(isBookmarked = !isCurrentlyBookmarked)
                )
            }
        }
    }

    suspend fun getStreams(postId: String, translatorId: String): StreamInfo {
        return repository.getStreamsForPost(postId, translatorId)
    }

    suspend fun getEpisodes(postId: String, translatorId: String, refererUrl: String? = null): Map<Int, List<Episode>> {
        return repository.getEpisodes(postId, translatorId, refererUrl)
    }

    suspend fun getStreamsForEpisode(postId: String, translatorId: String, season: String, episode: String): StreamInfo {
        return repository.getStreamsForPost(postId, translatorId, season, episode)
    }
}
