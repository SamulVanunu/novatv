package dev.anon.novatv.core.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    private fun onMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }
}
