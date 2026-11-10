package dev.anon.novatv.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * "Açılışta başlat" açıksa TV açılınca uygulamayı başlatır.
 *
 * Tercih okuması ana iş parçacığında yapılmaz; `goAsync` ile kısa bir süre
 * tanınır. Tercih okunamazsa hiçbir şey yapılmaz — TV açılışını bekletmek
 * yanlış olurdu.
 */
class AutoStartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? NovaApplication ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = withTimeoutOrNull(3_000L) {
                    app.graph.preferences.appearance.first().autoStart
                } ?: false
                if (!enabled) return@launch
                // Android 10+ arka plandan etkinlik başlatmayı kısıtlar. Tek
                // kullanıcı-onaylı istisna "Diğer uygulamaların üzerinde
                // göster" izni; yoksa açılış sessizce atlanır.
                if (!android.provider.Settings.canDrawOverlays(context)) {
                    Log.i(TAG, "Açılışta başlatma için üstte gösterme izni yok, atlandı")
                    return@launch
                }
                val launch = context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ?: return@launch
                context.startActivity(launch)
            } catch (error: Exception) {
                Log.w(TAG, "Otomatik başlatma atlandı: ${error.javaClass.simpleName}")
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AutoStartReceiver"
    }
}
