package dev.anon.novatv.core.epg

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XmltvParserTest {

    private val utc = ZoneId.of("UTC")

    @Test
    fun compactTimeWithPositiveOffset() {
        val millis = parseXmltvTime("20261109120000 +0300", utc)
        assertEquals(Instant.parse("2026-11-09T09:00:00Z").toEpochMilli(), millis)
    }

    @Test
    fun compactTimeWithoutOffsetUsesGivenZone() {
        val millis = parseXmltvTime("20261109120000", ZoneId.of("Europe/Istanbul"))
        assertEquals(Instant.parse("2026-11-09T09:00:00Z").toEpochMilli(), millis)
    }

    @Test
    fun compactTimeWithNegativeOffset() {
        val millis = parseXmltvTime("20261109120000 -0500", utc)
        assertEquals(Instant.parse("2026-11-09T17:00:00Z").toEpochMilli(), millis)
    }

    @Test
    fun isoAndSpacedTimesAreAccepted() {
        assertEquals(
            Instant.parse("2026-11-09T09:00:00Z").toEpochMilli(),
            parseXmltvTime("2026-11-09T12:00:00+03:00", utc),
        )
        assertEquals(
            Instant.parse("2026-11-09T09:00:00Z").toEpochMilli(),
            parseXmltvTime("2026-11-09 12:00:00", ZoneId.of("Europe/Istanbul")),
        )
    }

    @Test
    fun invalidTimesReturnNull() {
        assertNull(parseXmltvTime(null, utc))
        assertNull(parseXmltvTime("", utc))
        assertNull(parseXmltvTime("09/11/2026 12:00", utc))
        assertNull(parseXmltvTime("20261309120000", utc))
    }

    @Test
    fun xmltvNsEpisodeIsConvertedToHumanForm() {
        assertEquals("S2 B3", normalizeEpisodeNum("xmltv_ns", "1.2/12."))
        assertEquals("S2 B3/4", normalizeEpisodeNum("xmltv_ns", "1.2.3/4"))
        assertEquals("B3", normalizeEpisodeNum("xmltv_ns", ".2."))
        assertEquals("S2", normalizeEpisodeNum("xmltv_ns", "1."))
        assertNull(normalizeEpisodeNum("xmltv_ns", "."))
        assertEquals("Bölüm 7", normalizeEpisodeNum("onscreen", "Bölüm 7"))
    }

    @Test
    fun sampleDocumentIsParsedWithAllFields() {
        val channels = mutableListOf<XmltvChannel>()
        val programs = mutableListOf<XmltvProgram>()

        XmltvParser(utc).parse(
            ByteArrayInputStream(SAMPLE.toByteArray()),
            onChannel = channels::add,
            onProgram = programs::add,
        )

        assertEquals(1, channels.size)
        assertEquals("trt1.tr", channels[0].id)
        assertEquals("TRT 1", channels[0].displayName)
        assertEquals("http://logo/trt1.png", channels[0].iconUrl)

        assertEquals(2, programs.size)
        val first = programs[0]
        assertEquals("trt1.tr", first.channelId)
        assertEquals("Haber", first.title)
        assertEquals("Sabah bülteni", first.subTitle)
        assertEquals("Günün gelişmeleri", first.description)
        assertEquals("Haber", first.category)
        assertEquals("7", first.rating)
        assertEquals("S2 B3", first.episodeNum)
        assertTrue(first.isNew)
        assertEquals(Instant.parse("2026-11-09T06:00:00Z").toEpochMilli(), first.startAt)
        assertEquals(Instant.parse("2026-11-09T07:00:00Z").toEpochMilli(), first.endAt)

        val second = programs[1]
        assertEquals("Kapanış", second.title)
        assertNull("Eksik stop null kalmalı", second.endAt)
    }

    @Test
    fun gzipInputIsDetectedAutomatically() {
        val compressed = ByteArrayOutputStream().also { out ->
            GZIPOutputStream(out).use { it.write(SAMPLE.toByteArray()) }
        }.toByteArray()

        val programs = mutableListOf<XmltvProgram>()
        XmltvParser(utc).parse(ByteArrayInputStream(compressed), onProgram = programs::add)

        assertEquals(2, programs.size)
    }

    private companion object {
        val SAMPLE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="trt1.tr">
                <display-name>TRT 1</display-name>
                <icon src="http://logo/trt1.png"/>
              </channel>
              <programme start="20261109090000 +0300" stop="20261109100000 +0300" channel="trt1.tr">
                <title>Haber</title>
                <sub-title>Sabah bülteni</sub-title>
                <desc>Günün gelişmeleri</desc>
                <category>Haber</category>
                <rating system="TBM"><value>7</value></rating>
                <episode-num system="xmltv_ns">1.2.</episode-num>
                <new/>
              </programme>
              <programme start="20261109100000 +0300" channel="trt1.tr">
                <title>Kapanış</title>
              </programme>
            </tv>
        """.trimIndent()
    }
}
