package dev.anon.novatv.core.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NovaHttp {

    const val DEFAULT_USER_AGENT = "NovaTV/1.0 (Android)"

    fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}
