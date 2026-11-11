package dev.anon.novatv.app.common

import android.app.Application
import androidx.work.Configuration
import dev.anon.novatv.core.sync.SyncWorkerFactory

/**
 * İki uygulamanın paylaştığı Application: grafik, kayıt kurtarma ve
 * WorkManager yapılandırması tek yerde. `open`: TV uygulaması doğrudan bu
 * sınıfı, telefon uygulaması kendi `MobileApplication` alt sınıfını kullanır.
 */
open class NovaApplication : Application(), Configuration.Provider {

    val graph: NovaGraph by lazy { NovaGraph(this) }

    override fun onCreate() {
        super.onCreate()
        // Uygulama çökmüşse yarım kalan kayıt satırlarını kapat; dosya elde kalsın.
        runCatching { graph.recordings.recoverInterrupted() }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(SyncWorkerFactory(graph.syncEngine))
            .build()
}
