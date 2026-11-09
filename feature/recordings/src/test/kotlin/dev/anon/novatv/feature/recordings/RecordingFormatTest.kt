package dev.anon.novatv.feature.recordings

import dev.anon.novatv.core.recording.RecordingEntry
import dev.anon.novatv.core.recording.RecordingStatus
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingFormatTest {

    private val zone = ZoneId.of("Europe/Istanbul")

    /** 2026-11-09 20:00 İstanbul. */
    private val start = ZonedDateTime.of(2026, 11, 9, 20, 0, 0, 0, zone)
        .toInstant()
        .toEpochMilli()
    private val end = start + 90 * 60_000L

    @Test
    fun timeLabelShowsStartAndEnd() {
        val label = recordingTimeLabel(start, end, zone)
        assertTrue(label, label.contains("20:00"))
        assertTrue(label, label.endsWith("21:30"))
        assertTrue(label, label.contains("Kas"))
    }

    @Test
    fun statusLabelsAreTurkish() {
        assertEquals("Planlandı", recordingStatusLabel(RecordingStatus.SCHEDULED))
        assertEquals("Kaydediliyor", recordingStatusLabel(RecordingStatus.RECORDING))
        assertEquals("Tamamlandı", recordingStatusLabel(RecordingStatus.DONE))
        assertEquals("Başarısız", recordingStatusLabel(RecordingStatus.FAILED))
        assertEquals("İptal edildi", recordingStatusLabel(RecordingStatus.CANCELLED))
    }

    @Test
    fun progressClampsBetweenZeroAndOne() {
        val entry = entry(startAt = start, endAt = end)
        assertEquals(0f, recordingProgress(entry, start - 5_000L))
        assertEquals(0.5f, recordingProgress(entry, start + 45 * 60_000L))
        assertEquals(1f, recordingProgress(entry, end + 60_000L))
    }

    @Test
    fun progressHandlesZeroLengthWindow() {
        val entry = entry(startAt = start, endAt = start)
        assertEquals(0f, recordingProgress(entry, start))
    }

    private fun entry(startAt: Long, endAt: Long) = RecordingEntry(
        id = "rec-1",
        channelId = 3,
        channelName = "TRT 1",
        title = "Bülten",
        startAt = startAt,
        endAt = endAt,
        status = RecordingStatus.DONE.name,
    )
}
