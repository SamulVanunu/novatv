package dev.anon.novatv.core.settings

import dev.anon.novatv.core.model.Credentials

interface CredentialStore {
    suspend fun put(playlistId: Long, credentials: Credentials)
    suspend fun get(playlistId: Long): Credentials?
    suspend fun remove(playlistId: Long)
}
