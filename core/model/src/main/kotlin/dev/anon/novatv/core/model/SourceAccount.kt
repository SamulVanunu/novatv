package dev.anon.novatv.core.model

data class SourceAccount(
    val status: String,
    val expiresAt: Long? = null,
    val maxConnections: Int? = null,
    val serverInfo: String? = null,
)
