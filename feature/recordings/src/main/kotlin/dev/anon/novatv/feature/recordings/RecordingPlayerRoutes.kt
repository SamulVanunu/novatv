package dev.anon.novatv.feature.recordings

/** Biten kaydı uygulama içinde oynatma rotası; dosya yolu rota taşımaz. */
object RecordingPlayerRoutes {
    const val ARG_ID = "recordingId"
    const val PATTERN = "recording-play/{$ARG_ID}"

    fun of(id: String): String = "recording-play/" + android.net.Uri.encode(id)
}
