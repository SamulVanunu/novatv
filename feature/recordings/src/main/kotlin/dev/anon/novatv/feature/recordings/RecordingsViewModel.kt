package dev.anon.novatv.feature.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.anon.novatv.core.recording.RecordingEntry
import dev.anon.novatv.core.recording.RecordingScheduler
import dev.anon.novatv.core.recording.RecordingStatus
import dev.anon.novatv.core.recording.RecordingStorage
import dev.anon.novatv.core.recording.RecordingStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordingsUiState(
    val entries: List<RecordingEntry> = emptyList(),
    val message: String? = null,
) {
    val active: List<RecordingEntry> get() = entries.filter { it.isActive }
    val finished: List<RecordingEntry> get() = entries.filter { !it.isActive }
}

class RecordingsViewModel(
    private val store: RecordingStore,
    private val scheduler: RecordingScheduler,
    private val storage: RecordingStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(RecordingsUiState())
    val state: StateFlow<RecordingsUiState> = _state.asStateFlow()

    init {
        refresh()
        // Aktif kaydın boyutu ve durumu canlı görünsün.
        viewModelScope.launch {
            while (true) {
                delay(REFRESH_MS)
                if (_state.value.active.isNotEmpty()) refresh()
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(entries = store.entries()) }
    }

    fun cancel(entry: RecordingEntry) {
        when (entry.statusValue) {
            RecordingStatus.RECORDING -> scheduler.stopNow()
            RecordingStatus.SCHEDULED -> scheduler.cancel(entry.id)
            else -> Unit
        }
        viewModelScope.launch {
            delay(600)
            refresh()
            _state.update { it.copy(message = "Kayıt durduruldu") }
        }
    }

    fun delete(entry: RecordingEntry) {
        if (entry.isActive) return
        val removed = storage.delete(entry.filePath)
        store.removeEntry(entry.id)
        refresh()
        _state.update {
            it.copy(
                message = when {
                    entry.filePath == null -> "Kayıt satırı silindi"
                    removed -> "Dosya silindi"
                    else -> "Dosya bulunamadı, satır silindi"
                },
            )
        }
    }

    /** Biten kaydın oynatılabilir dosya yolu; yoksa null. */
    fun fileOf(entry: RecordingEntry): String? =
        entry.filePath?.takeIf { entry.statusValue == RecordingStatus.DONE }

    companion object {
        private const val REFRESH_MS = 3_000L

        fun factory(
            store: RecordingStore,
            scheduler: RecordingScheduler,
            storage: RecordingStorage,
        ) = viewModelFactory {
            initializer { RecordingsViewModel(store, scheduler, storage) }
        }
    }
}
