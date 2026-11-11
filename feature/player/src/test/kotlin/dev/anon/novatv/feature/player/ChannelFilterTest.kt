package dev.anon.novatv.feature.player

import dev.anon.novatv.core.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Dokunmatik kanal listesinin süzgeci ve saat biçimi. */
class ChannelFilterTest {

    private val order = listOf(
        channel(1, "TRT 1", 1),
        channel(2, "TRT 2", 2),
        channel(3, "TRT Spor", 3),
        channel(4, "Show TV", 4),
        channel(5, "Habertürk", 5),
    )

    @Test
    fun blankQueryKeepsOrder() {
        assertEquals(order, filterChannels(order, ""))
        assertEquals(order, filterChannels(order, "   "))
    }

    @Test
    fun matchesNameCaseInsensitively() {
        assertEquals(listOf(4L), filterChannels(order, "show").map { it.id })
        assertEquals(listOf(5L), filterChannels(order, "HABER").map { it.id })
    }

    @Test
    fun matchesNumber() {
        assertEquals(listOf(3L), filterChannels(order, "3").map { it.id })
    }

    @Test
    fun keepsOrderForSharedPrefix() {
        assertEquals(listOf(1L, 2L, 3L), filterChannels(order, "trt").map { it.id })
    }

    @Test
    fun noMatchIsEmpty() {
        assertTrue(filterChannels(order, "xyz").isEmpty())
    }

    @Test
    fun clockFormat() {
        assertEquals("0:00", formatClock(0))
        assertEquals("0:09", formatClock(9_400))
        assertEquals("1:05", formatClock(65_000))
        assertEquals("1:00:00", formatClock(3_600_000))
        assertEquals("0:00", formatClock(-1_000))
    }

    private fun channel(id: Long, name: String, number: Int) = Channel(
        id = id,
        playlistId = 1,
        remoteId = id.toString(),
        number = number,
        name = name,
        groupName = "Ulusal",
    )
}
