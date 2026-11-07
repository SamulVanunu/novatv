package dev.anon.novatv.core.source

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Movie
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.Series
import dev.anon.novatv.core.model.SourceAccount
import dev.anon.novatv.core.model.StreamFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SourceAdapterRegistryTest {

    private val fake = object : SourceAdapter {
        override val type = PlaylistType.XTREAM
        override suspend fun authenticate() = SourceAccount("Active")
        override suspend fun live(onProgress: (Int) -> Unit) = emptyList<Channel>()
        override suspend fun vod() = emptyList<Movie>()
        override suspend fun series() = emptyList<Series>()
        override suspend fun epg(): EpgSource? = null
        override fun streamUrl(channel: Channel, format: StreamFormat) = "http://stream/${channel.remoteId}"
        override fun catchupUrl(channel: Channel, at: Instant, minutes: Int): String? = null
    }

    private val factory = object : SourceAdapterFactory {
        override val type = PlaylistType.XTREAM
        override fun create(playlist: Playlist, credentials: Credentials?) = fake
    }

    @Test
    fun returnsAdapterForMatchingType() {
        val registry = SourceAdapterRegistry(setOf(factory))
        val playlist = Playlist(type = PlaylistType.XTREAM, name = "p", baseUrl = "http://x")
        assertEquals(fake, registry.adapterFor(playlist, Credentials("u", "p")))
    }

    @Test
    fun unknownTypeFailsLoudly() {
        val registry = SourceAdapterRegistry(setOf(factory))
        val playlist = Playlist(type = PlaylistType.M3U, name = "p", baseUrl = "http://x")
        val failure = runCatching { registry.adapterFor(playlist, null) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure!!.message!!.contains("M3U"))
    }
}
