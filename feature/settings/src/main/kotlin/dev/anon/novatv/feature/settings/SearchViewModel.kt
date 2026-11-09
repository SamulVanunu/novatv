package dev.anon.novatv.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.anon.novatv.core.sync.SearchHit
import dev.anon.novatv.core.sync.SearchKind
import dev.anon.novatv.core.sync.SearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val query: String = "",
    val filter: SearchKind? = null,
    val hits: List<SearchHit> = emptyList(),
    val loading: Boolean = false,
) {
    val visible: List<SearchHit>
        get() = if (filter == null) hits else hits.filter { it.kind == filter }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    repository: SearchRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<SearchKind?>(null)

    private val results = query
        .debounce { if (it.isBlank()) 0L else DEBOUNCE_MS }
        .flatMapLatest { value ->
            if (value.isBlank()) flowOf(emptyList()) else repository.search(value)
        }

    val state: StateFlow<SearchUiState> = combine(
        query,
        filter,
        results,
    ) { text, kind, hits ->
        SearchUiState(query = text, filter = kind, hits = hits, loading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: SearchKind?) {
        filter.value = value
    }

    companion object {
        private const val DEBOUNCE_MS = 220L

        fun factory(repository: SearchRepository) = viewModelFactory {
            initializer { SearchViewModel(repository) }
        }
    }
}
