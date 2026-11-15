package com.abhi.swiftdm.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * Entry point for the download engine. Pure Kotlin/JVM so it can be unit-tested without Android.
 *
 * Lifecycle: [start] probes the URL, resolves a destination file and begins writing. A paused
 * download keeps its partial bytes and can be resumed (with ETag / Last-Modified validation), and
 * a failed one can be resumed from the last persisted checkpoint.
 */
class DownloadEngine(
    private val client: OkHttpClient = HttpClients.shared,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val log: (String) -> Unit = {},
) {

    private val prober = HttpProbe(client)

    fun start(
        request: DownloadRequest,
        checkpointSink: CheckpointSink = CheckpointSink.None,
    ): DownloadHandle {
        val session = DownloadSession(
            request = request,
            client = client,
            prober = prober,
            retryPolicy = retryPolicy,
            checkpointSink = checkpointSink,
            clock = clock,
            log = log,
        )
        session.start()
        return session.handle
    }
}

/** Control surface for one download. All mutating calls are safe to make from the UI coroutine. */
class DownloadHandle internal constructor(
    val id: String,
    val state: StateFlow<DownloadSnapshot>,
    private val session: DownloadSession,
) {
    suspend fun pause() = session.pause()

    suspend fun resume() = session.resume()

    suspend fun cancel() = session.cancel()
}

