package dev.anon.novatv.core.model

enum class PlaylistType { XTREAM, M3U, STALKER }

enum class StreamFormat { AUTO, HLS, TS }

enum class SyncState { NEVER, RUNNING, OK, PARTIAL, FAILED }

data class Playlist(
    val id: Long = 0,
    val type: PlaylistType,
    val name: String,
    val baseUrl: String,
    val username: String? = null,
    val epgUrl: String? = null,
    val userAgent: String? = null,
    val referer: String? = null,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val lastSyncAt: Long? = null,
    val syncState: SyncState = SyncState.NEVER,
    val syncIntervalHours: Int = 24,
    /** Son senkron hatasının kullanıcıya gösterilebilir, arındırılmış metni. */
    val lastError: String? = null,
)
