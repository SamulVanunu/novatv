package dev.anon.novatv.core.sync

import dev.anon.novatv.core.database.entity.ChannelEntity
import dev.anon.novatv.core.model.SearchText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgChannelIndexTest {

    @Test
    fun channelWithoutTvgIdMatchesByDisplayName() {
        // Gerçek M3U listesinde tvg-id boş; XMLTV kanalı yalnızca adıyla gelir.
        val index = EpgChannelIndex(listOf(channel(id = 42, name = "TRT 1")))

        assertFalse(index.isEmpty)
        assertEquals(42L, index.byName("TRT 1"))
        assertNull(index.byTvgId("trt1"))
    }

    @Test
    fun spacedAndCompactNamesMatchEachOther() {
        val index = EpgChannelIndex(listOf(channel(id = 7, name = "TRT 1")))

        assertEquals(7L, index.byName("TRT1"))
        assertEquals(7L, index.byName("trt 1"))
    }

    @Test
    fun turkishCharactersAreNormalized() {
        val index = EpgChannelIndex(listOf(channel(id = 9, name = "Şeker TV")))

        assertEquals(9L, index.byName("seker tv"))
        assertEquals(9L, index.byName("ŞEKER TV"))
    }

    @Test
    fun tvgIdWinsOverName() {
        val index = EpgChannelIndex(
            listOf(
                channel(id = 1, name = "TRT 1", tvgId = "trt1"),
                channel(id = 2, name = "TRT 1 Yedek"),
            ),
        )

        assertEquals(1L, index.byTvgId("trt1"))
        assertEquals(1L, index.byName("TRT 1"))
        assertEquals(2L, index.byName("TRT 1 Yedek"))
    }

    @Test
    fun emptyChannelsLeaveIndexEmpty() {
        val index = EpgChannelIndex(emptyList())

        assertTrue(index.isEmpty)
        assertNull(index.byName("TRT 1"))
    }

    private fun channel(id: Long, name: String, tvgId: String? = null) = ChannelEntity(
        id = id,
        playlistId = 1,
        remoteId = name,
        number = null,
        name = name,
        nameNormalized = SearchText.normalize(name),
        groupName = "Ulusal",
        logoUrl = null,
        tvgId = tvgId,
        hasArchive = false,
        archiveDays = 0,
        streamFormat = "AUTO",
        syncToken = 1,
    )
}
