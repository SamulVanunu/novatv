package dev.anon.novatv.core.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HlsPlaylistTest {

    @Test
    fun mediaPlaylistResolvesRelativeSegmentsAndTargetDuration() {
        val media = parseHlsPlaylist(
            """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-TARGETDURATION:4
            #EXTINF:4.0,
            seg-1.ts
            #EXTINF:4.0,
            https://cdn.example.com/seg-2.ts?token=abc
            #EXT-X-ENDLIST
            """.trimIndent(),
            "https://cdn.example.com/live/index.m3u8",
        )

        assertEquals(2, media.segments.size)
        assertEquals("https://cdn.example.com/live/seg-1.ts", media.segments[0])
        assertEquals("https://cdn.example.com/seg-2.ts?token=abc", media.segments[1])
        assertEquals(4, media.targetSeconds)
        assertTrue(media.ended)
        assertFalse(media.encrypted)
    }

    @Test
    fun masterPlaylistKeepsVariantsAndBandwidths() {
        val media = parseHlsPlaylist(
            """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360
            low/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1920x1080
            high/index.m3u8
            """.trimIndent(),
            "https://cdn.example.com/master.m3u8",
        )

        assertEquals(2, media.variants.size)
        assertEquals("https://cdn.example.com/high/index.m3u8", media.variants[1].url)
        assertEquals(3_000_000L, media.variants[1].bandwidth)
        assertTrue(media.segments.isEmpty())
    }

    @Test
    fun encryptionIsDetectedButMethodNoneIsNot() {
        val encrypted = parseHlsPlaylist(
            """
            #EXTM3U
            #EXT-X-KEY:METHOD=AES-128,URI="key.bin"
            #EXTINF:4.0,
            s.ts
            """.trimIndent(),
            "https://cdn.example.com/live/index.m3u8",
        )
        assertTrue(encrypted.encrypted)

        val clear = parseHlsPlaylist(
            """
            #EXTM3U
            #EXT-X-KEY:METHOD=NONE
            #EXTINF:4.0,
            s.ts
            """.trimIndent(),
            "https://cdn.example.com/live/index.m3u8",
        )
        assertFalse(clear.encrypted)
    }

    @Test
    fun hlsUrlDetectionIgnoresQueryAndCase() {
        assertTrue(isHlsUrl("https://a.example/live/ch.m3u8?token=1"))
        assertTrue(isHlsUrl("HTTPS://A.EXAMPLE/CH.M3U8"))
        assertFalse(isHlsUrl("https://a.example/live/ch.ts"))
        assertFalse(isHlsUrl("https://a.example/live/ch"))
    }

    @Test
    fun resolveUrlHandlesRelativeAndAbsoluteReferences() {
        assertEquals(
            "https://a.example/x/y/seg.ts",
            resolveUrl("https://a.example/x/y/index.m3u8", "seg.ts"),
        )
        assertEquals(
            "https://b.example/seg.ts",
            resolveUrl("https://a.example/x/index.m3u8", "https://b.example/seg.ts"),
        )
        assertEquals(
            "https://a.example/seg.ts?x=1",
            resolveUrl("https://a.example/x/index.m3u8", "/seg.ts?x=1"),
        )
    }

    @Test
    fun fileNameIsFilesystemSafeAndTimestamped() {
        val plan = RecordingPlan(
            id = "p1",
            channelId = 1,
            channelName = "TRT 1 HD",
            title = "Habere Doğru / Bülten",
            startAt = 0,
            endAt = 60_000,
        )
        val name = recordingFileName(plan)
        assertTrue(name.endsWith(".ts"))
        assertFalse(name.contains("/"))
        assertFalse(name.contains(" "))
        assertTrue(name.length <= 90)
    }
}
