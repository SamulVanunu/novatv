package dev.anon.novatv.feature.player

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
import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.player.PlaybackController
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.sync.ChannelFilter
import dev.anon.novatv.core.sync.ChannelRepository
import dev.anon.novatv.core.sync.GroupItem
import dev.anon.novatv.ui.theme.NovaTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class PlayerOsdTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var controller: PlaybackController
    private lateinit var viewModel: PlayerViewModel
    private lateinit var server: ServerSocket
    private val held = CopyOnWriteArrayList<Socket>()
    private var backToChannels = 0

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Yayın isteğini yanıtsız bırakan yerel soket: oynatıcı Buffering'de kalır,
        // hata katmanı testin önüne geçmez.
        server = ServerSocket(0)
        Thread {
            runCatching {
                while (!server.isClosed) {
                    held += server.accept()
                }
            }
        }.apply { isDaemon = true }.start()

        onMain { controller = PlaybackController(context) }

        val preferences = PreferenceStore(
            PreferenceDataStoreFactory.create(
                produceFile = { File(context.cacheDir, "player-${System.nanoTime()}.preferences_pb") },
            ),
        )
        viewModel = PlayerViewModel(
            playbackController = controller,
            repository = FakeChannelRepository(),
            resolver = ChannelStreamResolver {
                "http://127.0.0.1:${server.localPort}/live.m3u8"
            },
            preferenceStore = preferences,
        )

        rule.setContent {
            NovaTheme {
                val state by viewModel.state.collectAsState()
                PlayerScreen(
                    state = state,
                    controller = controller,
                    onToggleOsd = viewModel::toggleOsd,
                    onHideOsd = viewModel::hideOsd,
                    onRetry = viewModel::retry,
                    onBackToChannels = { backToChannels += 1 },
                    onSeekBack = viewModel::seekBack,
                    onCycleAspect = viewModel::cycleAspect,
                    onCycleBuffer = viewModel::cycleBuffer,
                    onSelectAudio = viewModel::selectAudio,
                    onSelectSubtitle = viewModel::selectSubtitle,
                )
            }
        }

        onMain { viewModel.open(CHANNEL_ID) }
        rule.waitUntil(timeoutMillis = 5_000) { focused("player-root") }
    }

    @After
    fun tearDown() {
        runCatching { server.close() }
        held.forEach { runCatching { it.close() } }
        onMain { controller.release() }
    }

    @Test
    fun centerKeyOpensOsdAndItHidesAfterFourSeconds() {
        rule.onNodeWithTag("player-root").performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitUntil(timeoutMillis = 3_000) { exists("osd-channels") }

        rule.waitUntil(timeoutMillis = 8_000) { !exists("osd-channels") }
    }

    @Test
    fun infoKeyOpensOsd() {
        rule.onNodeWithTag("player-root").performKeyInput { pressKey(Key.Info) }

        rule.waitUntil(timeoutMillis = 3_000) { exists("osd-channels") }
        rule.onNodeWithTag("osd-channel-name").assertExists()
    }

    @Test
    fun channelsButtonReturnsToChannelList() {
        rule.onNodeWithTag("player-root").performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitUntil(timeoutMillis = 3_000) { exists("osd-channels") }

        rule.onNodeWithTag("osd-channels").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(1, backToChannels)
    }

    @Test
    fun focusReturnsToPlayerWhenOsdCloses() {
        rule.onNodeWithTag("player-root").performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitUntil(timeoutMillis = 3_000) { exists("osd-channels") }

        rule.runOnIdle { viewModel.hideOsd() }

        rule.waitUntil(timeoutMillis = 3_000) { focused("player-root") }
        rule.onNodeWithTag("player-root").assertIsFocused()
        assertTrue(!exists("osd-channels"))
    }

    private fun exists(tag: String): Boolean =
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun focused(tag: String): Boolean = runCatching {
        rule.onNodeWithTag(tag).assertIsFocused()
        true
    }.getOrDefault(false)

    private fun onMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    private inner class FakeChannelRepository : ChannelRepository {

        private val channel = Channel(
            id = CHANNEL_ID,
            playlistId = 1,
            remoteId = "1",
            name = "TRT 1",
            groupName = "Ulusal",
        )

        override fun groups(): Flow<List<GroupItem>> = flowOf(listOf(GroupItem("", "Tümü", 1)))

        override fun channels(filter: ChannelFilter): Flow<PagingData<Channel>> =
            flowOf(PagingData.from(listOf(channel)))

        override fun favoriteChannelIds(): Flow<Set<Long>> = flowOf(emptySet())

        override suspend fun byId(id: Long): Channel? = channel.takeIf { it.id == id }

        override suspend fun toggleFavorite(channel: Channel) = Unit

        override suspend fun hideChannel(channel: Channel) = Unit

        override suspend fun hideGroup(group: String) = Unit

        override suspend fun setNumber(channel: Channel, number: Int?) = Unit

        override suspend fun lastChannel(): Channel? = channel

        override suspend fun recordWatch(channel: Channel, positionMs: Long, durationMs: Long) = Unit
    }

    private companion object {
        const val CHANNEL_ID = 7L
    }
}
