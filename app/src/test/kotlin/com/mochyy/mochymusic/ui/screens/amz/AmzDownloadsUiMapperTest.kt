package com.mochyy.mochymusic.ui.screens.amz

import com.abhi.armz.data.local.DownloadEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AmzDownloadsUiMapperTest {
    @Test
    fun mapsEntityToVisibleRow() {
        val entity = DownloadEntity(
            id = "d1",
            url = "https://example.com/file.bin",
            fileName = "file.bin",
            filePath = "",
            totalBytes = 200,
            downloadedBytes = 50,
            status = "Downloading",
            etag = null,
            lastModified = null,
            segmentCount = 8,
            bytesPerSecond = 1024,
            sha256 = null,
            errorMessage = null,
            createdAt = 1,
            updatedAt = 2,
        )

        val row = AmzDownloadsUiMapper.toRow(entity)

        assertEquals("d1", row.id)
        assertEquals("file.bin", row.fileName)
        assertEquals("Downloading", row.status)
        assertEquals(0.25f, row.fraction, 0.001f)
        assertEquals(1024L, row.bytesPerSecond)
    }
}
