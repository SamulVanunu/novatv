package dev.anon.novatv.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.anon.novatv.core.model.ItemType
import dev.anon.novatv.core.model.Movie
import dev.anon.novatv.core.model.Series
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.sync.ContinueItem
import dev.anon.novatv.core.sync.LibraryFilter
import dev.anon.novatv.core.sync.LibraryRepository
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryTab { MOVIES, SERIES }

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.MOVIES,
    val query: String = "",
    val genre: String? = null,
    val year: Int? = null,
    val genres: List<String> = emptyList(),
    val years: List<Int> = emptyList(),
    val continueItems: List<ContinueItem> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val syncing: Boolean = false,
    val message: String? = null,
    val ready: Boolean = false,
) {
    val filtered: Boolean get() = query.isNotBlank() || genre != null || year != null
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class LibraryViewModel(
    private val repository: LibraryRepository,
    private val preferences: PreferenceStore,
) : ViewModel() {

    private val tab = MutableStateFlow(LibraryTab.MOVIES)
    private val query = MutableStateFlow("")
    private val genre = MutableStateFlow<String?>(null)
    private val year = MutableStateFlow<Int?>(null)
    private val syncing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val ready = MutableStateFlow(false)

    private val filters = combine(
        tab,
        query.debounce { if (it.isBlank()) 0L else QUERY_DEBOUNCE_MS },
        genre,
        year,
    ) { tab, query, genre, year -> LibraryFilter(query = query, genre = genre, year = year) }
        .distinctUntilChanged()

    val movies: Flow<PagingData<Movie>> = filters
        .flatMapLatest { repository.movies(it) }
        .cachedIn(viewModelScope)

    val series: Flow<PagingData<Series>> = filters
        .flatMapLatest { repository.series(it) }
        .cachedIn(viewModelScope)

    private val catalogMeta = combine(
        repository.genres().catch { emit(emptyList()) },
        repository.years().catch { emit(emptyList()) },
    ) { genres, years -> genres to years }

    private val status = combine(syncing, message, ready) { syncing, message, ready ->
        Triple(syncing, message, ready)
    }

    val state: StateFlow<LibraryUiState> = combine(
        combine(tab, query, genre, year) { tab, query, genre, year ->
            Quad(tab, query, genre, year)
        },
        catalogMeta,
        repository.continueWatching().catch { emit(emptyList()) },
        repository.favoriteIds(ItemType.MOVIE).catch { emit(emptySet()) },
        status,
    ) { selection, meta, continueItems, favorites, (syncing, message, ready) ->
        LibraryUiState(
            tab = selection.tab,
            query = selection.query,
            genre = selection.genre,
            year = selection.year,
            genres = meta.first,
            years = meta.second,
            continueItems = continueItems,
            favoriteIds = favorites,
            syncing = syncing,
            message = message,
            ready = ready,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    init {
        refresh(showMessage = false)
    }

    fun selectTab(value: LibraryTab) {
        tab.value = value
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setGenre(value: String?) {
        genre.value = value
    }

    fun setYear(value: Int?) {
        year.value = value
    }

    fun clearFilters() {
        query.value = ""
        genre.value = null
        year.value = null
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            runCatching { repository.toggleFavorite(ItemType.MOVIE, movie.id) }
                .onFailure { message.value = it.message ?: "Favori güncellenemedi" }
        }
    }

    /** Kitaplığı kaynaktan yeniler; ilk açılışta sessizce çalışır. */
    fun refresh(showMessage: Boolean = true) {
        if (syncing.value) return
        viewModelScope.launch {
            syncing.value = true
            if (showMessage) message.value = null
            val result = runCatching { repository.syncAll() }
                .onFailure { throwable ->
                    message.value = throwable.message ?: "Kitaplık yenilenemedi"
                }
                .getOrNull()
            ready.value = true
            syncing.value = false
            if (showMessage && result != null) {
                message.value = when {
                    result.movies + result.series == 0 -> "Kaynakta film/dizi bulunamadı"
                    else -> "${result.movies} film, ${result.series} dizi güncellendi"
                }
            }
        }
    }

    private data class Quad(
        val tab: LibraryTab,
        val query: String,
        val genre: String?,
        val year: Int?,
    )

    companion object {
        private const val QUERY_DEBOUNCE_MS = 250L

        fun factory(repository: LibraryRepository, preferences: PreferenceStore) =
            viewModelFactory {
                initializer { LibraryViewModel(repository, preferences) }
            }
    }
}
