package dev.anon.novatv.core.source.xstream

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Movie
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.Series
import dev.anon.novatv.core.model.SourceAccount
import dev.anon.novatv.core.model.StreamFormat
import dev.anon.novatv.core.network.NovaHttp
import dev.anon.novatv.core.network.SourceAuthException
import dev.anon.novatv.core.network.SourceHttp
import dev.anon.novatv.core.source.EpgSource
import dev.anon.novatv.core.source.SourceAdapter
import dev.anon.novatv.core.source.SourceAdapterFactory
import java.time.Instant

class XtreamAdapter(
    private val playlist: Playlist,
    credentials: Credentials,
    http: SourceHttp,
) : SourceAdapter {

    override val type = PlaylistType.XTREAM

    private val client = XtreamClient(
        http = http,
        baseUrl = playlist.baseUrl.trimEnd('/'),
        username = credentials.username.orEmpty(),
        password = credentials.password.orEmpty(),
    )

    private val headers: Map<String, String> = buildMap {
        playlist.userAgent?.let { put("User-Agent", it) }
        playlist.referer?.let { put("Referer", it) }
    }

    override suspend fun authenticate(): SourceAccount = client.authenticate()

    override suspend fun live(onProgress: (Int) -> Unit): List<Channel> =
        client.live(onProgress).map { it.copy(playlistId = playlist.id) }

    override suspend fun vod(): List<Movie> =
        client.vod().map { it.copy(playlistId = playlist.id) }

    override suspend fun series(): List<Series> =
        client.series().map { it.copy(playlistId = playlist.id) }

    override suspend fun epg(): EpgSource =
        EpgSource(url = playlist.epgUrl?.takeIf { it.isNotBlank() } ?: client.epgUrl(), headers = headers)

    override fun streamUrl(channel: Channel, format: StreamFormat): String =
        client.liveUrl(channel.remoteId, extensionFor(format))

    override fun catchupUrl(channel: Channel, at: Instant, minutes: Int): String? =
        if (channel.hasArchive) client.catchupUrl(channel.remoteId, at, minutes) else null

    private fun extensionFor(format: StreamFormat): String = when (format) {
        StreamFormat.HLS -> "m3u8"
        StreamFormat.TS -> "ts"
        StreamFormat.AUTO -> "ts"
    }
}

class XtreamFactory : SourceAdapterFactory {

    override val type = PlaylistType.XTREAM

    override fun create(playlist: Playlist, credentials: Credentials?): SourceAdapter {
        if (credentials == null || credentials.username.isNullOrBlank()) {
            throw SourceAuthException("Xtream kaynağı için kullanıcı adı ve şifre gerekli")
        }
        return XtreamAdapter(
            playlist = playlist,
            credentials = credentials,
            http = SourceHttp(
                client = NovaHttp.client(),
                userAgent = playlist.userAgent ?: NovaHttp.DEFAULT_USER_AGENT,
                referer = playlist.referer,
            ),
        )
    }
}
