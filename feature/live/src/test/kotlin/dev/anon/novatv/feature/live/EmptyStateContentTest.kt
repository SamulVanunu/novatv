package dev.anon.novatv.feature.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EmptyStateContentTest {

    @Test
    fun `kaynak yoksa kaynak ekle`() {
        val content = emptyStateContent(noSource = true, favoritesOnly = false, query = "")
        assertEquals("Henüz kanal yok", content.title)
        assertEquals(EmptyStateAction.OpenPlaylists, content.action)
    }

    @Test
    fun `bos olmayan sorgu kaynak yoklugundan once gelir`() {
        val content = emptyStateContent(noSource = true, favoritesOnly = false, query = "haber")
        assertEquals("«haber» için kanal yok", content.title)
        assertEquals(EmptyStateAction.ClearQuery, content.action)
    }

    @Test
    fun `favori suzgeci kaynak pilini gostermez`() {
        val content = emptyStateContent(noSource = false, favoritesOnly = true, query = "")
        assertEquals("Favori kanal yok", content.title)
        assertEquals(EmptyStateAction.None, content.action)
    }

    @Test
    fun `bos grup kendi metnini alir`() {
        val content = emptyStateContent(noSource = false, favoritesOnly = false, query = "")
        assertEquals("Bu grupta kanal yok", content.title)
        assertEquals(EmptyStateAction.OpenPlaylists, content.action)
    }

    @Test
    fun `sorgu yalniz bosluktan ibaretse arama sayilmaz`() {
        val content = emptyStateContent(noSource = false, favoritesOnly = false, query = "   ")
        assertEquals("Bu grupta kanal yok", content.title)
        assertFalse(content.action == EmptyStateAction.ClearQuery)
    }
}
