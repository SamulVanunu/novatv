package dev.anon.novatv.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val name: String,
    val baseUrl: String,
    val username: String?,
    val epgUrl: String?,
    val userAgent: String?,
    val isActive: Boolean,
    val sortOrder: Int,
    val lastSyncAt: Long?,
    val syncState: String,
    val syncIntervalHours: Int,
)
