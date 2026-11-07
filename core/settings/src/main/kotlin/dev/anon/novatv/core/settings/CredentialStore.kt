package dev.anon.novatv.core.settings

data class Credentials(
    val username: String?,
    val password: String?,
)

interface CredentialStore {
    suspend fun put(playlistId: Long, credentials: Credentials)
    suspend fun get(playlistId: Long): Credentials?
    suspend fun remove(playlistId: Long)
}
