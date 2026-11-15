package com.abhi.armz.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverageVerifierTest {

    @Test
    fun `missing middle range is reported as a gap`() {
        val report = CoverageVerifier.inspect(
            checkpoints = listOf(
                SegmentCheckpoint(0, 0, 100, 100),
                SegmentCheckpoint(1, 200, 300, 100),
            ),
            totalBytes = 300,
        )

        assertFalse(report.complete)
        assertEquals(listOf(100L..199L), report.gaps)
        assertEquals(200, report.coveredBytes)
    }

    @Test
    fun `overlapping checkpoints are rejected`() {
        val report = CoverageVerifier.inspect(
            checkpoints = listOf(
                SegmentCheckpoint(0, 0, 200, 200),
                SegmentCheckpoint(1, 100, 300, 200),
            ),
            totalBytes = 300,
        )

        assertFalse(report.complete)
        assertEquals(listOf(100L..199L), report.overlaps)
        assertTrue(report.gaps.isEmpty())
    }

    @Test
    fun `complete contiguous coverage passes`() {
        val report = CoverageVerifier.inspect(
            checkpoints = listOf(
                SegmentCheckpoint(0, 0, 128, 128),
                SegmentCheckpoint(1, 128, 256, 128),
            ),
            totalBytes = 256,
        )

        assertTrue(report.complete)
        assertTrue(report.gaps.isEmpty())
        assertTrue(report.overlaps.isEmpty())
    }
}

class PublishNamesTest {

    @Test
    fun `duplicate names get a numbered suffix and keep the extension`() {
        val existing = mutableSetOf("movie.mp4", "movie (1).mp4")

        val name = PublishNames.uniqueName("movie.mp4") { it in existing }

        assertEquals("movie (2).mp4", name)
    }

    @Test
    fun `mime type comes from the extension before the hint`() {
        assertEquals(
            "video/x-matroska",
            PublishNames.mimeTypeFor("movie.mkv", "application/octet-stream"),
        )
    }

    @Test
    fun `unknown extension falls back to a clean mime hint`() {
        assertEquals(
            "text/plain",
            PublishNames.mimeTypeFor("notes.armz", "text/plain; charset=utf-8"),
        )
    }

    @Test
    fun `apk mime is installable`() {
        assertEquals(
            "application/vnd.android.package-archive",
            PublishNames.mimeTypeFor("ARMz-debug.apk"),
        )
    }
}
