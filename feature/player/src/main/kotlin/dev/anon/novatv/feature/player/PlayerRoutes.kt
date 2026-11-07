package dev.anon.novatv.feature.player

import dev.anon.novatv.core.sync.ChannelRepository

/** Oynatıcı rotası ve "son kanal" açılış hedefi. */
object PlayerRoutes {

    const val ARG_CHANNEL_ID = "channelId"
    const val PATTERN = "player?$ARG_CHANNEL_ID={$ARG_CHANNEL_ID}"

    fun of(channelId: Long): String = "player?$ARG_CHANNEL_ID=$channelId"

    /** Uygulama açılışında son izlenen kanalın rotası; yoksa null. */
    suspend fun lastWatched(repository: ChannelRepository): String? =
        repository.lastChannel()?.let { of(it.id) }
}
