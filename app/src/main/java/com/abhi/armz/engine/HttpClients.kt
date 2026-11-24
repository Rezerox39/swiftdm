package com.abhi.armz.engine

import kotlinx.coroutines.sync.Semaphore
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.concurrent.TimeUnit

/**
 * Shared HTTP stack for the download engine. A single client is reused across sessions so the
 * connection pool and dispatcher stay warm, and one semaphore caps ranged GETs across every
 * active download instead of letting each session open its own full set of connections.
 */
object HttpClients {

    const val DefaultMaxSegments: Int = 48

    private val segmentSlots = Semaphore(DefaultMaxSegments)

    /** Suspends until a global connection slot is free. */
    suspend fun <T> withSegmentSlot(block: suspend () -> T): T {
        segmentSlots.acquire()
        try {
            return block()
        } finally {
            segmentSlots.release()
        }
    }

    fun downloadClient(maxConcurrentRequests: Int = DefaultMaxSegments): OkHttpClient {
        val concurrency = maxConcurrentRequests.coerceIn(1, 64)
        val dispatcher = Dispatcher().apply {
            maxRequests = concurrency
            maxRequestsPerHost = concurrency
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
