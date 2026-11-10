package dev.anon.novatv.core.settings

import dev.anon.novatv.core.model.Credentials
import java.util.concurrent.ConcurrentHashMap

/**
 * Şifreli depo hiç açılamazsa (bozuk anahtar zinciri, kilitli keystore) son
 * çare: uygulama açılabilsin diye kimlik bilgileri yalnızca bellekte tutulur.
 * Süreç ölünce kaybolur; kullanıcı kaynağı yeniden ekleyebilir.
 */
class InMemoryCredentialStore : CredentialStore {

    private val credentials = ConcurrentHashMap<Long, Credentials>()
    private val sourceUrls = ConcurrentHashMap<Long, String>()

    override suspend fun put(playlistId: Long, credentials: Credentials) {
        this.credentials[playlistId] = credentials
    }

    override suspend fun get(playlistId: Long): Credentials? = credentials[playlistId]

    override suspend fun remove(playlistId: Long) {
        credentials.remove(playlistId)
        sourceUrls.remove(playlistId)
    }

    override suspend fun putSourceUrl(playlistId: Long, url: String) {
        sourceUrls[playlistId] = url
    }

    override suspend fun getSourceUrl(playlistId: Long): String? = sourceUrls[playlistId]

    override suspend fun removeSourceUrl(playlistId: Long) {
        sourceUrls.remove(playlistId)
    }
}
