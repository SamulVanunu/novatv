package dev.anon.novatv.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.SyncState
import dev.anon.novatv.core.sync.SyncEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

/**
 * Üç kaynak tipinin de mock panelden kanal çektiğini doğrular.
 * Sunucu yoksa atlanır: `node tools/mock-server/server.mjs`.
 */
@RunWith(AndroidJUnit4::class)
class MockSourceSyncTest {

    private val application: NovaApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as NovaApplication

    @Test
    fun allThreeSourceTypesProduceChannels() = runBlocking {
        val base = BASE_URLS.firstOrNull(::reachable)
        assumeTrue("mock sunucuya erişilemiyor", base != null)

        val xtream = sync(
            base!!,
            PlaylistType.XTREAM,
            base,
            Credentials("demo", "demo"),
        )
        assertTrue("Xtream kanal döndürmeli", xtream > 0)

        val m3u = sync(
            base,
            PlaylistType.M3U,
            "$base/get.php?username=demo&password=demo&type=m3u_plus",
            Credentials("demo", "demo"),
        )
        assertEquals("M3U üç kanal içermeli", 3, m3u)

        val stalker = sync(
            base,
            PlaylistType.STALKER,
            "$base/portal",
            Credentials("00:1A:79:00:00:01", null),
        )
        assertTrue("Stalker kanal döndürmeli", stalker > 0)
    }

    @Test
    fun workManagerRunsSyncWorkerWithApplicationFactory() = runBlocking {
        val base = BASE_URLS.firstOrNull(::reachable)
        assumeTrue("mock sunucuya erişilemiyor", base != null)

        val graph = application.graph
        val id = graph.playlists.add(
            Playlist(
                type = PlaylistType.M3U,
                name = "Worker ${System.nanoTime()}",
                baseUrl = "$base/get.php?username=demo&password=demo&type=m3u_plus",
            ),
            Credentials("demo", "demo"),
        )

        val finished = async {
            withTimeout(60_000) {
                graph.syncEngine.events()
                    .filterIsInstance<SyncEvent.Finished>()
                    .first { it.playlistId == id }
            }
        }
        yield()
        graph.syncScheduler.syncNow(id)

        val event = finished.await()
        assertEquals("WorkManager senkronu: ${event.message}", SyncState.OK, event.state)
        assertEquals(3, graph.database.channelDao().countByPlaylist(id))
    }

    private suspend fun sync(
        base: String,
        type: PlaylistType,
        url: String,
        credentials: Credentials,
    ): Int {
        val graph = application.graph
        val id = graph.playlists.add(
            Playlist(type = type, name = "$type ${System.nanoTime()}", baseUrl = url),
            credentials,
        )
        val state: SyncState = graph.syncEngine.sync(id)
        val channels = graph.database.channelDao().countByPlaylist(id)
        assertEquals("$type senkronu başarısız ($base): $state", SyncState.OK, state)
        return channels
    }

    private fun reachable(baseUrl: String): Boolean = runCatching {
        val connection = URL("$baseUrl/health").openConnection() as HttpURLConnection
        connection.connectTimeout = 2_000
        connection.readTimeout = 2_000
        connection.responseCode == 200
    }.getOrDefault(false)

    private companion object {
        val BASE_URLS = listOf("http://10.0.2.2:8089", "http://127.0.0.1:8089")
    }
}
