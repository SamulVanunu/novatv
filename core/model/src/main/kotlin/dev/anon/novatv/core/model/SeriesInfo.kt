package dev.anon.novatv.core.model

/**
 * API'den gelen dizi ağacı. Veritabanına yazılmadan önce kimlikler 0'dır;
 * kalıcılaştırma sırasında atanır.
 */
data class SeriesInfo(
    val series: Series,
    val seasons: List<SeasonInfo>,
)

data class SeasonInfo(
    val number: Int,
    val name: String? = null,
    val episodes: List<EpisodeInfo> = emptyList(),
)

data class EpisodeInfo(
    val remoteId: String,
    val number: Int,
    val title: String,
    val plot: String? = null,
    val durationMin: Int? = null,
    val containerExt: String? = null,
    val stillUrl: String? = null,
)
