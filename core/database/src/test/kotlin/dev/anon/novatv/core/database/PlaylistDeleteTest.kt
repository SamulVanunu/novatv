package dev.anon.novatv.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.anon.novatv.core.database.entity.ChannelEntity
import dev.anon.novatv.core.database.entity.EpgChannelEntity
import dev.anon.novatv.core.database.entity.EpgProgramEntity
import dev.anon.novatv.core.database.entity.PlaylistEntity
import dev.anon.novatv.core.model.SearchText
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
class PlaylistDeleteTest {

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
    fun deletingPlaylistRemovesChannelsAndEpg() = runTest {
        db.playlistDao().upsert(
            PlaylistEntity(
                id = 7, type = "M3U", name = "M3U", baseUrl = "http://x",
                username = null, epgUrl = null, userAgent = null, isActive = true,
                sortOrder = 0, lastSyncAt = null, syncState = "NEVER", syncIntervalHours = 24,
            ),
        )
        db.channelDao().replaceForPlaylist(
            7, 1,
            listOf(
                ChannelEntity(
                    playlistId = 7, remoteId = "r1", number = null, name = "TRT 1",
                    nameNormalized = SearchText.normalize("TRT 1"), groupName = "Ulusal",
                    logoUrl = null, tvgId = null, hasArchive = false, archiveDays = 0,
                    streamFormat = "AUTO", syncToken = 1,
                ),
            ),
        )
        val epgChannelId = db.epgDao().upsertChannel(
            EpgChannelEntity(playlistId = 7, tvgId = "trt1", displayName = "TRT 1", iconUrl = null),
        )
        db.epgDao().upsertPrograms(
            listOf(
                EpgProgramEntity(
                    epgChannelId = epgChannelId, channelId = null, startAt = 0, endAt = 1000,
                    title = "Haber", subtitle = null, description = null, category = null,
                    rating = null, episodeNum = null, isNew = false,
                ),
            ),
        )

        db.playlistDao().delete(7)

        assertEquals(0, db.channelDao().countByPlaylist(7))
        assertEquals(0, db.epgDao().countChannels(7))
        assertEquals(0, db.epgDao().countPrograms())
    }
}
