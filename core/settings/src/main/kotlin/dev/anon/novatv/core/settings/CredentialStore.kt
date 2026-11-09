package dev.anon.novatv.core.settings

import dev.anon.novatv.core.model.Credentials

interface CredentialStore {
    suspend fun put(playlistId: Long, credentials: Credentials)
    suspend fun get(playlistId: Long): Credentials?
    suspend fun remove(playlistId: Long)

    /** M3U gibi kimlik bilgisini URL'de taşıyan kaynakların tam adresi. */
    suspend fun putSourceUrl(playlistId: Long, url: String)
    suspend fun getSourceUrl(playlistId: Long): String?
    suspend fun removeSourceUrl(playlistId: Long)
}
