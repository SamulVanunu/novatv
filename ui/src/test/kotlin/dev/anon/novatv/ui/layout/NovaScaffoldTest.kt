package dev.anon.novatv.ui.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class NovaScaffoldTest {

    @Test
    fun `expanded is rail`() {
        assertEquals(NovaNavMode.Rail, navigationMode(NovaWindow.Expanded))
    }

    @Test
    fun `compact is bottom`() {
        assertEquals(NovaNavMode.Bottom, navigationMode(NovaWindow.Compact))
    }

    @Test
    fun `medium is bottom`() {
        assertEquals(NovaNavMode.Bottom, navigationMode(NovaWindow.Medium))
    }

    @Test
    fun `tab keeps its key label and test tag`() {
        val tab = NovaTab(key = "playlists", label = "Oynatma Listeleri", testTag = "open-playlists")
        assertEquals("playlists", tab.key)
        assertEquals("Oynatma Listeleri", tab.label)
        assertEquals("open-playlists", tab.testTag)
        assertEquals(tab, tab.copy())
    }
}
