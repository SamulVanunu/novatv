package dev.anon.novatv.core.sync

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.database.entity.HiddenChannelEntity
import dev.anon.novatv.core.database.entity.HiddenGroupEntity
import dev.anon.novatv.core.database.entity.WatchHistoryEntity
import dev.anon.novatv.core.database.mapper.toModel
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.ItemType
import dev.anon.novatv.core.model.SearchText
import dev.anon.novatv.core.settings.PreferenceStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

/** "Son izlenenler" sanal grubunun anahtarı; gerçek bir grup adıyla çakışmaz. */
const val RECENT_GROUP_KEY = "__recent"

data class GroupItem(val key: String, val label: String, val count: Int)

data class ChannelFilter(
    val group: String? = null,
    val query: String = "",
    val favoritesOnly: Boolean = false,
)

interface ChannelRepository {
    fun groups(): Flow<List<GroupItem>>
    fun channels(filter: ChannelFilter): Flow<PagingData<Channel>>

    /** Aktif profilin favori kanal kimlikleri; satırlardaki yıldız için. */
    fun favoriteChannelIds(): Flow<Set<Long>>
    suspend fun byId(id: Long): Channel?

    /** Çoklu ekran ve seçiciler için düz liste; 0 = tümü. */
    suspend fun allChannels(limit: Int = 0): List<Channel>

    /**
     * Oynatıcıda kanal ilerletme sırası: gizli kanal/gruplar hariç, listedeki
     * sırayla (numara, sonra ad). Varsayılan, tüm kanallar.
     */
    suspend fun zappingOrder(): List<Channel> = allChannels()
    suspend fun toggleFavorite(channel: Channel)
    suspend fun hideChannel(channel: Channel)
    suspend fun hideGroup(group: String)
    suspend fun setNumber(channel: Channel, number: Int?)
    suspend fun lastChannel(): Channel?
    suspend fun recordWatch(channel: Channel, positionMs: Long = 0, durationMs: Long = 0)
}

class RoomChannelRepository(
    private val db: NovaDatabase,
    private val preferenceStore: PreferenceStore,
) : ChannelRepository {

    private val clock = AtomicLong(System.currentTimeMillis())

    override fun groups(): Flow<List<GroupItem>> =
        combine(
            db.playlistDao().observeActive(),
            preferenceStore.activeProfileId,
        ) { playlists, profileId -> playlists.map { it.id } to profileId }
            .flatMapLatest { (playlistIds, profileId) ->
                if (playlistIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        db.channelDao().observeGroupCounts(playlistIds, profileId),
                        db.channelDao().observeRecentCount(playlistIds, profileId),
                    ) { counts, recentCount ->
                        buildList {
                            add(GroupItem(key = ALL_GROUPS_KEY, label = "Tümü", count = counts.sumOf { it.count }))
                            if (recentCount > 0) {
                                add(GroupItem(key = RECENT_GROUP_KEY, label = "Son izlenenler", count = recentCount))
                            }
                            counts.forEach { add(GroupItem(key = it.name, label = it.name, count = it.count)) }
                        }
                    }
                }
            }

    override fun channels(filter: ChannelFilter): Flow<PagingData<Channel>> =
        combine(
            db.playlistDao().observeActive(),
            preferenceStore.activeProfileId,
        ) { playlists, profileId -> playlists.map { it.id } to profileId }
            .flatMapLatest { (playlistIds, profileId) ->
                if (playlistIds.isEmpty()) {
                    flowOf(PagingData.empty())
                } else {
                    Pager(
                        config = PagingConfig(
                            pageSize = PAGE_SIZE,
                            initialLoadSize = PAGE_SIZE * 2,
                            prefetchDistance = PAGE_SIZE / 2,
                            enablePlaceholders = false,
                        ),
                    ) {
                        val query = SearchText.normalize(filter.query)
                        if (filter.group == RECENT_GROUP_KEY) {
                            db.channelDao().recentPaging(
                                playlistIds = playlistIds,
                                q = query,
                                favoritesOnly = filter.favoritesOnly,
                                profileId = profileId,
                            )
                        } else {
                            db.channelDao().paging(
                                playlistIds = playlistIds,
                                group = filter.group,
                                q = query,
                                favoritesOnly = filter.favoritesOnly,
                                profileId = profileId,
                            )
                        }
                    }.flow.map { page -> page.map { it.toModel() } }
                }
            }

    override fun favoriteChannelIds(): Flow<Set<Long>> =
        preferenceStore.activeProfileId.flatMapLatest { profileId ->
            db.userDataDao().observeFavoriteIds(profileId, ItemType.CHANNEL.name)
                .map { ids -> ids.toSet() }
        }

    override suspend fun byId(id: Long): Channel? = db.channelDao().byId(id)?.toModel()

    override suspend fun allChannels(limit: Int): List<Channel> {
        val playlistIds = db.playlistDao().observeActive().first().map { it.id }
        val channels = playlistIds.flatMap { id ->
            db.channelDao().byPlaylist(id).map { it.toModel() }
        }
        return if (limit > 0) channels.take(limit) else channels
    }

    override suspend fun zappingOrder(): List<Channel> {
        val playlistIds = db.playlistDao().observeActive().first().map { it.id }
        val profileId = preferenceStore.activeProfileId.first()
        return db.channelDao()
            .observeForGuide(playlistIds, null, profileId)
            .first()
            .map { it.toModel() }
    }

    override suspend fun toggleFavorite(channel: Channel) {
        db.userDataDao().toggleFavorite(
            profileId = preferenceStore.activeProfileId.first(),
            itemType = ItemType.CHANNEL.name,
            itemId = channel.id,
        )
    }

    override suspend fun hideChannel(channel: Channel) {
        db.userDataDao().hideChannel(
            HiddenChannelEntity(
                profileId = preferenceStore.activeProfileId.first(),
                channelId = channel.id,
            ),
        )
    }

    override suspend fun hideGroup(group: String) {
        val profileId = preferenceStore.activeProfileId.first()
        db.playlistDao().observeActive().first().forEach { playlist ->
            db.userDataDao().hideGroup(
                HiddenGroupEntity(profileId = profileId, playlistId = playlist.id, groupName = group),
            )
        }
    }

    override suspend fun setNumber(channel: Channel, number: Int?) {
        db.channelDao().setNumber(channel.id, number)
    }

    override suspend fun lastChannel(): Channel? {
        val profileId = preferenceStore.activeProfileId.first()
        val id = db.userDataDao()
            .observeLastWatched(profileId, ItemType.CHANNEL.name)
            .first() ?: return null
        return byId(id)
    }

    override suspend fun recordWatch(channel: Channel, positionMs: Long, durationMs: Long) {
        db.userDataDao().upsertHistory(
            WatchHistoryEntity(
                profileId = preferenceStore.activeProfileId.first(),
                itemType = ItemType.CHANNEL.name,
                itemId = channel.id,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = clock.incrementAndGet(),
            ),
        )
    }

    private companion object {
        const val ALL_GROUPS_KEY = ""
        const val PAGE_SIZE = 60
    }
}
