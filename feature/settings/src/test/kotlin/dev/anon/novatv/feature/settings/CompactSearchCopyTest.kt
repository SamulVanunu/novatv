package dev.anon.novatv.feature.settings

import dev.anon.novatv.core.sync.SearchHit
import dev.anon.novatv.core.sync.SearchKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Kompakt arama ekranının boş durum metinleri; TV metinlerinden ayrışır. */
class CompactSearchCopyTest {

    private fun hit(title: String, kind: SearchKind = SearchKind.CHANNEL) = SearchHit(
        kind = kind,
        id = title.hashCode().toLong(),
        title = title,
        subtitle = null,
        startsAt = null,
    )

    @Test
    fun idleAndEmptyCopyDiffer() {
        assertNotEquals(compactSearchIdle().title, compactSearchEmpty("kanal").title)
    }

    @Test
    fun emptyCopyNamesTheQuery() {
        val text = compactSearchEmpty("belgesel")
        assertEquals("Sonuç yok", text.title)
        assertEquals(true, text.body.contains("belgesel"))
    }

    @Test
    fun idleCopyOnlyWhenQueryIsBlank() {
        val blank = SearchUiState(query = "   ", hits = emptyList())
        assertEquals("Aramaya hazır", compactEmptyCopy(blank).title)
    }

    @Test
    fun emptyCopyWhenQueryHasNoHits() {
        val state = SearchUiState(query = "yok", hits = emptyList())
        assertEquals("Sonuç yok", compactEmptyCopy(state).title)
    }

    @Test
    fun filterSelectionNarrowsVisibleHits() {
        val state = SearchUiState(
            query = "a",
            filter = SearchKind.MOVIE,
            hits = listOf(hit("Film", SearchKind.MOVIE), hit("Kanal")),
        )
        assertEquals(listOf(SearchKind.MOVIE), state.visible.map { it.kind })
    }
}
