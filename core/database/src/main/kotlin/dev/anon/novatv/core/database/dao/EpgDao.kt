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
}
