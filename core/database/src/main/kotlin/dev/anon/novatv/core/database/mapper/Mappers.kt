package dev.anon.novatv.core.database.mapper

import dev.anon.novatv.core.database.entity.ChannelEntity
import dev.anon.novatv.core.database.entity.EpgProgramEntity
import dev.anon.novatv.core.database.entity.EpisodeEntity
import dev.anon.novatv.core.database.entity.MovieEntity
import dev.anon.novatv.core.database.entity.PlaylistEntity
import dev.anon.novatv.core.database.entity.SeasonEntity
import dev.anon.novatv.core.database.entity.SeriesEntity
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.Episode
import dev.anon.novatv.core.model.EpgProgram
import dev.anon.novatv.core.model.Movie
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.SearchText
import dev.anon.novatv.core.model.Season
import dev.anon.novatv.core.model.Series
import dev.anon.novatv.core.model.StreamFormat
import dev.anon.novatv.core.model.SyncState

fun PlaylistEntity.toModel(): Playlist = Playlist(
    id = id,
    type = runCatching { PlaylistType.valueOf(type) }.getOrDefault(PlaylistType.M3U),
    name = name,
    baseUrl = baseUrl,
    username = username,
    epgUrl = epgUrl,
    userAgent = userAgent,
    referer = referer,
    isActive = isActive,
    sortOrder = sortOrder,
    lastSyncAt = lastSyncAt,
    syncState = runCatching { SyncState.valueOf(syncState) }.getOrDefault(SyncState.NEVER),
    syncIntervalHours = syncIntervalHours,
    lastError = lastError,
)

fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    type = type.name,
    name = name,
    baseUrl = baseUrl,
    username = username,
    epgUrl = epgUrl,
    userAgent = userAgent,
    referer = referer,
    isActive = isActive,
    sortOrder = sortOrder,
    lastSyncAt = lastSyncAt,
    syncState = syncState.name,
    syncIntervalHours = syncIntervalHours,
    lastError = lastError,
)

fun ChannelEntity.toModel(): Channel = Channel(
    id = id,
    playlistId = playlistId,
    remoteId = remoteId,
    number = number,
    name = name,
    groupName = groupName,
    logoUrl = logoUrl,
    tvgId = tvgId,
    hasArchive = hasArchive,
    archiveDays = archiveDays,
    format = runCatching { StreamFormat.valueOf(streamFormat) }.getOrDefault(StreamFormat.AUTO),
    streamUserAgent = streamUserAgent,
    streamReferer = streamReferer,
)

fun Channel.toEntity(syncToken: Long): ChannelEntity = ChannelEntity(
    id = id,
    playlistId = playlistId,
    remoteId = remoteId,
    number = number,
    name = name,
    nameNormalized = SearchText.normalize(name),
    groupName = groupName,
    logoUrl = logoUrl,
    tvgId = tvgId,
    hasArchive = hasArchive,
    archiveDays = archiveDays,
    streamFormat = format.name,
    syncToken = syncToken,
    streamUserAgent = streamUserAgent,
    streamReferer = streamReferer,
)

fun EpgProgramEntity.toModel(): EpgProgram = EpgProgram(
    id = id,
    epgChannelId = epgChannelId,
    channelId = channelId,
    startAt = startAt,
    endAt = endAt,
    title = title,
    subtitle = subtitle,
    description = description,
    category = category,
    rating = rating,
    episodeNum = episodeNum,
    isNew = isNew,
)

fun MovieEntity.toModel(): Movie = Movie(
    id = id,
    playlistId = playlistId,
    remoteId = remoteId,
    name = name,
    year = year,
    rating = rating,
    genres = genres.toGenres(),
    plot = plot,
    posterUrl = posterUrl,
    durationMin = durationMin,
    containerExt = containerExt,
)

fun Movie.toEntity(): MovieEntity = MovieEntity(
    id = id,
    playlistId = playlistId,
    remoteId = remoteId,
    name = name,
    nameNormalized = SearchText.normalize(name),
    year = year,
    rating = rating,
    genres = genres.toCsv(),
    plot = plot,
    posterUrl = posterUrl,
    durationMin = durationMin,
    containerExt = containerExt,
)

fun SeriesEntity.toModel(): Series = Series(
    id = id,
    playlistId = playlistId,
    remoteId = remoteId,
    name = name,
    year = year,
    rating = rating,
    genres = genres.toGenres(),
    plot = plot,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    episodeCount = episodeCount,
)

fun Series.toEntity(): SeriesEntity = SeriesEntity(
    id = id,
    playlistId = playlistId,
    remoteId = remoteId,
    name = name,
    nameNormalized = SearchText.normalize(name),
    year = year,
    rating = rating,
    genres = genres.toCsv(),
    plot = plot,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    episodeCount = episodeCount,
)

fun SeasonEntity.toModel(): Season = Season(
    id = id,
    seriesId = seriesId,
    number = number,
    name = name,
)

fun EpisodeEntity.toModel(): Episode = Episode(
    id = id,
    seriesId = seriesId,
    seasonId = seasonId,
    remoteId = remoteId,
    number = number,
    title = title,
    plot = plot,
    durationMin = durationMin,
    containerExt = containerExt,
    stillUrl = stillUrl,
)

private fun String?.toGenres(): List<String> =
    this?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

private fun List<String>.toCsv(): String? =
    map { it.trim() }?.filter { it.isNotEmpty() }?.joinToString(",")?.takeIf { it.isNotEmpty() }
