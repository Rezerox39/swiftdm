package com.mochyy.mochymusic.downloader

import com.abhi.armz.data.local.DownloadEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloaderUiMapperTest {
    private fun entity(
        status: String = "Downloading",
        downloaded: Long = 500L,
        total: Long = 1_000L,
        rate: Long = 100L,
        source: String? = "browser",
    ) = DownloadEntity(
        id = "d1",
        url = "https://example.com/archive.zip",
        fileName = "archive.zip",
        filePath = "/downloads/archive.zip",
        totalBytes = total,
        downloadedBytes = downloaded,
        status = status,
        etag = null,
        lastModified = null,
        segmentCount = 16,
        bytesPerSecond = rate,
        sha256 = null,
        errorMessage = if (status == "Failed") "HTTP 503" else null,
        contentUri = null,
        mimeType = "application/zip",
        requestHeaders = null,
        source = source,
        finalizeStep = null,
        finalizeProgress = 0f,
        createdAt = 1L,
        updatedAt = 2L,
    )

    @Test
    fun mapsProgressSpeedAndEta() {
        val row = DownloaderUiMapper.toRow(entity())

        assertEquals(0.5f, row.fraction, 0.001f)
        assertEquals(100L, row.bytesPerSecond)
        assertEquals(5L, row.etaSeconds)
        assertTrue(row.isActive)
        assertEquals("Downloading · 16 parts", DownloaderUiMapper.statusLabel(row))
        assertEquals("browser", row.source)
    }

    @Test
    fun aggregatesAndFiltersRows() {
        val withCompleted = listOf(
            DownloaderUiMapper.toRow(entity()),
            DownloaderUiMapper.toRow(entity(status = "Completed", downloaded = 2_000L, total = 2_000L)),
            DownloaderUiMapper.toRow(entity(status = "Failed")),
        )

        val stats = DownloaderUiMapper.stats(withCompleted)
        assertEquals(1, stats.active)
        assertEquals(1, stats.completed)
        assertEquals(1, stats.failed)
        assertEquals(100L, stats.bytesPerSecond)
        assertEquals(1, DownloaderUiMapper.filter(withCompleted, DownloaderFilter.Active).size)
        assertEquals(1, DownloaderUiMapper.filter(withCompleted, DownloaderFilter.Completed).size)
        assertEquals(1, DownloaderUiMapper.filter(withCompleted, DownloaderFilter.Failed).size)
    }

    @Test
    fun formatsHumanReadableValues() {
        assertEquals("1.5 MB", DownloaderUiMapper.formatBytes(1_500_000L))
        assertEquals("2.4 MB/s", DownloaderUiMapper.formatRate(2_400_000L))
        assertEquals("1m 5s", DownloaderUiMapper.formatEta(65L))
        assertEquals("—", DownloaderUiMapper.formatEta(null))
    }
}
