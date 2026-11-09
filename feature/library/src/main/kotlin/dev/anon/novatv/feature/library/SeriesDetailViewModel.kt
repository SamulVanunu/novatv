package dev.anon.novatv.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.anon.novatv.core.model.Episode
import dev.anon.novatv.core.model.ItemType
import dev.anon.novatv.core.model.Season
import dev.anon.novatv.core.model.Series
import dev.anon.novatv.core.sync.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeriesDetailUiState(
    val series: Series? = null,
    val seasons: List<Season> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val selectedSeason: Long = 0L,
    val favorite: Boolean = false,
    val loading: Boolean = true,
    val message: String? = null,
) {
    val visibleEpisodes: List<Episode>
        get() = episodes.filter { selectedSeason == 0L || it.seasonId == selectedSeason }
}

class SeriesDetailViewModel(
    private val repository: LibraryRepository,
    private val seriesId: Long,
) : ViewModel() {

    private val selectedSeason = MutableStateFlow(0L)
    private val loading = MutableStateFlow(true)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<SeriesDetailUiState> = combine(
        repository.seriesDetail(seriesId).catch { emit(null) },
        repository.favoriteIds(ItemType.SERIES).catch { emit(emptySet()) },
        selectedSeason,
        combine(loading, message) { loading, message -> loading to message },
    ) { detail, favorites, season, status ->
        SeriesDetailUiState(
            series = detail?.series,
            seasons = detail?.seasons.orEmpty(),
            episodes = detail?.episodes.orEmpty(),
            selectedSeason = season,
            favorite = detail?.series?.id in favorites,
            loading = status.first,
            message = status.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SeriesDetailUiState(),
    )

    init {
        viewModelScope.launch {
            runCatching { repository.refreshSeries(seriesId) }
                .onFailure { message.value = it.message ?: "Bölümler yüklenemedi" }
            loading.value = false
        }
    }

    fun selectSeason(seasonId: Long) {
        selectedSeason.value = seasonId
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            runCatching { repository.toggleFavorite(ItemType.SERIES, seriesId) }
                .onFailure { message.value = it.message ?: "Favori güncellenemedi" }
        }
    }

    companion object {
        fun factory(repository: LibraryRepository, seriesId: Long) = viewModelFactory {
            initializer { SeriesDetailViewModel(repository, seriesId) }
        }
    }
}

/** Sezon seçimi 0 = tüm bölümler; arayüzde "Tümü" sekmesi. */
const val ALL_SEASONS = 0L

internal fun SeriesDetailUiState.seasonTabs(): List<Pair<Long, String>> =
    listOf(ALL_SEASONS to "Tümü") +
        seasons.map { season -> season.id to (season.name ?: "Sezon ${season.number}") }
