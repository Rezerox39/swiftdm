package com.abhi.armz.engine

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit

/**
 * Shared HTTP stack for the download engine. A single client is reused across sessions so the
 * connection pool and dispatcher stay warm; per-host limits are raised above the default of 5 so
 * parallel segments are not queued behind each other.
 */
object HttpClients {

    const val DefaultMaxSegments: Int = 32

    fun downloadClient(maxConcurrentRequests: Int = DefaultMaxSegments): OkHttpClient {
        val concurrency = maxConcurrentRequests.coerceIn(1, 64)
        val dispatcher = Dispatcher().apply {
            maxRequests = concurrency * 4
            maxRequestsPerHost = concurrency * 4
        }
        return OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .dispatcher(dispatcher)
            .connectionPool(ConnectionPool(concurrency, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val shared: OkHttpClient by lazy { downloadClient() }
}
