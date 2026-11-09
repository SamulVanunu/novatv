@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.anon.novatv.core.player

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.DataSpec
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PlaybackControllerTest {

    @Test
    fun hlsUrlsPickHlsFactory() {
        val picked = mediaSourceFor(
            url = "http://example.com/live/channel.m3u8",
            hls = { "hls:$it" },
            dash = { "dash:$it" },
            progressive = { "progressive:$it" },
        )
        assertEquals("hls:http://example.com/live/channel.m3u8", picked)
    }

    @Test
    fun dashAndProgressiveUrlsPickTheirFactories() {
        assertEquals(
            "dash",
            mediaSourceFor("http://example.com/vod.mpd", { "hls" }, { "dash" }, { "progressive" }),
        )
        assertEquals(
            "progressive",
            mediaSourceFor("http://example.com/vod.mkv", { "hls" }, { "dash" }, { "progressive" }),
        )
    }

    @Test
    fun queryStringDoesNotHideExtension() {
        assertEquals(SourceKind.HLS, sourceKindFor("http://example.com/list.m3u8?token=abc"))
        assertEquals(SourceKind.HLS, sourceKindFor("http://example.com/LIST.M3U8#frag"))
    }

    @Test
    fun http404MapsToRetryableFailure() {
        val error = PlaybackException(
            "HTTP 404",
            HttpDataSource.InvalidResponseCodeException(
                404,
                "Not Found",
                null,
                emptyMap(),
                DataSpec(Uri.parse("http://example.com/missing.m3u8")),
                ByteArray(0),
            ),
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        )

        val failed = PlaybackErrorPolicy.map(error)

        assertTrue(failed.retryable)
        assertEquals("HTTP 404 Not Found", failed.reason)
    }

    @Test
    fun decoderInitFailureFallsBackToSoftwareDecodingAndPreparesAgain() {
        val controller = PlaybackController(
            org.robolectric.RuntimeEnvironment.getApplication(),
        )
        try {
            val codecError = PlaybackException(
                "decoder init failed",
                IllegalStateException("MediaCodec init failed"),
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            )

            assertTrue(controller.hardwareDecodingEnabled)
            controller.prepare("http://127.0.0.1:9/live.m3u8")
            val before = controller.prepareCount

            controller.handleErrorForTest(codecError)

            assertFalse("kodek hatasında yazılım çözmeye düşülmeli", controller.hardwareDecodingEnabled)
            assertTrue("yeniden hazırlanmalı", controller.prepareCount > before)
            assertEquals(
                "Donanım çözme başarısız; yazılım çözmeye geçildi.",
                controller.notice.value,
            )
        } finally {
            controller.release()
        }
    }

    @Test
    fun externalSubtitleIsAttachedToMediaItem() {
        val spec = SubtitleSpec(
            uri = Uri.parse("content://docs/42"),
            mimeType = "application/x-subrip",
            language = "tr",
        )

        val item = mediaItemFor("http://example.com/live/index.m3u8", spec)

        val configured = item.localConfiguration!!.subtitleConfigurations
        assertEquals(1, configured.size)
        val subtitle = configured.single()
        assertEquals(Uri.parse("content://docs/42"), subtitle.uri)
        assertEquals("application/x-subrip", subtitle.mimeType)
        assertEquals("tr", subtitle.language)
    }

    @Test
    fun mediaItemWithoutSubtitleHasNoTrackConfigurations() {
        val item = mediaItemFor("http://example.com/live/index.m3u8", null)

        assertTrue(item.localConfiguration!!.subtitleConfigurations.isEmpty())
    }

    @Test
    fun malformedManifestIsNotRetryable() {
        val error = PlaybackException(
            "bozuk manifest",
            IllegalStateException("unexpected tag"),
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        )

        val failed = PlaybackErrorPolicy.map(error)

        assertFalse(failed.retryable)
    }

    @Test
    fun aspectAndBufferDefaultsAreStable() {
        val controller = PlaybackController(org.robolectric.RuntimeEnvironment.getApplication())
        try {
            assertEquals(VideoAspect.FIT, controller.aspect.value)
            controller.setAspect(VideoAspect.ZOOM)
            assertEquals(VideoAspect.ZOOM, controller.aspect.value)
            assertEquals(PlaybackState.Idle, controller.state.value)
            assertFalse(controller.canSeekBack.value)
        } finally {
            controller.release()
        }
    }
}
