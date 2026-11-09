package dev.anon.novatv.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.SourceAccount
import dev.anon.novatv.core.model.SyncState
import dev.anon.novatv.core.source.SourceAdapterRegistry
import dev.anon.novatv.core.sync.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistFormState(
    val id: Long? = null,
    val name: String = "",
    val type: PlaylistType = PlaylistType.XTREAM,
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val epgUrl: String = "",
    val userAgent: String = "",
    val referer: String = "",
    val syncIntervalHours: String = "24",
    val testing: Boolean = false,
    val saving: Boolean = false,
    val testResult: String? = null,
    val error: String? = null,
    val savedId: Long? = null,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && url.isNotBlank() && when (type) {
            PlaylistType.XTREAM -> username.isNotBlank() && password.isNotBlank()
            PlaylistType.M3U -> true
            PlaylistType.STALKER -> MAC_PATTERN.matches(username.trim())
        }

    val connectionStatus: String?
        get() = testResult

    private companion object {
        val MAC_PATTERN = Regex("""^(?:[0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$""")
    }
}

class PlaylistFormViewModel(
    private val repository: PlaylistRepository,
    private val playlistId: Long? = null,
    private val testConnection: suspend (Playlist, Credentials) -> SourceAccount = { _, _ ->
        SourceAccount(status = "Active")
    },
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistFormState(id = playlistId))
    val state: StateFlow<PlaylistFormState> = _state.asStateFlow()

    init {
        if (playlistId != null) load(playlistId)
    }

    fun setName(value: String) = _state.update { it.copy(name = value, error = null) }
    fun setType(value: PlaylistType) {
        _state.update { it.copy(type = value, error = null, testResult = null) }
    }
    fun setUrl(value: String) = _state.update { it.copy(url = value, error = null) }
    fun setUsername(value: String) = _state.update { it.copy(username = value, error = null) }
    fun setPassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun setEpgUrl(value: String) = _state.update { it.copy(epgUrl = value) }
    fun setUserAgent(value: String) = _state.update { it.copy(userAgent = value) }
    fun setReferer(value: String) = _state.update { it.copy(referer = value) }
    fun setSyncInterval(value: String) = _state.update { it.copy(syncIntervalHours = value.filter { ch -> ch.isDigit() }) }

    fun test() {
        val snapshot = _state.value
        if (!snapshot.canSave || snapshot.testing) return
        _state.update { it.copy(testing = true, error = null, testResult = null) }
        viewModelScope.launch {
            runCatching { testConnection(snapshot.toPlaylist(), snapshot.toCredentials()) }
                .onSuccess { account ->
                    _state.update { it.copy(testing = false, testResult = account.status) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(testing = false, error = error.message ?: "Bağlantı kurulamadı")
                    }
                }
        }
    }

    fun save() {
        val snapshot = _state.value
        if (!snapshot.canSave || snapshot.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val existing = repository.observeAll().first().firstOrNull { it.id == snapshot.id }
                val nextOrder = existing?.sortOrder
                    ?: (repository.observeAll().first().maxOfOrNull { it.sortOrder }?.plus(1) ?: 0)
                val playlist = snapshot.toPlaylist().copy(
                    isActive = existing?.isActive ?: true,
                    sortOrder = nextOrder,
                    lastSyncAt = existing?.lastSyncAt,
                    syncState = existing?.syncState ?: SyncState.NEVER,
                )
                val credentials = snapshot.toCredentials()
                if (playlist.id == 0L) {
                    repository.add(playlist, credentials)
                } else {
                    repository.update(playlist)
                    repository.updateCredentials(playlist.id, credentials)
                    playlist.id
                }
            }.onSuccess { id ->
                _state.update { it.copy(saving = false, savedId = id) }
            }.onFailure { error ->
                _state.update { it.copy(saving = false, error = error.message ?: "Kaydedilemedi") }
            }
        }
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            runCatching { repository.load(id) }
                .onSuccess { (playlist, credentials) ->
                    _state.update {
                        it.copy(
                            id = playlist.id,
                            name = playlist.name,
                            type = playlist.type,
                            url = playlist.baseUrl,
                            username = credentials?.username ?: playlist.username.orEmpty(),
                            password = credentials?.password.orEmpty(),
                            epgUrl = playlist.epgUrl.orEmpty(),
                            userAgent = playlist.userAgent.orEmpty(),
                            referer = playlist.referer.orEmpty(),
                            syncIntervalHours = playlist.syncIntervalHours.toString(),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message ?: "Kaynak yüklenemedi") }
                }
        }
    }

    private fun PlaylistFormState.toPlaylist() = Playlist(
        id = id ?: 0L,
        type = type,
        name = name.trim(),
        baseUrl = url.trim(),
        // Kimlik bilgisi (Xtream kullanıcısı, Stalker MAC'i) Room'a yazılmaz;
        // yalnızca Keystore'lu depoda tutulur.
        username = null,
        epgUrl = epgUrl.trim().ifBlank { null },
        userAgent = userAgent.trim().ifBlank { null },
        referer = referer.trim().ifBlank { null },
        syncIntervalHours = syncIntervalHours.toIntOrNull()?.coerceIn(1, 168) ?: 24,
    )

    private fun PlaylistFormState.toCredentials() = Credentials(
        username = username.trim().ifBlank { null },
        password = password.takeIf { type == PlaylistType.XTREAM }?.ifBlank { null },
    )

    companion object {
        fun factory(
            repository: PlaylistRepository,
            playlistId: Long? = null,
            registry: SourceAdapterRegistry? = null,
        ) = viewModelFactory {
            initializer {
                PlaylistFormViewModel(
                    repository = repository,
                    playlistId = playlistId,
                    testConnection = { playlist, credentials ->
                        requireNotNull(registry) { "Kaynak kayıt defteri yok" }
                            .adapterFor(playlist, credentials)
                            .authenticate()
                    },
                )
            }
        }
    }
}
