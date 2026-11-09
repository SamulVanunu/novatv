package dev.anon.novatv.core.epg

import java.io.BufferedInputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/** XMLTV `<channel>` kaydı. */
data class XmltvChannel(
    val id: String,
    val displayName: String,
    val iconUrl: String? = null,
)

/** XMLTV `<programme>` kaydı. `endAt` yoksa akış sırasında süre tamamlanır. */
data class XmltvProgram(
    val channelId: String,
    val startAt: Long,
    val endAt: Long?,
    val title: String,
    val subTitle: String? = null,
    val description: String? = null,
    val category: String? = null,
    val rating: String? = null,
    val episodeNum: String? = null,
    val isNew: Boolean = false,
    val iconUrl: String? = null,
)

/**
 * XMLTV akış ayrıştırıcısı.
 *
 * Büyük (`100 MB+`) rehber dosyaları belleğe alınmaz; SAX olayları doğrudan
 * geri çağrılara akar. Gzip içerik sihirli baytlardan otomatik tanınır.
 *
 * Saat biçimleri: `YYYYMMDDhhmmss +HHMM`, `YYYYMMDDhhmmss` (bölge saati),
 * ISO-8601 ve `yyyy-MM-dd HH:mm:ss`.
 */
class XmltvParser(private val zone: ZoneId = ZoneId.systemDefault()) {

    fun parse(
        input: InputStream,
        onChannel: (XmltvChannel) -> Unit = {},
        onProgram: (XmltvProgram) -> Unit,
    ) {
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
            isValidating = false
        }
        val handler = Handler(zone, onChannel, onProgram)
        factory.newSAXParser().parse(input.gunzipIfNeeded(), handler)
    }
}

/** Gzip sihirli baytlarını (`1f 8b`) görürse açar. */
internal fun InputStream.gunzipIfNeeded(): InputStream {
    val buffered = if (this is BufferedInputStream) this else BufferedInputStream(this)
    buffered.mark(2)
    val first = buffered.read()
    val second = buffered.read()
    buffered.reset()
    return if (first == 0x1f && second == 0x8b) GZIPInputStream(buffered) else buffered
}

/** XMLTV zamanını epoch milisaniyeye çevirir; geçersizse `null`. */
fun parseXmltvTime(raw: String?, zone: ZoneId = ZoneId.systemDefault()): Long? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null

    val compact = COMPACT_TIME.matchEntire(value)
    if (compact != null) {
        val g = compact.groupValues
        return runCatching {
            val local = LocalDateTime.of(
                g[1].toInt(),
                g[2].toInt(),
                g[3].toInt(),
                g[4].toInt(),
                g[5].toInt(),
                g[6].toInt(),
            )
            val sign = g[7]
            if (sign.isNotEmpty()) {
                val offsetSeconds = g[8].toInt() * 3600 + g[9].toInt() * 60
                val total = if (sign == "-") -offsetSeconds else offsetSeconds
                local.toInstant(ZoneOffset.ofTotalSeconds(total)).toEpochMilli()
            } else {
                local.atZone(zone).toInstant().toEpochMilli()
            }
        }.getOrNull()
    }

    runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .getOrNull()
        ?.let { return it }

    return runCatching {
        LocalDateTime.parse(value, SPACED_TIME).atZone(zone).toInstant().toEpochMilli()
    }.getOrNull()
}

/**
 * `xmltv_ns` bölüm numarasını okunur biçime çevirir: `1.2/5.` → `S2 B3`.
 * Diğer sistemlerde (ör. `onscreen`) değer olduğu gibi kullanılır.
 */
fun normalizeEpisodeNum(system: String?, raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (!system.equals("xmltv_ns", ignoreCase = true)) return value

    val fields = value.split('.')
    fun part(index: Int): Int? =
        fields.getOrNull(index)
            ?.substringBefore('/')
            ?.trim()
            ?.toIntOrNull()

    val season = part(0)?.plus(1)
    val episode = part(1)?.plus(1)
    val partNumber = part(2)?.plus(1)
    return when {
        season != null && episode != null && partNumber != null -> "S$season B$episode/$partNumber"
        season != null && episode != null -> "S$season B$episode"
        episode != null -> "B$episode"
        season != null -> "S$season"
        else -> null
    }
}

