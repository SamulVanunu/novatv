package dev.anon.novatv.app

import android.content.Context
import androidx.work.WorkManager
import dev.anon.novatv.core.database.NovaDatabase
import dev.anon.novatv.core.settings.EncryptedCredentialStore
import dev.anon.novatv.core.settings.PreferenceStore
import dev.anon.novatv.core.source.SourceAdapterRegistry
import dev.anon.novatv.core.source.m3u.M3uFactory
import dev.anon.novatv.core.source.stalker.StalkerFactory
import dev.anon.novatv.core.source.xstream.XtreamFactory
import dev.anon.novatv.core.sync.RoomChannelRepository
import dev.anon.novatv.core.sync.RoomPlaylistRepository
import dev.anon.novatv.core.sync.StreamResolver
import dev.anon.novatv.core.sync.SyncEngine
import dev.anon.novatv.core.sync.SyncScheduler

/** Elle kurulan uygulama grafiği; Hilt P3'te devreye alınana kadar tek yerden bağlar. */
class NovaGraph(context: Context) {

    private val appContext = context.applicationContext

    val database: NovaDatabase = NovaDatabase.build(appContext)
    val preferences: PreferenceStore = PreferenceStore.create(appContext)
    val credentials: EncryptedCredentialStore = EncryptedCredentialStore(appContext)
    val registry: SourceAdapterRegistry = SourceAdapterRegistry(
        setOf(XtreamFactory(), M3uFactory(), StalkerFactory()),
    )

    val playlists: RoomPlaylistRepository = RoomPlaylistRepository(database, credentials, registry)
    val channels: RoomChannelRepository = RoomChannelRepository(database, preferences)
    val syncEngine: SyncEngine = SyncEngine(database, playlists, registry)
    val streamResolver: StreamResolver = StreamResolver(playlists, credentials, registry)

    // WorkManager yalnızca ilk ihtiyaçta kurulur; Application yapılandırmasıyla döngüye girmez.
    val syncScheduler: SyncScheduler by lazy {
        SyncScheduler(WorkManager.getInstance(appContext), syncEngine)
    }
}
