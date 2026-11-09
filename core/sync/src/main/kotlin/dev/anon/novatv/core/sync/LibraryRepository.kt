package dev.anon.novatv.core.sync

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.database.entity.EpisodeEntity
import dev.anon.novatv.core.database.entity.SeasonEntity
import dev.anon.novatv.core.database.mapper.toEntity
import dev.anon.novatv.core.database.mapper.toModel
import dev.anon.novatv.core.model.Episode
import dev.anon.novatv.core.model.ItemType
import dev.anon.novatv.core.model.Movie
import dev.anon.novatv.core.model.SearchText
import dev.anon.novatv.core.model.Season
import dev.anon.novatv.core.model.Series
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.source.SourceAdapterRegistry
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class LibraryFilter(
    val query: String = "",
    val genre: String? = null,
    val year: Int? = null,
)

data class SeriesDetail(
    val series: Series,
    val seasons: List<Season>,
    val episodes: List<Episode>,
)

data class ContinueItem(
    val itemType: ItemType,
    val itemId: Long,
    val seriesId: Long? = null,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
) {
    val progress: Float
        get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

data class LibrarySyncResult(val movies: Int = 0, val series: Int = 0)

interface LibraryRepository {
    fun movies(filter: LibraryFilter): Flow<PagingData<Movie>>
    fun series(filter: LibraryFilter): Flow<PagingData<Series>>
    fun genres(): Flow<List<String>>
    fun years(): Flow<List<Int>>
    suspend fun movie(id: Long): Movie?
    suspend fun series(id: Long): Series?
    fun seriesDetail(id: Long): Flow<SeriesDetail?>
    suspend fun refreshSeries(id: Long)
    suspend fun sync(playlistId: Long): LibrarySyncResult
    suspend fun syncAll(): LibrarySyncResult
    fun continueWatching(limit: Int = 12): Flow<List<ContinueItem>>
    suspend fun recordWatch(itemType: ItemType, itemId: Long, positionMs: Long, durationMs: Long)
    suspend fun resumePosition(itemType: ItemType, itemId: Long): Long
    fun favoriteIds(itemType: ItemType): Flow<Set<Long>>
    suspend fun toggleFavorite(itemType: ItemType, itemId: Long)
    suspend fun movieStream(movie: Movie): ResolvedStream
    suspend fun episodeStream(seriesId: Long, episode: Episode): ResolvedStream
}

class RoomLibraryRepository(
    private val db: NovaDatabase,
    private val playlists: PlaylistRepository,
    private val registry: SourceAdapterRegistry,
    private val streamResolver: StreamResolver,
    private val preferences: PreferenceStore,
) : LibraryRepository {

    private val clock = AtomicLong(System.currentTimeMillis())

    override fun movies(filter: LibraryFilter): Flow<PagingData<Movie>> =
        activePlaylistIds().flatMapLatest { ids ->
            if (ids.isEmpty()) {
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
                    db.libraryDao().moviePaging(
                        playlistIds = ids,
                        q = SearchText.normalize(filter.query),
                        genre = filter.genre,
                        year = filter.year,
                    )
                }.flow.map { page -> page.map { it.toModel() } }
            }
        }

    override fun series(filter: LibraryFilter): Flow<PagingData<Series>> =
        activePlaylistIds().flatMapLatest { ids ->
            if (ids.isEmpty()) {
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
                    db.libraryDao().seriesPaging(
                        playlistIds = ids,
                        q = SearchText.normalize(filter.query),
                        genre = filter.genre,
                        year = filter.year,
                    )
                }.flow.map { page -> page.map { it.toModel() } }
            }
        }

    override fun genres(): Flow<List<String>> =
        activePlaylistIds().flatMapLatest { ids ->
            combine(
                db.libraryDao().movieGenres(ids),
                db.libraryDao().seriesGenres(ids),
            ) { movieGenres, seriesGenres ->
                (movieGenres + seriesGenres)
                    .flatMap { it.split(',') }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()
            }
        }

    override fun years(): Flow<List<Int>> =
        activePlaylistIds().flatMapLatest { ids ->
            combine(
                db.libraryDao().movieYears(ids),
                db.libraryDao().seriesYears(ids),
            ) { movieYears, seriesYears -> (movieYears + seriesYears).distinct().sortedDescending() }
        }

    override suspend fun movie(id: Long): Movie? = db.libraryDao().movieById(id)?.toModel()

    override suspend fun series(id: Long): Series? = db.libraryDao().seriesById(id)?.toModel()

    override fun seriesDetail(id: Long): Flow<SeriesDetail?> =
        combine(
            db.libraryDao().observeSeries(id),
            db.libraryDao().seasons(id),
            db.libraryDao().episodes(id),
        ) { series, seasons, episodes ->
            series?.let {
                SeriesDetail(
                    series = it.toModel(),
                    seasons = seasons.map { season -> season.toModel() },
                    episodes = episodes.map { episode -> episode.toModel() },
                )
            }
        }

    override suspend fun refreshSeries(id: Long) {
        val series = db.libraryDao().seriesById(id) ?: return
        if (db.libraryDao().episodeCount(id) > 0) return
        val (playlist, credentials) = playlists.load(series.playlistId)
        val info = registry.adapterFor(playlist, credentials).seriesInfo(series.remoteId) ?: return

        val seasons = info.seasons.sortedBy { it.number }.map { season ->
            SeasonEntity(
                seriesId = id,
                number = season.number,
                name = season.name,
            ) to season.episodes.map { episode ->
                EpisodeEntity(
                    seriesId = id,
                    seasonId = 0,
                    remoteId = episode.remoteId,
                    number = episode.number,
                    title = episode.title,
                    plot = episode.plot,
                    durationMin = episode.durationMin,
                    containerExt = episode.containerExt,
                    stillUrl = episode.stillUrl,
                )
            }
        }
        db.libraryDao().replaceSeasons(id, seasons)
    }

    override suspend fun syncAll(): LibrarySyncResult {
        val active = db.playlistDao().observeActive().first()
        var movies = 0
        var series = 0
        active.forEach { playlist ->
            val result = runCatching { sync(playlist.id) }.getOrElse { LibrarySyncResult() }
            movies += result.movies
            series += result.series
        }
        return LibrarySyncResult(movies = movies, series = series)
    }

    override suspend fun sync(playlistId: Long): LibrarySyncResult {
        val (playlist, credentials) = playlists.load(playlistId)
        if (!playlist.isActive) return LibrarySyncResult()
        val adapter = registry.adapterFor(playlist, credentials)
        val movies = adapter.vod()
        db.libraryDao().replaceMovies(
            playlistId,
            movies.map { it.copy(playlistId = playlistId).toEntity() },
        )
        val series = adapter.series()
        db.libraryDao().replaceSeries(
            playlistId,
            series.map { it.copy(playlistId = playlistId).toEntity() },
        )
        return LibrarySyncResult(movies = movies.size, series = series.size)
    }

    override fun continueWatching(limit: Int): Flow<List<ContinueItem>> =
        preferences.activeProfileId.flatMapLatest { profileId ->
            combine(
                db.userDataDao().continueMovies(profileId, limit),
                db.userDataDao().continueEpisodes(profileId, limit),
            ) { movies, episodes ->
                (movies.map { row ->
                    ContinueItem(
                        itemType = ItemType.MOVIE,
                        itemId = row.movie.id,
                        title = row.movie.name,
                        subtitle = row.movie.year?.toString(),
                        imageUrl = row.movie.posterUrl,
                        positionMs = row.positionMs,
                        durationMs = row.durationMs,
                    )
                } + episodes.map { row ->
                    ContinueItem(
                        itemType = ItemType.EPISODE,
                        itemId = row.episode.id,
                        seriesId = row.episode.seriesId,
                        title = row.episode.title,
                        subtitle = "Bölüm ${row.episode.number}",
                        imageUrl = row.episode.stillUrl,
                        positionMs = row.positionMs,
                        durationMs = row.durationMs,
                    )
                }).sortedByDescending { it.itemId }.take(limit)
            }
        }

    override suspend fun recordWatch(
        itemType: ItemType,
        itemId: Long,
        positionMs: Long,
        durationMs: Long,
    ) {
        db.userDataDao().upsertHistory(
            dev.anon.novatv.core.database.entity.WatchHistoryEntity(
                profileId = preferences.activeProfileId.first(),
                itemType = itemType.name,
                itemId = itemId,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = clock.incrementAndGet(),
            ),
        )
    }

    override suspend fun resumePosition(itemType: ItemType, itemId: Long): Long {
        val profileId = preferences.activeProfileId.first()
        val history = db.userDataDao().history(profileId, itemType.name, itemId) ?: return 0L
        return if (history.durationMs > 0 && history.positionMs < history.durationMs * 0.95) {
            history.positionMs
        } else {
            0L
        }
    }

    override fun favoriteIds(itemType: ItemType): Flow<Set<Long>> =
        preferences.activeProfileId.flatMapLatest { profileId ->
            db.userDataDao().observeFavoriteIds(profileId, itemType.name)
                .map { ids -> ids.toSet() }
        }

    override suspend fun toggleFavorite(itemType: ItemType, itemId: Long) {
        db.userDataDao().toggleFavorite(
            profileId = preferences.activeProfileId.first(),
            itemType = itemType.name,
            itemId = itemId,
        )
    }

    override suspend fun movieStream(movie: Movie): ResolvedStream = streamResolver.resolveMovie(movie)

    override suspend fun episodeStream(seriesId: Long, episode: Episode): ResolvedStream {
        val playlistId = db.libraryDao().seriesById(seriesId)?.playlistId
            ?: throw IllegalStateException("Dizi bulunamadı")
        return streamResolver.resolveEpisode(episode, playlistId)
    }

    private fun activePlaylistIds(): Flow<List<Long>> =
        combine(
            db.playlistDao().observeActive(),
            preferences.activeProfileId,
        ) { active, _ -> active.map { it.id } }

    private companion object {
        const val PAGE_SIZE = 60
    }
}
