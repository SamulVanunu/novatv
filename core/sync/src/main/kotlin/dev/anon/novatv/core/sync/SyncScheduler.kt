package dev.anon.novatv.core.sync

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.anon.novatv.core.model.Playlist
import java.util.concurrent.TimeUnit

class SyncScheduler(
    private val workManager: WorkManager,
    @Suppress("unused") private val engine: SyncEngine,
) {

    /** Aktif playlistler için periyodik iş kurar; bayat olanı hemen bir kez tetikler. */
    fun scheduleAll(playlists: List<Playlist>) {
        val now = System.currentTimeMillis()
        playlists.filter { it.isActive }.forEach { playlist ->
            if (isStale(playlist, now)) syncNow(playlist.id)

            val intervalHours = playlist.syncIntervalHours.coerceAtLeast(1).toLong()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(intervalHours, TimeUnit.HOURS)
                .setInputData(SyncWorker.workData(playlist.id))
                .build()
            workManager.enqueueUniquePeriodicWork(
                periodicName(playlist.id),
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }

    fun syncNow(playlistId: Long) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(SyncWorker.workData(playlistId))
            .build()
        workManager.enqueueUniqueWork(
            oneShotName(playlistId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun isStale(playlist: Playlist, now: Long): Boolean {
        val last = playlist.lastSyncAt ?: return true
        val intervalMs = playlist.syncIntervalHours.coerceAtLeast(1) * 3_600_000L
        return now - last >= intervalMs
    }

    private fun periodicName(playlistId: Long) = "novatv-sync-periodic-$playlistId"
    private fun oneShotName(playlistId: Long) = "novatv-sync-now-$playlistId"
}
