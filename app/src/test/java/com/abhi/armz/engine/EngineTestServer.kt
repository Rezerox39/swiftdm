package com.abhi.armz.engine

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

/**
 * Minimal test origin: HEAD advertises metadata, ranged GETs return 206 slices and a plain GET
 * returns the whole body. It can ignore Range headers, inject 5xx responses, force a status or
 * throttle bodies so a pause can be observed deterministically.
 */
internal class EngineTestServer(
    content: ByteArray,
    supportsRanges: Boolean = true,
) : Dispatcher() {

    @Volatile
    var content: ByteArray = content

    @Volatile
    var supportsRanges: Boolean = supportsRanges

    @Volatile
    var etag: String? = "\"v1\""

    @Volatile
    var lastModified: String? = "Wed, 21 Oct 2026 07:28:00 GMT"

    @Volatile
    var failuresRemaining: Int = 0

    @Volatile
    var forcedGetStatus: Int? = null

    @Volatile
    var throttleBytes: Long = 0L

    @Volatile
    var throttlePeriodMs: Long = 0L

    @Volatile
    var headDelayMs: Long = 0L

    @Volatile
    var getDelayMs: Long = 0L

    val rangeHeaders = ConcurrentLinkedQueue<String>()
    val getCount = AtomicInteger()
    val headCount = AtomicInteger()

    override fun dispatch(request: RecordedRequest): MockResponse {
        return if (request.method == "HEAD") {
            headCount.incrementAndGet()
            headResponse()
        } else {
            getCount.incrementAndGet()
            getResponse(request)
        }
    }

    private fun headResponse(): MockResponse {
        val response = MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Length", content.size.toString())
            .setHeader("Content-Type", "application/octet-stream")
            .setHeader("Content-Disposition", "attachment; filename=\"sample.bin\"")
            .validators()
        if (supportsRanges) response.setHeader("Accept-Ranges", "bytes")
        if (headDelayMs > 0L) response.setHeadersDelay(headDelayMs, TimeUnit.MILLISECONDS)
        return response
    }

    private fun getResponse(request: RecordedRequest): MockResponse {
        forcedGetStatus?.let { status ->
            return MockResponse().setResponseCode(status)
        }
        if (failuresRemaining > 0) {
            failuresRemaining -= 1
            return MockResponse().setResponseCode(503)
        }

        val range = request.getHeader("Range")
        if (range == null || !supportsRanges) {
            val response = withThrottle(fullResponse())
            if (getDelayMs > 0L) response.setHeadersDelay(getDelayMs, TimeUnit.MILLISECONDS)
            return response
        }

        rangeHeaders += range
        val (start, endInclusive) = parseRange(range, content.size)
        if (start >= content.size) {
            return MockResponse()
                .setResponseCode(416)
                .setHeader("Content-Range", "bytes */${content.size}")
        }
        val safeEnd = min(endInclusive, content.size - 1)
        val slice = content.copyOfRange(start, safeEnd + 1)
        val response = MockResponse()
            .setResponseCode(206)
            .setHeader("Content-Range", "bytes $start-$safeEnd/${content.size}")
            .setHeader("Content-Length", slice.size.toString())
            .setHeader("Content-Type", "application/octet-stream")
            .validators()
            .setBody(Buffer().write(slice))
        val throttled = withThrottle(response)
        if (getDelayMs > 0L) throttled.setHeadersDelay(getDelayMs, TimeUnit.MILLISECONDS)
        return throttled
    }

    private fun fullResponse(): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Length", content.size.toString())
        .setHeader("Content-Type", "application/octet-stream")
        .setHeader("Content-Disposition", "attachment; filename=\"sample.bin\"")
        .validators()
        .setBody(Buffer().write(content))

    private fun MockResponse.validators(): MockResponse {
        etag?.let { setHeader("ETag", it) }
        lastModified?.let { setHeader("Last-Modified", it) }
        return this
    }

    private fun withThrottle(response: MockResponse): MockResponse {
        if (throttleBytes > 0L && throttlePeriodMs > 0L) {
            response.throttleBody(throttleBytes, throttlePeriodMs, TimeUnit.MILLISECONDS)
        }
        return response
    }

    private fun parseRange(range: String, size: Int): Pair<Int, Int> {
        val spec = range.substringAfter("bytes=", missingDelimiterValue = "")
        val start = spec.substringBefore('-').trim().toIntOrNull() ?: 0
        val end = spec.substringAfter('-', missingDelimiterValue = "").trim().toIntOrNull() ?: (size - 1)
        return start to end
    }
}

internal fun rangeStart(header: String): Int =
    header.substringAfter("bytes=", missingDelimiterValue = "")
        .substringBefore('-')
        .trim()
        .toIntOrNull() ?: -1
