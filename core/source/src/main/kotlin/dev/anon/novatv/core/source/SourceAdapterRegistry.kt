package dev.anon.novatv.core.source

import dev.anon.novatv.core.model.Credentials
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType

class SourceAdapterRegistry(factories: Set<SourceAdapterFactory>) {

    private val byType: Map<PlaylistType, SourceAdapterFactory>

    init {
        val map = LinkedHashMap<PlaylistType, SourceAdapterFactory>()
        for (factory in factories) {
            require(map.put(factory.type, factory) == null) {
                "Aynı kaynak tipi için iki fabrika kayıtlı: ${factory.type}"
            }
        }
        byType = map
    }

    fun adapterFor(playlist: Playlist, credentials: Credentials?): SourceAdapter {
        val factory = byType[playlist.type]
            ?: throw IllegalArgumentException("Bu kaynak tipi için adaptör yok: ${playlist.type}")
        return factory.create(playlist, credentials)
    }
}
