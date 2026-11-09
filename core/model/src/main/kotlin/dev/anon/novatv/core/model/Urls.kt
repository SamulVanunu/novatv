package dev.anon.novatv.core.model

import java.net.URI

/**
 * Adresten sorgu, parça ve kullanıcı bilgisini atar; geriye yalnızca
 * `şema://host[:port]/yol` kalır.
 *
 * M3U listeleri `?username=…&password=…` taşıyabildiği için kaynak adresi
 * Room'a bu biçimde yazılır; tam adres Keystore'lu depoda durur.
 */
fun redactUrl(url: String): String {
    val trimmed = url.trim()
    val fallback = trimmed.substringBefore('?').substringBefore('#')
    return runCatching {
        val uri = URI(trimmed)
        val scheme = uri.scheme ?: return fallback
        val host = uri.host ?: return fallback
        buildString {
            append(scheme).append("://")
            if (uri.port != -1) append(host).append(':').append(uri.port) else append(host)
            append(uri.rawPath.orEmpty())
        }
    }.getOrDefault(fallback)
}
