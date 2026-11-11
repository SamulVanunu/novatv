package dev.anon.novatv.feature.multiwall

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.ui.layout.NovaWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MultiWallStateTest {

    private fun channel(id: Long, name: String, group: String = "Haber") = Channel(
        id = id,
        playlistId = 1,
        remoteId = "r$id",
        name = name,
        groupName = group,
    )

    @Test
    fun visiblePanesFollowLayout() {
        assertEquals(2, MultiWallUiState(layout = 2).visiblePanes)
        assertEquals(4, MultiWallUiState(layout = 4).visiblePanes)
        assertEquals(4, MultiWallUiState(layout = 3).visiblePanes)
    }

    @Test
    fun pickerFiltersByNameAndGroupIgnoringTurkishCase() {
        val state = MultiWallUiState(
            channels = listOf(
                channel(1, "TRT 1", group = "Genel"),
                channel(2, "Show TV", group = "Eğlence"),
                channel(3, "Haber Türk"),
            ),
            query = "haber",
        )
        assertEquals(listOf(3L), state.pickerChannels.map { it.id })

        val turkish = state.copy(query = "EGLENCE")
        assertEquals(listOf(2L), turkish.pickerChannels.map { it.id })
    }

    @Test
    fun blankQueryReturnsAllChannels() {
        val channels = listOf(channel(1, "A"), channel(2, "B"))
        assertEquals(channels, MultiWallUiState(channels = channels, query = "   ").pickerChannels)
    }

    @Test
    fun streamLookupIsPerPane() {
        val stream = PaneStream(pane = 1, channelId = 2, title = "B", url = "https://x/2")
        val state = MultiWallUiState(streams = listOf(stream))
        assertEquals(stream, state.streamOf(1))
        assertNull(state.streamOf(0))
    }

    @Test
    fun compactDrawsAtMostTwoPanes() {
        assertEquals(2, paneLimitFor(NovaWindow.Compact, 4))
        assertEquals(2, paneLimitFor(NovaWindow.Medium, 4))
        assertEquals(2, paneLimitFor(NovaWindow.Compact, 2))
        assertEquals(4, paneLimitFor(NovaWindow.Expanded, 4))
        assertEquals(2, paneLimitFor(NovaWindow.Expanded, 2))
    }
}
