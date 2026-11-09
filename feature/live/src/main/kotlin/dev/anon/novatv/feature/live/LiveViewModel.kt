package dev.anon.novatv.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.SearchText
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.sync.ChannelFilter
import dev.anon.novatv.core.sync.ChannelRepository
import dev.anon.novatv.core.sync.GroupItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveUiState(
    val groups: List<GroupItem> = emptyList(),
    val selectedGroup: String? = null,
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val grid: Boolean = false,
    val favoriteIds: Set<Long> = emptySet(),
    val focusedChannel: Channel? = null,
    val error: String? = null,
    /** Akışlar ilk kez yayınlandı mı? İlk karede "kaynak yok" yanıp sönmesin. */
    val ready: Boolean = false,
) {
    val selectedGroupLabel: String
        get() = groups.firstOrNull { it.key == (selectedGroup ?: ALL_GROUPS_KEY) }?.label ?: "Tümü"

    companion object {
        const val ALL_GROUPS_KEY = ""
    }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LiveViewModel(
    private val repository: ChannelRepository,
    private val preferenceStore: PreferenceStore,
) : ViewModel() {

    private val selectedGroup = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")
    private val favoritesOnly = MutableStateFlow(false)
    private val focused = MutableStateFlow<Channel?>(null)
    private val error = MutableStateFlow<String?>(null)

    val channels: Flow<PagingData<Channel>> =
        combine(selectedGroup, query.debounce { if (it.isBlank()) 0L else QUERY_DEBOUNCE_MS }, favoritesOnly) {
                group, rawQuery, favorites ->
            ChannelFilter(
                group = group,
                query = SearchText.normalize(rawQuery),
                favoritesOnly = favorites,
            )
        }
            .distinctUntilChanged()
            .flatMapLatest { filter ->
                repository.channels(filter).catch { throwable ->
                    report(throwable)
                    emit(PagingData.empty())
                }
            }
            .cachedIn(viewModelScope)

    val state: StateFlow<LiveUiState> = combine(
        repository.groups(),
        combine(selectedGroup, query, favoritesOnly, focused, error, ::Controls),
        preferenceStore.liveGrid.catch { emit(false) },
        repository.favoriteChannelIds(),
    ) { groups, controls, grid, favoriteIds ->
        LiveUiState(
            groups = groups,
            selectedGroup = controls.group,
            query = controls.rawQuery,
            favoritesOnly = controls.favoritesOnly,
            grid = grid,
            favoriteIds = favoriteIds,
            focusedChannel = controls.focusedChannel,
            error = controls.message,
            ready = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LiveUiState(),
    )

    fun selectGroup(key: String?) {
        selectedGroup.value = key?.takeIf { it.isNotBlank() }
        focused.value = null
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleFavoritesOnly() {
        favoritesOnly.update { !it }
    }

    fun toggleGrid() {
        val next = !state.value.grid
        viewModelScope.launch {
            runCatching { preferenceStore.setLiveGrid(next) }.onFailure(::report)
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            runCatching { repository.toggleFavorite(channel) }.onFailure(::report)
        }
    }

    fun hideChannel(channel: Channel) {
        viewModelScope.launch {
            runCatching { repository.hideChannel(channel) }.onFailure(::report)
        }
    }

    fun hideGroup(group: String) {
        viewModelScope.launch {
            runCatching { repository.hideGroup(group) }.onFailure(::report)
        }
    }

    fun setNumber(channel: Channel, number: Int?) {
        viewModelScope.launch {
            runCatching { repository.setNumber(channel, number) }.onFailure(::report)
        }
    }

    fun onChannelFocused(channel: Channel?) {
        focused.value = channel
    }

    suspend fun lastChannel(): Channel? = repository.lastChannel()

    private fun report(throwable: Throwable) {
        error.value = throwable.message ?: throwable::class.simpleName
    }

    private data class Controls(
        val group: String?,
        val rawQuery: String,
        val favoritesOnly: Boolean,
        val focusedChannel: Channel?,
        val message: String?,
    )

    companion object {
        const val QUERY_DEBOUNCE_MS = 300L

        fun factory(repository: ChannelRepository, preferenceStore: PreferenceStore) = viewModelFactory {
            initializer { LiveViewModel(repository, preferenceStore) }
        }
    }
}
