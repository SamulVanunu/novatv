package dev.anon.novatv.feature.multiwall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.SearchText
import dev.anon.novatv.core.sync.ChannelRepository
import dev.anon.novatv.core.sync.ResolvedStream
import dev.anon.novatv.core.sync.StreamResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Bir kutunun çözülmüş akışı; [pane] 0..3. */
data class PaneStream(
    val pane: Int,
    val channelId: Long,
    val title: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

data class MultiWallUiState(
    val layout: Int = 4,
    val panes: List<Channel?> = List(4) { null },
    val streams: List<PaneStream> = emptyList(),
    val focused: Int = 0,
    val pickerPane: Int? = null,
    val channels: List<Channel> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val message: String? = null,
) {
    val visiblePanes: Int get() = if (layout == 2) 2 else 4
    val pickerChannels: List<Channel>
        get() {
            val text = SearchText.normalize(query)
            if (text.isEmpty()) return channels
            return channels.filter { channel ->
                SearchText.normalize(channel.name).contains(text) ||
                    SearchText.normalize(channel.groupName).contains(text)
            }
        }

    fun streamOf(pane: Int): PaneStream? = streams.firstOrNull { it.pane == pane }
}

class MultiWallViewModel(
    private val repository: ChannelRepository,
    private val resolver: StreamResolver,
    initialCount: Int = 4,
) : ViewModel() {

    private val _state = MutableStateFlow(MultiWallUiState(layout = if (initialCount == 2) 2 else 4))
    val state: StateFlow<MultiWallUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val channels = runCatching { repository.allChannels() }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(message = throwable.message ?: "Kanallar yüklenemedi", loading = false)
                    }
                }
                .getOrDefault(emptyList())
            _state.update { it.copy(channels = channels, loading = false) }
            // Açılışta ilk kutuyu son izlenen veya ilk kanalla doldur.
            val seed = channels.firstOrNull()
            if (seed != null && _state.value.panes[0] == null) {
                assign(0, seed)
            }
        }
    }

    fun setLayout(count: Int) {
        val layout = if (count == 2) 2 else 4
        _state.update { state ->
            state.copy(
                layout = layout,
                focused = state.focused.coerceAtMost(layout - 1),
                streams = state.streams.filter { it.pane < layout },
            )
        }
    }

    fun focus(pane: Int) {
        if (pane !in 0 until _state.value.visiblePanes) return
        _state.update { it.copy(focused = pane) }
    }

    fun openPicker(pane: Int) {
        if (pane !in 0 until _state.value.visiblePanes) return
        _state.update { it.copy(pickerPane = pane, focused = pane) }
    }

    fun closePicker() {
        _state.update { it.copy(pickerPane = null) }
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun assign(pane: Int, channel: Channel) {
        _state.update { it.copy(pickerPane = null) }
        viewModelScope.launch {
            val stream = runCatching { resolver.resolve(channel) }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(message = throwable.message ?: "Yayın açılamadı")
                    }
                }
                .getOrNull()
            if (stream == null) return@launch
            applyStream(pane, channel, stream)
        }
    }

    fun clearPane(pane: Int) {
        _state.update { state ->
            state.copy(
                panes = state.panes.mapIndexed { index, channel ->
                    if (index == pane) null else channel
                },
                streams = state.streams.filterNot { it.pane == pane },
            )
        }
    }

    fun retry(pane: Int) {
        val channel = _state.value.panes.getOrNull(pane) ?: return
        assign(pane, channel)
    }

    private fun applyStream(pane: Int, channel: Channel, stream: ResolvedStream) {
        _state.update { state ->
            state.copy(
                panes = state.panes.mapIndexed { index, item ->
                    if (index == pane) channel else item
                },
                streams = state.streams.filterNot { it.pane == pane } + PaneStream(
                    pane = pane,
                    channelId = channel.id,
                    title = channel.name,
                    url = stream.url,
                    headers = stream.headers,
                ),
                message = null,
            )
        }
    }

    companion object {
        const val MAX_PANES = 4

        fun factory(
            repository: ChannelRepository,
            resolver: StreamResolver,
            count: Int,
        ) = viewModelFactory {
            initializer { MultiWallViewModel(repository, resolver, count) }
        }
    }
}
