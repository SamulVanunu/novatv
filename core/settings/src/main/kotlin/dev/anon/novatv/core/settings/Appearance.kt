package dev.anon.novatv.core.settings

/** Görünüm ve sistem tercihleri; tek akışta toplanır. */
data class Appearance(
    val dark: Boolean = true,
    val accent: Accent = Accent.AMBER,
    val typeScale: Float = 1f,
    val clock24h: Boolean = true,
    val epgRowHeightDp: Int = 64,
    val epgWindowDays: Int = 3,
    val autoFrameRate: Boolean = false,
    val autoStart: Boolean = false,
    val sleepMinutes: Int = 0,
) {
    val epgRowHeight: Int get() = epgRowHeightDp.coerceIn(40, 110)
    val windowDays: Int get() = epgWindowDays.coerceIn(1, 7)
    val scale: Float get() = typeScale.coerceIn(0.85f, 1.35f)
}

enum class Accent { AMBER, TEAL, ROSE, BLUE }
