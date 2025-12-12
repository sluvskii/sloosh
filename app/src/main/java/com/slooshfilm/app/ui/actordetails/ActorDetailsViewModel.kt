package com.slooshfilm.app.ui.actordetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slooshfilm.app.data.model.ActorDetails
import com.slooshfilm.app.data.repository.HdRezkaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ActorDetailsState(
    val isLoading: Boolean = true,
    val actor: ActorDetails? = null,
    val error: String? = null
)

class ActorDetailsViewModel(
    private val repository: HdRezkaRepository,
    private val actorLink: String
) : ViewModel() {

    private val _state = MutableStateFlow(ActorDetailsState())
    val state: StateFlow<ActorDetailsState> = _state

    private var loadActorJob: Job? = null

    init {
        loadActorDetails()
    }

    private fun loadActorDetails() {
        loadActorJob?.cancel()
        loadActorJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val actor = repository.getActorDetails(actorLink)
                _state.value = _state.value.copy(
                    isLoading = false,
                    actor = actor,
                    error = if (actor == null) "Не удалось загрузить информацию об актере" else null
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                // This is expected, do not show an error
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки: ${e.message}"
                )
            }
        }
    }

    fun retry() {
        loadActorDetails()
    }
}
