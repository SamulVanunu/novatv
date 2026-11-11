package dev.anon.novatv.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Pencere genişliği sınıfı: telefon/dar ekran (Compact), tablet (Medium),
 * geniş tablet (Expanded). Boşluklar ve özelliğe özel ızgara kuralları buna
 * göre değişir; kütüphane sütunları `:feature:library` içindeki
 * `libraryGridColumns` ile tek yerden seçilir.
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

/**
 * Kabuğun sabitlediği pencere sınıfı; `null` ise sınıf ekran genişliğinden
 * okunur.
 *
 * Dokunmatik uygulama `Expanded`'a çıkmaz (bkz. `touchWindow`), ama alt
 * ekranlar sınıfı kendileri okuyunca bu kural beş ayrı çağrı yerinde
 * unutulabiliyordu. Kabuk sınıfı bir kez buraya yazar; içindeki her ekran ve
 * `rememberNovaSpacing()` aynı değeri görür.
 */
val LocalNovaWindow = compositionLocalOf<NovaWindow?> { null }

/** Ekran genişliğinden pencere sınıfı; yeniden boyutlandığında güncellenir. */
@Composable
fun rememberNovaWindow(): NovaWindow =
    LocalNovaWindow.current ?: windowClass(LocalConfiguration.current.screenWidthDp)
