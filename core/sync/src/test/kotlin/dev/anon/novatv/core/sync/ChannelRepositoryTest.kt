package dev.anon.novatv.core.sync

import androidx.paging.PagingSource
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
    fun recentGroupListsWatchedChannelsNewestFirstAndSkipsHidden() = runTest {
        seedPlaylist(db)
        db.channelDao().replaceForPlaylist(
            1, 1,
            listOf(
                channelEntity("10", "TRT 1", "Ulusal"),
                channelEntity("12", "beIN Sports", "Spor"),
            ),
        )
        val all = repository.channels(ChannelFilter()).asSnapshot()
        val trt1 = all.first { it.name == "TRT 1" }
        val beIn = all.first { it.name == "beIN Sports" }

        repository.recordWatch(trt1)
        repository.recordWatch(beIn)

        val groups = repository.groups().first()
        assertEquals(GroupItem(RECENT_GROUP_KEY, "Son izlenenler", 2), groups[1])
        assertEquals(
            listOf("beIN Sports", "TRT 1"),
            repository.channels(ChannelFilter(group = RECENT_GROUP_KEY)).asSnapshot().map { it.name },
        )

        repository.hideChannel(beIn)
        assertEquals(
            listOf("TRT 1"),
            repository.channels(ChannelFilter(group = RECENT_GROUP_KEY)).asSnapshot().map { it.name },
        )
        assertEquals(1, repository.groups().first()[1].count)
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

    @Test
    fun twentyThousandChannelsFirstPageIsUnder300ms() = runTest {
        seedPlaylist(db)
        val items = (1..20_000).map { index ->
            channelEntity(
                remoteId = "r$index",
                name = if (index % 10 == 0) "TRT $index" else "Kanal $index",
                group = "G${index % 50}",
            )
        }
        db.channelDao().replaceForPlaylist(1, 1, items)
        assertEquals(20_000, db.channelDao().countByPlaylist(1))

        suspend fun loadPage() = db.channelDao()
            .paging(
                playlistIds = listOf(1L),
                group = null,
                q = SearchText.normalize("trt"),
                favoritesOnly = false,
                profileId = 1L,
            )
            .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 60, placeholdersEnabled = false))

        check(loadPage() is PagingSource.LoadResult.Page) { "ısınma turu başarısız" }

        val startedAt = System.nanoTime()
        val page = loadPage() as PagingSource.LoadResult.Page
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

        assertEquals(60, page.data.size)
        assertTrue("ilk sayfa ${elapsedMs}ms sürdü", elapsedMs < 300)
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
