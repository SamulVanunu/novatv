package dev.anon.novatv.core.sync

import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.database.mapper.toEntity
import dev.anon.novatv.core.model.SyncState
import dev.anon.novatv.core.source.SourceAdapterRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

enum class SyncStage { AUTHENTICATING, CHANNELS, SAVING }

sealed interface SyncEvent {
    data class Progress(
        val playlistId: Long,
        val stage: SyncStage,
        val done: Int,
        val total: Int? = null,
    ) : SyncEvent

    data class Finished(
        val playlistId: Long,
        val state: SyncState,
        val message: String? = null,
    ) : SyncEvent
}

class SyncEngine(
    private val db: NovaDatabase,
    private val playlists: PlaylistRepository,
    private val registry: SourceAdapterRegistry,
) {

    private val mutex = Mutex()
    private val eventsFlow = MutableSharedFlow<SyncEvent>(extraBufferCapacity = 64)
    private val tokenSequence = AtomicLong(System.currentTimeMillis())

    fun events(): Flow<SyncEvent> = eventsFlow.asSharedFlow()

    /** Aynı anda tek senkron; ikinci çağrı kilitte bekler, çakışmaz. */
    suspend fun sync(playlistId: Long): SyncState = mutex.withLock {
        val (playlist, credentials) = playlists.load(playlistId)
        if (!playlist.isActive) {
            return@withLock finish(playlistId, SyncState.FAILED, "Playlist pasif")
        }

        db.playlistDao().setSyncState(playlistId, SyncState.RUNNING.name)
        emit(SyncEvent.Progress(playlistId, SyncStage.AUTHENTICATING, done = 0))

        try {
            val adapter = registry.adapterFor(playlist, credentials)
            adapter.authenticate()

            emit(SyncEvent.Progress(playlistId, SyncStage.CHANNELS, done = 0))
            val channels = adapter.live { done ->
                emit(SyncEvent.Progress(playlistId, SyncStage.CHANNELS, done = done))
            }

            if (channels.isEmpty()) {
                return@withLock finish(
                    playlistId,
                    SyncState.PARTIAL,
                    "Kaynak kanal döndürmedi; mevcut liste korundu",
                )
            }

            emit(SyncEvent.Progress(playlistId, SyncStage.SAVING, done = channels.size, total = channels.size))
            val token = tokenSequence.incrementAndGet()
            db.channelDao().replaceForPlaylist(
                playlistId = playlistId,
                token = token,
                items = channels.map { it.copy(playlistId = playlistId).toEntity(syncToken = token) },
            )
            finish(playlistId, SyncState.OK, null)
        } catch (e: CancellationException) {
            db.playlistDao().setSyncState(playlistId, SyncState.FAILED.name)
            throw e
        } catch (e: Exception) {
            finish(playlistId, SyncState.FAILED, e.message ?: e::class.simpleName)
        }
    }

    private suspend fun finish(playlistId: Long, state: SyncState, message: String?): SyncState {
        if (state == SyncState.OK || state == SyncState.PARTIAL) {
            db.playlistDao().markSync(playlistId, System.currentTimeMillis(), state.name)
        } else {
            db.playlistDao().setSyncState(playlistId, state.name)
        }
        emit(SyncEvent.Finished(playlistId, state, message))
        return state
    }

    private fun emit(event: SyncEvent) {
        eventsFlow.tryEmit(event)
    }
}
