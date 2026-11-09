package dev.anon.novatv.core.database.mapper

import dev.anon.novatv.core.database.entity.ChannelEntity
import dev.anon.novatv.core.database.entity.EpgProgramEntity
import dev.anon.novatv.core.database.entity.PlaylistEntity
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.EpgProgram
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.SearchText
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
