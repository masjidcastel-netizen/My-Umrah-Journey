package com.masjidcastel.myumrahjourney.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.masjidcastel.myumrahjourney.app.MyUmrahJourneyApplication
import com.masjidcastel.myumrahjourney.core.data.DataStoreRepository
import com.masjidcastel.myumrahjourney.core.player.PlayerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface JourneyUiState {
    /** DataStore has not delivered the stored player yet. */
    data object Loading : JourneyUiState

    data class Ready(val player: PlayerState) : JourneyUiState
}

/**
 * Single source of truth for the UI. It exposes the stored [PlayerState] and is the only place
 * that writes to [DataStoreRepository]. Writes run in [viewModelScope], so they finish even if
 * the user navigates away right after tapping Save.
 */
class JourneyViewModel(
    private val repository: DataStoreRepository,
) : ViewModel() {

    val uiState: StateFlow<JourneyUiState> = repository.playerState
        .map<PlayerState, JourneyUiState> { player -> JourneyUiState.Ready(player) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = JourneyUiState.Loading,
        )

    fun saveSettings(state: PlayerState) {
        viewModelScope.launch { repository.save(state) }
    }

    fun resetSettings() {
        viewModelScope.launch { repository.clear() }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as MyUmrahJourneyApplication
                JourneyViewModel(application.repository)
            }
        }
    }
}
