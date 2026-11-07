package dev.anon.novatv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.anon.novatv.core.database.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistDao {

    @Query("SELECT * FROM playlist ORDER BY sortOrder, id")
    abstract fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist WHERE isActive = 1 ORDER BY sortOrder, id")
    abstract fun observeActive(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist WHERE id = :id")
    abstract suspend fun byId(id: Long): PlaylistEntity?

    @Insert
    abstract suspend fun insert(entity: PlaylistEntity): Long

    @Update
    abstract suspend fun update(entity: PlaylistEntity)

    @Query("DELETE FROM playlist WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("UPDATE playlist SET lastSyncAt = :at, syncState = :state WHERE id = :id")
    abstract suspend fun markSync(id: Long, at: Long, state: String)

    @Query("UPDATE playlist SET isActive = :active WHERE id = :id")
    abstract suspend fun setActive(id: Long, active: Boolean)

    @Transaction
    open suspend fun upsert(entity: PlaylistEntity): Long {
        if (entity.id == 0L) return insert(entity)
        return if (byId(entity.id) == null) {
            insert(entity)
        } else {
            update(entity)
            entity.id
        }
    }
}
