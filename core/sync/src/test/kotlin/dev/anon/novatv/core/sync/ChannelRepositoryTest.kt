package dev.anon.novatv.core.sync

import androidx.paging.testing.asSnapshot
import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.database.entity.HiddenChannelEntity
import dev.anon.novatv.core.database.entity.HiddenGroupEntity
import dev.anon.novatv.core.model.SearchText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChannelRepositoryTest {

    private lateinit var db: NovaDatabase
    private lateinit var repository: RoomChannelRepository

    @Before
    fun setUp() {
        db = testDatabase()
        repository = RoomChannelRepository(db, testPreferenceStore())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun hiddenChannelAndGroupAreExcluded() = runTest {
        seedPlaylist(db)
        db.channelDao().replaceForPlaylist(
            1, 1,
            listOf(
                channelEntity("10", "TRT 1", "Ulusal"),
                channelEntity("11", "TRT 2", "Ulusal"),
                channelEntity("12", "beIN Sports", "Spor"),
            ),
        )
        db.userDataDao().hideChannel(HiddenChannelEntity(profileId = 1, channelId = 2))
        db.userDataDao().hideGroup(HiddenGroupEntity(profileId = 1, playlistId = 1, groupName = "Spor"))

        val names = repository.channels(ChannelFilter()).asSnapshot().map { it.name }

        assertEquals(listOf("TRT 1"), names)
    }

    @Test
    fun favoriteToggleIsPersistent() = runTest {
        seedPlaylist(db)
        db.channelDao().replaceForPlaylist(1, 1, listOf(channelEntity("10", "TRT 1", "Ulusal")))
        val channel = repository.channels(ChannelFilter()).asSnapshot().single()

        repository.toggleFavorite(channel)
        assertEquals(listOf("TRT 1"), repository.channels(ChannelFilter(favoritesOnly = true)).asSnapshot().map { it.name })

        repository.toggleFavorite(channel)
        assertTrue(repository.channels(ChannelFilter(favoritesOnly = true)).asSnapshot().isEmpty())
    }

    @Test
    fun setNumberChangesOrder() = runTest {
        seedPlaylist(db)
        db.channelDao().replaceForPlaylist(
            1, 1,
            listOf(channelEntity("10", "AAA", "Ulusal"), channelEntity("11", "BBB", "Ulusal")),
        )
        val bbb = repository.channels(ChannelFilter()).asSnapshot().first { it.name == "BBB" }

        repository.setNumber(bbb, 1)

        assertEquals(listOf("BBB", "AAA"), repository.channels(ChannelFilter()).asSnapshot().map { it.name })
    }

    @Test
    fun groupsIncludeAllWithCounts() = runTest {
        seedPlaylist(db)
        db.channelDao().replaceForPlaylist(
            1, 1,
            listOf(
                channelEntity("10", "TRT 1", "Ulusal"),
                channelEntity("11", "TRT 2", "Ulusal"),
                channelEntity("12", "beIN Sports", "Spor"),
            ),
        )

        val groups = repository.groups().first()

        assertEquals(GroupItem("", "Tümü", 3), groups[0])
        assertEquals(listOf("Spor", "Ulusal"), groups.drop(1).map { it.label })
        assertEquals(listOf(1, 2), groups.drop(1).map { it.count })
    }

    @Test
    fun inactivePlaylistChannelsAreNotListed() = runTest {
        seedPlaylist(db, id = 1, active = true)
        seedPlaylist(db, id = 2, active = false)
        db.channelDao().replaceForPlaylist(1, 1, listOf(channelEntity("10", "Aktif Kanal", "Ulusal")))
        db.channelDao().replaceForPlaylist(2, 1, listOf(channelEntity("20", "Pasif Kanal", "Ulusal", playlistId = 2)))

        val names = repository.channels(ChannelFilter()).asSnapshot().map { it.name }

        assertEquals(listOf("Aktif Kanal"), names)
        assertEquals(1, repository.groups().first()[0].count)
    }

    @Test
    fun lastChannelComesFromWatchHistory() = runTest {
        seedPlaylist(db)
        db.channelDao().replaceForPlaylist(1, 1, listOf(channelEntity("10", "TRT 1", "Ulusal")))
        val channel = repository.channels(ChannelFilter()).asSnapshot().single()

        repository.recordWatch(channel)

        assertEquals("TRT 1", repository.lastChannel()?.name)
    }

    private fun channelEntity(
        remoteId: String,
        name: String,
        group: String,
        playlistId: Long = 1,
    ) = dev.anon.novatv.core.database.entity.ChannelEntity(
        playlistId = playlistId,
        remoteId = remoteId,
        number = null,
        name = name,
        nameNormalized = SearchText.normalize(name),
        groupName = group,
        logoUrl = null,
        tvgId = null,
        hasArchive = false,
        archiveDays = 0,
        streamFormat = "AUTO",
        syncToken = 1,
    )
}
