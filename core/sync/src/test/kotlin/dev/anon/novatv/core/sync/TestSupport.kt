package dev.anon.novatv.core.sync

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.database.entity.ChannelEntity
import dev.anon.novatv.core.database.entity.PlaylistEntity
import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Movie
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import dev.anon.novatv.core.model.SearchText
import dev.anon.novatv.core.model.Series
import dev.anon.novatv.core.model.SourceAccount
import dev.anon.novatv.core.model.StreamFormat
import dev.anon.novatv.core.model.SyncState
import dev.anon.novatv.core.settings.CredentialStore
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.source.EpgSource
import dev.anon.novatv.core.source.SourceAdapter
import dev.anon.novatv.core.source.SourceAdapterFactory
import kotlinx.coroutines.delay
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class FakeCredentialStore : CredentialStore {
    val values = mutableMapOf<Long, Credentials>()

    override suspend fun put(playlistId: Long, credentials: Credentials) {
        values[playlistId] = credentials
    }

    override suspend fun get(playlistId: Long): Credentials? = values[playlistId]

    override suspend fun remove(playlistId: Long) {
        values.remove(playlistId)
    }
}

class FakeAdapter(
    var channels: List<Channel> = emptyList(),
    var authFailure: Throwable? = null,
    var liveFailure: Throwable? = null,
    var liveDelayMs: Long = 0,
) : SourceAdapter {

    override val type = PlaylistType.XTREAM
    val liveCalls = AtomicInteger()
    val maxConcurrent = AtomicInteger()
    private val active = AtomicInteger()

    override suspend fun authenticate(): SourceAccount {
        authFailure?.let { throw it }
        return SourceAccount(status = "Active")
    }

    override suspend fun live(onProgress: (Int) -> Unit): List<Channel> {
        liveCalls.incrementAndGet()
        val now = active.incrementAndGet()
        maxConcurrent.updateAndGet { maxOf(it, now) }
        try {
            if (liveDelayMs > 0) delay(liveDelayMs)
            liveFailure?.let { throw it }
            if (channels.isNotEmpty()) onProgress(channels.size)
            return channels
        } finally {
            active.decrementAndGet()
        }
    }

    override suspend fun vod(): List<Movie> = emptyList()
    override suspend fun series(): List<Series> = emptyList()
    override suspend fun epg(): EpgSource? = null

    override fun streamUrl(channel: Channel, format: StreamFormat): String =
        "http://stream/${channel.remoteId}.${when (format) {
            StreamFormat.HLS -> "m3u8"
            StreamFormat.TS -> "ts"
            StreamFormat.AUTO -> "auto"
        }}"

    override fun catchupUrl(channel: Channel, at: Instant, minutes: Int): String? =
        if (channel.hasArchive) "http://catchup/${channel.remoteId}?m=$minutes" else null
}

class FakeAdapterFactory(private val adapter: FakeAdapter) : SourceAdapterFactory {
    override val type = PlaylistType.XTREAM
    override fun create(playlist: Playlist, credentials: Credentials?): SourceAdapter {
        if (credentials == null) {
            throw dev.anon.novatv.core.network.SourceAuthException("kimlik yok")
        }
        return adapter
    }
}

fun testDatabase(): NovaDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.inMemoryDatabaseBuilder(context, NovaDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}

fun testPreferenceStore(): PreferenceStore {
    val dir = Files.createTempDirectory("novatv-sync-prefs").toFile()
    return PreferenceStore(
        PreferenceDataStoreFactory.create(produceFile = { File(dir, "test.preferences_pb") }),
    )
}

suspend fun seedPlaylist(db: NovaDatabase, id: Long = 1, active: Boolean = true) {
    db.playlistDao().upsert(
        PlaylistEntity(
            id = id,
            type = "XTREAM",
            name = "Kaynak $id",
            baseUrl = "http://localhost:$id",
            username = "demo",
            epgUrl = null,
            userAgent = null,
            isActive = active,
            sortOrder = id.toInt(),
            lastSyncAt = null,
            syncState = SyncState.NEVER.name,
            syncIntervalHours = 24,
        ),
    )
}

suspend fun seedChannel(
    db: NovaDatabase,
    id: Long = 0,
    playlistId: Long = 1,
    remoteId: String,
    name: String,
    group: String = "Ulusal",
    number: Int? = null,
) {
    db.channelDao().replaceForPlaylist(
        playlistId = playlistId,
        token = 1,
        items = listOf(
            ChannelEntity(
                id = id,
                playlistId = playlistId,
                remoteId = remoteId,
                number = number,
                name = name,
                nameNormalized = SearchText.normalize(name),
                groupName = group,
                logoUrl = null,
                tvgId = null,
                hasArchive = false,
                archiveDays = 0,
                streamFormat = "AUTO",
                syncToken = 1,
            ),
        ),
    )
}

fun channel(id: Long = 0, remoteId: String, name: String, group: String = "Ulusal") = Channel(
    id = id,
    playlistId = 1,
    remoteId = remoteId,
    name = name,
    groupName = group,
)
