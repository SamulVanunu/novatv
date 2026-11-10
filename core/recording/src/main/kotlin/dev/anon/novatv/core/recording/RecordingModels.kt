package dev.anon.novatv.core.recording

import kotlinx.serialization.Serializable

enum class RecordingStatus { SCHEDULED, RECORDING, DONE, FAILED, CANCELLED }

/** Planlanan kayıt. Akış adresi bilerek saklanmaz: kimlik bilgisi taşır. */
@Serializable
data class RecordingPlan(
    val id: String,
    val channelId: Long,
    val channelName: String,
    val title: String,
    val startAt: Long,
    val endAt: Long,
    val createdAt: Long = 0,
) {
    val durationMs: Long get() = (endAt - startAt).coerceAtLeast(60_000L)
}

/** Kullanıcıya görünen kayıt satırı; plan ve sonucu tek yerde tutar. */
@Serializable
data class RecordingEntry(
    val id: String,
    val channelId: Long,
    val channelName: String,
    val title: String,
    val startAt: Long,
    val endAt: Long,
    val status: String,
    val filePath: String? = null,
    val bytes: Long = 0,
    val message: String? = null,
    /** Kayıt gerçekten bu anda kapandı; elle durdurmada planlanan bitişten önce. */
    val finishedAt: Long? = null,
) {
    val statusValue: RecordingStatus
        get() = runCatching { RecordingStatus.valueOf(status) }
            .getOrDefault(RecordingStatus.SCHEDULED)

    val isActive: Boolean
        get() = statusValue == RecordingStatus.RECORDING || statusValue == RecordingStatus.SCHEDULED

    val sizeLabel: String
        get() = when {
            bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
            else -> "$bytes B"
        }

    fun plan(): RecordingPlan = RecordingPlan(
        id = id,
        channelId = channelId,
        channelName = channelName,
        title = title,
        startAt = startAt,
        endAt = endAt,
    )
}

/** Kayıt dosyası adı: kanal + program + tarih, dosya sistemi güvenli. */
fun recordingFileName(plan: RecordingPlan): String {
    val stamp = java.time.Instant.ofEpochMilli(plan.startAt)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
    val safe = (plan.channelName + "-" + plan.title)
        .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
        .trim('-')
        .take(70)
        .ifBlank { "kayit" }
    return "$stamp-$safe.ts"
}