private class Handler(
    private val zone: ZoneId,
    private val onChannel: (XmltvChannel) -> Unit,
    private val onProgram: (XmltvProgram) -> Unit,
) : DefaultHandler() {

    private var channelId: String? = null
    private var channelName: String? = null
    private var channelIcon: String? = null

    private var program: ProgramState? = null
    private var capture: StringBuilder? = null
    private var captureTarget: CaptureTarget? = null
    private var ratingSystem: String? = null

    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
        when (qName) {
            "channel" -> {
                channelId = attributes.value("id")
                channelName = null
                channelIcon = null
            }

            "display-name" -> if (channelId != null) startCapture(CaptureTarget.CHANNEL_NAME)

            "icon" -> {
                val src = attributes.value("src")?.takeIf { it.isNotBlank() }
                if (channelId != null) channelIcon = src
                program?.let { if (it.iconUrl == null) it.iconUrl = src }
            }

            "programme" -> {
                program = ProgramState(
                    channelId = attributes.value("channel").orEmpty(),
                    start = parseXmltvTime(attributes.value("start"), zone),
                    stop = parseXmltvTime(attributes.value("stop"), zone),
                )
            }

            "title" -> if (program != null) startCapture(CaptureTarget.TITLE)
            "sub-title" -> if (program != null) startCapture(CaptureTarget.SUB_TITLE)
            "desc" -> if (program != null) startCapture(CaptureTarget.DESC)
            "category" -> if (program != null) startCapture(CaptureTarget.CATEGORY)
            "rating" -> if (program != null) ratingSystem = attributes.value("system")
            "value" -> if (program != null && ratingSystem != null) startCapture(CaptureTarget.RATING)
            "episode-num" -> if (program != null) {
                program?.episodeSystem = attributes.value("system")
                startCapture(CaptureTarget.EPISODE)
            }

            "new" -> program?.let { it.isNew = true }
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        capture?.append(ch, start, length)
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        when (qName) {
            "display-name" -> {
                if (captureTarget == CaptureTarget.CHANNEL_NAME) {
                    channelName = captureText()
                }
            }

            "title" -> if (captureTarget == CaptureTarget.TITLE) program?.title = captureText().orEmpty()
            "sub-title" -> if (captureTarget == CaptureTarget.SUB_TITLE) program?.subTitle = captureText()
            "desc" -> if (captureTarget == CaptureTarget.DESC) program?.description = captureText()
            "category" -> if (captureTarget == CaptureTarget.CATEGORY) program?.category = captureText()
            "value" -> if (captureTarget == CaptureTarget.RATING) {
                program?.rating = captureText()
                ratingSystem = null
            }

            "episode-num" -> if (captureTarget == CaptureTarget.EPISODE) {
                program?.episodeNum = normalizeEpisodeNum(program?.episodeSystem, captureText())
            }

            "channel" -> {
                val id = channelId
                if (!id.isNullOrBlank()) {
                    onChannel(
                        XmltvChannel(
                            id = id,
                            displayName = channelName?.takeIf { it.isNotBlank() } ?: id,
                            iconUrl = channelIcon,
                        ),
                    )
                }
                channelId = null
                channelName = null
                channelIcon = null
            }

            "programme" -> {
                program?.let { state ->
                    val start = state.start
                    val title = state.title.trim()
                    if (start != null && state.channelId.isNotBlank() && title.isNotEmpty()) {
                        onProgram(
                            XmltvProgram(
                                channelId = state.channelId,
                                startAt = start,
                                endAt = state.stop,
                                title = title,
                                subTitle = state.subTitle?.trim()?.takeIf { it.isNotEmpty() },
                                description = state.description?.trim()?.takeIf { it.isNotEmpty() },
                                category = state.category?.trim()?.takeIf { it.isNotEmpty() },
                                rating = state.rating?.trim()?.takeIf { it.isNotEmpty() },
                                episodeNum = state.episodeNum,
                                isNew = state.isNew,
                                iconUrl = state.iconUrl,
                            ),
                        )
                    }
                }
                program = null
            }
        }
    }

    private fun startCapture(target: CaptureTarget) {
        capture = StringBuilder()
        captureTarget = target
    }

    private fun captureText(): String? {
        val text = capture?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        capture = null
        captureTarget = null
        return text
    }

    private fun Attributes.value(name: String): String? =
        getValue(name)?.takeIf { it.isNotBlank() }

    private class ProgramState(
        val channelId: String,
        val start: Long?,
        val stop: Long?,
    ) {
        var title: String = ""
        var subTitle: String? = null
        var description: String? = null
        var category: String? = null
        var rating: String? = null
        var episodeNum: String? = null
        var episodeSystem: String? = null
        var isNew: Boolean = false
        var iconUrl: String? = null
    }
}

private enum class CaptureTarget { CHANNEL_NAME, TITLE, SUB_TITLE, DESC, CATEGORY, RATING, EPISODE }

private val COMPACT_TIME = Regex("""^(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})(?:\s*([+-])(\d{2})(\d{2}))?$""")
private val SPACED_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
