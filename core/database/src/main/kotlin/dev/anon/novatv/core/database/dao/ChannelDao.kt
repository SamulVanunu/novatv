package dev.anon.novatv.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.anon.novatv.core.database.entity.ChannelEntity
import dev.anon.novatv.core.database.entity.ChannelIdRow
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChannelDao {

    @Query("SELECT DISTINCT groupName FROM channel WHERE playlistId IN (:playlistIds) ORDER BY groupName")
    abstract fun observeGroups(playlistIds: List<Long>): Flow<List<String>>

    @Query(
        """
        SELECT * FROM channel
        WHERE playlistId IN (:playlistIds)
          AND (:group IS NULL OR groupName = :group)
          AND (:q = '' OR nameNormalized LIKE '%' || :q || '%')
          AND (:favoritesOnly = 0 OR id IN (
              SELECT itemId FROM favorite WHERE profileId = :profileId AND itemType = 'CHANNEL'))
          AND id NOT IN (SELECT channelId FROM hidden_channel WHERE profileId = :profileId)
          AND NOT EXISTS (
              SELECT 1 FROM hidden_group hg
              WHERE hg.profileId = :profileId
                AND hg.playlistId = channel.playlistId
                AND hg.groupName = channel.groupName)
        ORDER BY number IS NULL, number, name
        """,
    )
    abstract fun paging(
        playlistIds: List<Long>,
        group: String?,
        q: String,
        favoritesOnly: Boolean,
        profileId: Long,
    ): PagingSource<Int, ChannelEntity>

    /** Son izlenenler rayı: gizli kanal/grup filtreleri normal listeyle aynı. */
    @Query(
        """
        SELECT channel.* FROM channel
        INNER JOIN watch_history wh
            ON wh.itemId = channel.id AND wh.itemType = 'CHANNEL' AND wh.profileId = :profileId
        WHERE channel.playlistId IN (:playlistIds)
          AND (:q = '' OR channel.nameNormalized LIKE '%' || :q || '%')
          AND (:favoritesOnly = 0 OR channel.id IN (
              SELECT itemId FROM favorite WHERE profileId = :profileId AND itemType = 'CHANNEL'))
          AND channel.id NOT IN (SELECT channelId FROM hidden_channel WHERE profileId = :profileId)
          AND NOT EXISTS (
              SELECT 1 FROM hidden_group hg
              WHERE hg.profileId = :profileId
                AND hg.playlistId = channel.playlistId
                AND hg.groupName = channel.groupName)
        ORDER BY wh.updatedAt DESC
        """,
    )
    abstract fun recentPaging(
        playlistIds: List<Long>,
        q: String,
        favoritesOnly: Boolean,
        profileId: Long,
    ): PagingSource<Int, ChannelEntity>

    @Query(
        """
        SELECT COUNT(*) FROM watch_history wh
        INNER JOIN channel ON channel.id = wh.itemId
        WHERE wh.profileId = :profileId
          AND wh.itemType = 'CHANNEL'
          AND channel.playlistId IN (:playlistIds)
          AND channel.id NOT IN (SELECT channelId FROM hidden_channel WHERE profileId = :profileId)
          AND NOT EXISTS (
              SELECT 1 FROM hidden_group hg
              WHERE hg.profileId = :profileId
                AND hg.playlistId = channel.playlistId
                AND hg.groupName = channel.groupName)
        """,
    )
    abstract fun observeRecentCount(playlistIds: List<Long>, profileId: Long): Flow<Int>

    @Query(
        """
        SELECT groupName AS name, COUNT(*) AS count FROM channel
        WHERE playlistId IN (:playlistIds)
          AND id NOT IN (SELECT channelId FROM hidden_channel WHERE profileId = :profileId)
          AND NOT EXISTS (
              SELECT 1 FROM hidden_group hg
              WHERE hg.profileId = :profileId
                AND hg.playlistId = channel.playlistId
                AND hg.groupName = channel.groupName)
        GROUP BY groupName
        ORDER BY groupName
        """,
    )
    abstract fun observeGroupCounts(playlistIds: List<Long>, profileId: Long): Flow<List<GroupCount>>

    @Query("SELECT * FROM channel WHERE id = :id")
    abstract suspend fun byId(id: Long): ChannelEntity?

    @Query("SELECT COUNT(*) FROM channel WHERE playlistId = :playlistId")
    abstract suspend fun countByPlaylist(playlistId: Long): Int

    @Query("SELECT id, remoteId FROM channel WHERE playlistId = :playlistId")
    abstract suspend fun idRows(playlistId: Long): List<ChannelIdRow>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertNew(items: List<ChannelEntity>): List<Long>

    @Update
    abstract suspend fun updateExisting(items: List<ChannelEntity>)

    @Query("DELETE FROM channel WHERE playlistId = :playlistId AND syncToken != :token")
    abstract suspend fun deleteStale(playlistId: Long, token: Long)

    @Query("UPDATE channel SET number = :number WHERE id = :id")
    abstract suspend fun setNumber(id: Long, number: Int?)

    @Transaction
    open suspend fun replaceForPlaylist(playlistId: Long, token: Long, items: List<ChannelEntity>) {
        val idByRemote = idRows(playlistId).associate { it.remoteId to it.id }
        val toInsert = ArrayList<ChannelEntity>(items.size)
        val toUpdate = ArrayList<ChannelEntity>(items.size)
        for (item in items) {
            val normalized = item.copy(playlistId = playlistId, syncToken = token)
            val existingId = idByRemote[normalized.remoteId]
            if (existingId == null) toInsert += normalized else toUpdate += normalized.copy(id = existingId)
        }
        toInsert.chunked(500).forEach { insertNew(it) }
        toUpdate.chunked(500).forEach { updateExisting(it) }
        deleteStale(playlistId, token)
    }
}

data class GroupCount(val name: String, val count: Int)
