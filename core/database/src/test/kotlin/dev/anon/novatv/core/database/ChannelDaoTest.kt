package dev.anon.novatv.core.database

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.anon.novatv.core.database.entity.ChannelEntity
import dev.anon.novatv.core.database.entity.HiddenChannelEntity
import dev.anon.novatv.core.database.entity.HiddenGroupEntity
import dev.anon.novatv.core.database.entity.PlaylistEntity
import dev.anon.novatv.core.model.SearchText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChannelDaoTest {

    private lateinit var db: NovaDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NovaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun pagingFiltersByGroupAndTurkishQuery() = runTest {
        seedPlaylist()
        db.channelDao().replaceForPlaylist(
            playlistId = 1,
            token = 1,
            items = listOf(
                channel(1, "TRT 1", "Ulusal"),
                channel(2, "İZ TV", "Ulusal"),
                channel(3, "beIN Sports", "Spor"),
            ),
        )

        val page = db.channelDao()
            .paging(listOf(1), "Ulusal", SearchText.normalize("iz"), false, 1)
            .load(PagingSource.LoadParams.Refresh(key = 0, loadSize = 50, placeholdersEnabled = false))

        val names = (page as PagingSource.LoadResult.Page).data.map { it.name }
        assertEquals(listOf("İZ TV"), names)
    }

    @Test
    fun hiddenChannelAndGroupAreExcluded() = runTest {
        seedPlaylist()
        db.channelDao().replaceForPlaylist(
            playlistId = 1,
            token = 1,
            items = listOf(
                channel(1, "TRT 1", "Ulusal"),
                channel(2, "TRT 2", "Ulusal"),
                channel(3, "beIN Sports", "Spor"),
            ),
        )
        db.userDataDao().hideChannel(HiddenChannelEntity(profileId = 1, channelId = 2))
        db.userDataDao().hideGroup(HiddenGroupEntity(profileId = 1, playlistId = 1, groupName = "Spor"))

        val page = db.channelDao()
            .paging(listOf(1), null, "", false, 1)
            .load(PagingSource.LoadParams.Refresh(key = 0, loadSize = 50, placeholdersEnabled = false))

        val names = (page as PagingSource.LoadResult.Page).data.map { it.name }
        assertEquals(listOf("TRT 1"), names)
    }

    @Test
    fun groupsAreDistinctAndSorted() = runTest {
        seedPlaylist()
        db.channelDao().replaceForPlaylist(
            playlistId = 1,
            token = 1,
            items = listOf(
                channel(1, "TRT 1", "Ulusal"),
                channel(2, "TRT 2", "Ulusal"),
                channel(3, "beIN Sports", "Spor"),
            ),
        )
        assertEquals(listOf("Spor", "Ulusal"), db.channelDao().observeGroups(listOf(1)).first())
    }

    @Test
    fun resyncKeepsStableIdsAndRemovesStaleChannels() = runTest {
        seedPlaylist()
        db.channelDao().replaceForPlaylist(
            playlistId = 1,
            token = 1,
            items = listOf(channel(0, "TRT 1", "Ulusal"), channel(0, "TRT 2", "Ulusal")),
        )
        val before = db.channelDao().paging(listOf(1), null, "", false, 1)
            .load(PagingSource.LoadParams.Refresh(key = 0, loadSize = 50, placeholdersEnabled = false))
        val trt1Id = (before as PagingSource.LoadResult.Page).data.first { it.name == "TRT 1" }.id

        db.channelDao().replaceForPlaylist(
            playlistId = 1,
            token = 2,
            items = listOf(channel(0, "TRT 1", "Ulusal")),
        )

        val after = db.channelDao().paging(listOf(1), null, "", false, 1)
            .load(PagingSource.LoadParams.Refresh(key = 0, loadSize = 50, placeholdersEnabled = false))
        val data = (after as PagingSource.LoadResult.Page).data
        assertEquals(listOf("TRT 1"), data.map { it.name })
        assertEquals(trt1Id, data.first().id)
    }

    private suspend fun seedPlaylist() {
        db.playlistDao().upsert(
            PlaylistEntity(
                id = 1,
                type = "XTREAM",
                name = "Test",
                baseUrl = "http://localhost",
                username = "demo",
                epgUrl = null,
                userAgent = null,
                isActive = true,
                sortOrder = 0,
                lastSyncAt = null,
                syncState = "NEVER",
                syncIntervalHours = 24,
            ),
        )
    }

    private fun channel(id: Long, name: String, group: String) = ChannelEntity(
        id = id,
        playlistId = 1,
        remoteId = "remote-$name",
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
