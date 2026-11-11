package dev.anon.novatv.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Pencere genişliği sınıfı: telefon/dar ekran (Compact), tablet (Medium),
 * geniş tablet (Expanded). Kütüphane ızgarası ve boşluklar buna göre değişir.
 */
enum class NovaWindow { Compact, Medium, Expanded }

/**
 * Genişlik eşikleri Material pencere sınıfıyla aynı:
 * 600 dp tablet, 840 dp geniş tablet başlar.
 */
fun windowClass(widthDp: Int): NovaWindow = when {
    widthDp >= 840 -> NovaWindow.Expanded
    widthDp >= 600 -> NovaWindow.Medium
    else -> NovaWindow.Compact
}

/** Kütüphane ızgarasının sütun sayısı: kompakt 2, orta 3, geniş 5. */
fun libraryColumns(window: NovaWindow): Int = when (window) {
    NovaWindow.Compact -> 2
    NovaWindow.Medium -> 3
    NovaWindow.Expanded -> 5
}

/** Ekran genişliğinden pencere sınıfı; yeniden boyutlandığında güncellenir. */
@Composable
fun rememberNovaWindow(): NovaWindow =
    windowClass(LocalConfiguration.current.screenWidthDp)
