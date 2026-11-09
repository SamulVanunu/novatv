package dev.anon.novatv.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.anon.novatv.core.settings.Accent
import dev.anon.novatv.core.settings.Appearance
import dev.anon.novatv.core.settings.PreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val appearance: Appearance = Appearance(),
    val recordingFolder: String? = null,
    val recordingPath: String = "",
    val busy: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(
    private val preferences: PreferenceStore,
    recordingPath: String = "",
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        preferences.appearance,
        preferences.recordingFolder,
        combine(busy, message) { busy, message -> busy to message },
    ) { appearance, folder, status ->
        SettingsUiState(
            appearance = appearance,
            recordingFolder = folder,
            recordingPath = recordingPath,
            busy = status.first,
            message = status.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(recordingPath = recordingPath),
    )

    fun setDark(value: Boolean) = update { preferences.setDarkTheme(value) }
    fun setAccent(value: Accent) = update { preferences.setAccent(value) }
    fun setClock24h(value: Boolean) = update { preferences.setClock24h(value) }
    fun setAutoFrameRate(value: Boolean) = update { preferences.setAutoFrameRate(value) }
    fun setAutoStart(value: Boolean) = update { preferences.setAutoStart(value) }
    fun setSleepMinutes(minutes: Int) = update { preferences.setSleepMinutes(minutes) }

    fun setTypeScale(value: Float) = update {
        preferences.setTypeScale(value.coerceIn(0.85f, 1.35f))
    }

    fun setEpgRowHeight(value: Int) = update {
        preferences.setEpgRowHeight(value.coerceIn(40, 110))
    }

    fun setEpgWindowDays(value: Int) = update {
        preferences.setEpgWindowDays(value.coerceIn(1, 7))
    }

    fun setMessage(text: String?) {
        message.value = text
    }

    fun setBusy(value: Boolean) {
        busy.value = value
    }

    fun backupFinished(text: String) {
        busy.value = false
        message.value = text
    }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { message.value = it.message ?: "Ayar kaydedilemedi" }
        }
    }

    companion object {
        fun factory(preferences: PreferenceStore, recordingPath: String) = viewModelFactory {
            initializer { SettingsViewModel(preferences, recordingPath) }
        }
    }
}
