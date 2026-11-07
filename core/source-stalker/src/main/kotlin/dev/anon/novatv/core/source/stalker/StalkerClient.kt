package dev.anon.novatv.core.source.stalker

import dev.anon.novatv.core.model.Channel
import dev.anon.novatv.core.model.Movie
import dev.anon.novatv.core.model.SourceAccount
import dev.anon.novatv.core.network.SourceHttp
import dev.anon.novatv.core.network.SourceProtocolException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.ZoneId

class StalkerClient(
    private val http: SourceHttp,
    private val portalUrl: String,
    private val mac: String,
) {

    private val base = portalUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun handshake(): String {
        val text = http.getText(
            "$base/portal.php?type=stb&action=handshake&token=&JsHttpRequest=1-xml",
            mapOf("Cookie" to "mac=$mac"),
        )
        val token = text.parse().js().string("token")
        if (token.isNullOrBlank()) throw SourceProtocolException("Portal el sıkışma yanıtı geçersiz")
        return token
    }

    suspend fun profile(token: String): SourceAccount {
        val js = request(token, "type=stb&action=get_profile&hd=1").js()
        val status = js.string("status")
        return SourceAccount(
            status = if (status == "1" || status.equals("active", ignoreCase = true)) "Active" else "Inactive",
            expiresAt = js.string("expire_billing_date")?.let(::parsePortalDate),
            maxConnections = js.string("max_connections")?.toIntOrNull(),
            serverInfo = base,
        )
    }

    suspend fun live(token: String, onProgress: (Int) -> Unit = {}): List<Channel> {
        val genres = runCatching { genres(token) }.getOrDefault(emptyMap())
        val items = request(token, "type=itv&action=get_all_channels").js().array()
        val channels = ArrayList<Channel>(items.size)
        for (item in items) {
            val id = item.string("id") ?: continue
            val name = item.string("name") ?: continue
            channels += Channel(
                playlistId = 0,
                remoteId = id,
                name = name,
                groupName = genres[item.string("tv_genre_id")] ?: DEFAULT_GROUP,
                logoUrl = item.string("logo"),
                tvgId = item.string("xmltv_id"),
                hasArchive = item.string("tv_archive").toFlag(),
                archiveDays = item.string("tv_archive_duration")?.toIntOrNull() ?: 0,
            )
            if (channels.size % PROGRESS_STEP == 0) onProgress(channels.size)
        }
        onProgress(channels.size)
        return channels
    }

    suspend fun vod(token: String): List<Movie> =
        request(token, "type=vod&action=get_ordered_list&genre=0&p=1").js().array()
            .mapNotNull { item ->
                val id = item.string("id") ?: return@mapNotNull null
                val name = item.string("name") ?: return@mapNotNull null
                Movie(
                    playlistId = 0,
                    remoteId = id,
                    name = name,
                    year = item.string("year")?.toIntOrNull(),
                    rating = item.string("rating_imdb")?.toDoubleOrNull()
                        ?: item.string("rating_kinopoisk")?.toDoubleOrNull(),
                    genres = item.string("genres_str")
                        ?.split(',')
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?: emptyList(),
                    plot = item.string("description"),
                    posterUrl = item.string("screenshot_uri") ?: item.string("cover"),
                    containerExt = null,
                )
            }

    fun liveUrl(streamId: String, ext: String): String =
        "$base/play/live.php?mac=$mac&stream=$streamId&extension=$ext"

    private suspend fun genres(token: String): Map<String, String> =
        request(token, "type=itv&action=get_genres").js().array()
            .mapNotNull { item ->
                val id = item.string("id") ?: return@mapNotNull null
                val title = item.string("title") ?: return@mapNotNull null
                id to title
            }
            .toMap()

    private suspend fun request(token: String, params: String): JsonElement {
        val text = http.getText(
            "$base/portal.php?$params&JsHttpRequest=1-xml",
            mapOf("Cookie" to "mac=$mac", "Authorization" to "Bearer $token"),
        )
        return text.parse()
    }

    private fun String.parse(): JsonElement =
        try {
            json.parseToJsonElement(this)
        } catch (e: Exception) {
            throw SourceProtocolException("Portal geçerli JSON döndürmedi", e)
        }

    /** Çoğu yanıt `{"js": ...}` zarfında gelir; bazıları doğrudan dizidir. */
    private fun JsonElement.js(): JsonElement =
        (this as? JsonObject)?.get("js") ?: this

    private fun JsonElement.array(): List<JsonElement> = when (this) {
        is JsonArray -> this
        is JsonObject -> (this["data"] as? JsonArray) ?: emptyList()
        else -> emptyList()
    }

    private fun JsonElement.string(key: String): String? =
        (this as? JsonObject)?.get(key)?.jsonPrimitive?.contentOrNull

    private fun String?.toFlag(): Boolean = this == "1" || this.equals("true", ignoreCase = true)

    private fun parsePortalDate(raw: String): Long? = runCatching {
        LocalDateTime.parse(raw.replace(' ', 'T'))
            .atZone(ZoneId.of("Europe/Istanbul"))
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

    private companion object {
        const val DEFAULT_GROUP = "Diğer"
        const val PROGRESS_STEP = 500
    }
}
