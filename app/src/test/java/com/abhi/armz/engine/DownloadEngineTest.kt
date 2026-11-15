package com.abhi.armz.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.Timeout
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Random
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.system.measureTimeMillis

class DownloadEngineTest {

    @get:Rule
    val timeout: Timeout = Timeout.seconds(90)

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var origin: EngineTestServer
    private lateinit var engine: DownloadEngine
    private lateinit var destination: File

    @Before
    fun setUp() {
        origin = EngineTestServer(sampleBytes(512 * 1024))
        server = MockWebServer()
        server.dispatcher = origin
        server.start()
        engine = DownloadEngine()
        destination = temporaryFolder.newFolder("downloads")
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `probe reports length etag and range support`() = runBlocking {
        origin.content = sampleBytes(256 * 1024)
        origin.etag = "\"probe-tag\""
        val probe = HttpProbe(HttpClients.downloadClient()).probe(url())

        assertEquals(origin.content.size.toLong(), probe.contentLength)
        assertTrue(probe.rangeSupported)
        assertEquals("\"probe-tag\"", probe.etag)
        assertEquals("sample.bin", probe.fileName)
        assertNotNull(probe.lastModified)
    }

    @Test
    fun `eight segments produce a byte-identical file`() = runBlocking {
        val content = sampleBytes(4 * 1024 * 1024)
        origin.content = content
        val handle = engine.start(request(segments = 8))
        val result = awaitTerminal(handle)

        assertEquals(DownloadStatus.Completed, result.status)
        assertEquals(content.size.toLong(), result.downloadedBytes)
        assertEquals(content.size.toLong(), File(result.filePath).length())
        assertEquals(Sha256.of(content), Sha256.of(File(result.filePath)))
        assertTrue("expected at least 8 ranged requests", origin.rangeHeaders.size >= 8)
        assertTrue(result.segments.size >= 8)
        assertTrue(result.segments.all { it.downloaded == it.end - it.start })
    }

    @Test
    fun `server without range support falls back to a single stream`() = runBlocking {
        origin.supportsRanges = false
        val content = sampleBytes(768 * 1024)
        origin.content = content
        val handle = engine.start(request(segments = 8))
        val result = awaitTerminal(handle)

        assertEquals(DownloadStatus.Completed, result.status)
        assertEquals(false, result.rangeSupported)
        assertEquals(Sha256.of(content), Sha256.of(File(result.filePath)))
        assertEquals(1, result.segments.size)
        assertTrue(result.downloadedBytes == content.size.toLong())
    }

    @Test
    fun `5xx responses are retried with backoff`() = runBlocking {
        val content = sampleBytes(256 * 1024)
        origin.content = content
        origin.failuresRemaining = 2
        val policy = RetryPolicy(maxAttempts = 5, initialDelayMs = 20L)
        val retryingEngine = DownloadEngine(retryPolicy = policy)

        val startedAt = System.nanoTime()
        val handle = retryingEngine.start(request(segments = 1))
        val result = awaitTerminal(handle)
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

        assertEquals(DownloadStatus.Completed, result.status)
        assertEquals(Sha256.of(content), Sha256.of(File(result.filePath)))
        assertEquals("503, 503, then 206", 3, origin.getCount.get())
        assertTrue("expected backoff of at least ~60 ms, was $elapsedMs", elapsedMs >= 50L)
    }

    @Test
    fun `terminal 404 fails immediately without range retries`() = runBlocking {
        origin.forcedGetStatus = 404
        val handle = engine.start(request(segments = 1))
        val result = awaitTerminal(handle)

        assertEquals(DownloadStatus.Failed, result.status)
        assertEquals(404, result.error?.httpCode)
        assertEquals(1, origin.getCount.get())
    }

    @Test
    fun `pause persists partial ranges and resume finishes the same bytes`() = runBlocking {
        val content = sampleBytes(6 * 1024 * 1024)
        origin.content = content
        origin.throttleBytes = 32L * 1024L
        origin.throttlePeriodMs = 40L

        val handle = engine.start(request(segments = 4))
        withTimeout(30_000) {
            handle.state.first { it.downloadedBytes >= 128L * 1024L }
        }
        handle.pause()
        val paused = handle.state.value

        assertEquals(DownloadStatus.Paused, paused.status)
        assertTrue("expected partial bytes", paused.downloadedBytes > 0L)
        assertTrue(paused.downloadedBytes < content.size.toLong())
        assertTrue(paused.segments.any { it.downloaded > 0L })

        origin.rangeHeaders.clear()
        val resumeState = ResumeState(
            filePath = paused.filePath,
            totalBytes = paused.totalBytes,
            etag = paused.etag,
            lastModified = paused.lastModified,
            segments = paused.segments.map {
                SegmentCheckpoint(it.index, it.start, it.end, it.downloaded)
            },
        )
        val resumed = engine.start(
            request(segments = 4, id = "resumed-${UUID.randomUUID()}", resume = resumeState)
        )
        val result = awaitTerminal(resumed)

        assertEquals(DownloadStatus.Completed, result.status)
        assertEquals(Sha256.of(content), Sha256.of(File(result.filePath)))
        assertTrue(
            "resume should reuse non-zero offsets",
            origin.rangeHeaders.any { rangeStart(it) > 0 },
        )
    }

    @Test
    fun `resume on the same paused handle finishes`() = runBlocking {
        val content = sampleBytes(6 * 1024 * 1024)
        origin.content = content
        origin.throttleBytes = 32L * 1024L
        origin.throttlePeriodMs = 40L

        val handle = engine.start(request(segments = 4))
        withTimeout(30_000) {
            handle.state.first { it.downloadedBytes >= 128L * 1024L }
        }
        handle.pause()
        handle.resume()

        val result = awaitTerminal(handle)
        assertEquals(DownloadStatus.Completed, result.status)
        assertEquals(Sha256.of(content), Sha256.of(File(result.filePath)))
        assertEquals(content.size.toLong(), result.downloadedBytes)
    }

    @Test
    fun `changed etag restarts from scratch`() = runBlocking {
        val firstContent = sampleBytes(3 * 1024 * 1024, seed = 1)
        val secondContent = sampleBytes(3 * 1024 * 1024, seed = 2)
        origin.content = firstContent
        origin.throttleBytes = 24L * 1024L
        origin.throttlePeriodMs = 40L

        val handle = engine.start(request(segments = 4))
        withTimeout(30_000) {
            handle.state.first { it.downloadedBytes >= 128L * 1024L }
        }
        handle.pause()
        val paused = handle.state.value
        assertTrue(paused.downloadedBytes > 0L)

        origin.content = secondContent
        origin.etag = "\"v2\""

        val resumeState = ResumeState(
            filePath = paused.filePath,
            totalBytes = paused.totalBytes,
            etag = paused.etag,
            lastModified = paused.lastModified,
            segments = paused.segments.map {
                SegmentCheckpoint(it.index, it.start, it.end, it.downloaded)
            },
        )
        val resumed = engine.start(
            request(segments = 4, id = "restarted-${UUID.randomUUID()}", resume = resumeState)
        )
        val result = awaitTerminal(resumed)

        assertEquals(DownloadStatus.Completed, result.status)
        assertEquals(Sha256.of(secondContent), Sha256.of(File(result.filePath)))
        assertEquals("\"v2\"", result.etag)
    }

    @Test
    fun `checksum mismatch fails the download`() = runBlocking {
        val content = sampleBytes(128 * 1024)
        origin.content = content
        val wrongHash = "0".repeat(64)
        val handle = engine.start(
            request(segments = 1).copy(expectedSha256 = wrongHash)
        )
        val result = awaitTerminal(handle)

        assertEquals(DownloadStatus.Failed, result.status)
        assertEquals("checksum", result.error?.type)
    }

    @Test
    fun `content-disposition filename is sanitised and used`() = runBlocking {
        val content = sampleBytes(96 * 1024)
        origin.content = content
        val handle = engine.start(request(segments = 1))
        val result = awaitTerminal(handle)

        assertEquals("sample.bin", result.fileName)
        assertTrue(result.filePath.endsWith("sample.bin"))
        assertNull(result.error)
    }

    @Test
    fun `completed download leaves exactly one output file and no chunks`() = runBlocking {
        val content = sampleBytes(4 * 1024 * 1024)
        origin.content = content
        val result = awaitTerminal(engine.start(request(segments = 8)))

        assertEquals(DownloadStatus.Completed, result.status)
        val files = destination.listFiles()
            ?.filter { it.isFile }
            ?.sortedBy { it.name }
            .orEmpty()
        assertEquals("one assembled output file", listOf("sample.bin"), files.map { it.name })
        assertFalse(files.any { it.name.endsWith(".part") })
        assertFalse(files.any { it.name.contains("segment", ignoreCase = true) })
        assertEquals(content.size.toLong(), files.single().length())
        assertEquals(Sha256.of(content), Sha256.of(files.single()))
    }

    @Test
    fun `resume with a coverage gap re-fetches only the missing range`() = runBlocking {
        val content = sampleBytes(384 * 1024, seed = 11L)
        origin.content = content
        val total = content.size.toLong()
        val segmentSize = content.size / 4
        val part = File(destination, "gap.bin.part")
        seedPart(part, content, listOf(0 until segmentSize, 2 * segmentSize until content.size))
        val resume = ResumeState(
            filePath = part.absolutePath,
            totalBytes = total,
            etag = origin.etag,
            lastModified = origin.lastModified,
            segments = listOf(
                SegmentCheckpoint(0, 0, segmentSize.toLong(), segmentSize.toLong()),
                SegmentCheckpoint(1, segmentSize.toLong(), (2 * segmentSize).toLong(), 0L),
                SegmentCheckpoint(2, (2 * segmentSize).toLong(), (3 * segmentSize).toLong(), segmentSize.toLong()),
                SegmentCheckpoint(3, (3 * segmentSize).toLong(), total, segmentSize.toLong()),
            ),
        )

        val result = awaitTerminal(engine.start(request(segments = 4, resume = resume)))

        assertEquals(DownloadStatus.Completed, result.status)
        assertEquals(DownloadStatus.Completed, handleStatus(result))
        assertEquals(Sha256.of(content), Sha256.of(File(result.filePath)))
        assertEquals(
            "only the gap is requested",
            listOf("bytes=$segmentSize-${2 * segmentSize - 1}"),
            origin.rangeHeaders.toList(),
        )
    }

    @Test
    fun `finalize failure keeps the part and resume publishes it`() = runBlocking {
        val content = sampleBytes(2 * 1024 * 1024)
        origin.content = content
        val flaky = FlakyPublisher()
        val flakyEngine = DownloadEngine(publisher = flaky)
        val handle = flakyEngine.start(request(segments = 4))

        val failed = awaitTerminal(handle)
        assertEquals(DownloadStatus.Failed, failed.status)
        assertEquals(1, flaky.attempts.get())
        assertTrue("part survives a finalize failure", File(failed.filePath).exists())

        handle.resume()
        val completed = withTimeout(60_000) {
            handle.state.first { it.status == DownloadStatus.Completed }
        }
        assertEquals(DownloadStatus.Completed, completed.status)
        assertEquals(2, flaky.attempts.get())
        assertNotNull(completed.contentUri)
        assertEquals(Sha256.of(content), Sha256.of(File(completed.filePath)))
        assertEquals(
            listOf("sample.bin"),
            destination.listFiles()?.filter { it.isFile }?.map { it.name },
        )
    }

    @Test
    fun `publishing exposes the Assembling step before completion`() = runBlocking {
        origin.content = sampleBytes(512 * 1024)
        val slow = SlowPublisher()
        val slowEngine = DownloadEngine(publisher = slow)
        val handle = slowEngine.start(request(segments = 2))
        val observed = ConcurrentLinkedQueue<DownloadStatus>()
        val watcher = launch {
            handle.state.collect { observed += it.status }
        }
        val result = awaitTerminal(handle)
        delay(30)
        watcher.cancel()

        assertEquals(DownloadStatus.Completed, result.status)
        assertTrue("Finalizing state was observed", observed.contains(DownloadStatus.Finalizing))
        assertTrue("Completed state was observed", observed.contains(DownloadStatus.Completed))
    }

    @Test
    fun `pause during probe returns in under 500 ms`() = runBlocking {
        origin.headDelayMs = 2_000L
        val handle = engine.start(request(segments = 4))
        withTimeout(5_000) {
            while (origin.headCount.get() == 0) delay(5)
        }
        val elapsed = measureTimeMillis {
            withTimeout(1_500) { handle.pause() }
        }

        assertTrue("pause took ${elapsed}ms", elapsed < 500L)
        assertEquals(DownloadStatus.Paused, handle.state.value.status)
    }

    @Test
    fun `cancel during probe returns promptly and leaves no part file`() = runBlocking {
        origin.headDelayMs = 2_000L
        val handle = engine.start(request(segments = 4))
        withTimeout(5_000) {
            while (origin.headCount.get() == 0) delay(5)
        }
        val elapsed = measureTimeMillis {
            withTimeout(1_500) { handle.cancel() }
        }

        assertTrue("cancel took ${elapsed}ms", elapsed < 500L)
        assertEquals(DownloadStatus.Cancelled, handle.state.value.status)
        assertTrue(destination.listFiles().isNullOrEmpty())
    }

    @Test
    fun `cancel removes the partial file`() = runBlocking {
        origin.content = sampleBytes(4 * 1024 * 1024)
        origin.throttleBytes = 24L * 1024L
        origin.throttlePeriodMs = 40L
        val handle = engine.start(request(segments = 2))
        withTimeout(30_000) {
            handle.state.first { it.downloadedBytes > 0L }
        }
        handle.cancel()
        val cancelled = handle.state.value

        assertEquals(DownloadStatus.Cancelled, cancelled.status)
        assertTrue(!File(cancelled.filePath).exists())
    }

    private fun handleStatus(snapshot: DownloadSnapshot): DownloadStatus = snapshot.status

    private fun seedPart(file: File, content: ByteArray, ranges: List<IntRange>) {
        RandomAccessFile(file, "rw").use { randomAccess ->
            randomAccess.setLength(content.size.toLong())
            for (range in ranges) {
                randomAccess.seek(range.first.toLong())
                randomAccess.write(content, range.first, range.last - range.first + 1)
            }
        }
    }

    private fun request(
        segments: Int,
        id: String = UUID.randomUUID().toString(),
        resume: ResumeState? = null,
    ): DownloadRequest = DownloadRequest(
        id = id,
        url = url(),
        destinationDir = destination,
        segments = segments,
        resume = resume,
    )

    private fun url(): String = server.url("/sample.bin").toString()

    private suspend fun awaitTerminal(handle: DownloadHandle): DownloadSnapshot =
        withTimeout(60_000) {
            handle.state.first { it.status.isTerminal }
        }

    private fun sampleBytes(size: Int, seed: Long = 7L): ByteArray {
        val bytes = ByteArray(size)
        Random(seed).nextBytes(bytes)
        return bytes
    }

    @Suppress("unused")
    private fun slice(content: ByteArray, start: Int, endInclusive: Int): ByteArray =
        content.copyOfRange(start, min(endInclusive + 1, content.size))
}

private class FlakyPublisher : FilePublisher {
    private val delegate = LocalFilePublisher()
    val attempts = AtomicInteger()

    override val requiresCopy: Boolean = false

    override suspend fun publish(
        partFile: File,
        displayName: String,
        mimeType: String?,
        onProgress: (copied: Long, total: Long) -> Unit,
    ): PublishedFile {
        if (attempts.incrementAndGet() == 1) throw IOException("simulated publish failure")
        return delegate.publish(partFile, displayName, mimeType, onProgress).copy(
            uri = "content://test/armz/${partFile.name}",
        )
    }
}

private class SlowPublisher : FilePublisher {
    private val delegate = LocalFilePublisher()

    override val requiresCopy: Boolean = false

    override suspend fun publish(
        partFile: File,
        displayName: String,
        mimeType: String?,
        onProgress: (copied: Long, total: Long) -> Unit,
    ): PublishedFile {
        delay(180)
        return delegate.publish(partFile, displayName, mimeType, onProgress)
    }
}
