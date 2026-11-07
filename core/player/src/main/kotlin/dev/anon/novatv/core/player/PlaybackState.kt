package dev.anon.novatv.core.player

/** Oynatıcının kullanıcıya görünen durumu. */
sealed interface PlaybackState {

    data object Idle : PlaybackState

    data object Buffering : PlaybackState

    data object Playing : PlaybackState

    /** [retryable] true ise "Tekrar dene" sunulabilir; false ise kaynak uyumsuz. */
    data class Failed(val reason: String, val retryable: Boolean) : PlaybackState
}

/** Ses/altyazı parçası seçeneği; [id] denetleyicinin iç kimliğidir. */
data class TrackChoice(
    val id: String,
    val label: String,
    val language: String?,
)
