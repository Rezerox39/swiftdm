package com.abhi.swiftdm.engine

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Locale
import java.util.UUID

/**
 * Real-network 1-vs-8-segment benchmark. Disabled unless -Dswiftdm.benchmarkUrl=<url> is passed,
 * which is what the Engine benchmark workflow does.
 */
class EngineBenchmarkTest {

    private val url: String =
        System.getProperty("swiftdm.benchmarkUrl").orEmpty().trim()

    @Before
    fun requireUrl() {
        assumeTrue("swiftdm.benchmarkUrl is not set", url.startsWith("http"))
    }

    @Test
    fun oneVersusEightSegments() = runBlocking {
        val directory = Files.createTempDirectory("swiftdm-bench").toFile()
        try {
            val one = measure(segments = 1, directory = directory)
            val eight = measure(segments = 8, directory = directory)

            val oneHash = Sha256.of(File(one.filePath))
            val eightHash = Sha256.of(File(eight.filePath))
            assertEquals("1 and 8 segment files must be identical", oneHash, eightHash)
            assertTrue("expected non-zero bytes", one.bytes > 0L)

            println("SWIFTDM_BENCH url=$url")
            println(
                "SWIFTDM_BENCH segments=1 bytes=${one.bytes} millis=${one.millis} " +
                    "mib_per_s=${format(one.mebibytesPerSecond)}"
            )
            println(
                "SWIFTDM_BENCH segments=8 bytes=${eight.bytes} millis=${eight.millis} " +
                    "mib_per_s=${format(eight.mebibytesPerSecond)}"
            )
            println("SWIFTDM_BENCH ratio=${format(eight.mebibytesPerSecond / one.mebibytesPerSecond)}")
            println("SWIFTDM_BENCH sha256=$oneHash")
        } finally {
            directory.deleteRecursively()
        }
    }

    private suspend fun measure(segments: Int, directory: File): Measurement {
        println("SWIFTDM_BENCH_START segments=$segments")
        val handle = DownloadEngine(log = { line -> println("SWIFTDM_ENGINE $line") }).start(
            DownloadRequest(
                id = "bench-${segments}-${UUID.randomUUID()}",
                url = url,
                destinationDir = directory,
                segments = segments,
            )
        )
        val startedAt = System.nanoTime()
        val result = withTimeout(15 * 60_000L) {
            handle.state.first { it.status.isTerminal }
        }
        val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L
        println("SWIFTDM_BENCH_END segments=$segments millis=$elapsedMillis status=${result.status}")
        check(result.status == DownloadStatus.Completed) {
            "Benchmark failed with ${result.status}: ${result.error?.message}"
        }
        check(result.downloadedBytes == result.totalBytes) {
            "Benchmark downloaded ${result.downloadedBytes} of ${result.totalBytes}"
        }
        return Measurement(
            filePath = result.filePath,
            bytes = result.downloadedBytes,
            millis = elapsedMillis,
        )
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.2f", value)

    private data class Measurement(
        val filePath: String,
        val bytes: Long,
        val millis: Long,
    ) {
        val mebibytesPerSecond: Double
            get() = if (millis <= 0L) 0.0 else (bytes / (1024.0 * 1024.0)) / (millis / 1000.0)
    }
}
