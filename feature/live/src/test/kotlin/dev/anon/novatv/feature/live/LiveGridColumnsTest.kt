package dev.anon.novatv.feature.live

import dev.anon.novatv.ui.layout.NovaWindow
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveGridColumnsTest {

    @Test
    fun `kompakt iki sutun`() {
        assertEquals(2, liveGridColumns(NovaWindow.Compact))
    }

    @Test
    fun `orta uc sutun`() {
        assertEquals(3, liveGridColumns(NovaWindow.Medium))
    }

    @Test
    fun `genis TV sutununu korur`() {
        assertEquals(3, liveGridColumns(NovaWindow.Expanded))
        assertEquals(TV_GRID_COLUMNS, liveGridColumns(NovaWindow.Expanded))
    }
}
