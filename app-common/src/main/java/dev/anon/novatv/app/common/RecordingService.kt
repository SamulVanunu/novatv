package dev.anon.novatv.app.common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.anon.novatv.core.recording.RecordingPlan
import dev.anon.novatv.core.recording.StreamRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Ön planda çalışan kayıt servisi.
 *
 * Akış adresi yalnızca burada, çalışma anında çözülür; plana ve diske yazılmaz.
 * Aynı anda tek kayıt: ikinci istek reddedilir, kullanıcı planı iptal edebilir.
 */
class RecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var currentId: String? = null
    private var notifiedBytes = -1L

    /** Kullanıcı "Durdur" dediğinde yazma döngüsüne bildirilir. */
    @Volatile
    private var stopRequested = false

    /** Bildirim en fazla 5 saniyede bir tazelenir: sistem kuyruğu taşmasın. */
    private var lastNotifyAt = 0L

    private val graph: NovaGraph
        get() = (application as NovaApplication).graph

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                // Kooperatif durdurma: job iptal edilmez, finish() dosyayı
                // tamamlanmış kayıt olarak yazar ve servisi kendisi kapatır.
                requestStop()
                return START_NOT_STICKY
            }

            ACTION_RECORD -> {
                val id = intent.getStringExtra(EXTRA_ID)
                if (id.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (job?.isActive == true) {
                    if (id != currentId) {
                        graph.recordings.markFailed(id, "Aynı anda tek kayıt yapılabilir")
                    }
                    return START_NOT_STICKY
                }
                startRecording(id)
            }

            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(id: String) {
        val plan = graph.recordings.plan(id)
        if (plan == null) {
            stopSelf()
            return
        }
        if (plan.endAt <= System.currentTimeMillis()) {
            graph.recordings.markFailed(id, "Kayıt zamanı kaçırıldı")
            stopSelf()
            return
        }

        ensureChannel()
        currentId = id
        notifiedBytes = -1
        lastNotifyAt = 0L
        stopRequested = false
        graph.recordings.markRecording(id)
        startForeground(NOTIFICATION_ID, buildNotification(plan, bytes = 0L, done = false))

        job = scope.launch {
            val channel = runCatching { graph.channels.byId(plan.channelId) }.getOrNull()
            if (channel == null) {
                finish(plan, bytes = 0L, error = "Kanal bulunamadı")
                return@launch
            }
            val stream = runCatching { graph.streamResolver.resolve(channel) }.getOrNull()
            if (stream == null) {
                finish(plan, bytes = 0L, error = "Yayın adresi çözülemedi")
                return@launch
            }

            val target = graph.recordingStorage.fileFor(plan)
            val result = StreamRecorder().record(
                plan = plan,
                url = stream.url,
                headers = stream.headers,
                target = target,
                onBytes = ::onBytes,
                shouldStop = { stopRequested },
            )
            finish(plan, result.bytes, result.error)
        }
    }

    private fun onBytes(bytes: Long) {
        val id = currentId ?: return
        val now = System.currentTimeMillis()
        if (now - lastNotifyAt < NOTIFY_INTERVAL_MS) return
        lastNotifyAt = now
        notifiedBytes = bytes
        val plan = graph.recordings.plan(id) ?: return
        runCatching {
            notificationManager()?.notify(
                NOTIFICATION_ID,
                buildNotification(plan, bytes, done = false),
            )
        }
    }

    private fun finish(plan: RecordingPlan, bytes: Long, error: String?) {
        val id = plan.id
        val file = graph.recordingStorage.fileFor(plan)
        // Disk gerçeği esastır: sayaç ile dosya boyutu ayrışırsa dosya kazanır.
        val size = if (file.exists()) file.length() else 0L
        if (error == null && size > 0) {
            graph.recordings.markDone(id, file.absolutePath, size)
            runCatching {
                notificationManager()?.notify(
                    NOTIFICATION_ID,
                    buildNotification(plan, size, done = true),
                )
            }
        } else {
            file.delete()
            graph.recordings.markFailed(id, error ?: "Kayıt tamamlanamadı", size)
            runCatching { notificationManager()?.cancel(NOTIFICATION_ID) }
        }
        currentId = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    /**
     * Durdurma isteği. Kayıt sürüyorsa iş iptal edilmez; döngü kısa süre
     * içinde kapanır ve [finish] dosyayı DONE olarak yazar. Kayıt yoksa
     * servis temizlenir.
     */
    private fun requestStop() {
        if (job?.isActive == true) {
            stopRequested = true
            return
        }
        currentId?.let { id ->
            val bytes = graph.recordings.entry(id)?.bytes ?: 0L
            graph.recordings.markFailed(id, "Kayıt yarıda kaldı", bytes)
        }
        currentId = null
        notificationManager()?.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        if (job?.isActive == true && !stopRequested) {
            val id = currentId
            if (id != null) {
                graph.recordings.markFailed(id, "Kayıt yarıda kaldı")
            }
        }
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(plan: RecordingPlan, bytes: Long, done: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle(if (done) "Kayıt tamamlandı" else "Kaydediliyor")
            .setContentText("${plan.channelName} · ${plan.title}")
            .setOngoing(!done)
            .setAutoCancel(done)
            .setContentIntent(open)

        if (!done) {
            builder.addAction(android.R.drawable.ic_media_pause, "Durdur", stop)
            if (bytes > 0) {
                builder.setSubText(sizeLabel(bytes))
            }
        } else {
            builder.setSubText(sizeLabel(bytes))
        }
        return builder.build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = notificationManager() ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Kayıtlar", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun notificationManager(): NotificationManager? =
        getSystemService(NotificationManager::class.java)

    private fun sizeLabel(bytes: Long): String = when {
        bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        else -> "${bytes / 1000} KB"
    }

    companion object {
        const val ACTION_RECORD = "dev.anon.novatv.action.RECORD"
        const val ACTION_STOP = dev.anon.novatv.core.recording.RecordingScheduler.ACTION_STOP
        const val EXTRA_ID = "recordingId"
        private const val CHANNEL_ID = "recordings"
        private const val NOTIFY_INTERVAL_MS = 5_000L
        private const val NOTIFICATION_ID = 41_001
    }
}

/** Alarm çaldığında servisi başlatan alıcı. */
class RecordingReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(dev.anon.novatv.core.recording.RecordingScheduler.EXTRA_ID)
            ?: return
        val launch = Intent(context, RecordingService::class.java)
            .setAction(RecordingService.ACTION_RECORD)
            .putExtra(RecordingService.EXTRA_ID, id)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(launch)
            } else {
                context.startService(launch)
            }
        }.onFailure { error ->
            Log.w("RecordingReceiver", "Kayıt başlatılamadı: ${error.javaClass.simpleName}")
        }
    }
}

/** Açılış sonrası planları yeniden kurar. */
class RecordingBootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? NovaApplication ?: return
        app.graph.recordingScheduler.reschedule()
    }
}
