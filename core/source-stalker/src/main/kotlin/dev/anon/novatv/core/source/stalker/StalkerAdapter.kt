package dev.anon.novatv.core.source.stalker

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

class StalkerAdapter(
    private val playlist: Playlist,
    credentials: Credentials,
    http: SourceHttp,
) : SourceAdapter {

    override val type = PlaylistType.STALKER

    private val mac = credentials.username.orEmpty()
    private val client = StalkerClient(
        http = http,
        portalUrl = playlist.baseUrl,
        mac = mac,
    )

    @Volatile
    private var token: String? = null

    override suspend fun authenticate(): SourceAccount {
        val fresh = client.handshake()
        token = fresh
        return client.profile(fresh)
    }

    override suspend fun live(onProgress: (Int) -> Unit): List<Channel> =
        client.live(tokenOrHandshake(), onProgress).map { it.copy(playlistId = playlist.id) }

    override suspend fun vod(): List<Movie> =
        client.vod(tokenOrHandshake()).map { it.copy(playlistId = playlist.id) }

    override suspend fun series(): List<Series> = emptyList()

    override suspend fun epg(): EpgSource? = null

    override fun streamUrl(channel: Channel, format: StreamFormat): String =
        client.liveUrl(channel.remoteId, extensionFor(format))

    override fun catchupUrl(channel: Channel, at: Instant, minutes: Int): String? = null

    private suspend fun tokenOrHandshake(): String =
        token ?: client.handshake().also { token = it }

    private fun extensionFor(format: StreamFormat): String = when (format) {
        StreamFormat.HLS -> "m3u8"
        StreamFormat.TS -> "ts"
        StreamFormat.AUTO -> "ts"
    }
}

class StalkerFactory : SourceAdapterFactory {

    override val type = PlaylistType.STALKER

    override fun create(playlist: Playlist, credentials: Credentials?): SourceAdapter {
        val mac = credentials?.username?.trim().orEmpty()
        if (!MAC_PATTERN.matches(mac)) {
            throw SourceAuthException("Stalker portal için geçerli MAC adresi gerekli (AA:BB:CC:DD:EE:FF)")
        }
        return StalkerAdapter(
            playlist = playlist,
            credentials = Credentials(username = mac.uppercase(), password = null),
            http = SourceHttp(
                client = NovaHttp.client(),
                userAgent = playlist.userAgent ?: NovaHttp.DEFAULT_USER_AGENT,
            ),
        )
    }

    private companion object {
        val MAC_PATTERN = Regex("""^(?:[0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$""")
    }
}
