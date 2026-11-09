package dev.anon.novatv.core.sync

import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.source.SourceAdapterRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaylistRepositoryTest {

    private lateinit var db: dev.anon.novatv.core.database.NovaDatabase
    private lateinit var credentials: FakeCredentialStore
    private lateinit var repository: RoomPlaylistRepository

    @Before
    fun setUp() {
        db = testDatabase()
        credentials = FakeCredentialStore()
        repository = RoomPlaylistRepository(db, credentials, SourceAdapterRegistry(emptySet()))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun m3uCredentialsStayOutOfRoomButLoadReturnsFullUrl() = runTest {
        val full = "https://panel.example/get.php?username=ali&password=secret"
        val id = repository.add(
            Playlist(type = PlaylistType.M3U, name = "Liste", baseUrl = full),
            Credentials(null, null),
        )

        val stored = db.playlistDao().byId(id)!!
        assertEquals("https://panel.example/get.php", stored.baseUrl)
        assertEquals(full, credentials.sourceUrls[id])
        assertEquals(full, repository.load(id).first.baseUrl)

        val listed = repository.observeAll().first().single()
        assertEquals("https://panel.example/get.php", listed.baseUrl)
    }

    @Test
    fun updateReplacesStoredSourceUrl() = runTest {
        val id = repository.add(
            Playlist(type = PlaylistType.M3U, name = "Liste", baseUrl = "https://panel.example/a.m3u?token=1"),
            Credentials(null, null),
        )
        repository.update(
            Playlist(
                id = id,
                type = PlaylistType.M3U,
                name = "Liste",
                baseUrl = "https://panel.example/b.m3u?token=2",
            ),
        )

        assertEquals("https://panel.example/b.m3u?token=2", repository.load(id).first.baseUrl)
        assertEquals("https://panel.example/b.m3u", db.playlistDao().byId(id)!!.baseUrl)
    }

    @Test
    fun legacyPlaintextBaseUrlIsMovedToSecretOnLoad() = runTest {
        val full = "https://panel.example/get.php?username=ali&password=secret"
        val id = db.playlistDao().upsert(
            snapshotEntity(full),
        )

        assertEquals(full, repository.load(id).first.baseUrl)
        assertEquals("https://panel.example/get.php", db.playlistDao().byId(id)!!.baseUrl)
        assertEquals(full, credentials.sourceUrls[id])
    }

    @Test
    fun removingPlaylistDropsStoredSourceUrl() = runTest {
        val id = repository.add(
            Playlist(type = PlaylistType.M3U, name = "Liste", baseUrl = "https://panel.example/a.m3u?token=1"),
            Credentials(null, null),
        )
        repository.remove(id)

        assertNull(credentials.sourceUrls[id])
        assertNull(db.playlistDao().byId(id))
    }

    private fun snapshotEntity(baseUrl: String) = dev.anon.novatv.core.database.entity.PlaylistEntity(
        type = PlaylistType.M3U.name,
        name = "Eski",
        baseUrl = baseUrl,
        username = null,
        epgUrl = null,
        userAgent = null,
        isActive = true,
        sortOrder = 0,
        lastSyncAt = null,
        syncState = "NEVER",
        syncIntervalHours = 24,
    )
}
