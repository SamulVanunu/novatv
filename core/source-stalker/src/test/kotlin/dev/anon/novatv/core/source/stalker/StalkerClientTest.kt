package dev.anon.novatv.core.source.stalker

import dev.anon.novatv.core.network.NovaHttp
import dev.anon.novatv.core.network.SourceAuthException
import dev.anon.novatv.core.network.SourceHttp
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StalkerClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: StalkerClient

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        client = StalkerClient(
            http = SourceHttp(NovaHttp.client(), userAgent = "NovaTV-test"),
            portalUrl = server.url("/c/").toString(),
            mac = "AA:BB:CC:DD:EE:FF",
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun handshakeReturnsTokenAndSendsMacCookie() = runBlocking {
        enqueue("handshake.json")

        val token = client.handshake()

        assertEquals("tok-123", token)
        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/c/portal.php?type=stb&action=handshake"))
        assertEquals("mac=AA:BB:CC:DD:EE:FF", request.getHeader("Cookie"))
    }

    @Test
    fun liveMapsChannelsAndGenres() = runBlocking {
        enqueue("genres.json")
        enqueue("channels.json")

        val channels = client.live("tok-123")

        assertEquals(2, channels.size)
        assertEquals("TRT 1", channels[0].name)
        assertEquals("Ulusal", channels[0].groupName)
        assertEquals("101", channels[0].remoteId)
        assertEquals("http://logo/trt1.png", channels[0].logoUrl)
        assertEquals("Spor", channels[1].groupName)

        val genresRequest = server.takeRequest()
        val channelsRequest = server.takeRequest()
        assertTrue(genresRequest.path!!.contains("action=get_genres"))
        assertTrue(channelsRequest.path!!.contains("action=get_all_channels"))
        assertEquals("Bearer tok-123", channelsRequest.getHeader("Authorization"))
    }

    @Test
    fun missingGenresFallBackToOtherGroup() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        enqueue("channels.json")

        val channels = client.live("tok-123")

        assertEquals(listOf("Diğer", "Diğer"), channels.map { it.groupName })
    }

    @Test
    fun unauthorizedMapsToAuthException() {
        server.enqueue(MockResponse().setResponseCode(401))

        val failure = runCatching { runBlocking { client.handshake() } }.exceptionOrNull()

        assertTrue(failure is SourceAuthException)
    }

    @Test
    fun vodUsesOrderedList() = runBlocking {
        enqueue("ordered_list.json")

        val movies = client.vod("tok-123")

        assertEquals(1, movies.size)
        assertEquals("Kış Uykusu", movies[0].name)
        assertEquals(2014, movies[0].year)
        assertEquals(8.1, movies[0].rating!!, 0.001)
        assertEquals("http://poster/kis.jpg", movies[0].posterUrl)
        assertEquals(listOf("Dram", "Komedi"), movies[0].genres)
        assertTrue(server.takeRequest().path!!.contains("type=vod&action=get_ordered_list"))
    }

    @Test
    fun liveUrlContainsMacAndStream() {
        val url = "http://panel/c/".let { StalkerClient(SourceHttp(NovaHttp.client()), it, "AA:BB:CC:DD:EE:FF") }
        assertEquals(
            "http://panel/c/play/live.php?mac=AA:BB:CC:DD:EE:FF&stream=101&extension=ts",
            url.liveUrl("101", "ts"),
        )
    }

    private fun enqueue(resource: String) {
        val body = javaClass.classLoader!!.getResource("stalker/$resource")!!.readText()
        server.enqueue(MockResponse().setBody(body))
    }
}