internal class DownloadSession(
    private val request: DownloadRequest,
    private val client: OkHttpClient,
    private val prober: HttpProbe,
    private val retryPolicy: RetryPolicy,
    private val checkpointSink: CheckpointSink,
    private val clock: () -> Long,
    private val log: (String) -> Unit,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(request.placeholderSnapshot())
    val state: StateFlow<DownloadSnapshot> = mutableState.asStateFlow()
    val handle: DownloadHandle = DownloadHandle(request.id, state, this)

    @Volatile
    private var status: DownloadStatus = DownloadStatus.Probing

    @Volatile
    private var lastError: EngineError? = null

    @Volatile
    private var probeResult: ProbeResult? = null

    @Volatile
    private var targetFile: File? = null

    @Volatile
    private var sha256: String? = null

    @Volatile
    private var singleCursor: Long = 0L

    @Volatile
    private var singleTotal: Long = -1L

    @Volatile
    private var store: SegmentStore? = null

    @Volatile
    private var writer: SegmentFileWriter? = null

    private val speedMeter = SpeedMeter(clock = clock)
    private val activeCalls: MutableSet<Call> =
        Collections.newSetFromMap(ConcurrentHashMap<Call, Boolean>())

    private var job: Job? = null

    @Volatile
    private var persistAt: Long = 0L

    init {
        mutableState.value = buildSnapshot()
    }

    fun start() {
        if (job?.isActive == true) return
        startJob(revalidate = false)
    }

    private fun startJob(revalidate: Boolean) {
        val newJob = scope.launch { runDownload(revalidate) }
        job = newJob
        newJob.invokeOnCompletion { activeCalls.forEach { it.cancel() } }
    }

    private suspend fun runDownload(revalidate: Boolean) {
        try {
            log("start ${request.id} ${request.url} segments=${request.safeSegments}")
            status = DownloadStatus.Probing
            lastError = null
            publish(persist = true)

            val probe = probeWithRetry()
            probeResult = probe
            log(
                "probe total=${probe.contentLength} ranges=${probe.rangeSupported} " +
                    "etag=${probe.etag} lastModified=${probe.lastModified}"
            )

            val resume = if (revalidate) currentResumeState() else request.resume
            val resumeFile = resume?.let { File(it.filePath) }
            val missingFile = resumeFile != null && !resumeFile.exists()
            val contentChanged = resume != null && (missingFile || metadataChanged(resume, probe))

            val target = resumeFile ?: resolveTarget(probe)
            targetFile = target
            log("target ${target.absolutePath} resume=${resume != null} restart=$contentChanged")
            target.parentFile?.mkdirs()
            if (contentChanged) resetForRestart()

            val alreadyDownloaded = if (!contentChanged) {
                resume?.segments?.sumOf { it.downloaded } ?: 0L
            } else {
                0L
            }
            val remaining = if (probe.contentLength > 0L) {
                (probe.contentLength - alreadyDownloaded).coerceAtLeast(0L)
            } else {
                0L
            }
            ensureFreeSpace(target.parentFile ?: request.destinationDir, remaining)

            if (probe.rangeSupported && probe.contentLength > 0L) {
                runSegmented(probe, resume.takeIf { !contentChanged })
            } else {
                runSingle(probe, resume.takeIf { !contentChanged })
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: EngineException) {
            fail(failure)
        } catch (failure: IOException) {
            fail(EngineException.Network(failure))
        } catch (failure: Exception) {
            fail(EngineException.Unexpected(failure.message ?: "Unexpected engine failure", failure))
        }
    }

    private suspend fun probeWithRetry(): ProbeResult {
        var attempt = 0
        while (true) {
            attempt++
            try {
                return prober.probe(request.url, request.headers)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (retryable: EngineException.RetryableHttp) {
                if (attempt >= retryPolicy.maxAttempts) {
                    throw EngineException.Http(retryable.code, request.url)
                }
                log("probe retry $attempt after HTTP ${retryable.code}")
                delay(retryPolicy.delayMillisAfter(attempt))
            } catch (network: IOException) {
                if (attempt >= retryPolicy.maxAttempts) throw EngineException.Network(network)
                log("probe retry $attempt after ${network.message}")
                delay(retryPolicy.delayMillisAfter(attempt))
            }
        }
    }

    // ---------------------------------------------------------------- segmented downloads

    private suspend fun runSegmented(probe: ProbeResult, resume: ResumeState?) {
        val total = probe.contentLength
        val file = targetFile ?: throw EngineException.Unexpected("No destination file")
        val segmentStore = store ?: buildStore(resume, total).also { store = it }
        file.parentFile?.mkdirs()
        if (!file.exists()) file.createNewFile()
        val fileWriter = writer ?: SegmentFileWriter(file, total).also { writer = it }

        speedMeter.reset()
        speedMeter.onProgress(segmentStore.downloadedTotal())
        status = DownloadStatus.Downloading
        log("segmented total=$total ranges=${segmentStore.size} already=${segmentStore.downloadedTotal()}")
        publish(persist = true)

        coroutineScope {
            val telemetry = launch { telemetryLoop() }
            try {
                // Inner scope waits for every worker; only then is telemetry stopped.
                coroutineScope {
                    repeat(request.safeSegments) {
                        launch { workerLoop(segmentStore) }
                    }
                }
            } finally {
                telemetry.cancel()
            }
        }

        currentCoroutineContext().ensureActive()
        if (status != DownloadStatus.Downloading) return
        if (!segmentStore.isComplete() || segmentStore.downloadedTotal() != total) {
            throw EngineException.Unexpected(
                "Download stopped at ${segmentStore.downloadedTotal()} of $total bytes"
            )
        }
        fileWriter.flush()
        finish(file)
    }

    private fun buildStore(resume: ResumeState?, total: Long): SegmentStore {
        val persisted = resume?.segments?.takeIf { segments ->
            segments.isNotEmpty() &&
                segments.sumOf { it.end - it.start } == total &&
                segments.all { it.start >= 0L && it.end <= total && it.start < it.end }
        }
        return SegmentStore(
            initial = persisted ?: SegmentPlanner.plan(total, request.safeSegments),
            totalBytes = total,
        )
    }

    private suspend fun telemetryLoop() {
        while (currentCoroutineContext().isActive) {
            publish(persist = clock() - persistAt >= CheckpointIntervalMs)
            delay(TelemetryIntervalMs)
        }
    }

    private suspend fun workerLoop(segmentStore: SegmentStore) {
        var index = segmentStore.claimNext()
        while (index != null) {
            downloadSegment(segmentStore, index)
            currentCoroutineContext().ensureActive()
            segmentStore.markDone(index)
            index = segmentStore.splitLargest(MinSplitBytes) ?: segmentStore.claimNext()
        }
    }

    private suspend fun downloadSegment(segmentStore: SegmentStore, index: Int) {
        var attempt = 0
        while (true) {
            attempt++
            try {
                downloadSegmentOnce(segmentStore, index)
                return
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (retryable: EngineException.RetryableHttp) {
                currentCoroutineContext().ensureActive()
                if (attempt >= retryPolicy.maxAttempts) {
                    throw EngineException.RetryExhausted(attempt, retryable)
                }
                log("segment $index retry $attempt after HTTP ${retryable.code}")
                delay(retryPolicy.delayMillisAfter(attempt))
            } catch (network: IOException) {
                currentCoroutineContext().ensureActive()
                if (attempt >= retryPolicy.maxAttempts) {
                    throw EngineException.RetryExhausted(attempt, network)
                }
                log("segment $index retry $attempt after ${network.message}")
                delay(retryPolicy.delayMillisAfter(attempt))
            }
        }
    }

    private suspend fun downloadSegmentOnce(segmentStore: SegmentStore, index: Int) {
        var cursor = segmentStore.cursorOf(index)
        val end = segmentStore.endOf(index)
        if (cursor >= end) return

        val url = probeResult?.finalUrl ?: request.url
        val builder = Request.Builder()
            .url(url)
            .get()
            .header("Range", "bytes=$cursor-${end - 1}")
        applyHeaders(builder)

        val call = client.newCall(builder.build())
        activeCalls += call
        try {
            call.execute().use { response ->
                when {
                    response.code == 206 -> Unit
                    response.code == 200 ->
                        throw EngineException.Unexpected("Server ignored the byte range request")
                    response.code == 416 -> {
                        if (segmentStore.cursorOf(index) >= segmentStore.endOf(index)) return
                        throw EngineException.Http(416, url)
                    }
                    isTerminalStatus(response.code) ->
                        throw EngineException.Http(response.code, url)
                    isRetryableStatus(response.code) ->
                        throw EngineException.RetryableHttp(response.code, url)
                    else -> throw EngineException.Http(response.code, url)
                }

                val body = response.body ?: throw IOException("Response has no body")
                body.source().use { source ->
                    val buffer = ByteArray(BufferSize)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        cursor = segmentStore.cursorOf(index)
                        val currentEnd = segmentStore.endOf(index)
                        if (cursor >= currentEnd) break
                        val want = min(BufferSize.toLong(), currentEnd - cursor).toInt()
                        val read = source.read(buffer, 0, want)
                        if (read == -1) {
                            if (segmentStore.cursorOf(index) < segmentStore.endOf(index)) {
                                throw IOException("Connection closed before the range was complete")
                            }
                            break
                        }
                        val sink = writer ?: throw IOException("Writer closed")
                        sink.write(buffer, 0, read, cursor)
                        segmentStore.advance(index, read)
                        speedMeter.onProgress(segmentStore.downloadedTotal())
                    }
                }
            }
        } finally {
            activeCalls -= call
        }
    }

    // ---------------------------------------------------------------- single-stream fallback

    private suspend fun runSingle(probe: ProbeResult, resume: ResumeState?) {
        val file = targetFile ?: throw EngineException.Unexpected("No destination file")
        file.parentFile?.mkdirs()

        singleTotal = probe.contentLength
        if (resume == null) {
            file.delete()
            file.createNewFile()
            singleCursor = 0L
        } else {
            singleCursor = resume.segments.firstOrNull()?.downloaded ?: 0L
            if (!file.exists() || file.length() < singleCursor) {
                file.delete()
                file.createNewFile()
                singleCursor = 0L
            }
        }
        if (!file.exists()) file.createNewFile()

        writer = writer ?: SegmentFileWriter(file, -1L).also { writer = it }
        speedMeter.reset()
        speedMeter.onProgress(singleCursor)
        status = DownloadStatus.Downloading
        publish(persist = true)

        coroutineScope {
            val telemetry = launch { telemetryLoop() }
            try {
                downloadSingleStream()
            } finally {
                telemetry.cancel()
            }
        }

        currentCoroutineContext().ensureActive()
        if (status != DownloadStatus.Downloading) return
        writer?.flush()
        val length = file.length()
        if (singleTotal > 0L && length != singleTotal) {
            throw EngineException.Unexpected("Download ended at $length of $singleTotal bytes")
        }
        if (singleTotal <= 0L) singleTotal = length
        singleCursor = length
        finish(file)
    }

    private suspend fun downloadSingleStream() {
        var attempt = 0
        while (true) {
            attempt++
            try {
                downloadSingleStreamOnce()
                return
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (retryable: EngineException.RetryableHttp) {
                currentCoroutineContext().ensureActive()
                if (attempt >= retryPolicy.maxAttempts) {
                    throw EngineException.RetryExhausted(attempt, retryable)
                }
                log("stream retry $attempt after HTTP ${retryable.code}")
                delay(retryPolicy.delayMillisAfter(attempt))
            } catch (network: IOException) {
                currentCoroutineContext().ensureActive()
                if (attempt >= retryPolicy.maxAttempts) {
                    throw EngineException.RetryExhausted(attempt, network)
                }
                log("stream retry $attempt after ${network.message}")
                delay(retryPolicy.delayMillisAfter(attempt))
            }
        }
    }

    private suspend fun downloadSingleStreamOnce() {
        val url = probeResult?.finalUrl ?: request.url
        val startCursor = singleCursor
        val builder = Request.Builder().url(url).get()
        if (startCursor > 0L) builder.header("Range", "bytes=$startCursor-")
        applyHeaders(builder)

        val call = client.newCall(builder.build())
        activeCalls += call
        try {
            call.execute().use { response ->
                when {
                    response.code == 206 -> {
                        totalFromContentRange(response.header("Content-Range"))?.let { singleTotal = it }
                    }
                    response.code == 200 -> {
                        val contentLength = response.header("Content-Length")?.toLongOrNull()
                        if (singleCursor > 0L) {
                            // Server ignored our resume offset: start over from byte zero.
                            singleCursor = 0L
                            writer?.truncate()
                        }
                        if (contentLength != null) singleTotal = contentLength
                    }
                    response.code == 416 -> {
                        if (singleTotal > 0L && singleCursor >= singleTotal) return
                        throw EngineException.Http(416, url)
                    }
                    isTerminalStatus(response.code) ->
                        throw EngineException.Http(response.code, url)
                    isRetryableStatus(response.code) ->
                        throw EngineException.RetryableHttp(response.code, url)
                    else -> throw EngineException.Http(response.code, url)
                }

                val body = response.body ?: throw IOException("Response has no body")
                body.source().use { source ->
                    val buffer = ByteArray(BufferSize)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = source.read(buffer)
                        if (read == -1) break
                        val sink = writer ?: throw IOException("Writer closed")
                        sink.write(buffer, 0, read, singleCursor)
                        singleCursor += read
                        speedMeter.onProgress(singleCursor)
                    }
                }
            }
        } finally {
            activeCalls -= call
        }
    }

    // ---------------------------------------------------------------- lifecycle

    suspend fun pause() {
        val current = state.value.status
        if (current != DownloadStatus.Probing && current != DownloadStatus.Downloading) return
        activeCalls.forEach { it.cancel() }
        job?.cancelAndJoin()
        if (status == DownloadStatus.Completed || status == DownloadStatus.Failed) return
        status = DownloadStatus.Paused
        speedMeter.reset()
        writer?.close()
        writer = null
        log("paused downloaded=${store?.downloadedTotal() ?: singleCursor}")
        publish(persist = true)
    }

    suspend fun resume() {
        val current = state.value.status
        if (current != DownloadStatus.Paused && current != DownloadStatus.Failed) return
        lastError = null
        startJob(revalidate = true)
    }

    suspend fun cancel() {
        val current = state.value.status
        if (current == DownloadStatus.Completed || current == DownloadStatus.Cancelled) return
        activeCalls.forEach { it.cancel() }
        job?.cancelAndJoin()
        if (status == DownloadStatus.Completed) return
        status = DownloadStatus.Cancelled
        log("cancelled ${targetFile?.absolutePath}")
        speedMeter.reset()
        writer?.close()
        writer = null
        targetFile?.delete()
        publish(persist = true)
    }

    // ---------------------------------------------------------------- helpers

    private fun currentResumeState(): ResumeState? {
        val file = targetFile ?: return null
        val probe = probeResult ?: return null
        val segments = store?.checkpoints() ?: run {
            val end = when {
                singleTotal > 0L -> singleTotal
                else -> singleCursor
            }
            listOf(SegmentCheckpoint(0, 0, end.coerceAtLeast(singleCursor), singleCursor))
        }
        return ResumeState(
            filePath = file.absolutePath,
            totalBytes = probe.contentLength,
            etag = probe.etag,
            lastModified = probe.lastModified,
            segments = segments,
        )
    }

    private fun metadataChanged(resume: ResumeState, probe: ProbeResult): Boolean {
        if (resume.totalBytes > 0L && probe.contentLength > 0L &&
            resume.totalBytes != probe.contentLength
        ) {
            return true
        }
        if (resume.etag != null && probe.etag != null && resume.etag != probe.etag) return true
        if (resume.lastModified != null && probe.lastModified != null &&
            resume.lastModified != probe.lastModified
        ) {
            return true
        }
        return false
    }

    private fun resetForRestart() {
        writer?.close()
        writer = null
        store = null
        singleCursor = 0L
        singleTotal = probeResult?.contentLength ?: -1L
        sha256 = null
        speedMeter.reset()
        targetFile?.delete()
    }

    private fun resolveTarget(probe: ProbeResult): File {
        val directory = request.destinationDir
        directory.mkdirs()
        val proposed = request.fileNameOverride
            ?: probe.fileName
            ?: FileNames.fromUrl(probe.finalUrl)
            ?: DefaultFileName
        val safe = FileNames.sanitize(proposed) ?: DefaultFileName
        return FileNames.unique(directory, safe)
    }

    private fun applyHeaders(builder: Request.Builder) {
        request.headers.forEach { (name, value) ->
            if (!name.equals("Range", ignoreCase = true) &&
                !name.equals("Accept-Encoding", ignoreCase = true)
            ) {
                builder.header(name, value)
            }
        }
        builder.header("Accept-Encoding", "identity")
    }

    private fun totalFromContentRange(header: String?): Long? {
        if (header.isNullOrBlank()) return null
        val total = header.substringAfterLast('/', missingDelimiterValue = "").trim()
        if (total.isEmpty() || total == "*") return null
        return total.toLongOrNull()
    }

    private suspend fun finish(file: File) {
        val digest = Sha256.of(file)
        val expected = request.expectedSha256
        if (expected != null && !digest.equals(expected, ignoreCase = true)) {
            throw EngineException.Checksum(expected, digest)
        }
        sha256 = digest
        log("completed ${file.absolutePath} sha256=$digest")
        status = DownloadStatus.Completed
        speedMeter.reset()
        writer?.close()
        writer = null
        publish(persist = true)
    }

    private suspend fun fail(failure: EngineException) {
        log("failed ${failure.message}")
        lastError = failure.toEngineError()
        status = DownloadStatus.Failed
        speedMeter.reset()
        writer?.close()
        writer = null
        publish(persist = true)
    }

    private fun buildSnapshot(): DownloadSnapshot {
        val probe = probeResult
        val segments = store?.snapshot() ?: singleSegmentSnapshot()
        val downloaded = store?.downloadedTotal() ?: singleCursor
        val total = probe?.contentLength?.takeIf { it > 0L } ?: singleTotal
        val speed = if (status == DownloadStatus.Downloading) speedMeter.bytesPerSecond() else 0L
        val eta = if (speed > 0L && total > 0L && downloaded < total) {
            (total - downloaded) / speed
        } else {
            null
        }
        val file = targetFile
        return DownloadSnapshot(
            id = request.id,
            url = request.url,
            fileName = file?.name ?: request.fileNameOverride ?: probe?.fileName ?: "",
            filePath = file?.absolutePath ?: "",
            totalBytes = total,
            downloadedBytes = downloaded,
            status = status,
            bytesPerSecond = speed,
            etaSeconds = eta,
            rangeSupported = probe?.rangeSupported ?: false,
            etag = probe?.etag ?: request.resume?.etag,
            lastModified = probe?.lastModified ?: request.resume?.lastModified,
            segmentCount = request.safeSegments,
            segments = segments,
            sha256 = sha256,
            error = if (status == DownloadStatus.Failed) lastError else null,
        )
    }

    private fun singleSegmentSnapshot(): List<SegmentSnapshot> {
        if (status == DownloadStatus.Probing && singleCursor == 0L && singleTotal <= 0L) {
            return emptyList()
        }
        val state = when {
            status == DownloadStatus.Completed -> SegmentState.Done
            status == DownloadStatus.Failed -> SegmentState.Failed
            status == DownloadStatus.Paused ->
                if (singleCursor > 0L) SegmentState.Active else SegmentState.Pending
            else -> SegmentState.Active
        }
        return listOf(
            SegmentSnapshot(
                index = 0,
                start = 0L,
                end = singleTotal.coerceAtLeast(singleCursor),
                downloaded = singleCursor,
                state = state,
            )
        )
    }

    private suspend fun publish(persist: Boolean) {
        val snapshot = buildSnapshot()
        mutableState.value = snapshot
        if (persist) {
            persistAt = clock()
            runCatching { checkpointSink.persist(snapshot) }
        }
    }

    private companion object {
        const val BufferSize: Int = 64 * 1024
        const val MinSplitBytes: Long = 1L * 1024L * 1024L
        const val TelemetryIntervalMs: Long = 200L
        const val CheckpointIntervalMs: Long = 500L
        const val DefaultFileName: String = "download"
    }
}
