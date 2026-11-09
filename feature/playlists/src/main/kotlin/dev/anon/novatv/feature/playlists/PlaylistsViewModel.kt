package dev.anon.novatv.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.sync.PlaylistRepository
import dev.anon.novatv.core.sync.SyncEngine
import dev.anon.novatv.core.sync.SyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistsUiState(
    val items: List<Playlist> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class PlaylistsViewModel(
    private val repository: PlaylistRepository,
    private val engine: SyncEngine,
    private val scheduler: SyncScheduler? = null,
) : ViewModel() {

    val state: StateFlow<PlaylistsUiState> = repository.observeAll()
        .map { PlaylistsUiState(items = it, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PlaylistsUiState(),
        )

    init {
        viewModelScope.launch {
            repository.observeAll().first().let { scheduler?.scheduleAll(it) }
        }
    }

    fun refresh(playlist: Playlist) {
        // Ekran hemen kapansa da senkron tamamlanmalı.
        engine.requestSync(playlist.id)
    }

    fun setActive(playlist: Playlist, active: Boolean) {
        viewModelScope.launch {
            runCatching {
                repository.update(playlist.copy(isActive = active))
                scheduler?.scheduleAll(listOf(playlist.copy(isActive = active)))
            }.onFailure { message -> report(message) }
        }
    }

    fun move(playlist: Playlist, direction: Int) {
        viewModelScope.launch {
            runCatching {
                val ordered = repository.observeAll().first().sortedBy { it.sortOrder }
                val index = ordered.indexOfFirst { it.id == playlist.id }
                val target = index + direction
                if (index < 0 || target !in ordered.indices) return@runCatching
                val neighbour = ordered[target]
                repository.update(playlist.copy(sortOrder = neighbour.sortOrder))
                repository.update(neighbour.copy(sortOrder = playlist.sortOrder))
            }.onFailure { message -> report(message) }
        }
    }

    fun delete(playlist: Playlist) {
        viewModelScope.launch {
            runCatching { repository.remove(playlist.id) }
                .onFailure { message -> report(message) }
        }
    }

    private fun report(error: Throwable) {
        // Hata durumu sonraki sürümde snackbar ile gösterilecek; şimdilik sessizce yutulmuyor.
        _lastError.value = error.message ?: error::class.simpleName
    }

    private val _lastError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    companion object {
        fun factory(
            repository: PlaylistRepository,
            engine: SyncEngine,
            scheduler: SyncScheduler? = null,
        ) = viewModelFactory {
            initializer { PlaylistsViewModel(repository, engine, scheduler) }
        }
    }
}
