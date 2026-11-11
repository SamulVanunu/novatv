package dev.anon.novatv.feature.live

import dev.anon.novatv.ui.layout.NovaWindow

/**
 * Canlı ızgarasının sütun sayısı: kompakt 2, orta 3, geniş 3.
 *
 * Geniş değer ekranın bugünkü TV düzenidir ([TV_GRID_COLUMNS]); TV yolu bu
 * sabiti doğrudan kullanır, kompakt yol bu işlevden geçer.
 */
fun liveGridColumns(window: NovaWindow): Int = when (window) {
    NovaWindow.Compact -> 2
    NovaWindow.Medium -> 3
    NovaWindow.Expanded -> TV_GRID_COLUMNS
}

/** TV ızgarasının sütun sayısı; bugünkü görünüm korunur. */
const val TV_GRID_COLUMNS = 3
