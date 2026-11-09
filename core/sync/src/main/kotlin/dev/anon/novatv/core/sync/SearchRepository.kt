package dev.anon.novatv.core.sync

import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.model.SearchText
import dev.anon.novatv.core.settings.PreferenceStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

enum class SearchKind { CHANNEL, MOVIE, SERIES, PROGRAM }

data class SearchHit(
    val kind: SearchKind,
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val startsAt: Long? = null,
)

/**
 * Tek arama: kanal, film, dizi ve rehber başlığı.
 * Rehber sonuçları yalnızca önümüzdeki programları kapsar.
 */
class SearchRepository(
    private val db: NovaDatabase,
    private val preferences: PreferenceStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    fun search(rawQuery: String): Flow<List<SearchHit>> {
        val query = SearchText.normalize(rawQuery.trim())
        if (query.isEmpty()) return flowOf(emptyList())

        return combine(
            db.playlistDao().observeActive(),
            preferences.activeProfileId,
        ) { active, profileId -> active.map { it.id } to profileId }
            .flatMapLatest { (playlistIds, profileId) ->
                if (playlistIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        db.channelDao().search(playlistIds, query, profileId, LIMIT),
                        db.libraryDao().searchMovies(playlistIds, query, LIMIT),
                        db.libraryDao().searchSeries(playlistIds, query, LIMIT),
                        db.epgDao().searchWithChannel(query, clock(), LIMIT),
                    ) { channels, movies, series, programs ->
                        buildList {
                            channels.forEach { channel ->
                                add(
                                    SearchHit(
                                        kind = SearchKind.CHANNEL,
                                        id = channel.id,
                                        title = channel.name,
                                        subtitle = channel.groupName,
                                        imageUrl = channel.logoUrl,
                                    ),
                                )
                            }
                            movies.forEach { movie ->
                                add(
                                    SearchHit(
                                        kind = SearchKind.MOVIE,
                                        id = movie.id,
                                        title = movie.name,
                                        subtitle = movie.year?.toString() ?: "Film",
                                        imageUrl = movie.posterUrl,
                                    ),
                                )
                            }
                            series.forEach { item ->
                                add(
                                    SearchHit(
                                        kind = SearchKind.SERIES,
                                        id = item.id,
                                        title = item.name,
                                        subtitle = item.year?.toString() ?: "Dizi",
                                        imageUrl = item.posterUrl,
                                    ),
                                )
                            }
                            programs.forEach { row ->
                                add(
                                    SearchHit(
                                        kind = SearchKind.PROGRAM,
                                        id = row.program.channelId ?: 0,
                                        title = row.program.title,
                                        subtitle = row.channelName + " · " +
                                            DAY_FORMAT.format(
                                                Instant.ofEpochMilli(row.program.startAt)
                                                    .atZone(ZoneId.systemDefault()),
                                            ),
                                        startsAt = row.program.startAt,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
    }

    private companion object {
        const val LIMIT = 30
        val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
