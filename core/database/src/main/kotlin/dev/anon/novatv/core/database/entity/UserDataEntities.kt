package dev.anon.novatv.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pinHash: String?,
    val maxRating: String?,
    val isKids: Boolean,
)

@Entity(
    tableName = "favorite",
    primaryKeys = ["profileId", "itemType", "itemId"],
    indices = [Index(value = ["profileId", "itemType"])],
)
data class FavoriteEntity(
    val profileId: Long,
    val itemType: String,
    val itemId: Long,
)

@Entity(tableName = "hidden_channel", primaryKeys = ["profileId", "channelId"])
data class HiddenChannelEntity(
    val profileId: Long,
    val channelId: Long,
)

@Entity(tableName = "hidden_group", primaryKeys = ["profileId", "playlistId", "groupName"])
data class HiddenGroupEntity(
    val profileId: Long,
    val playlistId: Long,
    val groupName: String,
)

@Entity(
    tableName = "watch_history",
    primaryKeys = ["profileId", "itemType", "itemId"],
    indices = [Index(value = ["profileId", "updatedAt"])],
)
data class WatchHistoryEntity(
    val profileId: Long,
    val itemType: String,
    val itemId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)
