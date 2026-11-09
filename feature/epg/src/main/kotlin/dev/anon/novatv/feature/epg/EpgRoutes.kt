package dev.anon.novatv.feature.epg

/** Rehber rotası; gün sekmesi argümanla taşınır (0 = bugün). */
object EpgRoutes {
    const val ARG_DAY = "day"
    const val PATTERN = "epg?$ARG_DAY={$ARG_DAY}"
    const val ROUTE = "epg"

    fun of(dayOffset: Int = 0): String = "epg?$ARG_DAY=$dayOffset"
}
