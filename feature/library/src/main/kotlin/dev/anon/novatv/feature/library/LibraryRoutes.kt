package dev.anon.novatv.feature.library

/** Kütüphane rotaları. */
object LibraryRoutes {
    const val ROUTE = "library"
    const val ARG_SERIES_ID = "seriesId"
    const val SERIES_PATTERN = "library/series/{$ARG_SERIES_ID}"

    fun series(seriesId: Long): String = "library/series/$seriesId"
}
