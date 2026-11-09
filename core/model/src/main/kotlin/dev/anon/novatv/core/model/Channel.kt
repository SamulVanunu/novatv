package dev.anon.novatv.core.model

enum class ItemType { CHANNEL, MOVIE, SERIES, EPISODE }

data class Channel(
    val id: Long = 0,
    val playlistId: Long,
    val remoteId: String,
    val number: Int? = null,
    val name: String,
    val groupName: String,
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val hasArchive: Boolean = false,
    val archiveDays: Int = 0,
    val format: StreamFormat = StreamFormat.AUTO,
    /** Listede kanal başına tanımlı akış başlıkları (#EXTVLCOPT). */
    val streamUserAgent: String? = null,
    val streamReferer: String? = null,
)
