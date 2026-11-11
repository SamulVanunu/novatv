package dev.anon.novatv.feature.player

import dev.anon.novatv.core.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelZappingTest {

    private val channels = listOf(
        channel(1, "TRT 1", 1),
        channel(2, "TRT 2", 2),
        channel(3, "TRT Spor", 3),
        channel(4, "TRT Haber", 4),
    )

    @Test
    fun stepsForward() {
        assertEquals(3L, channelStep(channels, currentId = 2, delta = 1)?.id)
    }

    @Test
    fun stepsBackward() {
        assertEquals(1L, channelStep(channels, currentId = 2, delta = -1)?.id)
    }

    @Test
    fun wrapsFromLastToFirst() {
        assertEquals(1L, channelStep(channels, currentId = 4, delta = 1)?.id)
    }

    @Test
    fun wrapsFromFirstToLast() {
        assertEquals(4L, channelStep(channels, currentId = 1, delta = -1)?.id)
    }

    @Test
    fun wrapsWithLargeDelta() {
        assertEquals(2L, channelStep(channels, currentId = 4, delta = 6)?.id)
        assertEquals(4L, channelStep(channels, currentId = 2, delta = -6)?.id)
    }

    @Test
    fun returnsNullForSingleChannel() {
        assertNull(channelStep(listOf(channel(1, "Tek", 1)), currentId = 1, delta = 1))
    }

    @Test
    fun returnsNullWhenCurrentIsMissing() {
        assertNull(channelStep(channels, currentId = 99, delta = 1))
    }

    @Test
    fun returnsNullForEmptyListOrZeroDelta() {
        assertNull(channelStep(emptyList(), currentId = 1, delta = 1))
        assertNull(channelStep(channels, currentId = 2, delta = 0))
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
