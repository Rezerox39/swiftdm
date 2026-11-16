package com.abhi.armz.engine

import java.io.File

enum class DownloadStatus {
    Idle,
    Probing,
    Downloading,
    Finalizing,
    Paused,
    Completed,
    Failed,
    Cancelled;

    val isTerminal: Boolean
        get() = this == Completed || this == Failed || this == Cancelled

    val isActive: Boolean
        get() = this == Probing || this == Downloading || this == Finalizing
}

/** Visible stages between the last received byte and a published file. */
enum class FinalizeStep(val label: String) {
    Verifying("Verifying"),
    Syncing("Syncing"),
    Hashing("Checking integrity"),
    Publishing("Saving"),
}

enum class SegmentState {
    Pending,
    Active,
    Done,
    Failed,
}

data class SegmentCheckpoint(
    val index: Int,
    val start: Long,
    val end: Long,
    val downloaded: Long,
)

data class ResumeState(
    val filePath: String,
    val totalBytes: Long,
    val etag: String?,
    val lastModified: String?,
    val segments: List<SegmentCheckpoint>,
)

data class DownloadRequest(
    val id: String,
    val url: String,
    val destinationDir: File,
    val segments: Int = 8,
    val fileNameOverride: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val resume: ResumeState? = null,
    val expectedSha256: String? = null,
    val mimeTypeHint: String? = null,
) {
    val safeSegments: Int
        get() = segments.coerceIn(MinSegments, MaxSegments)

    companion object {
        const val MinSegments = 1
        const val MaxSegments = 32
    }
}

internal fun DownloadRequest.placeholderSnapshot(): DownloadSnapshot = DownloadSnapshot(
    id = id,
    url = url,
    fileName = fileNameOverride ?: "",
    filePath = "",
    totalBytes = resume?.totalBytes ?: -1L,
    downloadedBytes = 0L,
    status = DownloadStatus.Probing,
    bytesPerSecond = 0L,
    etaSeconds = null,
    rangeSupported = false,
    etag = resume?.etag,
    lastModified = resume?.lastModified,
    segmentCount = safeSegments,
    segments = emptyList(),
    sha256 = null,
    error = null,
)

data class SegmentSnapshot(
    val index: Int,
    val start: Long,
    val end: Long,
    val downloaded: Long,
    val state: SegmentState,
) {
    val fraction: Float
        get() {
            val length = end - start
            if (length <= 0L) return if (state == SegmentState.Done) 1f else 0f
            return (downloaded.toDouble() / length.toDouble()).toFloat().coerceIn(0f, 1f)
        }
}

data class DownloadSnapshot(
    val id: String,
    val url: String,
    val fileName: String,
    val filePath: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val bytesPerSecond: Long,
    val etaSeconds: Long?,
    val rangeSupported: Boolean,
    val etag: String?,
    val lastModified: String?,
    val segmentCount: Int,
    val segments: List<SegmentSnapshot>,
    val sha256: String?,
    val error: EngineError?,
    val finalizeStep: FinalizeStep? = null,
    val finalizeProgress: Float = 0f,
    val contentUri: String? = null,
    val mimeType: String? = null,
) {
    val fraction: Float
        get() {
            if (totalBytes <= 0L) return 0f
            return (downloadedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
        }

    /** 0..1 progress across the whole visible operation, including Assembling. */
    val overallFraction: Float
        get() = when (status) {
            DownloadStatus.Finalizing -> finalizeProgress.coerceIn(0f, 1f)
            else -> fraction
        }
}

data class EngineError(
    val type: String,
    val message: String,
    val httpCode: Int? = null,
)

/** Terminal engine failures. Retryable conditions use [EngineException.RetryableHttp] / IOException. */
sealed class EngineException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Http(val code: Int, val url: String) : EngineException("HTTP $code for $url")

    class RetryableHttp(val code: Int, val url: String) :
        EngineException("Temporary HTTP $code for $url")

    class RetryExhausted(val attempts: Int, cause: Throwable?) :
        EngineException(
            "Giving up after $attempts attempts: ${cause?.message ?: "unknown error"}",
            cause,
        )

    class Network(cause: Throwable) : EngineException("Network error: ${cause.message}", cause)

    class Storage(message: String, cause: Throwable? = null) : EngineException(message, cause)

    class Checksum(val expected: String, val actual: String) :
        EngineException("SHA-256 mismatch: expected $expected, got $actual")

    class Coverage(message: String) : EngineException(message)

    class Unexpected(message: String, cause: Throwable? = null) : EngineException(message, cause)
}

fun EngineException.toEngineError(): EngineError = when (this) {
    is EngineException.Http -> EngineError("http", message.orEmpty(), code)
    is EngineException.RetryableHttp -> EngineError("http", message.orEmpty(), code)
    is EngineException.RetryExhausted -> EngineError("retry_exhausted", message.orEmpty())
    is EngineException.Network -> EngineError("network", message.orEmpty())
    is EngineException.Storage -> EngineError("storage", message.orEmpty())
    is EngineException.Checksum -> EngineError("checksum", message.orEmpty())
    is EngineException.Coverage -> EngineError("coverage", message.orEmpty())
    is EngineException.Unexpected -> EngineError("unexpected", message.orEmpty())
}

/**
 * Persistence hook. [clearSegments] is called after a successful publish so completed rows keep
 * their history without carrying checkpoint rows forever.
 */
interface CheckpointSink {
    suspend fun persist(snapshot: DownloadSnapshot)

    suspend fun clearSegments(downloadId: String) = Unit

    companion object {
        val None: CheckpointSink = object : CheckpointSink {
            override suspend fun persist(snapshot: DownloadSnapshot) = Unit
        }
    }
}
