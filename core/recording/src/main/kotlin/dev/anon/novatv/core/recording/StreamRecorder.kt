package dev.anon.novatv.core.recording

import dev.anon.novatv.core.model.redactUrl
import dev.anon.novatv.core.network.NovaHttp
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Kayıt sonucu; hata varsa [bytes] o ana kadar yazılan bayt sayısıdır. */
data class RecordResult(
    val bytes: Long,
    val path: String?,
    val error: String? = null,
) {
    val success: Boolean get() = error == null && path != null
}

/**
 * Yayını diske yazan çekirdek.
 *
 * İki biçim desteklenir:
 * - MPEG-TS / ham akış: bağlantı kopunca artan beklemeyle yeniden bağlanır,
 *   dosyanın sonuna eklemeye devam eder.
 * - HLS: canlı medya listesi hedef süre kadar aralıklarla yeniden okunur,
 *   yalnızca yeni segmentler indirilir; şifreli (`EXT-X-KEY`) akış reddedilir.
 *
 * Akış adresi hiçbir yere yazılmaz; hata metinleri `redactUrl` ile temizlenir.
 */
class StreamRecorder(
    private val client: OkHttpClient = NovaHttp.client(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val sleep: suspend (Long) -> Unit = { delay(it) },
) {

    suspend fun record(
        plan: RecordingPlan,
        url: String,
        headers: Map<String, String> = emptyMap(),
        target: File,
        onBytes: (Long) -> Unit = {},
    ): RecordResult = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        runCatching {
            if (isHlsUrl(url)) {
                recordHls(plan, url, headers, target, onBytes)
            } else {
                recordTs(plan, url, headers, target, onBytes)
            }
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            RecordResult(
                bytes = target.length().takeIf { target.exists() } ?: 0L,
                path = null,
                error = throwable.message ?: "Kayıt başarısız",
            )
        }
    }

    private suspend fun recordTs(
        plan: RecordingPlan,
        url: String,
        headers: Map<String, String>,
        target: File,
        onBytes: (Long) -> Unit,
    ): RecordResult {
        var total = if (target.exists()) target.length() else 0L
        var attempt = 0

        FileOutputStream(target, true).use { output ->
            while (clock() < plan.endAt) {
                try {
                    downloadRaw(url, headers, output, plan.endAt) { written ->
                        total += written
                        onBytes(total)
                    }
                    attempt = 0
                    // Yayın erken kapandıysa hemen yeniden bağlanma.
                    val remaining = plan.endAt - clock()
                    if (remaining > 0) sleep(RECONNECT_DELAY_MS)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    attempt++
                    if (clock() >= plan.endAt) break
                    if (attempt >= MAX_CONSECUTIVE_FAILURES && total == 0L) {
                        return RecordResult(
                            bytes = total,
                            path = null,
                            error = "Kayıt akışına bağlanılamadı: ${describe(error, url)}",
                        )
                    }
                    sleep(backoff(attempt))
                }
            }
        }

        if (total <= 0L) {
            target.delete()
            return RecordResult(0, null, "Kayıt akışı boş döndü")
        }
        return RecordResult(bytes = total, path = target.absolutePath)
    }

    private fun downloadRaw(
        url: String,
        headers: Map<String, String>,
        output: FileOutputStream,
        deadline: Long,
        onWritten: (Long) -> Unit,
    ) {
        client.newCall(request(url, headers)).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Yanıt gövdesi yok")
            val input = body.byteStream()
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                if (clock() >= deadline) break
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                output.flush()
                onWritten(read.toLong())
            }
        }
    }

    private suspend fun recordHls(
        plan: RecordingPlan,
        url: String,
        headers: Map<String, String>,
        target: File,
        onBytes: (Long) -> Unit,
    ): RecordResult {
        var playlistUrl = url
        val first = fetchPlaylist(playlistUrl, headers)
        if (first.encrypted) {
            return RecordResult(0, null, "Şifreli HLS akışı kaydedilemez")
        }
        if (first.variants.isNotEmpty()) {
            playlistUrl = first.variants.maxByOrNull { it.bandwidth }?.url ?: playlistUrl
        }

        val seen = HashSet<String>()
        var total = if (target.exists()) target.length() else 0L
        var failures = 0

        FileOutputStream(target, true).use { output ->
            while (clock() < plan.endAt) {
                try {
                    val media = fetchPlaylist(playlistUrl, headers)
                    if (media.encrypted) {
                        return RecordResult(
                            bytes = total,
                            path = null,
                            error = "Şifreli HLS akışı kaydedilemez",
                        )
                    }
                    media.segments.forEach { segment ->
                        if (!seen.add(segment)) return@forEach
                        downloadSegment(segment, headers, output, plan.endAt) { written ->
                            total += written
                            onBytes(total)
                        }
                        failures = 0
                    }
                    if (media.ended) break

                    val remaining = plan.endAt - clock()
                    if (remaining <= 0) break
                    sleep(media.targetSeconds.coerceIn(2, 10) * 1000L)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    failures++
                    if (failures >= MAX_CONSECUTIVE_FAILURES) {
                        return RecordResult(
                            bytes = total,
                            path = null,
                            error = "HLS akışı kesildi: ${describe(error, playlistUrl)}",
                        )
                    }
                    sleep(backoff(failures))
                }
            }
        }

        if (total <= 0L) {
            target.delete()
            return RecordResult(0, null, "HLS akışında segment bulunamadı")
        }
        return RecordResult(bytes = total, path = target.absolutePath)
    }

    private suspend fun fetchPlaylist(url: String, headers: Map<String, String>): HlsMedia =
        withContext(Dispatchers.IO) {
            client.newCall(request(url, headers)).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val text = response.body?.string().orEmpty()
                if (text.isBlank()) throw IOException("Liste boş")
                parseHlsPlaylist(text, url)
            }
        }

    private fun downloadSegment(
        url: String,
        headers: Map<String, String>,
        output: FileOutputStream,
        deadline: Long,
        onWritten: (Long) -> Unit,
    ) {
        client.newCall(request(url, headers)).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Segment gövdesi yok")
            val input = body.byteStream()
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                if (clock() >= deadline) break
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                output.flush()
                onWritten(read.toLong())
            }
        }
    }

    private fun request(url: String, headers: Map<String, String>): Request {
        val builder = Request.Builder().url(url)
        headers.forEach { (name, value) -> runCatching { builder.header(name, value) } }
        if (headers.keys.none { it.equals("User-Agent", ignoreCase = true) }) {
            runCatching { builder.header("User-Agent", NovaHttp.DEFAULT_USER_AGENT) }
        }
        return builder.build()
    }

    private fun describe(error: Throwable, url: String): String =
        (error.message ?: error::class.simpleName ?: "hata") + " · " + redactUrl(url)

    private fun backoff(attempt: Int): Long =
        (BACKOFF_BASE_MS shl (attempt - 1).coerceIn(0, 5)).coerceAtMost(MAX_BACKOFF_MS)

    private companion object {
        const val BUFFER_BYTES = 64 * 1024
        const val RECONNECT_DELAY_MS = 400L
        const val BACKOFF_BASE_MS = 1_000L
        const val MAX_BACKOFF_MS = 20_000L
        const val MAX_CONSECUTIVE_FAILURES = 6
    }
}
