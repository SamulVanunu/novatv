package dev.anon.novatv.core.sync

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.StreamFormat
import dev.anon.novatv.core.network.SourceAuthException
import dev.anon.novatv.core.source.SourceAdapterRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class StreamResolverTest {

    private val adapter = FakeAdapter()

    private fun resolver(
        playlist: Playlist,
        credentials: Credentials? = Credentials("demo", "demo"),
    ) = StreamResolver(
        playlists = object : PlaylistRepository {
            override fun observeAll(): Flow<List<Playlist>> = flowOf(listOf(playlist))
            override suspend fun add(playlist: Playlist, credentials: Credentials): Long = 1
            override suspend fun update(playlist: Playlist) = Unit
            override suspend fun updateCredentials(id: Long, credentials: Credentials) = Unit
            override suspend fun remove(id: Long) = Unit
            override suspend fun load(id: Long): Pair<Playlist, Credentials?> = playlist to credentials
        },
        credentials = FakeCredentialStore(),
        registry = SourceAdapterRegistry(setOf(FakeAdapterFactory(adapter))),
    )

    @Test
    fun resolveBuildsStreamUrlAtRuntime() = runTest {
        val url = resolver(playlist()).resolve(channel(), StreamFormat.HLS)
        assertEquals("http://stream/10.m3u8", url)
    }

    @Test
    fun inactivePlaylistIsRejected() = runTest {
        val failure = runCatching {
            resolver(playlist(active = false)).resolve(channel(), StreamFormat.TS)
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
    }

    @Test
    fun missingCredentialsFailWithAuthException() = runTest {
        val failure = runCatching {
            resolver(playlist(), credentials = null).resolve(channel(), StreamFormat.TS)
        }.exceptionOrNull()

        assertTrue(failure is SourceAuthException)
    }

    @Test
    fun catchupResolvesOnlyWhenChannelArchives() = runTest {
        val withArchive = channel().copy(hasArchive = true)

        assertEquals(
            "http://catchup/10?m=30",
            resolver(playlist()).resolveCatchup(withArchive, Instant.EPOCH, 30),
        )
        assertNull(resolver(playlist()).resolveCatchup(channel(), Instant.EPOCH, 30))
    }

    private fun playlist(active: Boolean = true) =
        Playlist(id = 1, type = PlaylistType.XTREAM, name = "X", baseUrl = "http://panel", isActive = active)

    private fun channel() = Channel(id = 1, playlistId = 1, remoteId = "10", name = "TRT 1", groupName = "Ulusal")
}
