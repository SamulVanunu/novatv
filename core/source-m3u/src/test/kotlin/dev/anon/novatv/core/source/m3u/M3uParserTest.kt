package dev.anon.novatv.core.source.m3u

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.StreamFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {

    @Test
    fun parsesExtendedAttributes() {
        val out = mutableListOf<Channel>()
        val meta = M3uParser().parse(1, resource("m3u/basic.m3u"), out::add)

        assertEquals(3, meta.parsed)
        assertEquals(0, meta.skipped)
        assertEquals("TRT 1", out[0].name)
        assertEquals("Ulusal", out[0].groupName)
        assertEquals("trt1.tr", out[0].tvgId)
        assertEquals("http://logo/trt1.png", out[0].logoUrl)
        assertEquals(1, out[0].number)
        assertEquals("http://a/1.ts", out[0].remoteId)
        assertEquals(StreamFormat.TS, out[0].format)
        assertEquals(StreamFormat.HLS, out[2].format)
    }

    @Test
    fun brokenLinesAreSkippedNotFatal() {
        val out = mutableListOf<Channel>()
        val meta = M3uParser().parse(1, resource("m3u/broken.m3u"), out::add)

        assertEquals(2, meta.parsed)
        assertEquals(3, meta.skipped)
        assertEquals(listOf("Kanal A", "Kanal E"), out.map { it.name })
    }

    @Test
    fun extvlcoptSetsUserAgent() {
        val out = mutableListOf<Channel>()
        val meta = M3uParser().parse(1, resource("m3u/with_vlc_opts.m3u"), out::add)

        assertEquals(1, meta.parsed)
        assertEquals("VLC/3.0", meta.userAgents["http://a/vlc.ts"])
        assertEquals("http://ref/", meta.referrers["http://a/vlc.ts"])
    }

    @Test
    fun tvgUrlHeaderIsCaptured() {
        val out = mutableListOf<Channel>()
        val meta = M3uParser().parse(1, resource("m3u/with_tvg_header.m3u"), out::add)

        assertEquals(1, meta.parsed)
        assertEquals("http://epg.example/guide.xml", meta.tvgUrl)
        assertNull(meta.userAgents["http://a/1.ts"])
    }

    @Test
    fun extgrpOverridesGroupTitle() {
        val input = """
            #EXTM3U
            #EXTINF:-1 group-title="Yanlis",Kanal A
            #EXTGRP:Dogru
            http://a/1.ts
        """.trimIndent().byteInputStream()

        val out = mutableListOf<Channel>()
        M3uParser().parse(1, input, out::add)

        assertEquals("Dogru", out.single().groupName)
    }

    private fun resource(name: String) =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "kaynak yok: $name" }
}
