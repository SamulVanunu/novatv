package dev.anon.novatv.feature.player

import dev.anon.novatv.core.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Dokunmatik kanal listesinin seçim kararı.
 *
 * `selectChannel` oynatıcı denetleyicisine (final, `Context` ister) bağlı olduğu
 * için "prepare çağrılmadı" doğrudan doğrulanamıyor; onun yerine kararı veren
 * saf fonksiyon test edilir: null dönmesi "yeniden yükleme yok" demektir.
 */
class ChannelOrderTest {

    private val order = listOf(
        channel(1, "TRT 1", 1),
        channel(2, "TRT 2", 2),
        channel(3, "TRT Spor", 3),
    )

    @Test
    fun sameChannelIsNoOp() {
        assertNull(channelSwitchTarget(order, currentId = 2, targetId = 2))
    }

    @Test
    fun presentChannelIsReturned() {
        val target = channelSwitchTarget(order, currentId = 1, targetId = 3)
        assertEquals(3L, target?.id)
        assertEquals("TRT Spor", target?.name)
    }

    @Test
    fun missingChannelReturnsNull() {
        assertNull(channelSwitchTarget(order, currentId = 1, targetId = 99))
    }

    @Test
    fun nullCurrentSelectsAnyListedChannel() {
        // Oynatıcı henüz kanal tutmuyorsa (açılış) listedeki her kanal hedeftir.
        assertEquals(1L, channelSwitchTarget(order, currentId = null, targetId = 1)?.id)
        assertNull(channelSwitchTarget(order, currentId = null, targetId = 0))
    }

    @Test
    fun emptyOrderReturnsNull() {
        assertNull(channelSwitchTarget(emptyList(), currentId = null, targetId = 1))
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
