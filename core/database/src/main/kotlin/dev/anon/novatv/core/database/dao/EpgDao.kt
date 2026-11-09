package dev.anon.novatv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.anon.novatv.core.database.entity.EpgChannelEntity
import dev.anon.novatv.core.database.entity.EpgProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EpgDao {

    @Query("SELECT * FROM epg_channel WHERE playlistId = :playlistId")
    abstract suspend fun channels(playlistId: Long): List<EpgChannelEntity>

    @Query("SELECT id FROM epg_channel WHERE playlistId = :playlistId AND tvgId = :tvgId")
    abstract suspend fun channelId(playlistId: Long, tvgId: String): Long?

    @Insert
    abstract suspend fun insertChannel(entity: EpgChannelEntity): Long

    @Update
    abstract suspend fun updateChannel(entity: EpgChannelEntity)

    @Transaction
    open suspend fun upsertChannel(entity: EpgChannelEntity): Long {
        val existing = channelId(entity.playlistId, entity.tvgId)
        return if (existing == null) {
            insertChannel(entity)
        } else {
            updateChannel(entity.copy(id = existing))
            existing
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertPrograms(items: List<EpgProgramEntity>)

    @Query("SELECT COUNT(*) FROM epg_channel WHERE playlistId = :playlistId")
    abstract suspend fun countChannels(playlistId: Long): Int

    @Query("SELECT COUNT(*) FROM epg_program")
    abstract suspend fun countPrograms(): Int

    @Query(
        "SELECT * FROM epg_program WHERE channelId = :channelId AND endAt > :from AND startAt < :to ORDER BY startAt",
    )
    abstract fun observeProgramsFor(channelId: Long, from: Long, to: Long): Flow<List<EpgProgramEntity>>

    /** Şu an yayında olan program; yoksa null. */
    @Query(
        "SELECT * FROM epg_program WHERE channelId = :channelId AND startAt <= :now AND endAt > :now ORDER BY startAt LIMIT 1",
    )
    abstract fun observeCurrent(channelId: Long, now: Long): Flow<EpgProgramEntity?>

    /** Sıradaki program. */
    @Query(
        "SELECT * FROM epg_program WHERE channelId = :channelId AND startAt > :now ORDER BY startAt LIMIT 1",
    )
    abstract fun observeNext(channelId: Long, now: Long): Flow<EpgProgramEntity?>

    @Query(
        """
        SELECT * FROM epg_program
        WHERE channelId IN (:channelIds) AND endAt > :from AND startAt < :to
        ORDER BY startAt
        """,
    )
    abstract fun observeWindow(
        channelIds: List<Long>,
        from: Long,
        to: Long,
    ): Flow<List<EpgProgramEntity>>

    @Query(
        "SELECT * FROM epg_program WHERE channelId = :channelId AND endAt > :from AND startAt < :to ORDER BY startAt",
    )
    abstract suspend fun programsFor(channelId: Long, from: Long, to: Long): List<EpgProgramEntity>

    /** Bir kaynağın tüm rehberini yeniden yazmadan önce temizler. */
    @Query(
        "DELETE FROM epg_program WHERE epgChannelId IN (SELECT id FROM epg_channel WHERE playlistId = :playlistId)",
    )
    abstract suspend fun clearPlaylist(playlistId: Long)

    @Query("DELETE FROM epg_program WHERE endAt < :before")
    abstract suspend fun deleteEndedBefore(before: Long)

    @Query("DELETE FROM epg_program WHERE startAt > :after")
    abstract suspend fun deleteStartingAfter(after: Long)

    @Query("SELECT * FROM epg_program WHERE endAt > :from AND startAt < :to ORDER BY startAt LIMIT :limit")
    abstract suspend fun window(from: Long, to: Long, limit: Int): List<EpgProgramEntity>

    @Query(
        "SELECT * FROM epg_program WHERE title LIKE '%' || :q || '%' AND endAt > :from ORDER BY startAt LIMIT :limit",
    )
    abstract fun search(q: String, from: Long, limit: Int): Flow<List<EpgProgramEntity>>

    @Query(
        """
        SELECT p.*, c.name AS channelName FROM epg_program p
        INNER JOIN channel c ON c.id = p.channelId
        WHERE p.title LIKE '%' || :q || '%' AND p.endAt > :from
        ORDER BY p.startAt LIMIT :limit
        """,
    )
    abstract fun searchWithChannel(
        q: String,
        from: Long,
        limit: Int,
    ): Flow<List<ProgramHitRow>>
}

data class ProgramHitRow(
    @androidx.room.Embedded val program: EpgProgramEntity,
    val channelName: String,
)
