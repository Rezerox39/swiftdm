package com.abhi.swiftdm.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 * A HEAD request is attempted first. If HEAD fails, is blocked, or does not advertise ranges we
 * fall back to a one-byte ranged GET (`Range: bytes=0-0`), which is the most reliable signal:
 * 206 means ranges work and Content-Range carries the full length, 200 means we must fall back to
 * a single connection.
 */
class HttpProbe(private val client: OkHttpClient) {

    suspend fun probe(url: String, headers: Map<String, String> = emptyMap()): ProbeResult =
        withContext(Dispatchers.IO) { probeOnce(url, headers) }

    private fun probeOnce(url: String, headers: Map<String, String>): ProbeResult {
        var headLength = -1L
        var headDisposition: String? = null
        var headContentType: String? = null
        var headEtag: String? = null
        var headLastModified: String? = null
        var headFinalUrl: String? = null

        val headRequest = buildRequest(url = url, headers = headers, head = true)
        client.newCall(headRequest).execute().use { response ->
            headFinalUrl = response.request.url.toString()
            headDisposition = response.header("Content-Disposition")
            headContentType = response.header("Content-Type")
            headEtag = response.header("ETag")
            headLastModified = response.header("Last-Modified")
            headLength = response.header("Content-Length")?.toLongOrNull() ?: -1L

            when {
                response.code == 404 || response.code == 410 ->
                    throw EngineException.Http(response.code, url)

                response.code == 403 -> Unit // Some CDNs block HEAD only; retry with GET below.
                response.code in 500..599 || response.code == 408 || response.code == 429 ->
                    throw EngineException.RetryableHttp(response.code, url)
            }

            val advertisesRanges = response.header("Accept-Ranges")?.equals("bytes", true) == true
            if (response.isSuccessful && headLength > 0 && advertisesRanges) {
                return ProbeResult(
                    finalUrl = headFinalUrl ?: url,
                    contentLength = headLength,
                    rangeSupported = true,
                    etag = headEtag,
                    lastModified = headLastModified,
                    fileName = FileNames.fromContentDisposition(headDisposition),
                    mimeType = headContentType,
                )
            }
        }

        val rangeRequest = buildRequest(url = url, headers = headers, range = "bytes=0-0")
        client.newCall(rangeRequest).execute().use { response ->
            val finalUrl = response.request.url.toString()
            val disposition = response.header("Content-Disposition") ?: headDisposition
            val contentType = response.header("Content-Type") ?: headContentType
            val etag = response.header("ETag") ?: headEtag
            val lastModified = response.header("Last-Modified") ?: headLastModified
            val fileName = FileNames.fromContentDisposition(disposition)
                ?: FileNames.fromUrl(finalUrl)

            when {
                response.code == 206 -> {
                    val total = parseRangeTotal(response.header("Content-Range")) ?: headLength
                    if (total <= 0L) throw EngineException.Unexpected("Server returned 206 without a usable total size")
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
                    val total = response.header("Content-Length")?.toLongOrNull() ?: headLength
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

                response.code == 416 -> throw EngineException.Unexpected("Server rejected byte range 0-0 (HTTP 416)")

                response.code in 500..599 || response.code == 408 || response.code == 429 ->
                    throw EngineException.RetryableHttp(response.code, url)

                else -> throw EngineException.Http(response.code, url)
            }
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
        head: Boolean = false,
    ): Request {
        val builder = Request.Builder().url(url)
        if (head) builder.head() else builder.get()
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
