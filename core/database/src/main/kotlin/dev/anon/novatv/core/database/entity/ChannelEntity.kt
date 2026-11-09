package dev.anon.novatv.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channel",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["playlistId", "remoteId"], unique = true),
        Index(value = ["playlistId", "groupName"]),
        Index(value = ["playlistId", "nameNormalized"]),
    ],
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val remoteId: String,
    val number: Int?,
    val name: String,
    val nameNormalized: String,
    val groupName: String,
    val logoUrl: String?,
    val tvgId: String?,
    val hasArchive: Boolean,
    val archiveDays: Int,
    val streamFormat: String,
    val syncToken: Long,
    val streamUserAgent: String? = null,
    val streamReferer: String? = null,
)

data class ChannelIdRow(val id: Long, val remoteId: String)
