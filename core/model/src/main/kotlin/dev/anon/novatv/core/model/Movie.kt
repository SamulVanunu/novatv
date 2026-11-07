package dev.anon.novatv.core.model

data class Movie(
    val id: Long = 0,
    val playlistId: Long,
    val remoteId: String,
    val name: String,
    val year: Int? = null,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    val plot: String? = null,
    val posterUrl: String? = null,
    val durationMin: Int? = null,
    val containerExt: String? = null,
)

data class Series(
    val id: Long = 0,
    val playlistId: Long,
    val remoteId: String,
    val name: String,
    val year: Int? = null,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    val plot: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val episodeCount: Int? = null,
)

data class Season(
    val id: Long = 0,
    val seriesId: Long,
    val number: Int,
    val name: String? = null,
)

data class Episode(
    val id: Long = 0,
    val seriesId: Long,
    val seasonId: Long,
    val remoteId: String,
    val number: Int,
    val title: String,
    val plot: String? = null,
    val durationMin: Int? = null,
    val containerExt: String? = null,
    val stillUrl: String? = null,
)
