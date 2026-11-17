package com.abhi.armz.engine

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
 * Real-network 8-vs-16-vs-32-segment benchmark. Disabled unless -Darmz.benchmarkUrl=<url> is passed,
 * which is what the Engine benchmark workflow does.
 */
class EngineBenchmarkTest {

    private val url: String =
        System.getProperty("armz.benchmarkUrl").orEmpty().trim()

    @Before
    fun requireUrl() {
        assumeTrue("armz.benchmarkUrl is not set", url.startsWith("http"))
    }

    @Test
    fun segmentCountComparison() = runBlocking {
        val directory = Files.createTempDirectory("armz-bench").toFile()
        try {
            val results = linkedMapOf<Int, Measurement>()
            for (segments in listOf(8, 16, 32)) {
                results[segments] = measure(segments = segments, directory = directory)
            }

            val hashes = results.values.map { Sha256.of(File(it.filePath)) }.toSet()
            assertEquals("all segment counts must produce identical bytes", 1, hashes.size)
            assertTrue(results.values.all { it.bytes > 0L })

            println("ARMz_BENCH url=$url")
            results.forEach { (segments, measurement) ->
                println(
                    "ARMz_BENCH segments=$segments bytes=${measurement.bytes} " +
                        "millis=${measurement.millis} " +
                        "mib_per_s=${format(measurement.mebibytesPerSecond)}"
                )
            }
            val baseline = results.getValue(8).mebibytesPerSecond
            println("ARMz_BENCH ratio16v8=${format(results.getValue(16).mebibytesPerSecond / baseline)}")
            println("ARMz_BENCH ratio32v8=${format(results.getValue(32).mebibytesPerSecond / baseline)}")
            println("ARMz_BENCH sha256=${hashes.single()}")
        } finally {
            directory.deleteRecursively()
        }
    }

    private suspend fun measure(segments: Int, directory: File): Measurement {
        println("ARMz_BENCH_START segments=$segments")
        val handle = DownloadEngine(log = { line -> println("ARMz_ENGINE $line") }).start(
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
        println("ARMz_BENCH_END segments=$segments millis=$elapsedMillis status=${result.status}")
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
