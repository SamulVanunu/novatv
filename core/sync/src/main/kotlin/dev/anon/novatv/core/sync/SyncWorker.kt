package dev.anon.novatv.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.anon.novatv.core.model.SyncState

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val engine: SyncEngine,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val playlistId = inputData.getLong(KEY_PLAYLIST_ID, -1L)
        if (playlistId <= 0L) return Result.failure()

        return when (engine.sync(playlistId)) {
            SyncState.OK, SyncState.PARTIAL -> Result.success()
            SyncState.FAILED -> if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
            SyncState.NEVER, SyncState.RUNNING -> Result.success()
        }
    }

    companion object {
        const val KEY_PLAYLIST_ID = "playlistId"
        private const val MAX_ATTEMPTS = 2

        fun workData(playlistId: Long) = workDataOf(KEY_PLAYLIST_ID to playlistId)
    }
}

class SyncWorkerFactory(private val engine: SyncEngine) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        if (workerClassName == SyncWorker::class.java.name) {
            SyncWorker(appContext, workerParameters, engine)
        } else {
            null
        }
}
