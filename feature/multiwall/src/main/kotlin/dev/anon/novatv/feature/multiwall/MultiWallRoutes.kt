package dev.anon.novatv.feature.multiwall

/** Çoklu ekran rotası; kutu sayısı argümanla açılır (2 veya 4). */
object MultiWallRoutes {
    const val ARG_COUNT = "count"
    const val ROUTE = "multiwall"
    const val PATTERN = "multiwall?$ARG_COUNT={$ARG_COUNT}"

    fun of(count: Int = 4): String = "multiwall?$ARG_COUNT=${if (count == 2) 2 else 4}"
}
