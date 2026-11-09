package dev.anon.novatv.core.sync

/** Oynatıcıya verilecek çözülmüş akış: URL ve yalnızca bu istek için başlıklar. */
data class ResolvedStream(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)
