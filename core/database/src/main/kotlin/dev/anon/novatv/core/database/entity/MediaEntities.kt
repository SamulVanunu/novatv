package dev.anon.novatv.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movie",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["playlistId", "remoteId"], unique = true)],
)
data class MovieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val remoteId: String,
    val name: String,
    val nameNormalized: String,
    val year: Int?,
    val rating: Double?,
    val genres: String?,
    val plot: String?,
    val posterUrl: String?,
    val durationMin: Int?,
    val containerExt: String?,
)

@Entity(
    tableName = "series",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["playlistId", "remoteId"], unique = true)],
)
data class SeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val remoteId: String,
    val name: String,
    val nameNormalized: String,
    val year: Int?,
    val rating: Double?,
    val genres: String?,
    val plot: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val episodeCount: Int?,
)

@Entity(
    tableName = "season",
    foreignKeys = [
        ForeignKey(
            entity = SeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["seriesId", "number"], unique = true)],
)
data class SeasonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesId: Long,
    val number: Int,
    val name: String?,
)

@Entity(
    tableName = "episode",
    foreignKeys = [
        ForeignKey(
            entity = SeasonEntity::class,
            parentColumns = ["id"],
            childColumns = ["seasonId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["seasonId", "number"]), Index(value = ["seriesId"])],
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesId: Long,
    val seasonId: Long,
    val remoteId: String,
    val number: Int,
    val title: String,
    val plot: String?,
    val durationMin: Int?,
    val containerExt: String?,
    val stillUrl: String?,
)
