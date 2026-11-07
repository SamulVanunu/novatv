package dev.anon.novatv.core.sync

import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.model.SyncState
import dev.anon.novatv.core.network.SourceAuthException
import dev.anon.novatv.core.source.SourceAdapterRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
class SyncEngineTest {

    private lateinit var db: NovaDatabase
    private lateinit var credentials: FakeCredentialStore
    private lateinit var fake: FakeAdapter
    private lateinit var engine: SyncEngine

    @Before
    fun setUp() {
        db = testDatabase()
        credentials = FakeCredentialStore()
        fake = FakeAdapter()
        engine = SyncEngine(
            db = db,
            playlists = RoomPlaylistRepository(db, credentials, SourceAdapterRegistry(setOf(FakeAdapterFactory(fake)))),
            registry = SourceAdapterRegistry(setOf(FakeAdapterFactory(fake))),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun successfulSyncWritesChannelsAndMarksOk() = runTest {
        seedPlaylist(db)
        credentials.put(1, demoCredentials())
        fake.channels = listOf(channel(remoteId = "10", name = "TRT 1"), channel(remoteId = "11", name = "TRT 2"))

        assertEquals(SyncState.OK, engine.sync(1))

        assertEquals(2, db.channelDao().countByPlaylist(1))
        assertEquals(SyncState.OK.name, db.playlistDao().byId(1)!!.syncState)
        assert(db.playlistDao().byId(1)!!.lastSyncAt != null)
    }

    @Test
    fun authFailureMarksFailedAndKeepsOldChannels() = runTest {
        seedPlaylist(db)
        credentials.put(1, demoCredentials())
        fake.channels = listOf(channel(remoteId = "10", name = "TRT 1"), channel(remoteId = "11", name = "TRT 2"))
        assertEquals(SyncState.OK, engine.sync(1))

        fake.authFailure = SourceAuthException("kimlik reddedildi")

        assertEquals(SyncState.FAILED, engine.sync(1))
        assertEquals(2, db.channelDao().countByPlaylist(1))
        assertEquals(SyncState.FAILED.name, db.playlistDao().byId(1)!!.syncState)
    }

    @Test
    fun twoChannelsWithSameTvgIdAreBothWritten() = runTest {
        seedPlaylist(db)
        credentials.put(1, demoCredentials())
        fake.channels = listOf(
            channel(remoteId = "20", name = "TRT 1 HD"),
            channel(remoteId = "21", name = "TRT 1 Yedek"),
        )

        assertEquals(SyncState.OK, engine.sync(1))
        assertEquals(2, db.channelDao().countByPlaylist(1))
    }

    @Test
    fun concurrentSyncCallsNeverOverlap() = runTest {
        seedPlaylist(db)
        credentials.put(1, demoCredentials())
        fake.channels = listOf(channel(remoteId = "10", name = "TRT 1"))
        fake.liveDelayMs = 100

        coroutineScope {
            val first = async { engine.sync(1) }
            val second = async { engine.sync(1) }
            first.await()
            second.await()
        }

        assertEquals(1, fake.maxConcurrent.get())
    }

    @Test
    fun emptyLiveMarksPartialAndKeepsOldChannels() = runTest {
        seedPlaylist(db)
        credentials.put(1, demoCredentials())
        fake.channels = listOf(channel(remoteId = "10", name = "TRT 1"))
        assertEquals(SyncState.OK, engine.sync(1))

        fake.channels = emptyList()

        assertEquals(SyncState.PARTIAL, engine.sync(1))
        assertEquals(1, db.channelDao().countByPlaylist(1))
        assertEquals(SyncState.PARTIAL.name, db.playlistDao().byId(1)!!.syncState)
    }

    @Test
    fun inactivePlaylistIsNotSynced() = runTest {
        seedPlaylist(db, active = false)
        credentials.put(1, demoCredentials())
        fake.channels = listOf(channel(remoteId = "10", name = "TRT 1"))

        assertEquals(SyncState.FAILED, engine.sync(1))
        assertEquals(0, fake.liveCalls.get())
        assertEquals(0, db.channelDao().countByPlaylist(1))
    }

    private fun demoCredentials() = dev.anon.novatv.core.model.Credentials("demo", "demo")
}
