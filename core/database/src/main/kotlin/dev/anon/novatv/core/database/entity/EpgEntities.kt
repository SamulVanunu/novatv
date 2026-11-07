package dev.anon.novatv.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "epg_channel",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["playlistId", "tvgId"], unique = true)],
)
data class EpgChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val tvgId: String,
    val displayName: String,
    val iconUrl: String?,
)

@Entity(
    tableName = "epg_program",
    foreignKeys = [
        ForeignKey(
            entity = EpgChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["epgChannelId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["epgChannelId", "startAt"]),
        Index(value = ["channelId", "startAt"]),
    ],
)
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epgChannelId: Long,
    val channelId: Long?,
    val startAt: Long,
    val endAt: Long,
    val title: String,
    val subtitle: String?,
    val description: String?,
    val category: String?,
    val rating: String?,
    val episodeNum: String?,
    val isNew: Boolean,
)
