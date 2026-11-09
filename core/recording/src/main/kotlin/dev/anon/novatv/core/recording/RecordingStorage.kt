package dev.anon.novatv.core.recording

import android.content.Context
import android.os.StatFs
import java.io.File

/**
 * Kayıt dosyalarının yeri.
 *
 * Öncelik: kullanıcının seçtiği klasör (varsa), yoksa uygulamanın dış dosya
 * dizini. TV kutularında SAF seçici çoğu zaman yok; bu yüzden dış dizin
 * varsayılan ve izin gerektirmeyen yoldur.
 */
class RecordingStorage(context: Context) {

    private val appContext = context.applicationContext

    fun directory(): File {
        val external = appContext.getExternalFilesDir(null)
        val base = external ?: appContext.filesDir
        return File(base, "recordings").apply { mkdirs() }
    }

    fun fileFor(plan: RecordingPlan): File = File(directory(), recordingFileName(plan))

    fun freeBytes(): Long = runCatching {
        val stat = StatFs(directory().absolutePath)
        stat.availableBlocksLong * stat.blockSizeLong
    }.getOrDefault(Long.MAX_VALUE)

    /** Kayıt süresi ve tahmini bit hızına göre yer var mı? */
    fun hasRoom(plan: RecordingPlan, bitsPerSecond: Long = ESTIMATED_BPS): Boolean {
        val estimated = plan.durationMs / 1000L * bitsPerSecond / 8L
        return freeBytes() > estimated + MIN_FREE_BYTES
    }

    fun delete(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return runCatching { File(path).takeIf { it.exists() }?.delete() == true }
            .getOrDefault(false)
    }

    private companion object {
        /** ~4 Mbit/s: 1080p yayın için cömert üst tahmin. */
        const val ESTIMATED_BPS = 4_000_000L
        const val MIN_FREE_BYTES = 300L * 1024 * 1024
    }
}
