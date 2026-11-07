package dev.anon.novatv.core.source.m3u

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.StreamFormat
import dev.anon.novatv.core.network.NovaHttp
import dev.anon.novatv.core.network.SourceHttp
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class M3uAdapterTest {

    private lateinit var server: MockWebServer
    private lateinit var adapter: M3uAdapter

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        adapter = M3uAdapter(
            playlist = Playlist(id = 3, type = PlaylistType.M3U, name = "M3U", baseUrl = server.url("/list.m3u").toString()),
            credentials = null,
            http = SourceHttp(NovaHttp.client(), userAgent = "NovaTV-test"),
            baseUrl = server.url("/list.m3u").toString(),
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun liveStreamsChannelsAndReportsProgress() = runBlocking {
        val body = buildString {
            appendLine("#EXTM3U")
            repeat(1001) { i ->
                appendLine("#EXTINF:-1 group-title=\"G\",Kanal $i")
                appendLine("http://a/$i.ts")
            }
        }
        server.enqueue(MockResponse().setBody(body))

        val progress = mutableListOf<Int>()
        val channels = adapter.live { progress += it }

        assertEquals(1001, channels.size)
        assertEquals(listOf(500, 1000), progress)
        assertEquals("Kanal 0", channels.first().name)
        assertEquals("NovaTV-test", server.takeRequest().getHeader("User-Agent"))
    }

    @Test
    fun streamUrlIsTheChannelUrlAndCatchupIsNull() {
        val channel = Channel(playlistId = 3, remoteId = "http://a/1.ts", name = "A", groupName = "G")
        assertEquals("http://a/1.ts", adapter.streamUrl(channel, StreamFormat.TS))
        assertNull(adapter.catchupUrl(channel, java.time.Instant.EPOCH, 0))
    }
}
