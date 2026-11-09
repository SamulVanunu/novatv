package dev.anon.novatv.app

import android.app.Application
import androidx.work.Configuration
import dev.anon.novatv.core.sync.SyncWorkerFactory

class NovaApplication : Application(), Configuration.Provider {

    val graph: NovaGraph by lazy { NovaGraph(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(SyncWorkerFactory(graph.syncEngine))
            .build()
}
