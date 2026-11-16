package com.abhi.armz.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Result of the capability probe performed before a download starts. */
data class ProbeResult(
    val finalUrl: String,
    val contentLength: Long,
    val rangeSupported: Boolean,
    val etag: String?,
    val lastModified: String?,
    val fileName: String?,
    val mimeType: String?,
)

/**
 * Detects size, byte-range support, validators and a suggested file name.
 *
 * A single `Range: bytes=0-0` GET is the most reliable signal and avoids the extra round trip a
 * HEAD request would add. 206 means ranges work and Content-Range carries the full length; 200
 * means the server ignored the range and the engine must fall back to one connection.
 */
class HttpProbe(private val client: OkHttpClient) {

    suspend fun probe(
        url: String,
        headers: Map<String, String> = emptyMap(),
        registerCall: (Call) -> Unit = {},
        unregisterCall: (Call) -> Unit = {},
    ): ProbeResult = withContext(Dispatchers.IO) {
        probeOnce(url, headers, registerCall, unregisterCall)
    }

    private fun probeOnce(
        url: String,
        headers: Map<String, String>,
        registerCall: (Call) -> Unit,
        unregisterCall: (Call) -> Unit,
    ): ProbeResult {
        val request = buildRequest(url = url, headers = headers, range = "bytes=0-0")
        val call = client.newCall(request)
        registerCall(call)
        try {
            call.execute().use { response ->
                val finalUrl = response.request.url.toString()
                val disposition = response.header("Content-Disposition")
                val contentType = response.header("Content-Type")
                val etag = response.header("ETag")
                val lastModified = response.header("Last-Modified")
                val fileName = FileNames.fromContentDisposition(disposition)
                    ?: FileNames.fromUrl(finalUrl)

                when {
                    response.code == 206 -> {
                        val total = parseRangeTotal(response.header("Content-Range"))
                        if (total == null || total <= 0L) {
                            throw EngineException.Unexpected("Server returned 206 without a usable total size")
                        }
                        return ProbeResult(
                            finalUrl = finalUrl,
                            contentLength = total,
                            rangeSupported = true,
                            etag = etag,
                            lastModified = lastModified,
                            fileName = fileName,
                            mimeType = contentType,
                        )
                    }

                    response.code == 200 -> {
                        val total = response.header("Content-Length")?.toLongOrNull() ?: -1L
                        return ProbeResult(
                            finalUrl = finalUrl,
                            contentLength = total,
                            rangeSupported = false,
                            etag = etag,
                            lastModified = lastModified,
                            fileName = fileName,
                            mimeType = contentType,
                        )
                    }

                    response.code == 416 ->
                        throw EngineException.Unexpected("Server rejected byte range 0-0 (HTTP 416)")

                    isRetryableStatus(response.code) ->
                        throw EngineException.RetryableHttp(response.code, url)

                    isTerminalStatus(response.code) ->
                        throw EngineException.Http(response.code, url)

                    else -> throw EngineException.Http(response.code, url)
                }
            }
        } finally {
            unregisterCall(call)
        }
    }

    private fun parseRangeTotal(header: String?): Long? {
        if (header.isNullOrBlank()) return null
        val total = header.substringAfterLast('/', missingDelimiterValue = "").trim()
        if (total.isEmpty() || total == "*") return null
        return total.toLongOrNull()
    }

    private fun buildRequest(
        url: String,
        headers: Map<String, String>,
        range: String? = null,
    ): Request {
        val builder = Request.Builder().url(url).get()
        headers.forEach { (name, value) ->
            if (!name.equals("Range", ignoreCase = true) &&
                !name.equals("Accept-Encoding", ignoreCase = true)
            ) {
                builder.header(name, value)
            }
        }
        builder.header("Accept-Encoding", "identity")
        range?.let { builder.header("Range", it) }
        return builder.build()
    }
}

internal fun isRetryableStatus(code: Int): Boolean =
    code == 408 || code == 429 || code in 500..599

internal fun isTerminalStatus(code: Int): Boolean =
    code == 403 || code == 404 || code == 410

internal fun isBodylessSuccess(code: Int): Boolean = code in 200..299

@Suppress("unused")
internal fun IOException.asNetworkFailure(): EngineException = EngineException.Network(this)
