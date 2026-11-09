package dev.anon.novatv.feature.epg

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class EpgReminder(
    val id: String,
    val channelId: Long,
    val channelName: String,
    val title: String,
    val startAt: Long,
    val endAt: Long,
)

/** Hatırlatıcılar küçük bir JSON kümesi; Room şeması büyümez. */
class ReminderStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("novatv_reminders", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun all(): List<EpgReminder> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<EpgReminder>>(raw) }.getOrDefault(emptyList())
    }

    fun add(reminder: EpgReminder) {
        val next = all().filterNot { it.id == reminder.id } + reminder
        prefs.edit().putString(KEY, json.encodeToString<List<EpgReminder>>(next.sortedBy { it.startAt })).apply()
    }

    fun remove(id: String) {
        val next = all().filterNot { it.id == id }
        prefs.edit().putString(KEY, json.encodeToString<List<EpgReminder>>(next)).apply()
    }

    fun removePast(now: Long = System.currentTimeMillis()) {
        val next = all().filter { it.endAt > now }
        prefs.edit().putString(KEY, json.encodeToString<List<EpgReminder>>(next)).apply()
    }

    private companion object {
        const val KEY = "items"
    }
}

/**
 * `AlarmManager` ile hatırlatıcı; kesin alarm izni yoksa uygulama süresi
 * içinde yaklaşık alarm kurulur, hiçbir durumda çökme olmaz.
 */
class ReminderScheduler(private val context: Context) {

    private val store = ReminderStore(context)

    fun schedule(reminder: EpgReminder) {
        store.add(reminder)
        arm(reminder)
    }

    fun cancel(id: String) {
        store.remove(id)
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(pendingIntent(id))
    }

    fun rescheduleAll() {
        store.removePast()
        store.all().forEach(::arm)
    }

    /** Kurulu hatırlatıcı kimlikleri; ızgarada işaret göstermek için. */
    fun ids(): Set<String> = store.all().map { it.id }.toSet()

    private fun arm(reminder: EpgReminder) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = (reminder.startAt - LEAD_MS).coerceAtLeast(System.currentTimeMillis() + 1_000)
        val pending = pendingIntent(reminder.id, reminder)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()
        runCatching {
            if (canExact) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        }.onFailure {
            alarm.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun pendingIntent(id: String, reminder: EpgReminder? = null): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_ID, id)
            reminder?.let {
                putExtra(EXTRA_CHANNEL_ID, it.channelId)
                putExtra(EXTRA_CHANNEL_NAME, it.channelName)
                putExtra(EXTRA_TITLE, it.title)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_REMIND = "dev.anon.novatv.action.EPG_REMIND"
        const val EXTRA_ID = "reminderId"
        const val EXTRA_CHANNEL_ID = "channelId"
        const val EXTRA_CHANNEL_NAME = "channelName"
        const val EXTRA_TITLE = "programTitle"
        const val EXTRA_CHANNEL_LAUNCH = "dev.anon.novatv.extra.CHANNEL_ID"
        private const val LEAD_MS = 60_000L
        private const val CHANNEL_ID = "epg_reminders"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Rehber hatırlatıcıları",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }

        fun notify(context: Context, channelId: Long, channelName: String, title: String) {
            ensureChannel(context)
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?.apply {
                    putExtra(EXTRA_CHANNEL_LAUNCH, channelId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            val pending = launch?.let {
                PendingIntent.getActivity(
                    context,
                    channelId.hashCode(),
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle("$channelName · $title")
                .setContentText("Program birazdan başlıyor")
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            runCatching {
                NotificationManagerCompat.from(context).notify(channelId.hashCode(), notification)
            }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = intent.getLongExtra(ReminderScheduler.EXTRA_CHANNEL_ID, -1)
        val channelName = intent.getStringExtra(ReminderScheduler.EXTRA_CHANNEL_NAME).orEmpty()
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        if (channelId <= 0) return
        ReminderScheduler.notify(context, channelId, channelName, title)
        ReminderStore(context).remove(intent.getStringExtra(ReminderScheduler.EXTRA_ID).orEmpty())
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler(context).rescheduleAll()
        }
    }
}
