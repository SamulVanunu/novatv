package dev.anon.novatv.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.anon.novatv.core.database.entity.EpisodeEntity
import dev.anon.novatv.core.database.entity.FavoriteEntity
import dev.anon.novatv.core.database.entity.HiddenChannelEntity
import dev.anon.novatv.core.database.entity.HiddenGroupEntity
import dev.anon.novatv.core.database.entity.MovieEntity
import dev.anon.novatv.core.database.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UserDataDao {

    @Query("SELECT itemId FROM favorite WHERE profileId = :profileId AND itemType = :itemType")
    abstract fun observeFavoriteIds(profileId: Long, itemType: String): Flow<List<Long>>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM favorite WHERE profileId = :profileId AND itemType = :itemType AND itemId = :itemId)",
    )
    abstract suspend fun isFavorite(profileId: Long, itemType: String, itemId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE profileId = :profileId AND itemType = :itemType AND itemId = :itemId")
    abstract suspend fun removeFavorite(profileId: Long, itemType: String, itemId: Long)

    @Transaction
    open suspend fun toggleFavorite(profileId: Long, itemType: String, itemId: Long): Boolean {
        return if (isFavorite(profileId, itemType, itemId)) {
            removeFavorite(profileId, itemType, itemId)
            false
        } else {
            addFavorite(FavoriteEntity(profileId, itemType, itemId))
            true
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun hideChannel(entity: HiddenChannelEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun hideGroup(entity: HiddenGroupEntity)

    @Query("SELECT COUNT(*) FROM hidden_channel WHERE profileId = :profileId")
    abstract suspend fun hiddenChannelCount(profileId: Long): Int

    @Query("SELECT COUNT(*) FROM hidden_group WHERE profileId = :profileId")
    abstract suspend fun hiddenGroupCount(profileId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertHistory(entity: WatchHistoryEntity)

    @Query(
        "SELECT itemId FROM watch_history WHERE profileId = :profileId AND itemType = :itemType ORDER BY updatedAt DESC LIMIT 1",
    )
    abstract fun observeLastWatched(profileId: Long, itemType: String): Flow<Long?>

    @Query(
        "SELECT itemId FROM watch_history WHERE profileId = :profileId AND itemType = :itemType ORDER BY updatedAt DESC LIMIT :limit",
    )
    abstract fun observeRecent(profileId: Long, itemType: String, limit: Int): Flow<List<Long>>

    @Query(
        """
        SELECT movie.*, wh.positionMs AS positionMs, wh.durationMs AS durationMs,
               wh.updatedAt AS updatedAt
        FROM movie
        INNER JOIN watch_history wh
            ON wh.itemId = movie.id AND wh.itemType = 'MOVIE' AND wh.profileId = :profileId
        WHERE wh.durationMs > 0 AND wh.positionMs < wh.durationMs * 0.95
        ORDER BY wh.updatedAt DESC LIMIT :limit
        """,
    )
    abstract fun continueMovies(profileId: Long, limit: Int): Flow<List<MovieProgressRow>>

    @Query(
        """
        SELECT episode.*, wh.positionMs AS positionMs, wh.durationMs AS durationMs,
               wh.updatedAt AS updatedAt
        FROM episode
        INNER JOIN watch_history wh
            ON wh.itemId = episode.id AND wh.itemType = 'EPISODE' AND wh.profileId = :profileId
        WHERE wh.durationMs > 0 AND wh.positionMs < wh.durationMs * 0.95
        ORDER BY wh.updatedAt DESC LIMIT :limit
        """,
    )
    abstract fun continueEpisodes(profileId: Long, limit: Int): Flow<List<EpisodeProgressRow>>

    @Query(
        "SELECT * FROM watch_history WHERE profileId = :profileId AND itemType = :itemType ORDER BY updatedAt DESC LIMIT :limit",
    )
    abstract fun observeHistory(
        profileId: Long,
        itemType: String,
        limit: Int,
    ): Flow<List<WatchHistoryEntity>>

    @Query(
        "SELECT * FROM watch_history WHERE profileId = :profileId AND itemType = :itemType AND itemId = :itemId",
    )
    abstract suspend fun history(
        profileId: Long,
        itemType: String,
        itemId: Long,
    ): WatchHistoryEntity?

    @Query("DELETE FROM watch_history WHERE profileId = :profileId")
    abstract suspend fun clearHistory(profileId: Long)

    @Query("DELETE FROM favorite WHERE profileId = :profileId")
    abstract suspend fun clearFavorites(profileId: Long)

    @Query("DELETE FROM hidden_channel WHERE profileId = :profileId")
    abstract suspend fun clearHiddenChannels(profileId: Long)

    @Query("DELETE FROM hidden_group WHERE profileId = :profileId")
    abstract suspend fun clearHiddenGroups(profileId: Long)
}

data class MovieProgressRow(
    @Embedded val movie: MovieEntity,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)

data class EpisodeProgressRow(
    @Embedded val episode: EpisodeEntity,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)
