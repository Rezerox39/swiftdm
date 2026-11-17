package com.mochyy.mochymusic.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadLinkPolicyTest {
    @Test
    fun normalizesBareDomainsAndQuotedLinks() {
        assertEquals("https://example.com", DownloadLinkPolicy.normalize("example.com"))
        assertEquals("https://www.example.com/a", DownloadLinkPolicy.normalize("\"www.example.com/a\""))
        assertEquals("https://example.com", DownloadLinkPolicy.normalize(" HTTPS://example.com "))
        assertNull(DownloadLinkPolicy.normalize("not a link"))
        assertNull(DownloadLinkPolicy.normalize("ftp://example.com/file.zip"))
    }

    @Test
    fun capturesDirectDownloadsButNotPages() {
        assertTrue(DownloadLinkPolicy.shouldCapture("https://cdn.example.com/file.apk"))
        assertTrue(DownloadLinkPolicy.shouldCapture("https://cdn.example.com/get?download=1"))
        assertTrue(
            DownloadLinkPolicy.shouldCapture(
                "https://storage.example.com/o/key?response-content-disposition=attachment%3B%20filename%3Da.zip",
            ),
        )
        assertFalse(DownloadLinkPolicy.shouldCapture("https://example.com/blog/post"))
        assertFalse(DownloadLinkPolicy.shouldCapture("file:///sdcard/file.apk"))
    }

    @Test
    fun parsesContentDispositionNames() {
        assertEquals("movie.mp4", DownloadLinkPolicy.fileNameFrom("attachment; filename=\"movie.mp4\""))
        assertEquals(
            "spring notes.pdf",
            DownloadLinkPolicy.fileNameFrom("attachment; filename*=UTF-8''spring%20notes.pdf"),
        )
        assertNull(DownloadLinkPolicy.fileNameFrom(null))
    }
}
