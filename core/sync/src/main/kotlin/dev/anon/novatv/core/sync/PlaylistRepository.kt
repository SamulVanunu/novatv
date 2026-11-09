package dev.anon.novatv.core.sync

import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.database.mapper.toEntity
import dev.anon.novatv.core.database.mapper.toModel
import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.redactUrl
import dev.anon.novatv.core.settings.CredentialStore
import dev.anon.novatv.core.source.SourceAdapterRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PlaylistRepository {
    fun observeAll(): Flow<List<Playlist>>
    suspend fun add(playlist: Playlist, credentials: Credentials): Long
    suspend fun update(playlist: Playlist)
    suspend fun updateCredentials(id: Long, credentials: Credentials)
    suspend fun remove(id: Long)
    suspend fun load(id: Long): Pair<Playlist, Credentials?>
}

class RoomPlaylistRepository(
    private val db: NovaDatabase,
    private val credentials: CredentialStore,
    @Suppress("unused") private val registry: SourceAdapterRegistry,
) : PlaylistRepository {

    override fun observeAll(): Flow<List<Playlist>> =
        db.playlistDao().observeAll().map { rows -> rows.map { it.toModel() } }

    override suspend fun add(playlist: Playlist, credentials: Credentials): Long {
        val id = db.playlistDao().upsert(playlist.copy(id = 0, baseUrl = storageUrl(playlist)).toEntity())
        try {
            this.credentials.put(id, credentials)
            storeSourceUrl(playlist, id)
        } catch (t: Throwable) {
            // Şifreli adres yazılamadıysa yarım kayıt bırakma.
            db.playlistDao().delete(id)
            throw t
        }
        return id
    }

    override suspend fun update(playlist: Playlist) {
        db.playlistDao().upsert(playlist.copy(baseUrl = storageUrl(playlist)).toEntity())
        storeSourceUrl(playlist, playlist.id)
    }

    override suspend fun updateCredentials(id: Long, credentials: Credentials) {
        this.credentials.put(id, credentials)
    }

    override suspend fun remove(id: Long) {
        db.playlistDao().delete(id)
        credentials.remove(id)
    }

    override suspend fun load(id: Long): Pair<Playlist, Credentials?> {
        val entity = db.playlistDao().byId(id) ?: throw IllegalArgumentException("Playlist bulunamadı: $id")
        val playlist = entity.toModel()
        val creds = credentials.get(id)
        if (playlist.type != PlaylistType.M3U) return playlist to creds

        val stored = credentials.getSourceUrl(id)
        if (stored != null) return playlist.copy(baseUrl = stored) to creds

        // v1 kurulumundan kalan düz metin adresi ilk okumada şifreli depoya taşı.
        val fullUrl = playlist.baseUrl
        val redacted = redactUrl(fullUrl)
        if (redacted != fullUrl) {
            credentials.putSourceUrl(id, fullUrl)
            db.playlistDao().upsert(entity.copy(baseUrl = redacted))
            return playlist.copy(baseUrl = fullUrl) to creds
        }
        return playlist to creds
    }

    /** M3U adresi sorgu parametrelerinde kimlik taşıyabilir; Room'a yalnızca arındırılmış hâli yazılır. */
    private fun storageUrl(playlist: Playlist): String =
        if (playlist.type == PlaylistType.M3U) redactUrl(playlist.baseUrl) else playlist.baseUrl

    private suspend fun storeSourceUrl(playlist: Playlist, id: Long) {
        if (playlist.type == PlaylistType.M3U && playlist.baseUrl.isNotBlank()) {
            credentials.putSourceUrl(id, playlist.baseUrl.trim())
        }
    }
}
