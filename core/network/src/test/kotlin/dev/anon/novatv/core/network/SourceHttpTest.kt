package dev.anon.novatv.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SourceHttpTest {

    private lateinit var server: MockWebServer
    private lateinit var http: SourceHttp

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        http = SourceHttp(NovaHttp.client(), userAgent = "NovaTV-test")
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun sendsConfiguredUserAgent() = runBlocking {
        server.enqueue(MockResponse().setBody("ok"))
        http.getText(server.url("/a").toString())
        assertEquals("NovaTV-test", server.takeRequest().getHeader("User-Agent"))
    }

    @Test
    fun mapsUnauthorizedToAuthException() {
        server.enqueue(MockResponse().setResponseCode(401))
        val failure = runCatching { runBlocking { http.getText(server.url("/a").toString()) } }.exceptionOrNull()
        assertTrue(failure is SourceAuthException)
    }

    @Test
    fun mapsServerErrorToProtocolException() {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(500).setBody("boom")) }
        val failure = runCatching { runBlocking { http.getText(server.url("/a").toString()) } }.exceptionOrNull()
        assertTrue(failure is SourceNetworkException)
    }

    @Test
    fun retriesTransientFailureThenSucceeds() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(MockResponse().setBody("second"))
        assertEquals("second", http.getText(server.url("/a").toString()))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun clientUsesDocumentedTimeouts() {
        val client = NovaHttp.client()
        assertEquals(15_000, client.connectTimeoutMillis)
        assertEquals(30_000, client.readTimeoutMillis)
    }
}
