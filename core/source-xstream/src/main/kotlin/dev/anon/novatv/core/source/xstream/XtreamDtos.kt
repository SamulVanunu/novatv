package dev.anon.novatv.core.source.xstream

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.contentOrNull

/** Xtream panelleri aynı alanı bazen sayı, bazen tırnaklı metin döndürür. */
object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val input = decoder as? JsonDecoder ?: return decoder.decodeString()
        return (input.decodeJsonElement() as? JsonPrimitive)?.contentOrNull.orEmpty()
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

object FlexibleNullableStringSerializer : KSerializer<String?> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleNullableString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val input = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = input.decodeJsonElement()
        if (element is JsonNull) return null
        return (element as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }
}

object FlexibleIntSerializer : KSerializer<Int?> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int? {
        val input = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val raw = (input.decodeJsonElement() as? JsonPrimitive)?.contentOrNull ?: return null
        return raw.trim().toIntOrNull() ?: raw.trim().toDoubleOrNull()?.toInt()
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }
}

object FlexibleLongSerializer : KSerializer<Long?> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long? {
        val input = decoder as? JsonDecoder ?: return decoder.decodeLong()
        val raw = (input.decodeJsonElement() as? JsonPrimitive)?.contentOrNull ?: return null
        return raw.trim().toLongOrNull() ?: raw.trim().toDoubleOrNull()?.toLong()
    }

    override fun serialize(encoder: Encoder, value: Long?) {
        if (value == null) encoder.encodeNull() else encoder.encodeLong(value)
    }
}

object FlexibleDoubleSerializer : KSerializer<Double?> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double? {
        val input = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        val raw = (input.decodeJsonElement() as? JsonPrimitive)?.contentOrNull ?: return null
        return raw.trim().toDoubleOrNull()
    }

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeNull() else encoder.encodeDouble(value)
    }
}

object FlexibleBooleanSerializer : KSerializer<Boolean> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val input = decoder as? JsonDecoder ?: return decoder.decodeBoolean()
        val raw = (input.decodeJsonElement() as? JsonPrimitive)?.contentOrNull?.trim() ?: return false
        return when (raw.lowercase()) {
            "1", "true", "yes" -> true
            else -> false
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)
}

@Serializable
data class AuthResponseDto(
    @SerialName("user_info") val userInfo: UserInfoDto? = null,
    @SerialName("server_info") val serverInfo: ServerInfoDto? = null,
)

@Serializable
data class UserInfoDto(
    val username: String? = null,
    val password: String? = null,
    val message: String? = null,
    val status: String? = null,
    @SerialName("exp_date")
    @Serializable(with = FlexibleLongSerializer::class)
    val expDate: Long? = null,
    @SerialName("max_connections")
    @Serializable(with = FlexibleIntSerializer::class)
    val maxConnections: Int? = null,
)

@Serializable
data class ServerInfoDto(
    val url: String? = null,
    val port: String? = null,
    @SerialName("https_port") val httpsPort: String? = null,
    @SerialName("server_protocol") val serverProtocol: String? = null,
    val timezone: String? = null,
)

@Serializable
data class CategoryDto(
    @SerialName("category_id")
    @Serializable(with = FlexibleStringSerializer::class)
    val categoryId: String = "",
    @SerialName("category_name") val categoryName: String = "",
)

@Serializable
data class LiveStreamDto(
    @SerialName("stream_id")
    @Serializable(with = FlexibleStringSerializer::class)
    val streamId: String = "",
    val name: String = "",
    @SerialName("stream_icon")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val streamIcon: String? = null,
    @SerialName("category_id")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val categoryId: String? = null,
    @SerialName("epg_channel_id")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val epgChannelId: String? = null,
    @SerialName("tv_archive")
    @Serializable(with = FlexibleIntSerializer::class)
    val tvArchive: Int? = 0,
    @SerialName("tv_archive_duration")
    @Serializable(with = FlexibleIntSerializer::class)
    val tvArchiveDuration: Int? = 0,
    @Serializable(with = FlexibleIntSerializer::class)
    val num: Int? = null,
)

@Serializable
data class VodStreamDto(
    @SerialName("stream_id")
    @Serializable(with = FlexibleStringSerializer::class)
    val streamId: String = "",
    val name: String = "",
    @SerialName("stream_icon")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val streamIcon: String? = null,
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val rating: String? = null,
    @SerialName("rating_5based")
    @Serializable(with = FlexibleDoubleSerializer::class)
    val rating5Based: Double? = null,
    @Serializable(with = FlexibleIntSerializer::class)
    val year: Int? = null,
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val genre: String? = null,
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val plot: String? = null,
    @SerialName("container_extension")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val containerExtension: String? = null,
    @SerialName("category_id")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val categoryId: String? = null,
)

@Serializable
data class SeriesDto(
    @SerialName("series_id")
    @Serializable(with = FlexibleStringSerializer::class)
    val seriesId: String = "",
    val name: String = "",
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val cover: String? = null,
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val plot: String? = null,
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val genre: String? = null,
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val releaseDate: String? = null,
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val rating: String? = null,
    @SerialName("backdrop_path")
    val backdropPath: List<String>? = null,
    @SerialName("episode_run_time")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val episodeRunTime: String? = null,
    @SerialName("category_id")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val categoryId: String? = null,
)

@Serializable
data class SeriesInfoDto(
    val info: SeriesDto? = null,
    val seasons: List<SeasonDto>? = null,
    val episodes: Map<String, List<EpisodeDto>>? = null,
)

@Serializable
data class SeasonDto(
    val name: String? = null,
    @SerialName("season_number")
    @Serializable(with = FlexibleIntSerializer::class)
    val seasonNumber: Int? = null,
    @SerialName("episode_count")
    @Serializable(with = FlexibleIntSerializer::class)
    val episodeCount: Int? = null,
)

@Serializable
data class EpisodeDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String = "",
    @SerialName("episode_num")
    @Serializable(with = FlexibleIntSerializer::class)
    val episodeNum: Int? = null,
    val title: String = "",
    @SerialName("container_extension")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val containerExtension: String? = null,
    val info: EpisodeInfoDto? = null,
    @Serializable(with = FlexibleIntSerializer::class)
    val season: Int? = null,
)

@Serializable
data class EpisodeInfoDto(
    @SerialName("movie_image")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val movieImage: String? = null,
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val plot: String? = null,
    @SerialName("duration_secs")
    @Serializable(with = FlexibleIntSerializer::class)
    val durationSecs: Int? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val rating: Double? = null,
)
