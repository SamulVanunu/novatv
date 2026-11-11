package dev.anon.novatv.app.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.anon.novatv.core.model.ParentalPolicy
import dev.anon.novatv.core.model.Profile
import dev.anon.novatv.core.sync.ProfileRepository
import dev.anon.novatv.ui.component.PinDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ebeveyn kilidi kapısı.
 *
 * Aktif profilin yaş sınırı/çocuk moduna göre içeriği engeller; profil PIN'i
 * varsa PIN sorar, yoksa engel uygulanamaz (kilit kuramayan profile kilitlemek
 * anlamsız olurdu) ve içerik açılır.
 *
 * Chrome tarafında tek bir [PinDialog] çizilir; her ekran aynı kapıyı kullanır.
 */
@Stable
class ParentalGuardState(
    private val profiles: ProfileRepository,
    private val scope: CoroutineScope,
) {

    var pendingTitle by mutableStateOf("")
        private set
    var pendingAction by mutableStateOf<(() -> Unit)?>(null)
        private set
    var pinError by mutableStateOf(false)
        private set
    private var pendingProfile: Profile? = null

    /** Puanı (0..10) verilen içeriği yaş sınırına göre denetler. */
    fun requestScore(rating: Double?, title: String, action: () -> Unit) {
        scope.launch {
            val profile = runCatching { profiles.active().first() }.getOrNull()
            if (profile == null || !isScoreBlocked(profile, rating)) {
                action()
                return@launch
            }
            gate(profile, title, action)
        }
    }

    /** EPG reytingi (yaş etiketi) verilen içeriği denetler. */
    fun requestRating(rating: String?, title: String, action: () -> Unit) {
        scope.launch {
            val profile = runCatching { profiles.active().first() }.getOrNull()
            if (profile == null || !isRatingBlocked(profile, rating)) {
                action()
                return@launch
            }
            gate(profile, title, action)
        }
    }

    fun submitPin(pin: String) {
        val profile = pendingProfile ?: return
        scope.launch {
            val ok = runCatching { profiles.verifyPin(profile.id, pin) }.getOrDefault(false)
            if (ok) {
                val action = pendingAction
                clear()
                action?.invoke()
            } else {
                pinError = true
            }
        }
    }

    fun cancel() {
        pendingAction = null
        pendingProfile = null
        pendingTitle = ""
        pinError = false
    }

    private fun gate(profile: Profile, title: String, action: () -> Unit) {
        if (profile.pinHash.isNullOrBlank()) {
            action()
            return
        }
        pendingProfile = profile
        pendingTitle = title
        pendingAction = action
        pinError = false
    }

    private fun clear() {
        pendingAction = null
        pendingProfile = null
        pendingTitle = ""
        pinError = false
    }

    private fun isScoreBlocked(profile: Profile, rating: Double?): Boolean =
        ParentalPolicy.isMovieBlocked(rating, profile.maxRating, profile.isKids)

    private fun isRatingBlocked(profile: Profile, rating: String?): Boolean =
        ParentalPolicy.isBlocked(rating, profile.maxRating)
}

@Composable
fun rememberParentalGuard(profiles: ProfileRepository): ParentalGuardState {
    val scope = rememberCoroutineScope()
    return remember(profiles) { ParentalGuardState(profiles, scope) }
}

/**
 * Kapı bekliyorsa PIN panelini çizer.
 *
 * [touch] doğrudan [PinDialog]'a geçer: dokunmatik kabuk tuşları 48 dp'ye
 * çeker, televizyon çağrısı varsayılanı kullanır ve ölçüleri değişmez.
 */
@Composable
fun ParentalGuardDialog(state: ParentalGuardState, touch: Boolean = false) {
    val action = state.pendingAction
    if (action != null) {
        PinDialog(
            title = "Kilitli içerik",
            subtitle = "\"${state.pendingTitle}\" için PIN girin",
            error = state.pinError,
            onSubmit = state::submitPin,
            onDismiss = state::cancel,
            touch = touch,
        )
    }
}
