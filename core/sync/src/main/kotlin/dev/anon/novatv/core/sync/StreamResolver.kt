package dev.anon.novatv.core.sync

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.StreamFormat
import dev.anon.novatv.core.settings.CredentialStore
import dev.anon.novatv.core.source.SourceAdapterRegistry
import java.time.Instant

/**
 * Akış URL'i veritabanında tutulmaz; her oynatmada kaynak adaptöründen üretilir.
 * Xtream URL'leri kullanıcı/şifre taşıdığı için bu ayrım gizlilik gereğidir.
 */
class StreamResolver(
    private val playlists: PlaylistRepository,
    private val credentials: CredentialStore,
    private val registry: SourceAdapterRegistry,
) {

    suspend fun resolve(channel: Channel, format: StreamFormat = channel.format): String {
        val (playlist, creds) = loadActive(channel.playlistId)
        return registry.adapterFor(playlist, creds).streamUrl(channel, format)
    }

    suspend fun resolveCatchup(channel: Channel, at: Instant, minutes: Int): String? {
        val (playlist, creds) = loadActive(channel.playlistId)
        return registry.adapterFor(playlist, creds).catchupUrl(channel, at, minutes)
    }

    private suspend fun loadActive(playlistId: Long): Pair<Playlist, Credentials?> {
        val (playlist, stored) = playlists.load(playlistId)
        check(playlist.isActive) { "Playlist pasif: ${playlist.id}" }
        return playlist to (stored ?: credentials.get(playlistId))
    }
}
