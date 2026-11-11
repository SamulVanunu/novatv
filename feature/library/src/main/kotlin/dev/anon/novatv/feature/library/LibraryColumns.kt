package dev.anon.novatv.feature.library

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.dp
import dev.anon.novatv.ui.layout.NovaWindow

/** Geniş ekranda (TV, büyük tablet) kartların en dar genişliği; bugünkü TV düzeni. */
internal val LibraryPosterWidth = 150.dp

/**
 * Izgara kuralı türü; [libraryGridColumns] tek yerden `GridCells` üretir,
 * testler bu saf sınıflandırmayı doğrular.
 */
internal enum class LibraryGridKind { Fixed, Adaptive }

/** Pencere sınıfına göre sabit sütun sayısı; geniş sınıfta `null` (uyarlanabilir). */
internal fun libraryFixedColumnCount(window: NovaWindow): Int? = when (window) {
    NovaWindow.Compact -> 2
    NovaWindow.Medium -> 3
    NovaWindow.Expanded -> null
}

/**
 * Kütüphane ızgarasının sütun kuralı — filmler ve diziler aynı kuralı kullanır.
 *
 * Kompakt 2, orta 3 sabit sütun: telefon ve tablette kart sayısı ekranla
 * birlikte artmaz, kartlar genişliğe tam oturur. Geniş sınıf (TV ve büyük
 * tablet) bugünkü `Adaptive(150.dp)` ölçüsünü birebir korur; TV görünümü bu
 * görevde değişmez.
 */
internal fun libraryGridColumns(window: NovaWindow): GridCells {
    val fixed = libraryFixedColumnCount(window)
    return if (fixed != null) GridCells.Fixed(fixed) else GridCells.Adaptive(LibraryPosterWidth)
}

/** Kuralın türü: sabit mi uyarlanabilir mi. */
internal fun libraryGridKind(window: NovaWindow): LibraryGridKind =
    if (libraryFixedColumnCount(window) != null) LibraryGridKind.Fixed else LibraryGridKind.Adaptive
