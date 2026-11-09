package dev.anon.novatv.core.recording

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

sealed interface ScheduleResult {
    data class Scheduled(val plan: RecordingPlan, val startsAt: Long) : ScheduleResult
    data class Rejected(val reason: String) : ScheduleResult
}

/**
 * Kayıt planlarını AlarmManager'a bağlar.
 *
 * Aynı anda tek kayıt kuralı: çakışan plan reddedilir. Cihaz uykuya dalsa da
 * alarm çalsın diye `setExactAndAllowWhileIdle` denenir; izin yoksa (API 31+)
 * `setAndAllowWhileIdle` ile birkaç dakikalık sapma kabul edilir.
 *
 * Servis ve alıcı sınıfları dışarıdan verilir: bu modül `app`e bağlı değildir.
 */
class RecordingScheduler(
    private val context: Context,
    private val store: RecordingStore,
    private val serviceClass: Class<*>,
    private val receiverClass: Class<out BroadcastReceiver>,
    private val storage: RecordingStorage = RecordingStorage(context),
    private val clock: () -> Long = System::currentTimeMillis,
) {

    fun schedule(plan: RecordingPlan): ScheduleResult {
        val now = clock()
        if (plan.endAt <= now) {
            return ScheduleResult.Rejected("Program sona ermiş")
        }
        if (!storage.hasRoom(plan)) {
            return ScheduleResult.Rejected("Kayıt için yeterli boş alan yok")
        }
        val conflict = store.plans().firstOrNull { existing ->
            existing.id != plan.id && existing.startAt < plan.endAt && plan.startAt < existing.endAt
        }
        if (conflict != null) {
            return ScheduleResult.Rejected(
                "Aynı saatte kayıt var: ${conflict.channelName} · ${conflict.title}",
            )
        }
        if (!store.addPlan(plan)) {
            return ScheduleResult.Rejected("Plan kaydedilemedi")
        }
        val startsAt = plan.startAt.coerceAtLeast(now)
        arm(plan, startsAt)
        return ScheduleResult.Scheduled(plan, startsAt)
    }

    fun cancel(id: String) {
        val plan = store.plan(id)
        if (plan != null) disarm(plan)
        store.cancel(id)
    }

    /** Açılışta / BOOT_COMPLETED sonrası alarmları yeniden kurar. */
    fun reschedule() {
        val now = clock()
        store.plans().forEach { plan ->
            when {
                plan.endAt <= now -> store.markFailed(plan.id, "Kayıt zamanı kaçırıldı")
                plan.startAt <= now -> arm(plan, now)
                else -> arm(plan, plan.startAt)
            }
        }
    }

    /** Çalışan kaydı durdurur; servis satırı başarısız olarak kapatır. */
    fun stopNow() {
        val intent = Intent(context, serviceClass).setAction(ACTION_STOP)
        runCatching { context.startService(intent) }
    }

    fun startNow(id: String) {
        val intent = Intent(context, serviceClass)
            .setAction(ACTION_RECORD)
            .putExtra(EXTRA_ID, id)
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun arm(plan: RecordingPlan, at: Long) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(plan.id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val exact = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
        runCatching {
            if (exact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
        }.onFailure { error ->
            Log.w(TAG, "Kayıt alarmı kurulamadı: ${error.javaClass.simpleName}")
            // Alarm kurulamazsa plan kaybolmasın: hemen başlatmayı dene.
            if (at <= clock()) startNow(plan.id)
        }
    }

    private fun disarm(plan: RecordingPlan) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(plan.id, PendingIntent.FLAG_NO_CREATE) ?: return
        runCatching { manager.cancel(pending) }
    }

    private fun pendingIntent(id: String, extraFlags: Int): PendingIntent? {
        val intent = Intent(context, receiverClass)
            .setAction(ACTION_RECORD)
            .putExtra(EXTRA_ID, id)
        val flags = extraFlags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, flags)
    }

    companion object {
        const val ACTION_RECORD = "dev.anon.novatv.action.RECORD"
        const val ACTION_STOP = "dev.anon.novatv.action.RECORD_STOP"
        const val EXTRA_ID = "recordingId"
        private const val TAG = "RecordingScheduler"
    }
}
