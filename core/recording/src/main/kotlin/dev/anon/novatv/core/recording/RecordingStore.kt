package dev.anon.novatv.core.recording

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Planlar ve sonuçlar küçük bir JSON kümesi olarak `SharedPreferences`'ta
 * tutulur: kayıt özelliği Room şemasını büyütmez, yedekleme dosyası da sade kalır.
 */
class RecordingStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("novatv_recordings", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun plans(): List<RecordingPlan> {
        val raw = prefs.getString(KEY_PLANS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<RecordingPlan>>(raw) }
            .getOrDefault(emptyList())
    }

    fun entries(): List<RecordingEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<RecordingEntry>>(raw) }
            .getOrDefault(emptyList())
            .sortedByDescending { it.startAt }
    }

    fun entry(id: String): RecordingEntry? = entries().firstOrNull { it.id == id }

    fun plan(id: String): RecordingPlan? = plans().firstOrNull { it.id == id }

    /** Aynı saat aralığında başka plan varsa yeni plan eklenmez. */
    fun hasOverlap(plan: RecordingPlan): Boolean = plans().any { existing ->
        existing.id != plan.id && existing.startAt < plan.endAt && plan.startAt < existing.endAt
    }

    fun addPlan(plan: RecordingPlan): Boolean {
        if (hasOverlap(plan)) return false
        prefs.edit()
            .putString(KEY_PLANS, json.encodeToString(plans().filterNot { it.id == plan.id } + plan))
            .putString(
                KEY_ENTRIES,
                json.encodeToString(
                    entries().filterNot { it.id == plan.id } + RecordingEntry(
                        id = plan.id,
                        channelId = plan.channelId,
                        channelName = plan.channelName,
                        title = plan.title,
                        startAt = plan.startAt,
                        endAt = plan.endAt,
                        status = RecordingStatus.SCHEDULED.name,
                    ),
                ),
            )
            .apply()
        return true
    }

    fun removePlan(id: String) {
        prefs.edit()
            .putString(KEY_PLANS, json.encodeToString(plans().filterNot { it.id == id }))
            .apply()
    }

    /** Planı ve planlanmış satırı siler; tamamlanan dosyaya dokunmaz. */
    fun cancel(id: String) {
        removePlan(id)
        updateEntry(id) { it.copy(status = RecordingStatus.CANCELLED.name) }
    }

    fun upsertEntry(entry: RecordingEntry) {
        updateEntry(entry.id) { entry }
    }

    fun markRecording(id: String) = updateEntry(id) {
        it.copy(status = RecordingStatus.RECORDING.name, message = null)
    }

    fun markDone(id: String, filePath: String, bytes: Long) {
        removePlan(id)
        updateEntry(id) {
            it.copy(
                status = RecordingStatus.DONE.name,
                filePath = filePath,
                bytes = bytes,
                message = null,
            )
        }
    }

    fun markFailed(id: String, message: String, bytes: Long = 0) {
        removePlan(id)
        updateEntry(id) {
            it.copy(
                status = RecordingStatus.FAILED.name,
                bytes = bytes,
                message = message.take(180),
            )
        }
    }

    fun removeEntry(id: String) {
        prefs.edit()
            .putString(KEY_ENTRIES, json.encodeToString(entries().filterNot { it.id == id }))
            .apply()
    }

    fun removePast(now: Long = System.currentTimeMillis()) {
        val stale = entries().filter {
            it.statusValue == RecordingStatus.SCHEDULED && it.endAt < now
        }
        stale.forEach { markFailed(it.id, "Kayıt başlatılamadı") }
    }

    /** Uygulama çöktüyse yarım kalan "RECORDING" satırlarını kapatır. */
    fun recoverInterrupted(now: Long = System.currentTimeMillis()) {
        entries().filter { it.statusValue == RecordingStatus.RECORDING }.forEach { entry ->
            if (entry.endAt < now) {
                markFailed(entry.id, "Kayıt yarıda kaldı", entry.bytes)
            }
        }
    }

    private fun updateEntry(id: String, transform: (RecordingEntry) -> RecordingEntry) {
        val current = entries()
        if (current.none { it.id == id }) return
        val next = current.map { if (it.id == id) transform(it) else it }
        prefs.edit().putString(KEY_ENTRIES, json.encodeToString(next)).apply()
    }

    private companion object {
        const val KEY_PLANS = "plans"
        const val KEY_ENTRIES = "entries"
    }
}
