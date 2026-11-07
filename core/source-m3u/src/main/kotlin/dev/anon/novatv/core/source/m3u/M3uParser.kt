package dev.anon.novatv.core.source.m3u

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.StreamFormat
import java.io.InputStream

data class M3uMeta(
    val tvgUrl: String?,
    val parsed: Int,
    val skipped: Int,
    val userAgents: Map<String, String> = emptyMap(),
    val referrers: Map<String, String> = emptyMap(),
)

/**
 * Satır bazlı M3U/M3U8 ayrıştırıcı.
 *
 * Bozuk satır (geçersiz süre, adsız, URL'siz) tüm listeyi düşürmez: satır atlanır,
 * `skipped` artar. `#EXTM3U` başlığındaki `x-tvg-url`/`url-tvg` EPG adresi olarak
 * döner; `#EXTVLCOPT` başlıkları akış URL'i başına taşınır.
 */
class M3uParser {

    fun parse(playlistId: Long, input: InputStream, onChannel: (Channel) -> Unit): M3uMeta {
        var tvgUrl: String? = null
        var parsed = 0
        var skipped = 0
        val userAgents = LinkedHashMap<String, String>()
        val referrers = LinkedHashMap<String, String>()

        var pending: PendingEntry? = null

        input.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (raw in lines) {
                val line = raw.trim('\uFEFF', ' ', '\t', '\r')
                if (line.isEmpty()) {
                    if (pending != null) {
                        skipped++
                        pending = null
                    }
                    continue
                }

                when {
                    line.startsWith(HEADER, ignoreCase = true) -> {
                        tvgUrl = parseAttributes(line.substring(HEADER.length))["x-tvg-url"]
                            ?: parseAttributes(line.substring(HEADER.length))["url-tvg"]
                    }

                    line.startsWith(EXTINF, ignoreCase = true) -> {
                        if (pending != null) skipped++
                        pending = parseExtinf(line)
                    }

                    line.startsWith(EXTGRP, ignoreCase = true) -> {
                        pending?.groupOverride = line.substring(EXTGRP.length).trim()
                    }

                    line.startsWith(EXTVLCOPT, ignoreCase = true) -> {
                        pending?.addOption(line.substring(EXTVLCOPT.length))
                    }

                    line.startsWith("#") -> Unit // bilinmeyen yorum satırı

                    else -> {
                        val entry = pending
                        pending = null
                        if (entry == null || !entry.isValid) {
                            skipped++
                            continue
                        }
                        val url = line
                        entry.userAgent?.let { userAgents[url] = it }
                        entry.referrer?.let { referrers[url] = it }
                        onChannel(entry.toChannel(playlistId, url))
                        parsed++
                    }
                }
            }
        }
        if (pending != null) skipped++

        return M3uMeta(
            tvgUrl = tvgUrl,
            parsed = parsed,
            skipped = skipped,
            userAgents = userAgents,
            referrers = referrers,
        )
    }

    private fun parseExtinf(line: String): PendingEntry {
        val body = line.substring(EXTINF.length)
        val durationEnd = body.indexOfFirst { it == ' ' || it == '\t' || it == ',' }
            .let { if (it < 0) body.length else it }
        val duration = body.substring(0, durationEnd).trim()
        val validDuration = duration.toDoubleOrNull() != null

        val rest = body.substring(durationEnd)
        val commaIndex = firstCommaOutsideQuotes(rest)
        val attributesPart = if (commaIndex >= 0) rest.substring(0, commaIndex) else rest
        val name = if (commaIndex >= 0) rest.substring(commaIndex + 1).trim() else ""

        val attrs = parseAttributes(attributesPart)
        val archive = attrs["catchup"]?.isNotBlank() == true || attrs["timeshift"]?.isNotBlank() == true
        return PendingEntry(
            valid = validDuration && name.isNotBlank(),
            name = name,
            attrs = attrs,
            hasArchive = archive,
        )
    }

    private fun parseAttributes(text: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (match in ATTRIBUTE.findAll(text)) {
            val key = match.groupValues[1].lowercase()
            val value = if (match.groupValues[2].isNotEmpty()) {
                match.groupValues[2]
            } else {
                match.groupValues[3]
            }
            if (value.isNotBlank()) out[key] = value
        }
        return out
    }

    /** Tırnak içindeki virgülleri atlayan ilk virgül konumu; yoksa -1. */
    private fun firstCommaOutsideQuotes(text: String): Int {
        var inQuotes = false
        text.forEachIndexed { index, ch ->
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) return index
            }
        }
        return -1
    }

    private class PendingEntry(
        val valid: Boolean,
        val name: String,
        val attrs: Map<String, String>,
        val hasArchive: Boolean,
    ) {
        var groupOverride: String? = null
        var userAgent: String? = null
        var referrer: String? = null

        val isValid: Boolean get() = valid

        fun addOption(option: String) {
            // VLC hem `http-user-agent:VLC/3.0` hem `http-user-agent=VLC/3.0` yazar.
            val separator = option.indexOfFirst { it == ':' || it == '=' }
            if (separator < 0) return
            when (option.substring(0, separator).trim().lowercase()) {
                "http-user-agent" -> userAgent = option.substring(separator + 1).trim()
                "http-referrer", "http-referer" -> referrer = option.substring(separator + 1).trim()
            }
        }

        fun toChannel(playlistId: Long, url: String) = Channel(
            playlistId = playlistId,
            remoteId = url,
            number = attrs["tvg-chno"]?.toIntOrNull() ?: attrs["channel-number"]?.toIntOrNull(),
            name = name,
            groupName = groupOverride
                ?: attrs["group-title"]
                ?: attrs["group"]
                ?: DEFAULT_GROUP,
            logoUrl = attrs["tvg-logo"] ?: attrs["logo"],
            tvgId = attrs["tvg-id"] ?: attrs["tvg-name"],
            hasArchive = hasArchive,
            archiveDays = attrs["catchup-days"]?.toIntOrNull()
                ?: attrs["timeshift"]?.toIntOrNull()
                ?: 0,
            format = formatOf(url),
        )

        private fun formatOf(url: String): StreamFormat {
            val path = url.substringBefore('?').lowercase()
            return when {
                path.endsWith(".m3u8") -> StreamFormat.HLS
                path.endsWith(".ts") || path.endsWith(".mpegts") -> StreamFormat.TS
                else -> StreamFormat.AUTO
            }
        }
    }

    private companion object {
        const val HEADER = "#EXTM3U"
        const val EXTINF = "#EXTINF:"
        const val EXTGRP = "#EXTGRP:"
        const val EXTVLCOPT = "#EXTVLCOPT:"
        const val DEFAULT_GROUP = "Genel"

        val ATTRIBUTE = Regex("""([A-Za-z0-9_-]+)\s*=\s*(?:"([^"]*)"|([^\s,]*))""")
    }
}
