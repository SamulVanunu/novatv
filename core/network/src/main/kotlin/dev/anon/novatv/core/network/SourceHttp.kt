package dev.anon.novatv.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class SourceHttp(
    private val client: OkHttpClient,
    private val userAgent: String = NovaHttp.DEFAULT_USER_AGENT,
) {

    suspend fun getText(url: String, headers: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            var lastError: IOException? = null
            repeat(MAX_ATTEMPTS) { attempt ->
                try {
                    val request = request(url, headers)
                    client.newCall(request).execute().use { response ->
                        when {
                            response.isSuccessful -> return@withContext response.body?.string().orEmpty()
                            response.code == 401 || response.code == 403 ->
                                throw SourceAuthException("Kimlik reddedildi (HTTP ${response.code})")
                            response.code in 500..599 ->
                                lastError = SourceNetworkException("Sunucu hatası (HTTP ${response.code})")
                            else ->
                                throw SourceNetworkException("İstek başarısız (HTTP ${response.code})")
                        }
                    }
                } catch (e: SourceAuthException) {
                    throw e
                } catch (e: IOException) {
                    lastError = e
                }
                if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_BASE_DELAY_MS * (attempt + 1))
            }
            throw lastError ?: SourceNetworkException("İstek başarısız")
        }

    fun newCall(url: String, headers: Map<String, String> = emptyMap()): Call =
        client.newCall(request(url, headers))

    suspend fun openStream(url: String, headers: Map<String, String> = emptyMap()): Response =
        withContext(Dispatchers.IO) {
            val response = client.newCall(request(url, headers)).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                if (code == 401 || code == 403) throw SourceAuthException("Kimlik reddedildi (HTTP $code)")
                throw SourceNetworkException("Akış açılamadı (HTTP $code)")
            }
            response
        }

    private fun request(url: String, headers: Map<String, String>): Request =
        Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val RETRY_BASE_DELAY_MS = 200L
    }
}
