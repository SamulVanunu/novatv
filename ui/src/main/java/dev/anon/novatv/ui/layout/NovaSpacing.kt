package dev.anon.novatv.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ölçü tokenları: `screen` ekran kenarı boşluğu, `gap` öğeler arası boşluk.
 * Pencere sınıfına göre değişir — `object` tutamaz, bu yüzden veri sınıfı.
 */
data class NovaSpacing(val screen: Dp, val gap: Dp)

/**
 * Kompakt gövdelerin içerik sınırı: geniş ekranda satır 720 dp'yi aşmaz ve
 * içerik ortalanır. Televizyon gövdeleri bu sınırı kullanmaz.
 */
val CompactContentMaxWidth = 720.dp

/** Pencere sınıfına göre boşluklar: kompakt 16/8, orta 24/12, geniş 32/16. */
fun spacingFor(window: NovaWindow): NovaSpacing = when (window) {
    NovaWindow.Compact -> NovaSpacing(screen = 16.dp, gap = 8.dp)
    NovaWindow.Medium -> NovaSpacing(screen = 24.dp, gap = 12.dp)
    NovaWindow.Expanded -> NovaSpacing(screen = 32.dp, gap = 16.dp)
}

/** Geçerli pencere sınıfının boşlukları. */
@Composable
fun rememberNovaSpacing(): NovaSpacing = spacingFor(rememberNovaWindow())
