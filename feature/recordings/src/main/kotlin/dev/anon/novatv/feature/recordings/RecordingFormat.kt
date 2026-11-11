package dev.anon.novatv.feature.recordings

import dev.anon.novatv.core.recording.RecordingEntry
import dev.anon.novatv.core.recording.RecordingStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val RECORDING_TIME: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM EEE · HH:mm", Locale("tr"))

internal fun recordingTimeLabel(startAt: Long, endAt: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val start = Instant.ofEpochMilli(startAt).atZone(zone)
    val end = Instant.ofEpochMilli(endAt).atZone(zone)
    val endFmt = DateTimeFormatter.ofPattern("HH:mm", Locale("tr"))
    return RECORDING_TIME.format(start) + "–" + endFmt.format(end)
}

/** Biten kayıtta gerçek bitişi, süren/planlanan kayıtta planlanan bitişi gösterir. */
internal fun recordingEntryTimeLabel(entry: RecordingEntry, zone: ZoneId = ZoneId.systemDefault()): String =
    recordingTimeLabel(entry.startAt, entry.finishedAt ?: entry.endAt, zone)

/**
 * Kaydın süresi dakika olarak: elle durdurulmuş kayıtta gerçek bitiş,
 * diğerlerinde planlanan pencere. Kompakt kartta süre satırı için.
 */
internal fun recordingDurationLabel(entry: RecordingEntry): String {
    val end = entry.finishedAt ?: entry.endAt
    val minutes = ((end - entry.startAt) / 60_000L).coerceAtLeast(1L)
    return "$minutes dk"
}

internal fun recordingStatusLabel(status: RecordingStatus): String = when (status) {
    RecordingStatus.SCHEDULED -> "Planlandı"
    RecordingStatus.RECORDING -> "Kaydediliyor"
    RecordingStatus.DONE -> "Tamamlandı"
    RecordingStatus.FAILED -> "Başarısız"
    RecordingStatus.CANCELLED -> "İptal edildi"
}

/** Kayıt ilerlemesi 0..1; planlanan pencereye göre. */
internal fun recordingProgress(entry: RecordingEntry, now: Long): Float {
    if (entry.endAt <= entry.startAt) return 0f
    return ((now - entry.startAt).toFloat() / (entry.endAt - entry.startAt)).coerceIn(0f, 1f)
}
