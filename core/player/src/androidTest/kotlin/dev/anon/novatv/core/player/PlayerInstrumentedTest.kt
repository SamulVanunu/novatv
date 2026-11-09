package dev.anon.novatv.core.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Gerçek ExoPlayer üzerinde durum/parça davranışı.
 *
 * Canlı oynatma testi, Task 14'te üretilecek yerel HLS fixture'ına bağlıdır:
 * `-Pandroid.testInstrumentationRunnerArguments.hlsUrl=http://10.0.2.2:PORT/hls/index.m3u8`
 * verilmezse o test atlanır.
 */
@RunWith(AndroidJUnit4::class)
class PlayerInstrumentedTest {

    private lateinit var controller: PlaybackController

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        onMain { controller = PlaybackController(context) }
    }

    @After
    fun tearDown() {
        onMain { controller.release() }
    }

    @Test
    fun unreachableSourceEndsInFailedState() {
        onMain { controller.prepare("http://127.0.0.1:9/live.m3u8") }

        val failed = runBlocking {
            withTimeout(30_000) { controller.state.first { it is PlaybackState.Failed } }
        } as PlaybackState.Failed

        assertTrue("bağlantı hatası yeniden denenebilir olmalı", failed.retryable)
        assertTrue("hata nedeni boş olmamalı", failed.reason.isNotBlank())
    }

    /**
     * Harici SRT, akışa yan yüklenince parça listesinde görünmeli.
     * Mock panel erişilebilir değilse test atlanır.
     */
    @Test
    fun externalSrtAppearsAsSubtitleTrack() {
        val base = mockServerBase()
        assumeTrue("mock panel erişilebilir değil (Task 14)", base != null)

        onMain {
            controller.prepare(
                url = "$base/hls/index.m3u8",
                subtitle = SubtitleSpec(
                    uri = Uri.parse("$base/hls/test.srt"),
                    mimeType = "application/x-subrip",
                    language = "tr",
                ),
            )
        }

        runBlocking {
            withTimeout(20_000) { controller.state.first { it is PlaybackState.Playing } }
        }

        val tracks = runBlocking {
            withTimeout(20_000) {
                controller.subtitleTracks.first { list -> list.any { it.language == "tr" } }
            }
        }

        assertTrue(
            "yan yüklenen SRT parça listesinde olmalı, şu an: $tracks",
            tracks.any { it.language == "tr" },
        )
    }

    /**
     * Var olmayan akış adresi (HTTP 404) çökmeden yeniden denenebilir hata
     * durumuna düşmeli. Mock panel çalışmıyorsa test atlanır.
     */
    @Test
    fun http404EndsInRetryableFailure() {
        val base = mockServerBase()
        assumeTrue("mock panel erişilebilir değil (Task 14)", base != null)

        onMain { controller.prepare("$base/missing.m3u8") }

        val failed = runBlocking {
            withTimeout(30_000) { controller.state.first { it is PlaybackState.Failed } }
        } as PlaybackState.Failed

        assertTrue("HTTP 404 yeniden denenebilir olmalı", failed.retryable)
        assertTrue("hata nedeni HTTP 404 içermeli, şu an: ${failed.reason}", failed.reason.contains("404"))
    }

    @Test
    fun hlsFixturePlaysExposesAudioTrackAndSurvivesSeekBack() {
        val hlsUrl = InstrumentationRegistry.getArguments().getString("hlsUrl")
        assumeTrue("HLS fixture adresi verilmedi (Task 14)", hlsUrl != null)

        onMain { controller.prepare(hlsUrl!!) }

        runBlocking {
            withTimeout(20_000) { controller.state.first { it is PlaybackState.Playing } }
        }

        assertTrue(
            "HLS akışında en az bir ses parçası olmalı",
            controller.audioTracks.value.isNotEmpty(),
        )

        onMain { controller.seekBack(10_000) }
        Thread.sleep(500)

        val after = controller.state.value
        assertTrue(
            "geri sarma durumu bozmamalı, şu an: $after",
            after is PlaybackState.Playing || after is PlaybackState.Buffering,
        )
    }

    /** Emülatörde host 10.0.2.2; `adb reverse` çalışan cihazda 127.0.0.1. */
    private fun mockServerBase(): String? = listOf(
        "http://10.0.2.2:8089",
        "http://127.0.0.1:8089",
    ).firstOrNull { base ->
        runCatching {
            val connection = java.net.URL("$base/health").openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 2_000
            connection.readTimeout = 2_000
            try {
                connection.responseCode == 200
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(false)
    }

    private fun onMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }
}
