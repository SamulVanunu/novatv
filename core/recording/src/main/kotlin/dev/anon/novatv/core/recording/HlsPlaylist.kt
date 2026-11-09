package dev.anon.novatv.core.recording

import java.net.URI

data class HlsVariant(val url: String, val bandwidth: Long)

/**
 * Basit HLS medya listesi çözümleyici: segment adresleri, ana liste
 * varyantları, şifreleme ve bitiş işareti. Tam RFC değil; kayıt için gereken
 * kadarı — yayınların ezici çoğunluğu bu biçimde.
 */
data class HlsMedia(
    val segments: List<String>,
    val variants: List<HlsVariant>,
    val encrypted: Boolean,
    val ended: Boolean,
    /** `#EXT-X-TARGETDURATION`; canlı listeyi yeniden okuma aralığı. */
    val targetSeconds: Int = 6,
)

fun parseHlsPlaylist(text: String, baseUrl: String): HlsMedia {
    val segments = ArrayList<String>()
    val variants = ArrayList<HlsVariant>()
    var encrypted = false
    var ended = false
    var targetSeconds = 6
    var pendingBandwidth: Long? = null

    text.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.isEmpty() -> Unit

            line.startsWith("#EXT-X-STREAM-INF") -> {
                pendingBandwidth = Regex("BANDWIDTH=(\\d+)")
                    .find(line)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toLongOrNull()
                    ?: 0L
            }

            line.startsWith("#EXT-X-KEY") -> {
                val method = Regex("METHOD=([A-Z0-9-]+)")
                    .find(line)
                    ?.groupValues
                    ?.getOrNull(1)
                if (method != null && method != "NONE") encrypted = true
            }

            line.startsWith("#EXT-X-TARGETDURATION") ->
                targetSeconds = line.substringAfter(':', "").toDoubleOrNull()?.toInt() ?: 6

            line == "#EXT-X-ENDLIST" -> ended = true

            line.startsWith("#") -> Unit

            else -> {
                val absolute = resolveUrl(baseUrl, line)
                val bandwidth = pendingBandwidth
                if (bandwidth != null) {
                    variants += HlsVariant(url = absolute, bandwidth = bandwidth)
                    pendingBandwidth = null
                } else {
                    segments += absolute
                }
            }
        }
    }

    return HlsMedia(
        segments = segments,
        variants = variants,
        encrypted = encrypted,
        ended = ended,
        targetSeconds = targetSeconds.coerceIn(1, 30),
    )
}

/** Göreli segment adresini liste adresine göre mutlaklaştırır. */
fun resolveUrl(baseUrl: String, reference: String): String = runCatching {
    URI(baseUrl).resolve(reference.trim()).toString()
}.getOrElse { reference.trim() }

/** HLS adresi mi? Sorgu dizesi yok sayılır. */
fun isHlsUrl(url: String): Boolean =
    url.substringBefore('?').substringBefore('#').lowercase().endsWith(".m3u8")
