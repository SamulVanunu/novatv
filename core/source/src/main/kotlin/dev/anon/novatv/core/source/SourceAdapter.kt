package dev.anon.novatv.core.source

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Episode
import dev.anon.novatv.core.model.Movie
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.Series
import dev.anon.novatv.core.model.SeriesInfo
import dev.anon.novatv.core.model.SourceAccount
import dev.anon.novatv.core.model.StreamFormat
import java.time.Instant

data class EpgSource(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

interface SourceAdapter {
    val type: PlaylistType

    suspend fun authenticate(): SourceAccount
    suspend fun live(onProgress: (Int) -> Unit = {}): List<Channel>
    suspend fun vod(): List<Movie>
    suspend fun series(): List<Series>
    suspend fun epg(): EpgSource?

    /** Dizi sezon/bölüm ağacı; desteklemeyen kaynaklar `null` döner. */
    suspend fun seriesInfo(seriesId: String): SeriesInfo? = null

    fun streamUrl(channel: Channel, format: StreamFormat): String
    fun catchupUrl(channel: Channel, at: Instant, minutes: Int): String?

    /** Film/bölüm akışı; kaynak VOD sunmuyorsa `null`. */
    fun movieUrl(movie: Movie): String? = null
    fun episodeUrl(episode: Episode): String? = null
}

interface SourceAdapterFactory {
    val type: PlaylistType
    fun create(playlist: Playlist, credentials: Credentials?): SourceAdapter
}
