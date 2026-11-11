package dev.anon.novatv.feature.library

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.dp
import dev.anon.novatv.ui.layout.NovaWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryColumnsTest {

    /** `GridCells.Fixed` veri sınıfıdır: eşitlik sütun sayısını da karşılaştırır. */
    @Test
    fun `kompakt iki sabit sutun`() {
        assertEquals(GridCells.Fixed(2), libraryGridColumns(NovaWindow.Compact))
        assertEquals(2, libraryFixedColumnCount(NovaWindow.Compact))
    }

    @Test
    fun `orta uc sabit sutun`() {
        assertEquals(GridCells.Fixed(3), libraryGridColumns(NovaWindow.Medium))
        assertEquals(3, libraryFixedColumnCount(NovaWindow.Medium))
    }

    /** TV ve büyük tablet: bugünkü `Adaptive(150.dp)` birebir korunur. */
    @Test
    fun `genis uyarlanabilir 150 dp`() {
        assertEquals(GridCells.Adaptive(150.dp), libraryGridColumns(NovaWindow.Expanded))
        assertEquals(GridCells.Adaptive(LibraryPosterWidth), libraryGridColumns(NovaWindow.Expanded))
        assertNull(libraryFixedColumnCount(NovaWindow.Expanded))
    }

    @Test
    fun `kural turu pencere sinifina gore`() {
        assertEquals(LibraryGridKind.Fixed, libraryGridKind(NovaWindow.Compact))
        assertEquals(LibraryGridKind.Fixed, libraryGridKind(NovaWindow.Medium))
        assertEquals(LibraryGridKind.Adaptive, libraryGridKind(NovaWindow.Expanded))
    }

    /** 150 dp altına inen bir uyarlanabilir ölçü TV düzenini değiştirirdi. */
    @Test
    fun `genis olcu 150 dp`() {
        assertTrue(LibraryPosterWidth == 150.dp)
    }
}
