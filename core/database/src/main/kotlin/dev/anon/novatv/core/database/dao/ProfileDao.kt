package dev.anon.novatv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.anon.novatv.core.database.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProfileDao {

    @Query("SELECT * FROM profile ORDER BY id")
    abstract fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profile WHERE id = :id")
    abstract suspend fun byId(id: Long): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profile")
    abstract suspend fun count(): Int

    @Insert
    abstract suspend fun insert(entity: ProfileEntity): Long

    @Update
    abstract suspend fun update(entity: ProfileEntity)

    @Query("DELETE FROM profile WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Transaction
    open suspend fun deleteWithUserData(id: Long, userData: UserDataDao) {
        userData.clearHistory(id)
        userData.clearFavorites(id)
        userData.clearHiddenChannels(id)
        userData.clearHiddenGroups(id)
        delete(id)
    }
}
