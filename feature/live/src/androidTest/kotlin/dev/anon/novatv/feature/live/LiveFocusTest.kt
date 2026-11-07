package dev.anon.novatv.feature.live

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.database.entity.ChannelEntity
import dev.anon.novatv.core.database.entity.PlaylistEntity
import dev.anon.novatv.core.model.SearchText
import dev.anon.novatv.core.model.SyncState
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.sync.RoomChannelRepository
import dev.anon.novatv.ui.theme.NovaTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class LiveFocusTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var db: NovaDatabase
    private lateinit var repository: RoomChannelRepository
    private lateinit var viewModel: LiveViewModel

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, NovaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            db.playlistDao().upsert(
                PlaylistEntity(
                    id = 1,
                    type = "M3U",
                    name = "Test",
                    baseUrl = "http://localhost/list.m3u",
                    username = null,
                    epgUrl = null,
                    userAgent = null,
                    isActive = true,
                    sortOrder = 0,
                    lastSyncAt = null,
                    syncState = SyncState.OK.name,
                    syncIntervalHours = 24,
                ),
            )
            db.channelDao().replaceForPlaylist(
                playlistId = 1,
                token = 1,
                items = listOf(
                    channelEntity("1", "TRT 1", "Ulusal"),
                    channelEntity("2", "TRT 2", "Ulusal"),
                    channelEntity("3", "beIN Sports 1", "Spor"),
                ),
            )
        }
        val preferences = PreferenceStore(
            PreferenceDataStoreFactory.create(
                produceFile = { File(context.cacheDir, "live-${System.nanoTime()}.preferences_pb") },
            ),
        )
        repository = RoomChannelRepository(db, preferences)
        viewModel = LiveViewModel(repository, preferences)
    }

    @After
    fun tearDown() {
        runCatching { db.close() }
    }

    @Test
    fun firstChannelTakesFocusAndGroupChangeRefocusesIt() {
        setContent()

        rule.waitUntil(timeoutMillis = 5_000) { focused("channel-0") }

        rule.onNodeWithTag("group-Spor").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(timeoutMillis = 5_000) { viewModel.state.value.selectedGroup == "Spor" }
        rule.waitUntil(timeoutMillis = 5_000) { focused("channel-0") }

        rule.onNodeWithTag("channel-0").assertIsFocused()
    }

    @Test
    fun emptyResultKeepsFocusOnGroupRail() {
        setContent()
        rule.waitUntil(timeoutMillis = 5_000) { focused("channel-0") }

        viewModel.setQuery("zzzz")
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag("empty-state").fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitUntil(timeoutMillis = 5_000) { focused("group-all") }

        rule.onNodeWithTag("group-all").assertIsFocused()
    }

    @Test
    fun hiddenGroupDisappearsFromRail() {
        setContent()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag("group-Spor").fetchSemanticsNodes().isNotEmpty()
        }

        runBlocking { repository.hideGroup("Spor") }

        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag("group-Spor").fetchSemanticsNodes().isEmpty()
        }
        assertTrue(rule.onAllNodesWithTag("group-Ulusal").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun backKeyReturnsFocusToGroupRail() {
        setContent()
        rule.waitUntil(timeoutMillis = 5_000) { focused("channel-0") }

        rule.onNodeWithTag("channel-0").performKeyInput { pressKey(Key.Back) }

        rule.waitUntil(timeoutMillis = 5_000) { focused("group-all") }
        rule.onNodeWithTag("group-all").assertIsFocused()
    }

    private fun setContent() {
        rule.setContent {
            NovaTheme {
                val state by viewModel.state.collectAsState()
                val channels = viewModel.channels.collectAsLazyPagingItems()
                LiveScreen(
                    state = state,
                    channels = channels,
                    onSelectGroup = viewModel::selectGroup,
                    onQueryChange = viewModel::setQuery,
                    onToggleFavorites = viewModel::toggleFavoritesOnly,
                    onToggleGrid = viewModel::toggleGrid,
                    onChannelClick = {},
                    onToggleFavorite = viewModel::toggleFavorite,
                    onHideChannel = viewModel::hideChannel,
                    onHideGroup = viewModel::hideGroup,
                    onSetNumber = viewModel::setNumber,
                    onChannelFocused = viewModel::onChannelFocused,
                )
            }
        }
    }

    private fun focused(tag: String): Boolean = runCatching {
        rule.onNodeWithTag(tag).assertIsFocused()
        true
    }.getOrDefault(false)

    private fun channelEntity(remoteId: String, name: String, group: String) = ChannelEntity(
        playlistId = 1,
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
