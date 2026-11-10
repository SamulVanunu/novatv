package dev.anon.novatv.core.recording

import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StreamRecorderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private var now = 0L

    private val plan = RecordingPlan(
        id = "rec-1",
        channelId = 4,
        channelName = "Test Kanal",
        title = "Test Program",
        startAt = 0,
        endAt = 30_000,
    )

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        now = 0
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun tsStreamReconnectsAndAppendsBytes() = runBlocking {
        server.enqueue(MockResponse().setBody("AAA"))
        server.enqueue(MockResponse().setBody("BB"))

        val target = File(folder.root, "out.ts")
        val result = recorder(sleepStep = 5_000).record(
            plan = plan.copy(endAt = 10_000),
            url = server.url("/live/ch.ts").toString(),
            target = target,
        )

        assertNull(result.error)
        assertEquals(5L, result.bytes)
        assertEquals("AAABB", target.readText())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun hlsRecordsOnlyNewSegments() = runBlocking {
        val playlist = """
            #EXTM3U
            #EXT-X-TARGETDURATION:2
            #EXTINF:2.0,
            seg-1.ts
            #EXTINF:2.0,
            seg-2.ts
        """.trimIndent()
        // İlk liste ana/varyant kontrolü için, sonraki liste döngü içindir.
        server.enqueue(MockResponse().setBody(playlist))
        server.enqueue(MockResponse().setBody(playlist))
        server.enqueue(MockResponse().setBody("AAA"))
        server.enqueue(MockResponse().setBody("BB"))
        server.enqueue(MockResponse().setBody(playlist))

        val target = File(folder.root, "out.ts")
        val result = recorder(sleepStep = 2_000).record(
            plan = plan.copy(endAt = 2_500),
            url = server.url("/live/index.m3u8").toString(),
            target = target,
        )

        assertNull(result.error)
        assertEquals(5L, result.bytes)
        assertEquals("AAABB", target.readText())
        // Aynı segment ikinci turda yeniden indirilmedi.
        assertEquals(5, server.requestCount)
    }

    @Test
    fun encryptedHlsIsRejected() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                #EXTM3U
                #EXT-X-KEY:METHOD=AES-128,URI="key.bin"
                #EXTINF:2.0,
                seg-1.ts
                """.trimIndent(),
            ),
        )

        val result = recorder(sleepStep = 1_000).record(
            plan = plan,
            url = server.url("/live/index.m3u8").toString(),
            target = File(folder.root, "out.ts"),
        )

        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Şifreli"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun userStopEndsLoopAndKeepsWrittenBytes() = runBlocking {
        // Kullanıcı "Durdur" dediğinde iş iptal edilmemeli: o ana kadar yazılan
        // baytlar geçerli bir kayıt olarak dönmeli, dosya silinmemeli.
        server.enqueue(MockResponse().setBody("AAA"))

        var stop = false
        val target = File(folder.root, "stopped.ts")
        val result = recorder(sleepStep = 5_000).record(
            plan = plan,
            url = server.url("/live/ch.ts").toString(),
            target = target,
            onBytes = { stop = true },
            shouldStop = { stop },
        )

        assertEquals(null, result.error)
        assertEquals(3L, result.bytes)
        assertEquals(target.absolutePath, result.path)
        assertEquals("AAA", target.readText())
        // Durdurma isteğinden sonra yeniden bağlanılmaz.
        assertEquals(1, server.requestCount)
    }

    @Test
    fun emptyStreamLeavesNoFile() = runBlocking {
        server.enqueue(MockResponse().setBody(""))

        val target = File(folder.root, "empty.ts")
        val result = recorder(sleepStep = 5_000).record(
            plan = plan.copy(endAt = 1_000),
            url = server.url("/live/ch.ts").toString(),
            target = target,
        )

        assertFalse(result.success)
        assertFalse(target.exists())
    }

    private fun recorder(sleepStep: Long) = StreamRecorder(
        client = OkHttpClient(),
        clock = { now },
        sleep = { now += sleepStep },
    )

    private fun assertNull(value: String?) = assertEquals(null, value)
}
