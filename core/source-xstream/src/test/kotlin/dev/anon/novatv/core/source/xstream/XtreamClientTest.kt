package dev.anon.novatv.core.source.xstream

import dev.anon.novatv.core.model.StreamFormat
import dev.anon.novatv.core.network.NovaHttp
import dev.anon.novatv.core.network.SourceAuthException
import dev.anon.novatv.core.network.SourceHttp
import dev.anon.novatv.core.network.SourceProtocolException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class XtreamClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: XtreamClient

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        client = XtreamClient(
            http = SourceHttp(NovaHttp.client(), userAgent = "NovaTV-test"),
            baseUrl = server.url("/").toString().trimEnd('/'),
            username = "demo",
            password = "demo",
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun authenticateReturnsAccountDetails() = runBlocking {
        enqueue("auth_ok.json")

        val account = client.authenticate()

        assertEquals("Active", account.status)
        assertEquals(2, account.maxConnections)
        assertEquals(1_893_456_000_000L, account.expiresAt)
        assertTrue(server.takeRequest().path!!.startsWith("/player_api.php?username=demo&password=demo"))
    }

    @Test
    fun inactiveAccountFailsWithAuthException() {
        enqueue("auth_fail.json")

        val failure = runCatching { runBlocking { client.authenticate() } }.exceptionOrNull()

        assertTrue(failure is SourceAuthException)
        assertTrue(failure!!.message!!.contains("Expired"))
    }

    @Test
    fun htmlResponseFailsAsProtocolError() {
        enqueue("error.html")

        val failure = runCatching { runBlocking { client.authenticate() } }.exceptionOrNull()

        assertTrue(failure is SourceProtocolException)
        assertTrue(failure!!.message!!.contains("JSON"))
    }

    @Test
    fun liveMapsCategoriesAndArchive() = runBlocking {
        enqueue("live_categories.json")
        enqueue("live.json")
        val progress = mutableListOf<Int>()

        val channels = client.live { progress += it }

        assertEquals(2, channels.size)
        assertEquals("TRT 1", channels[0].name)
        assertEquals("Ulusal", channels[0].groupName)
        assertEquals("10", channels[0].remoteId)
        assertEquals(1, channels[0].number)
        assertEquals("http://logo/trt1.png", channels[0].logoUrl)
        assertEquals("trt1.tr", channels[0].tvgId)
        assertTrue(channels[0].hasArchive)
        assertEquals(7, channels[0].archiveDays)
        assertEquals("Spor", channels[1].groupName)
        assertEquals("11", channels[1].remoteId)
        assertEquals(2, channels[1].number)
        assertEquals(listOf(2), progress)
    }

    @Test
    fun vodParsesYearAndRating() = runBlocking {
        enqueue("vod.json")

        val movies = client.vod()

        assertEquals(1, movies.size)
        assertEquals("Kış Uykusu", movies[0].name)
        assertEquals(2014, movies[0].year)
        assertEquals(8.1, movies[0].rating!!, 0.001)
        assertEquals(listOf("Dram", "Komedi"), movies[0].genres)
        assertEquals("mkv", movies[0].containerExt)
        assertEquals("55", movies[0].remoteId)
    }

    @Test
    fun seriesAndSeriesInfoAreParsed() = runBlocking {
        enqueue("series.json")
        enqueue("series_info.json")

        val series = client.series().single()
        assertEquals("Bir Başkadır", series.name)
        assertEquals(2020, series.year)
        assertEquals(8.4, series.rating!!, 0.001)
        assertEquals("77", series.remoteId)

        val info = client.seriesInfo("77")
        assertEquals(1, info.seasons.size)
        assertEquals(2, info.seasons[0].episodes.size)
        assertEquals("Bölüm 2", info.seasons[0].episodes[1].title)
        assertEquals(45, info.seasons[0].episodes[0].durationMin)
    }

    @Test
    fun urlsEmbedCredentialsAndFollowPanelLayout() {
        val base = "http://panel:8080"
        val client = XtreamClient(
            http = SourceHttp(NovaHttp.client()),
            baseUrl = base,
            username = "u",
            password = "p",
        )

        assertEquals("http://panel:8080/live/u/p/10.ts", client.liveUrl("10", "ts"))
        assertEquals("http://panel:8080/movie/u/p/55.mkv", client.vodUrl("55", "mkv"))
        assertEquals("http://panel:8080/series/u/p/1001.mkv", client.seriesUrl("1001", "mkv"))
        assertEquals(
            "http://panel:8080/xmltv.php?username=u&password=p",
            client.epgUrl(),
        )
        assertEquals(
            "http://panel:8080/timeshift/u/p/60/2026-11-07:20-30/10.ts",
            client.catchupUrl("10", Instant.parse("2026-11-07T17:30:00Z"), 60),
        )
    }

    @Test
    fun adapterSelectsExtensionPerFormat() {
        val playlist = dev.anon.novatv.core.model.Playlist(
            id = 4,
            type = dev.anon.novatv.core.model.PlaylistType.XTREAM,
            name = "X",
            baseUrl = server.url("/").toString().trimEnd('/'),
        )
        val adapter = XtreamAdapter(
            playlist = playlist,
            credentials = dev.anon.novatv.core.model.Credentials("demo", "demo"),
            http = SourceHttp(NovaHttp.client()),
        )
        val channel = dev.anon.novatv.core.model.Channel(
            playlistId = 4, remoteId = "10", name = "TRT 1", groupName = "Ulusal",
        )

        assertTrue(adapter.streamUrl(channel, StreamFormat.HLS).endsWith("/live/demo/demo/10.m3u8"))
        assertTrue(adapter.streamUrl(channel, StreamFormat.TS).endsWith("/live/demo/demo/10.ts"))
    }

    private fun enqueue(resource: String) {
        val body = javaClass.classLoader!!.getResource("xtream/$resource")!!.readText()
        server.enqueue(MockResponse().setBody(body))
    }
}
