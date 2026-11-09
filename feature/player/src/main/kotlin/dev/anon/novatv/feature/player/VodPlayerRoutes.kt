package dev.anon.novatv.feature.player

/** Film ve dizi bölümü oynatma rotası; kimlik taşır, URL taşımaz. */
object VodPlayerRoutes {

    const val ARG_MOVIE_ID = "movieId"
    const val ARG_SERIES_ID = "seriesId"
    const val ARG_EPISODE_ID = "episodeId"

    const val PATTERN =
        "vod?$ARG_MOVIE_ID={$ARG_MOVIE_ID}" +
            "&$ARG_SERIES_ID={$ARG_SERIES_ID}" +
            "&$ARG_EPISODE_ID={$ARG_EPISODE_ID}"

    fun movie(movieId: Long): String =
        "vod?$ARG_MOVIE_ID=$movieId&$ARG_SERIES_ID=-1&$ARG_EPISODE_ID=-1"

    fun episode(seriesId: Long, episodeId: Long): String =
        "vod?$ARG_MOVIE_ID=-1&$ARG_SERIES_ID=$seriesId&$ARG_EPISODE_ID=$episodeId"
}
