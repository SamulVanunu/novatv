package dev.anon.novatv.feature.player

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.Display
import kotlin.math.abs

/** Ekran modu seçimi için gereken en küçük veri; birim testte Android'e ihtiyaç duymaz. */
internal data class DisplayModeOption(val id: Int, val refreshRateHz: Float)

/**
 * Otomatik kare hızı (AFR): video kare hızına en yakın ekran modunu seçer.
 * Yalnızca destekleyen cihazlarda ve 0.75 Hz tolerans içinde uygulanır.
 */
object AutoFrameRate {

    fun apply(activity: Activity?, frameRate: Float) {
        if (activity == null || frameRate <= 0f) return
        val display = displayOf(activity) ?: return
        val modes = runCatching { display.supportedModes.asList() }.getOrNull() ?: return
        val options = modes.map { DisplayModeOption(it.modeId, it.refreshRate) }
        val best = selectMode(options, frameRate) ?: return
        val window = activity.window ?: return
        if (window.attributes.preferredDisplayModeId == best.id) return
        runCatching {
            val attributes = window.attributes
            attributes.preferredDisplayModeId = best.id
            window.attributes = attributes
        }.onFailure { error ->
            Log.d("AutoFrameRate", "Mod değiştirilemedi: ${error.javaClass.simpleName}")
        }
    }

    /** Oynatıcıdan çıkarken ekranı varsayılan moda döndürür. */
    fun reset(activity: Activity?) {
        val window = activity?.window ?: return
        if (window.attributes.preferredDisplayModeId == 0) return
        runCatching {
            val attributes = window.attributes
            attributes.preferredDisplayModeId = 0
            window.attributes = attributes
        }.onFailure { error ->
            Log.d("AutoFrameRate", "Varsayılan moda dönülemedi: ${error.javaClass.simpleName}")
        }
    }

    /**
     * Hedefe en yakın modu döndürür. Tolerans dışındaysa `null`: yanlış kare
     * hızına geçmek, hiç geçmemekten daha kötü (titreme).
     */
    internal fun selectMode(options: List<DisplayModeOption>, frameRate: Float): DisplayModeOption? {
        if (frameRate <= 0f) return null
        val best = options
            .filter { it.refreshRateHz > 0f }
            .minByOrNull { abs(it.refreshRateHz - frameRate) }
            ?: return null
        return best.takeIf { abs(it.refreshRateHz - frameRate) <= TOLERANCE_HZ }
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
