package dev.anon.novatv.core.model

data class EpgChannel(
    val id: Long = 0,
    val playlistId: Long,
    val tvgId: String,
    val displayName: String,
    val iconUrl: String? = null,
)

data class EpgProgram(
    val id: Long = 0,
    val epgChannelId: Long,
    val channelId: Long? = null,
    val startAt: Long,
    val endAt: Long,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val category: String? = null,
    val rating: String? = null,
    val episodeNum: String? = null,
    val isNew: Boolean = false,
)
