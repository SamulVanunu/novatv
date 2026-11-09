package dev.anon.novatv.feature.player

import dev.anon.novatv.core.sync.ChannelRepository

/** Oynatıcı rotası ve "son kanal" açılış hedefi. */
object PlayerRoutes {

    const val ARG_CHANNEL_ID = "channelId"
    const val ARG_CATCHUP_START = "catchupStart"
    const val ARG_CATCHUP_MINUTES = "catchupMinutes"

    const val PATTERN =
        "player?$ARG_CHANNEL_ID={$ARG_CHANNEL_ID}" +
            "&$ARG_CATCHUP_START={$ARG_CATCHUP_START}" +
            "&$ARG_CATCHUP_MINUTES={$ARG_CATCHUP_MINUTES}"

    fun of(channelId: Long): String =
        "player?$ARG_CHANNEL_ID=$channelId&$ARG_CATCHUP_START=-1&$ARG_CATCHUP_MINUTES=0"

    /** Arşivden (catch-up) oynatma; [startAt] program başlangıcı, [minutes] süre. */
    fun catchup(channelId: Long, startAt: Long, minutes: Int): String =
        "player?$ARG_CHANNEL_ID=$channelId" +
            "&$ARG_CATCHUP_START=$startAt" +
            "&$ARG_CATCHUP_MINUTES=${minutes.coerceAtLeast(1)}"

    /** Uygulama açılışında son izlenen kanalın rotası; yoksa null. */
    suspend fun lastWatched(repository: ChannelRepository): String? =
        repository.lastChannel()?.let { of(it.id) }
}
