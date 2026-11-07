package dev.anon.novatv.core.source.xstream

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.EpisodeInfo
import dev.anon.novatv.core.model.Movie
import dev.anon.novatv.core.model.SeasonInfo
import dev.anon.novatv.core.model.Series
import dev.anon.novatv.core.model.SeriesInfo
import dev.anon.novatv.core.model.SourceAccount
import dev.anon.novatv.core.network.SourceAuthException
import dev.anon.novatv.core.network.SourceHttp
import dev.anon.novatv.core.network.SourceProtocolException
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class XtreamClient(
    private val http: SourceHttp,
    val baseUrl: String,
    val username: String,
    val password: String,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    suspend fun authenticate(): SourceAccount {
        val dto = decode<AuthResponseDto>(apiUrl())
        val status = dto.userInfo?.status
        if (status.isNullOrBlank()) throw SourceProtocolException("Panel geçerli JSON döndürmedi")
        if (!status.equals("Active", ignoreCase = true)) {
            throw SourceAuthException("Hesap aktif değil: $status")
        }
        val expirySeconds = dto.userInfo.expDate
        return SourceAccount(
            status = status,
            expiresAt = expirySeconds?.takeIf { it > 0 }?.let { if (it < 100_000_000_000L) it * 1000 else it },
            maxConnections = dto.userInfo.maxConnections,
            serverInfo = dto.serverInfo?.let { server ->
                listOfNotNull(server.serverProtocol, server.url, server.port).joinToString("://")
                    .ifBlank { null }
            },
        )
    }

    suspend fun live(onProgress: (Int) -> Unit = {}): List<Channel> {
        val categories = runCatching {
            decode<List<CategoryDto>>(apiUrl("get_live_categories"))
        }.getOrDefault(emptyList())
        val groupByCategory = categories.associate { it.categoryId to it.categoryName }

        val streams = decode<List<LiveStreamDto>>(apiUrl("get_live_streams"))
        val channels = ArrayList<Channel>(streams.size)
        for (dto in streams) {
            if (dto.streamId.isBlank() || dto.name.isBlank()) continue
            val archiveDays = dto.tvArchiveDuration ?: 0
            channels += Channel(
                playlistId = 0,
                remoteId = dto.streamId,
                number = dto.num,
                name = dto.name,
                groupName = groupByCategory[dto.categoryId]?.takeIf { it.isNotBlank() } ?: DEFAULT_GROUP,
                logoUrl = dto.streamIcon,
                tvgId = dto.epgChannelId,
                hasArchive = (dto.tvArchive ?: 0) > 0,
                archiveDays = archiveDays,
            )
            if (channels.size % PROGRESS_STEP == 0) onProgress(channels.size)
        }
        onProgress(channels.size)
        return channels
    }

    suspend fun vod(): List<Movie> = decode<List<VodStreamDto>>(apiUrl("get_vod_streams"))
        .filter { it.streamId.isNotBlank() && it.name.isNotBlank() }
        .map { dto ->
            Movie(
                playlistId = 0,
                remoteId = dto.streamId,
                name = dto.name,
                year = dto.year,
                rating = dto.rating?.trim()?.toDoubleOrNull() ?: dto.rating5Based,
                genres = dto.genre.splitGenres(),
                plot = dto.plot,
                posterUrl = dto.streamIcon,
                containerExt = dto.containerExtension,
            )
        }

    suspend fun series(): List<Series> = decode<List<SeriesDto>>(apiUrl("get_series"))
        .filter { it.seriesId.isNotBlank() && it.name.isNotBlank() }
        .map { it.toModel(remoteId = it.seriesId) }

    suspend fun seriesInfo(seriesId: String): SeriesInfo {
        val dto = decode<SeriesInfoDto>(apiUrl("get_series_info", "series_id" to seriesId))
        val series = dto.info?.toModel(remoteId = seriesId)
            ?: Series(playlistId = 0, remoteId = seriesId, name = seriesId)

        val seasons = dto.seasons.orEmpty().mapIndexed { index, season ->
            val number = season.seasonNumber ?: (index + 1)
            val episodes = dto.episodes?.get(number.toString())
                ?: dto.episodes?.get(season.seasonNumber?.toString().orEmpty())
                ?: emptyList()
            SeasonInfo(
                number = number,
                name = season.name,
                episodes = episodes
                    .filter { it.id.isNotBlank() }
                    .map { episode ->
                        EpisodeInfo(
                            remoteId = episode.id,
                            number = episode.episodeNum ?: 0,
                            title = episode.title.ifBlank { "Bölüm ${episode.episodeNum ?: 0}" },
                            plot = episode.info?.plot,
                            durationMin = episode.info?.durationSecs?.takeIf { it > 0 }?.div(60),
                            containerExt = episode.containerExtension,
                            stillUrl = episode.info?.movieImage,
                        )
                    },
            )
        }
        return SeriesInfo(series = series, seasons = seasons)
    }

    fun epgUrl(): String = "$baseUrl/xmltv.php?username=${encode(username)}&password=${encode(password)}"

    fun liveUrl(streamId: String, ext: String): String =
        "$baseUrl/live/$username/$password/$streamId.$ext"

    fun vodUrl(streamId: String, ext: String): String =
        "$baseUrl/movie/$username/$password/$streamId.$ext"

    fun seriesUrl(episodeId: String, ext: String): String =
        "$baseUrl/series/$username/$password/$episodeId.$ext"

    fun catchupUrl(streamId: String, at: Instant, minutes: Int): String {
        val start = CATCHUP_TIME_FORMAT.format(at.atZone(CATCHUP_ZONE))
        return "$baseUrl/timeshift/$username/$password/$minutes/$start/$streamId.ts"
    }

    private fun apiUrl(action: String? = null, extra: Pair<String, String>? = null): String = buildString {
        append(baseUrl).append("/player_api.php?username=").append(encode(username))
        append("&password=").append(encode(password))
        if (action != null) append("&action=").append(action)
        if (extra != null) append('&').append(extra.first).append('=').append(encode(extra.second))
    }

    private suspend inline fun <reified T> decode(url: String): T {
        val text = http.getText(url)
        return try {
            json.decodeFromString<T>(text)
        } catch (e: Exception) {
            throw SourceProtocolException("Panel geçerli JSON döndürmedi", e)
        }
    }

    private fun SeriesDto.toModel(remoteId: String) = Series(
        playlistId = 0,
        remoteId = remoteId,
        name = name,
        year = releaseDate?.take(4)?.toIntOrNull(),
        rating = rating?.trim()?.toDoubleOrNull(),
        genres = genre.splitGenres(),
        plot = plot,
        posterUrl = cover,
        backdropUrl = backdropPath?.firstOrNull(),
    )

    private fun String?.splitGenres(): List<String> =
        this?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    private companion object {
        const val DEFAULT_GROUP = "Genel"
        const val PROGRESS_STEP = 500
        val CATCHUP_ZONE: ZoneId = ZoneId.of("Europe/Istanbul")
        val CATCHUP_TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm", Locale.ROOT)

        fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    }
}
