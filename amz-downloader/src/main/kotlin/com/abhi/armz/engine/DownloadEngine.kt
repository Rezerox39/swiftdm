package com.abhi.armz.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
 * Lifecycle: [start] probes the URL, resolves a part file and begins writing. Pausing keeps the
 * random-access part file; cancel deletes it. After every byte arrives the session enters
 * [DownloadStatus.Finalizing], verifies coverage, syncs, hashes with progress, then publishes.
 */
class DownloadEngine(
    private val client: OkHttpClient = HttpClients.shared,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val log: (String) -> Unit = {},
    private val publisher: FilePublisher = LocalFilePublisher(),
) {

    private val prober = HttpProbe(client)

    /** Probe used by the Add Download sheet before a request exists. */
    suspend fun probe(url: String, headers: Map<String, String> = emptyMap()): ProbeResult =
        prober.probe(url, headers)

    fun start(
        request: DownloadRequest,
        checkpointSink: CheckpointSink = CheckpointSink.None,
        publisherOverride: FilePublisher? = null,
    ): DownloadHandle {
        val session = DownloadSession(
            request = request,
            client = client,
            prober = prober,
            retryPolicy = retryPolicy,
            checkpointSink = checkpointSink,
            clock = clock,
            log = log,
            publisher = publisherOverride ?: publisher,
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
    private val publisher: FilePublisher,
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

    /** Random-access part file while downloading. */
    @Volatile
    private var targetFile: File? = null

    @Volatile
    private var displayName: String = request.fileNameOverride ?: ""

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

    @Volatile
    private var publishedFile: PublishedFile? = null

    @Volatile
    private var mimeType: String? = request.mimeTypeHint

    @Volatile
    private var finalizeStep: FinalizeStep? = null

    @Volatile
    private var finalizeProgress: Float = 0f

    private val speedMeter = SpeedMeter(clock = clock)
    private val activeCalls: MutableSet<Call> =
        Collections.newSetFromMap(ConcurrentHashMap<Call, Boolean>())

    private var job: Job? = null

    @Volatile
    private var persistAt: Long = 0L

    @Volatile
    private var lastFinalizeUiAt: Long = 0L

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
            finalizeStep = null
            finalizeProgress = 0f
            publish(persist = true)

            val probe = probeWithRetry()
            probeResult = probe
            mimeType = request.mimeTypeHint
                ?: probe.mimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
            log(
                "probe total=${probe.contentLength} ranges=${probe.rangeSupported} " +
                    "etag=${probe.etag} lastModified=${probe.lastModified} mime=${probe.mimeType}"
            )

            val resume = if (revalidate) currentResumeState() else request.resume
            val resumeFile = resume?.let { File(it.filePath) }
            val missingFile = resumeFile != null && !resumeFile.exists()
            val contentChanged = resume != null && (missingFile || metadataChanged(resume, probe))

            val target: File
            if (resumeFile != null) {
                target = resumeFile
                displayName = request.fileNameOverride
                    ?: probe.fileName
                    ?: resumeFile.name.removeSuffix(".part")
            } else {
                target = resolvePart(probe)
            }
            targetFile = target
            log("part ${target.absolutePath} resume=${resume != null} restart=$contentChanged")
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
            // The fast part lives in app-specific external storage; publishing needs a second copy
            // in MediaStore, so reserve roughly twice the remaining bytes.
            val copies = if (publisher.requiresCopy) 2 else 1
            ensureFreeSpace(target.parentFile ?: request.destinationDir, remaining * copies)

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
                return prober.probe(
                    url = request.url,
                    headers = request.headers,
                    registerCall = { call -> activeCalls += call },
                    unregisterCall = { call -> activeCalls -= call },
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (retryable: EngineException.RetryableHttp) {
                currentCoroutineContext().ensureActive()
                if (attempt >= retryPolicy.maxAttempts) {
                    throw EngineException.Http(retryable.code, request.url)
                }
                log("probe retry $attempt after HTTP ${retryable.code}")
                delay(retryPolicy.delayMillisAfter(attempt))
            } catch (network: IOException) {
                currentCoroutineContext().ensureActive()
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

        runWorkers(segmentStore)

        currentCoroutineContext().ensureActive()
        if (status != DownloadStatus.Downloading) return
        if (!segmentStore.isComplete() || segmentStore.downloadedTotal() != total) {
            throw EngineException.Unexpected(
                "Download stopped at ${segmentStore.downloadedTotal()} of $total bytes"
            )
        }
        fileWriter.flush()
        finish(file, total)
    }

    private fun buildStore(resume: ResumeState?, total: Long): SegmentStore {
        val persisted = resume?.segments
            ?.takeIf { it.isNotEmpty() && it.all { segment -> segment.start < segment.end } }
            ?.let { segments ->
                val repaired = SegmentPlanner.fromResume(segments, total, request.safeSegments)
                val gaps = CoverageVerifier.inspect(
                    repaired.map { SegmentCheckpoint(it.index, it.start, it.end, it.downloaded) },
                    total,
                ).gaps
                if (gaps.isNotEmpty()) {
                    log("resume coverage gaps ${gaps.joinToString { "${it.first}-${it.last}" }}")
                }
                repaired
            }
        return SegmentStore(
            initial = persisted ?: SegmentPlanner.plan(total, request.safeSegments),
            totalBytes = total,
        )
    }

    private suspend fun runWorkers(segmentStore: SegmentStore) {
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
        finish(file, length)
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

    // ---------------------------------------------------------------- finalization

    /**
     * Explicit finalize stage: verify coverage (repair gaps), fsync, cancellable SHA-256, publish.
     * Only a successful publish sets [DownloadStatus.Completed].
     */
    private suspend fun finish(file: File, total: Long) {
        status = DownloadStatus.Finalizing
        finalizeProgress = 0f
        setFinalizeStep(FinalizeStep.Verifying)

        var baseline = currentCheckpoints()
        var report = CoverageVerifier.inspect(baseline, total)
        log("finalize ${report.describe()}")
        if (!report.complete) {
            val gaps = report.gaps
            if (gaps.isEmpty() || store == null) {
                throw EngineException.Coverage(report.describe())
            }
            // Re-fetch only the missing intervals, then verify the union again.
            val gapSegments = gaps.mapIndexed { index, gap ->
                SegmentCheckpoint(1_000 + index, gap.first, gap.last + 1, 0L)
            }
            val repairStore = SegmentStore(gapSegments, total)
            log("finalize repairing gaps ${gaps.joinToString { "${it.first}-${it.last}" }}")
            runWorkers(repairStore)
            currentCoroutineContext().ensureActive()
            baseline = baseline + repairStore.checkpoints()
            // Keep a single complete store so snapshot.downloadedBytes stays equal to the file size.
            store = SegmentStore(
                initial = SegmentPlanner.fromResume(baseline, total, request.safeSegments),
                totalBytes = total,
            )
            report = CoverageVerifier.inspect(baseline, total)
            log("finalize after repair ${report.describe()}")
            if (!report.complete) throw EngineException.Coverage(report.describe())
        }

        setFinalizeStep(FinalizeStep.Syncing, progress = 0.1f)
        writer?.flush()
        writer?.close()
        writer = null
        val onDisk = file.length()
        if (onDisk != total) {
            throw EngineException.Storage("Part file is $onDisk bytes, expected $total")
        }

        setFinalizeStep(FinalizeStep.Hashing, progress = 0.15f)
        val digest = Sha256.of(file) { hashed, hashTotal ->
            val fraction = if (hashTotal > 0L) hashed.toDouble() / hashTotal else 0.0
            val mapped = (0.15 + 0.6 * fraction).toFloat()
            updateFinalizeProgress(mapped)
        }
        val expected = request.expectedSha256
        if (expected != null && !digest.equals(expected, ignoreCase = true)) {
            throw EngineException.Checksum(expected, digest)
        }
        sha256 = digest

        setFinalizeStep(FinalizeStep.Publishing, progress = 0.78f)
        val published = publisher.publish(
            partFile = file,
            displayName = displayName.ifBlank { file.name.removeSuffix(".part") },
            mimeType = mimeType,
        ) { copied, publishTotal ->
            val fraction = if (publishTotal > 0L) copied.toDouble() / publishTotal else 1.0
            updateFinalizeProgress((0.78 + 0.22 * fraction).toFloat())
        }
        publishedFile = published
        mimeType = published.mimeType
        displayName = published.displayName
        log(
            "published uri=${published.uri} path=${published.path} name=${published.displayName} " +
                "mime=${published.mimeType}"
        )

        val directoryFiles = file.parentFile?.listFiles()
        log("directory after publish: ${directoryFiles?.joinToString { "${it.name}:${it.length()}" } ?: "unavailable"}")

        checkpointSink.clearSegments(request.id)
        status = DownloadStatus.Completed
        finalizeStep = null
        finalizeProgress = 1f
        speedMeter.reset()
        log("completed ${published.path.ifBlank { published.uri.orEmpty() }} sha256=$digest")
        publish(persist = true)
    }

    private suspend fun setFinalizeStep(step: FinalizeStep, progress: Float = 0f) {
        status = DownloadStatus.Finalizing
        finalizeStep = step
        finalizeProgress = progress
        publish(persist = true)
    }

    private fun updateFinalizeProgress(value: Float) {
        finalizeProgress = value.coerceIn(0f, 1f)
        val now = clock()
        if (now - lastFinalizeUiAt >= 120L) {
            lastFinalizeUiAt = now
            mutableState.value = buildSnapshot()
        }
    }

    private fun currentCheckpoints(): List<SegmentCheckpoint> {
        store?.let { return it.checkpoints() }
        val end = if (singleTotal > 0L) singleTotal else singleCursor
        return listOf(
            SegmentCheckpoint(
                index = 0,
                start = 0L,
                end = end.coerceAtLeast(singleCursor),
                downloaded = singleCursor.coerceAtMost(end.coerceAtLeast(singleCursor)),
            )
        )
    }

    // ---------------------------------------------------------------- lifecycle

    suspend fun pause() {
        val current = state.value.status
        if (current != DownloadStatus.Probing &&
            current != DownloadStatus.Downloading &&
            current != DownloadStatus.Finalizing
        ) {
            return
        }
        activeCalls.forEach { it.cancel() }
        job?.cancelAndJoin()
        if (status == DownloadStatus.Completed || status == DownloadStatus.Failed) return
        status = DownloadStatus.Paused
        finalizeStep = null
        speedMeter.reset()
        writer?.close()
        writer = null
        log("paused downloaded=${store?.downloadedTotal() ?: singleCursor}")
        publish(persist = true)
    }

    suspend fun resume() {
        val current = state.value.status
        if (current != DownloadStatus.Paused && current != DownloadStatus.Failed &&
            current != DownloadStatus.Cancelled
        ) {
            return
        }
        lastError = null
        // Pause/failure leaves claimed ranges marked Active. Rebuilding from checkpoints preserves
        // every downloaded byte while letting workers claim the remaining ranges again.
        store = store?.let { SegmentStore(it.checkpoints(), it.totalBytes) }
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
        finalizeStep = null
        publish(persist = true)
    }

    // ---------------------------------------------------------------- helpers

    private fun currentResumeState(): ResumeState? {
        val file = targetFile ?: return null
        val probe = probeResult ?: return null
        return ResumeState(
            filePath = file.absolutePath,
            totalBytes = probe.contentLength,
            etag = probe.etag,
            lastModified = probe.lastModified,
            segments = currentCheckpoints(),
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

    private fun resolvePart(probe: ProbeResult): File {
        val directory = request.destinationDir
        directory.mkdirs()
        val proposed = request.fileNameOverride
            ?: probe.fileName
            ?: FileNames.fromUrl(probe.finalUrl)
            ?: DefaultFileName
        val safe = FileNames.sanitize(proposed) ?: DefaultFileName
        displayName = safe
        val partName = FileNames.unique(directory, "$safe.part").name
        return File(directory, partName)
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

    private suspend fun fail(failure: EngineException) {
        log("failed ${failure.message}")
        lastError = failure.toEngineError()
        status = DownloadStatus.Failed
        finalizeStep = null
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
        val part = targetFile
        val published = publishedFile
        return DownloadSnapshot(
            id = request.id,
            url = request.url,
            fileName = published?.displayName ?: displayName.ifBlank {
                part?.name?.removeSuffix(".part") ?: request.fileNameOverride ?: probe?.fileName ?: ""
            },
            filePath = published?.path ?: part?.absolutePath ?: "",
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
            finalizeStep = finalizeStep,
            finalizeProgress = finalizeProgress,
            contentUri = published?.uri,
            mimeType = mimeType,
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
        const val BufferSize: Int = 256 * 1024
        const val MinSplitBytes: Long = 1L * 1024L * 1024L
        const val TelemetryIntervalMs: Long = 200L
        const val CheckpointIntervalMs: Long = 500L
        const val DefaultFileName: String = "download"
    }
}
