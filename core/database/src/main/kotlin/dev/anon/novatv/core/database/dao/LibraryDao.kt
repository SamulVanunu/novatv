package dev.anon.novatv.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.anon.novatv.core.database.entity.EpisodeEntity
import dev.anon.novatv.core.database.entity.MovieEntity
import dev.anon.novatv.core.database.entity.SeasonEntity
import dev.anon.novatv.core.database.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class LibraryDao {

    @Query(
        """
        SELECT * FROM movie
        WHERE playlistId IN (:playlistIds)
          AND (:q = '' OR nameNormalized LIKE '%' || :q || '%')
          AND (:genre IS NULL OR genres LIKE '%' || :genre || '%')
          AND (:year IS NULL OR year = :year)
        ORDER BY name
        """,
    )
    abstract fun moviePaging(
        playlistIds: List<Long>,
        q: String,
        genre: String?,
        year: Int?,
    ): PagingSource<Int, MovieEntity>

    @Query(
        """
        SELECT * FROM series
        WHERE playlistId IN (:playlistIds)
          AND (:q = '' OR nameNormalized LIKE '%' || :q || '%')
          AND (:genre IS NULL OR genres LIKE '%' || :genre || '%')
          AND (:year IS NULL OR year = :year)
        ORDER BY name
        """,
    )
    abstract fun seriesPaging(
        playlistIds: List<Long>,
        q: String,
        genre: String?,
        year: Int?,
    ): PagingSource<Int, SeriesEntity>

    @Query("SELECT * FROM movie WHERE id = :id")
    abstract suspend fun movieById(id: Long): MovieEntity?

    @Query("SELECT * FROM series WHERE id = :id")
    abstract suspend fun seriesById(id: Long): SeriesEntity?

    @Query("SELECT * FROM series WHERE id = :id")
    abstract fun observeSeries(id: Long): Flow<SeriesEntity?>

    @Query("SELECT * FROM movie WHERE id = :id")
    abstract fun observeMovie(id: Long): Flow<MovieEntity?>

    @Query("SELECT * FROM series WHERE playlistId = :playlistId AND remoteId = :remoteId")
    abstract suspend fun seriesByRemote(playlistId: Long, remoteId: String): SeriesEntity?

    @Query("SELECT * FROM episode WHERE id = :id")
    abstract suspend fun episodeById(id: Long): EpisodeEntity?

    @Query("SELECT * FROM season WHERE seriesId = :seriesId ORDER BY number")
    abstract fun seasons(seriesId: Long): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM episode WHERE seriesId = :seriesId ORDER BY number")
    abstract fun episodes(seriesId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT COUNT(*) FROM episode WHERE seriesId = :seriesId")
    abstract suspend fun episodeCount(seriesId: Long): Int

    @Query("SELECT DISTINCT genres FROM movie WHERE playlistId IN (:playlistIds) AND genres IS NOT NULL")
    abstract fun movieGenres(playlistIds: List<Long>): Flow<List<String>>

    @Query("SELECT DISTINCT genres FROM series WHERE playlistId IN (:playlistIds) AND genres IS NOT NULL")
    abstract fun seriesGenres(playlistIds: List<Long>): Flow<List<String>>

    @Query(
        "SELECT DISTINCT year FROM movie WHERE playlistId IN (:playlistIds) AND year IS NOT NULL ORDER BY year DESC",
    )
    abstract fun movieYears(playlistIds: List<Long>): Flow<List<Int>>

    @Query(
        "SELECT DISTINCT year FROM series WHERE playlistId IN (:playlistIds) AND year IS NOT NULL ORDER BY year DESC",
    )
    abstract fun seriesYears(playlistIds: List<Long>): Flow<List<Int>>

    @Query(
        """
        SELECT * FROM movie
        WHERE playlistId IN (:playlistIds) AND nameNormalized LIKE '%' || :q || '%'
        ORDER BY name LIMIT :limit
        """,
    )
    abstract fun searchMovies(playlistIds: List<Long>, q: String, limit: Int): Flow<List<MovieEntity>>

    @Query(
        """
        SELECT * FROM series
        WHERE playlistId IN (:playlistIds) AND nameNormalized LIKE '%' || :q || '%'
        ORDER BY name LIMIT :limit
        """,
    )
    abstract fun searchSeries(playlistIds: List<Long>, q: String, limit: Int): Flow<List<SeriesEntity>>

    @Query("SELECT playlistId, remoteId, id FROM movie WHERE playlistId = :playlistId")
    abstract suspend fun movieIds(playlistId: Long): List<RemoteIdRow>

    @Query("SELECT playlistId, remoteId, id FROM series WHERE playlistId = :playlistId")
    abstract suspend fun seriesIds(playlistId: Long): List<RemoteIdRow>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertMovies(items: List<MovieEntity>): List<Long>

    @Update
    abstract suspend fun updateMovies(items: List<MovieEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertSeries(items: List<SeriesEntity>): List<Long>

    @Update
    abstract suspend fun updateSeries(items: List<SeriesEntity>)

    @Query("DELETE FROM movie WHERE id IN (:ids)")
    abstract suspend fun deleteMoviesById(ids: List<Long>)

    @Query("DELETE FROM series WHERE id IN (:ids)")
    abstract suspend fun deleteSeriesById(ids: List<Long>)

    @Query("DELETE FROM series WHERE playlistId = :playlistId")
    abstract suspend fun deleteSeriesForPlaylist(playlistId: Long)

    @Query("DELETE FROM movie WHERE playlistId = :playlistId")
    abstract suspend fun deleteMoviesForPlaylist(playlistId: Long)

    @Transaction
    open suspend fun replaceMovies(playlistId: Long, items: List<MovieEntity>) {
        val existing = movieIds(playlistId).associate { it.remoteId to it.id }
        val toInsert = ArrayList<MovieEntity>(items.size)
        val toUpdate = ArrayList<MovieEntity>(items.size)
        for (item in items) {
            val normalized = item.copy(playlistId = playlistId)
            val id = existing[normalized.remoteId]
            if (id == null) toInsert += normalized else toUpdate += normalized.copy(id = id)
        }
        toInsert.chunked(500).forEach { insertMovies(it) }
        toUpdate.chunked(500).forEach { updateMovies(it) }
        if (items.isNotEmpty()) {
            val kept = items.mapTo(HashSet(items.size)) { it.remoteId }
            existing.filterKeys { it !in kept }.values
                .chunked(500)
                .forEach { deleteMoviesById(it) }
        } else {
            deleteMoviesForPlaylist(playlistId)
        }
    }

    @Transaction
    open suspend fun replaceSeries(playlistId: Long, items: List<SeriesEntity>) {
        val existing = seriesIds(playlistId).associate { it.remoteId to it.id }
        val toInsert = ArrayList<SeriesEntity>(items.size)
        val toUpdate = ArrayList<SeriesEntity>(items.size)
        for (item in items) {
            val normalized = item.copy(playlistId = playlistId)
            val id = existing[normalized.remoteId]
            if (id == null) toInsert += normalized else toUpdate += normalized.copy(id = id)
        }
        toInsert.chunked(500).forEach { insertSeries(it) }
        toUpdate.chunked(500).forEach { updateSeries(it) }
        if (items.isNotEmpty()) {
            val kept = items.mapTo(HashSet(items.size)) { it.remoteId }
            existing.filterKeys { it !in kept }.values
                .chunked(500)
                .forEach { deleteSeriesById(it) }
        } else {
            deleteSeriesForPlaylist(playlistId)
        }
    }

    @Insert
    abstract suspend fun insertSeason(entity: SeasonEntity): Long

    @Insert
    abstract suspend fun insertEpisode(entity: EpisodeEntity): Long

    @Query("DELETE FROM season WHERE seriesId = :seriesId")
    abstract suspend fun deleteSeasons(seriesId: Long)

    @Transaction
    open suspend fun replaceSeasons(
        seriesId: Long,
        seasons: List<Pair<SeasonEntity, List<EpisodeEntity>>>,
    ) {
        deleteSeasons(seriesId)
        for ((season, episodes) in seasons) {
            val seasonId = insertSeason(season.copy(id = 0, seriesId = seriesId))
            episodes.forEach { episode ->
                insertEpisode(episode.copy(id = 0, seriesId = seriesId, seasonId = seasonId))
            }
        }
    }
}

data class RemoteIdRow(val playlistId: Long, val remoteId: String, val id: Long)
