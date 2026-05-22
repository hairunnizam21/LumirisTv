package com.suzunei.lumirisiptv.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suzunei.lumirisiptv.data.repository.PlaylistRepository
import com.suzunei.lumirisiptv.domain.model.Category
import com.suzunei.lumirisiptv.domain.model.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Top-level UI state for the main screen.
 *
 * @param categories ordered list of categories with their channels.
 * @param currentChannel the channel currently bound to the player. Null until the playlist loads.
 * @param isLoading true for the initial full-screen load.
 * @param isRefreshing true when a manual refresh is in flight (inline spinner only).
 * @param isFullscreen true when the user has tapped the player to hide the channel list.
 * @param errorMessage transient error to surface in a snackbar / toast.
 */
data class MainUiState(
    val categories: List<Category> = emptyList(),
    val currentChannel: Channel? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isFullscreen: Boolean = false,
    val errorMessage: String? = null,
)

class MainViewModel(
    private val repository: PlaylistRepository = PlaylistRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState(isLoading = true))
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.loadPlaylist(forceRefresh = false) }
                .onSuccess { categories ->
                    _state.update {
                        it.copy(
                            categories = categories,
                            currentChannel = categories.firstOrNull()?.channels?.firstOrNull(),
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = t.message ?: "Failed to load playlist",
                        )
                    }
                }
        }
    }

    /**
     * Manual refresh from the toolbar. Re-fetches the playlist with cache bypass. If the
     * currently-playing channel still exists in the new list (matched by `id`), keep it as the
     * current channel so the player isn't interrupted.
     */
    fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, errorMessage = null) }
            runCatching { repository.loadPlaylist(forceRefresh = true) }
                .onSuccess { categories ->
                    _state.update { prev ->
                        val previousId = prev.currentChannel?.id
                        val flat = categories.flatMap { it.channels }
                        val preserved = previousId?.let { id -> flat.firstOrNull { it.id == id } }
                        prev.copy(
                            categories = categories,
                            currentChannel = preserved
                                ?: prev.currentChannel
                                ?: categories.firstOrNull()?.channels?.firstOrNull(),
                            isRefreshing = false,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = t.message ?: "Refresh failed",
                        )
                    }
                }
        }
    }

    fun selectChannel(channel: Channel) {
        _state.update { it.copy(currentChannel = channel) }
    }

    fun toggleFullscreen() {
        _state.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun exitFullscreen() {
        if (_state.value.isFullscreen) {
            _state.update { it.copy(isFullscreen = false) }
        }
    }

    fun consumeError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
