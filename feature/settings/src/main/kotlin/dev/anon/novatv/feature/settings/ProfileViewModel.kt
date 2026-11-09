package dev.anon.novatv.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.anon.novatv.core.model.ParentalPolicy
import dev.anon.novatv.core.model.Profile
import dev.anon.novatv.core.sync.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val activeId: Long? = null,
    val pinTarget: Profile? = null,
    val pinError: Boolean = false,
    val message: String? = null,
) {
    val active: Profile? get() = profiles.firstOrNull { it.id == activeId }
}

class ProfileViewModel(
    private val repository: ProfileRepository,
) : ViewModel() {

    private val pinTarget = MutableStateFlow<Profile?>(null)
    private val pinError = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<ProfilesUiState> = combine(
        repository.observeAll(),
        repository.active(),
        pinTarget,
        combine(pinError, message) { error, text -> error to text },
    ) { profiles, active, target, status ->
        ProfilesUiState(
            profiles = profiles,
            activeId = active?.id,
            pinTarget = target,
            pinError = status.first,
            message = status.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfilesUiState(),
    )

    init {
        viewModelScope.launch {
            runCatching { repository.ensureDefault() }
                .onFailure { message.value = it.message ?: "Profiller yüklenemedi" }
        }
    }

    /** PIN varsa önce sor; yoksa hemen geç. */
    fun select(profile: Profile) {
        if (profile.id == state.value.activeId) return
        if (profile.pinHash.isNullOrBlank()) {
            switchTo(profile.id)
        } else {
            pinError.value = false
            pinTarget.value = profile
        }
    }

    fun submitPin(pin: String) {
        val target = pinTarget.value ?: return
        viewModelScope.launch {
            val ok = runCatching { repository.verifyPin(target.id, pin) }.getOrDefault(false)
            if (ok) {
                pinTarget.value = null
                pinError.value = false
                switchTo(target.id)
            } else {
                pinError.value = true
            }
        }
    }

    fun cancelPin() {
        pinTarget.value = null
        pinError.value = false
    }

    fun add(name: String, pin: String?, maxRating: String?, kids: Boolean) {
        viewModelScope.launch {
            runCatching { repository.add(name, pin, maxRating, kids) }
                .onSuccess { message.value = "Profil eklendi" }
                .onFailure { message.value = it.message ?: "Profil eklenemedi" }
        }
    }

    fun update(profile: Profile, name: String, pin: String?, maxRating: String?, kids: Boolean) {
        viewModelScope.launch {
            runCatching {
                repository.update(
                    profile.copy(
                        name = name,
                        maxRating = maxRating,
                        isKids = kids,
                    ),
                )
                if (!pin.isNullOrBlank()) repository.setPin(profile.id, pin)
            }.onSuccess { message.value = "Profil güncellendi" }
                .onFailure { message.value = it.message ?: "Profil güncellenemedi" }
        }
    }

    fun removePin(profile: Profile) {
        viewModelScope.launch {
            runCatching { repository.setPin(profile.id, null) }
                .onSuccess { message.value = "PIN kaldırıldı" }
                .onFailure { message.value = it.message ?: "PIN kaldırılamadı" }
        }
    }

    fun delete(profile: Profile) {
        viewModelScope.launch {
            runCatching { repository.delete(profile.id) }
                .onSuccess { message.value = "Profil silindi" }
                .onFailure { message.value = it.message ?: "Profil silinemedi" }
        }
    }

    fun clearMessage() {
        message.value = null
    }

    /** Profil formundaki sınır seçenekleri; arayüz bu listeyi çizer. */
    fun limits(): List<String?> = listOf<String?>(null) + ParentalPolicy.LIMITS

    private fun switchTo(id: Long) {
        viewModelScope.launch {
            runCatching { repository.setActive(id) }
                .onFailure { message.value = it.message ?: "Profil değiştirilemedi" }
        }
    }

    companion object {
        fun factory(repository: ProfileRepository) = viewModelFactory {
            initializer { ProfileViewModel(repository) }
        }
    }
}
