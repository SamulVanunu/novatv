package dev.anon.novatv.core.source.m3u

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Movie
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.Series
import dev.anon.novatv.core.model.SourceAccount
import dev.anon.novatv.core.model.StreamFormat
import dev.anon.novatv.core.network.NovaHttp
import dev.anon.novatv.core.network.SourceHttp
import dev.anon.novatv.core.network.SourceNetworkException
import dev.anon.novatv.core.source.EpgSource
import dev.anon.novatv.core.source.SourceAdapter
import dev.anon.novatv.core.source.SourceAdapterFactory
import java.time.Instant

class M3uAdapter(
    private val playlist: Playlist,
    private val credentials: Credentials?,
    private val http: SourceHttp,
    private val baseUrl: String,
) : SourceAdapter {

    override val type = PlaylistType.M3U

    private val headers: Map<String, String> = buildMap {
        playlist.userAgent?.let { put("User-Agent", it) }
        playlist.referer?.let { put("Referer", it) }
    }

    override suspend fun authenticate(): SourceAccount = SourceAccount(status = "Active")

    override suspend fun live(onProgress: (Int) -> Unit): List<Channel> {
        val channels = ArrayList<Channel>()
        http.openStream(baseUrl, headers).use { response ->
            val body = response.body ?: throw SourceNetworkException("M3U yanıtı boş")
            body.byteStream().use { stream ->
                M3uParser().parse(playlist.id, stream) { channel ->
                    channels += channel
                    if (channels.size % PROGRESS_STEP == 0) onProgress(channels.size)
                }
            }
        }
        return channels
    }

    override suspend fun vod(): List<Movie> = emptyList()

    override suspend fun series(): List<Series> = emptyList()

    override suspend fun epg(): EpgSource? =
        playlist.epgUrl?.takeIf { it.isNotBlank() }?.let { EpgSource(url = it, headers = headers) }

    override fun streamUrl(channel: Channel, format: StreamFormat): String =
        resolve(channel.remoteId)

    override fun catchupUrl(channel: Channel, at: Instant, minutes: Int): String? = null

    /** Göreli akış adreslerini liste adresine göre çözer; mutlak adresler dokunulmadan döner. */
    private fun resolve(url: String): String =
        if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            runCatching { java.net.URI(baseUrl).resolve(url).toString() }.getOrDefault(url)
        }

    private companion object {
        const val PROGRESS_STEP = 500
    }
}

class M3uFactory : SourceAdapterFactory {

    override val type = PlaylistType.M3U

    override fun create(playlist: Playlist, credentials: Credentials?): SourceAdapter = M3uAdapter(
        playlist = playlist,
        credentials = credentials,
        http = SourceHttp(
            client = NovaHttp.client(),
            userAgent = playlist.userAgent ?: NovaHttp.DEFAULT_USER_AGENT,
            referer = playlist.referer,
        ),
        baseUrl = playlist.baseUrl,
    )
}
