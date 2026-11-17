package com.abhi.armz.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeaderCodecTest {
    @Test
    fun `round trips browser headers`() {
        val headers = mapOf(
            "Cookie" to "session=abc; path=/; theme=dark",
            "User-Agent" to "AMz/1.0 (Linux; Android 14)",
            "Referer" to "https://example.com/page?a=1&b=2",
        )
        val encoded = HeaderCodec.encode(headers)
        assertEquals(headers, HeaderCodec.decode(encoded))
    }

    @Test
    fun `empty headers encode to null`() {
        assertNull(HeaderCodec.encode(emptyMap()))
        assertEquals(emptyMap(), HeaderCodec.decode(null))
        assertEquals(emptyMap(), HeaderCodec.decode(""))
    }
}
