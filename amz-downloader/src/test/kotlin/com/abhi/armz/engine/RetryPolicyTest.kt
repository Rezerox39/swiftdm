package com.abhi.armz.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyTest {

    @Test
    fun `default policy backs off one two four eight seconds`() {
        val policy = RetryPolicy()
        assertEquals(1_000L, policy.delayMillisAfter(1))
        assertEquals(2_000L, policy.delayMillisAfter(2))
        assertEquals(4_000L, policy.delayMillisAfter(3))
        assertEquals(8_000L, policy.delayMillisAfter(4))
        assertEquals(8_000L, policy.delayMillisAfter(5))
    }

    @Test
    fun `custom initial delay scales the same way`() {
        val policy = RetryPolicy(maxAttempts = 4, initialDelayMs = 25L)
        assertEquals(25L, policy.delayMillisAfter(1))
        assertEquals(50L, policy.delayMillisAfter(2))
        assertEquals(100L, policy.delayMillisAfter(3))
    }

    @Test
    fun `retryable and terminal status classification`() {
        assertTrue(isRetryableStatus(500))
        assertTrue(isRetryableStatus(503))
        assertTrue(isRetryableStatus(429))
        assertTrue(isRetryableStatus(408))
        assertFalse(isRetryableStatus(404))
        assertTrue(isTerminalStatus(403))
        assertTrue(isTerminalStatus(404))
        assertTrue(isTerminalStatus(410))
        assertFalse(isTerminalStatus(500))
    }

    @Test
    fun `filename sanitising strips separators and traversal`() {
        assertEquals("foo_bar.zip", FileNames.sanitize("foo/bar.zip"))
        assertEquals(null, FileNames.sanitize(".."))
        assertEquals(null, FileNames.sanitize("  "))
        assertEquals("report.pdf", FileNames.fromContentDisposition("attachment; filename=\"report.pdf\""))
        assertEquals(
            "my file.zip",
            FileNames.fromContentDisposition("attachment; filename*=UTF-8''my%20file.zip"),
        )
        assertEquals("100Mb.dat", FileNames.fromUrl("https://proof.ovh.net/files/100Mb.dat?x=1"))
    }
}
