package dev.anon.novatv.feature.live

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.paging.PagingData
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.SearchText
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.sync.ChannelFilter
import dev.anon.novatv.core.sync.ChannelRepository
import dev.anon.novatv.core.sync.GroupItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class LiveViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var repository: FakeChannelRepository
    private lateinit var preferences: PreferenceStore
    private lateinit var viewModel: LiveViewModel

    @Before
    fun setUp() {
        val directory = Files.createTempDirectory("novatv-live-prefs").toFile()
        preferences = PreferenceStore(
            PreferenceDataStoreFactory.create(
                produceFile = { File(directory, "live.preferences_pb") },
            ),
        )
        repository = FakeChannelRepository()
        viewModel = LiveViewModel(repository, preferences)
    }

    @Test
    fun groupsReachStateWithAllFirst() = runTest(mainRule.dispatcher) {
        val state = viewModel.state.first { it.groups.isNotEmpty() }

        assertEquals(listOf("Tümü", "Spor", "Ulusal"), state.groups.map { it.label })
        assertEquals("", state.groups.first().key)
        assertEquals(3, state.groups.first().count)
        assertNull(state.selectedGroup)
    }

    @Test
    fun rapidTypingCollapsesToOneQueryAfterDebounce() = runTest(mainRule.dispatcher) {
        collectChannels()
        repository.filters.clear()

        viewModel.setQuery("t")
        viewModel.setQuery("tr")
        viewModel.setQuery("trt")

        advanceTimeBy(LiveViewModel.QUERY_DEBOUNCE_MS - 1)
        assertEquals(0, repository.filters.size)

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(1, repository.filters.size)
        assertEquals("trt", repository.filters.single().query)
    }

    @Test
    fun turkishCharactersAreNormalizedBeforeQuerying() = runTest(mainRule.dispatcher) {
        collectChannels()
        repository.filters.clear()

        viewModel.setQuery("İSTANBUL ŞÖLENİ")
        advanceTimeBy(LiveViewModel.QUERY_DEBOUNCE_MS)
        advanceUntilIdle()

        assertEquals("istanbul soleni", repository.filters.single().query)
        assertEquals("İSTANBUL ŞÖLENİ", viewModel.state.value.query)
    }

    @Test
    fun favoritesOnlyReachesFilter() = runTest(mainRule.dispatcher) {
        collectChannels()
        repository.filters.clear()

        viewModel.toggleFavoritesOnly()
        advanceUntilIdle()

        assertTrue(repository.filters.last().favoritesOnly)
        assertTrue(viewModel.state.first { it.favoritesOnly }.favoritesOnly)
    }

    @Test
    fun gridFlagIsPersistedAcrossViewModels() = runTest(mainRule.dispatcher) {
        assertFalse(viewModel.state.first { it.groups.isNotEmpty() }.grid)

        viewModel.toggleGrid()
        advanceUntilIdle()
        assertTrue(preferences.liveGrid.first { it })

        val second = LiveViewModel(repository, preferences)
        assertTrue(second.state.first { it.grid }.grid)
    }

    @Test
    fun selectingGroupClearsFocusedChannel() = runTest(mainRule.dispatcher) {
        viewModel.onChannelFocused(channel(1, "TRT 1", "Ulusal"))
        assertEquals("TRT 1", viewModel.state.first { it.focusedChannel != null }.focusedChannel?.name)

        viewModel.selectGroup("Spor")

        val state = viewModel.state.first { it.selectedGroup == "Spor" }
        assertNull(state.focusedChannel)
    }

    private fun TestScope.collectChannels() {
        backgroundScope.launch { viewModel.channels.collect { } }
        advanceUntilIdle()
    }

    private fun channel(id: Long, name: String, group: String) = Channel(
        id = id,
        playlistId = 1,
        remoteId = id.toString(),
        name = name,
        groupName = group,
    )

    private inner class FakeChannelRepository : ChannelRepository {

        val filters = mutableListOf<ChannelFilter>()

        private val groups = listOf(
            GroupItem("", "Tümü", 3),
            GroupItem("Spor", "Spor", 1),
            GroupItem("Ulusal", "Ulusal", 2),
        )

        private val channels = listOf(
            channel(1, "TRT 1", "Ulusal"),
            channel(2, "TRT Spor", "Spor"),
            channel(3, "İstanbul TV", "Ulusal"),
        )

        override fun groups(): Flow<List<GroupItem>> = flowOf(groups)

        override fun channels(filter: ChannelFilter): Flow<PagingData<Channel>> {
            filters += filter
            val matched = channels.filter { channel ->
                (filter.group == null || channel.groupName == filter.group) &&
                    (filter.query.isBlank() || SearchText.normalize(channel.name).contains(filter.query)) &&
                    (!filter.favoritesOnly || channel.id == 1L)
            }
            return flowOf(PagingData.from(matched))
        }

        override fun favoriteChannelIds(): Flow<Set<Long>> = flowOf(setOf(1L))

        override suspend fun byId(id: Long): Channel? = channels.firstOrNull { it.id == id }

        override suspend fun toggleFavorite(channel: Channel) = Unit

        override suspend fun hideChannel(channel: Channel) = Unit

        override suspend fun hideGroup(group: String) = Unit

        override suspend fun setNumber(channel: Channel, number: Int?) = Unit

        override suspend fun lastChannel(): Channel? = channels.first()

        override suspend fun recordWatch(channel: Channel, positionMs: Long, durationMs: Long) = Unit
    }
}
