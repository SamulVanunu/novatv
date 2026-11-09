package dev.anon.novatv.feature.player

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.Display
import kotlin.math.abs

/**
 * Otomatik kare hızı (AFR): video kare hızına en yakın ekran modunu seçer.
 * Yalnızca destekleyen cihazlarda ve 0.75 Hz tolerans içinde uygulanır.
 */
object AutoFrameRate {

    fun apply(activity: Activity?, frameRate: Float) {
        if (activity == null || frameRate <= 0f) return
        val display = displayOf(activity) ?: return
        val modes = runCatching { display.supportedModes.asList() }.getOrNull() ?: return
        val best = modes
            .filter { it.refreshRate > 0f }
            .minByOrNull { abs(it.refreshRate - frameRate) }
            ?: return
        if (abs(best.refreshRate - frameRate) > TOLERANCE_HZ) return
        val window = activity.window ?: return
        if (window.attributes.preferredDisplayModeId == best.modeId) return
        runCatching {
            val attributes = window.attributes
            attributes.preferredDisplayModeId = best.modeId
            window.attributes = attributes
        }.onFailure { error ->
            Log.d("AutoFrameRate", "Mod değiştirilemedi: ${error.javaClass.simpleName}")
        }
    }

    @Suppress("DEPRECATION")
    private fun displayOf(activity: Activity): Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            activity.windowManager.defaultDisplay
        }

    private const val TOLERANCE_HZ = 0.75f
}
