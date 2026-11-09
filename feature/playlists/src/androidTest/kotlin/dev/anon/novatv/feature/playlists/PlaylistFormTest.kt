package dev.anon.novatv.feature.playlists

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.database.entity.ChannelEntity
import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.SearchText
import dev.anon.novatv.core.network.NovaHttp
import dev.anon.novatv.core.network.SourceHttp
import dev.anon.novatv.core.settings.CredentialStore
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.source.SourceAdapterRegistry
import dev.anon.novatv.core.source.m3u.M3uFactory
import dev.anon.novatv.core.source.stalker.StalkerFactory
import dev.anon.novatv.core.source.xstream.XtreamFactory
import dev.anon.novatv.core.sync.ChannelFilter
import dev.anon.novatv.core.sync.ChannelRepository
import dev.anon.novatv.core.sync.PlaylistRepository
import dev.anon.novatv.core.sync.RoomChannelRepository
import dev.anon.novatv.core.sync.RoomPlaylistRepository
import dev.anon.novatv.core.sync.SyncEngine
import dev.anon.novatv.ui.theme.NovaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PlaylistFormTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var db: NovaDatabase
    private var server: MockWebServer? = null
    private lateinit var credentials: MapCredentialStore
    private lateinit var playlists: PlaylistRepository
    private lateinit var channels: ChannelRepository
    private lateinit var engine: SyncEngine
    private lateinit var registry: SourceAdapterRegistry

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, NovaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        credentials = MapCredentialStore()
        registry = SourceAdapterRegistry(setOf(XtreamFactory(), M3uFactory(), StalkerFactory()))
        playlists = RoomPlaylistRepository(db, credentials, registry)
        channels = RoomChannelRepository(
            db,
            PreferenceStore(
                PreferenceDataStoreFactory.create(
                    produceFile = { File(context.cacheDir, "prefs-${System.nanoTime()}.preferences_pb") },
                ),
            ),
        )
        engine = SyncEngine(db, playlists, registry)
    }

    @After
    fun tearDown() {
        runCatching { db.close() }
        server?.shutdown()
    }

    private fun startServer(): MockWebServer =
        MockWebServer().apply { start() }.also { server = it }

    @Test
    fun saveStaysDisabledUntilTypeFieldsAreValid() {
        val form = PlaylistFormViewModel(playlists, testConnection = { _, _ -> error("çağrılmamalı") })
        rule.setContent {
            NovaTheme { PlaylistFormHost(form) }
        }

        rule.onNodeWithTag("save-source").assertIsNotEnabled()

        rule.onNodeWithTag("type-M3U").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag("field-name").performTextInput("Ev Listesi")
        rule.onNodeWithTag("field-url").performTextInput("http://example.com/list.m3u")

        rule.onNodeWithTag("save-source").assertIsEnabled()
    }

    @Test
    fun validXtreamSourceIsTestedSavedAndListed() {
        val server = startServer()
        server.enqueue(
            MockResponse().setBody(
                """{"user_info":{"status":"Active","exp_date":"1893456000","max_connections":"2"}}""",
            ),
        )
        val baseUrl = server.url("/").toString().trimEnd('/')
        val listModel = PlaylistsViewModel(playlists, engine)
        val form = PlaylistFormViewModel(
            repository = playlists,
            testConnection = { playlist, creds ->
                registry.adapterFor(playlist, creds).authenticate()
            },
        )

        rule.setContent {
            NovaTheme {
                val formState by form.state.collectAsState()
                val listState by listModel.state.collectAsState()
                var showList by remember { mutableStateOf(false) }
                LaunchedEffect(formState.savedId) { if (formState.savedId != null) showList = true }

                if (showList) {
                    PlaylistsScreen(
                        state = listState,
                        onAdd = {}, onEdit = {}, onRefresh = {}, onToggleActive = {},
                        onMoveUp = {}, onMoveDown = {}, onDelete = {},
                    )
                } else {
                    PlaylistFormHost(form)
                }
            }
        }

        rule.onNodeWithTag("field-name").performTextInput("Xtream Demo")
        rule.onNodeWithTag("field-url").performTextInput(baseUrl)
        rule.onNodeWithTag("field-username").performTextInput("demo")
        rule.onNodeWithTag("field-password").performTextInput("demo")

        rule.onNodeWithTag("test-connection").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                rule.onNodeWithTag("connection-status").assertTextEquals("Active")
                true
            }.getOrDefault(false)
        }

        rule.onNodeWithTag("save-source").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { playlists.observeAll().first() }.isNotEmpty()
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Xtream Demo").fetchSemanticsNodes().isNotEmpty()
        }

        val saved = runBlocking { playlists.observeAll().first().single() }
        assertEquals(PlaylistType.XTREAM, saved.type)
        assertEquals("demo", credentials.values[saved.id]?.username)
        assertNotNull(credentials.values[saved.id]?.password)
        assertNull("kimlik Room'a düz metin yazılmamalı", saved.username)
    }

    @Test
    fun passivePlaylistDropsOutOfChannelsAndOrderCanChange() = runBlocking {
        val first = playlists.add(
            Playlist(type = PlaylistType.M3U, name = "Birinci", baseUrl = "http://a", sortOrder = 0),
            Credentials(null, null),
        )
        val second = playlists.add(
            Playlist(type = PlaylistType.M3U, name = "İkinci", baseUrl = "http://b", sortOrder = 1),
            Credentials(null, null),
        )
        db.channelDao().replaceForPlaylist(first, 1, listOf(channelEntity(first, "1", "Birinci Kanal")))
        db.channelDao().replaceForPlaylist(second, 1, listOf(channelEntity(second, "2", "İkinci Kanal")))

        assertEquals(2, channels.channels(ChannelFilter()).asSnapshot().size)

        val firstPlaylist = playlists.load(first).first
        playlists.update(firstPlaylist.copy(isActive = false))

        assertEquals(listOf("İkinci Kanal"), channels.channels(ChannelFilter()).asSnapshot().map { it.name })

        val model = PlaylistsViewModel(playlists, engine)
        rule.waitUntil(timeoutMillis = 5_000) {
            model.state.value.items.size == 2
        }
        model.move(model.state.value.items.first { it.id == second }, -1)
        rule.waitUntil(timeoutMillis = 5_000) {
            model.state.value.items.firstOrNull()?.id == second
        }
    }

    private fun channelEntity(playlistId: Long, remoteId: String, name: String) = ChannelEntity(
        playlistId = playlistId,
        remoteId = remoteId,
        number = null,
        name = name,
        nameNormalized = SearchText.normalize(name),
        groupName = "Genel",
        logoUrl = null,
        tvgId = null,
        hasArchive = false,
        archiveDays = 0,
        streamFormat = "AUTO",
        syncToken = 1,
    )

    private class MapCredentialStore : CredentialStore {
        val values = mutableMapOf<Long, Credentials>()
        val urls = mutableMapOf<Long, String>()

        override suspend fun put(playlistId: Long, credentials: Credentials) {
            values[playlistId] = credentials
        }

        override suspend fun get(playlistId: Long): Credentials? = values[playlistId]

        override suspend fun remove(playlistId: Long) {
            values.remove(playlistId)
            urls.remove(playlistId)
        }

        override suspend fun putSourceUrl(playlistId: Long, url: String) {
            urls[playlistId] = url
        }

        override suspend fun getSourceUrl(playlistId: Long): String? = urls[playlistId]

        override suspend fun removeSourceUrl(playlistId: Long) {
            urls.remove(playlistId)
        }
    }
}

@androidx.compose.runtime.Composable
private fun PlaylistFormHost(form: PlaylistFormViewModel) {
    val state by form.state.collectAsState()
    PlaylistFormScreen(
        state = state,
        onName = form::setName,
        onType = form::setType,
        onUrl = form::setUrl,
        onUsername = form::setUsername,
        onPassword = form::setPassword,
        onEpgUrl = form::setEpgUrl,
        onUserAgent = form::setUserAgent,
        onReferer = form::setReferer,
        onSyncInterval = form::setSyncInterval,
        onTest = form::test,
        onSave = form::save,
        onBack = {},
    )
}
