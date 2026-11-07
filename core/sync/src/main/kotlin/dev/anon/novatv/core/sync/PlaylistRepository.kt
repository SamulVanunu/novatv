package dev.anon.novatv.core.sync

import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.database.mapper.toEntity
import dev.anon.novatv.core.database.mapper.toModel
import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Playlist
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
        val id = db.playlistDao().upsert(playlist.copy(id = 0).toEntity())
        this.credentials.put(id, credentials)
        return id
    }

    override suspend fun update(playlist: Playlist) {
        db.playlistDao().upsert(playlist.toEntity())
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
        return entity.toModel() to credentials.get(id)
    }
}
