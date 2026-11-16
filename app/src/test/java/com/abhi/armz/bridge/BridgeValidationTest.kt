package com.abhi.armz.bridge

import com.abhi.armz.data.local.DownloadEntity
import com.abhi.armz.engine.ProbeResult
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeValidationTest {

    @Test
    fun startInputAcceptsHttpAndClampsHeaders() {
        val parsed = BridgeValidation.parseStart(
            """{"url":"https://example.com/a.zip","segments":8,"headers":{"X-Test":"yes"}}""",
        )

        assertNotNull(parsed)
        assertEquals("https://example.com/a.zip", parsed?.url)
        assertEquals(8, parsed?.segments)
        assertEquals("yes", parsed?.headers?.get("X-Test"))
    }

    @Test
    fun startInputRejectsUnsafeUrlsAndOutOfRangeSegments() {
        assertNull(BridgeValidation.parseStart("""{"url":"file:///etc/passwd"}"""))
        assertNull(BridgeValidation.parseStart("""{"url":"javascript:alert(1)"}"""))
        assertNull(BridgeValidation.parseStart("""{"url":"https://example.com","segments":0}"""))
        assertNull(BridgeValidation.parseStart("""{"url":"https://example.com","segments":17}"""))
        assertNull(BridgeValidation.parseStart("""{"url":"https://example.com","segments":"eight"}"""))
        assertNull(BridgeValidation.parseStart("""not-json"""))
    }

    @Test
    fun idsAndTextPayloadsAreValidated() {
        assertTrue(BridgeValidation.isId("0f4c1e44-7f27-4d06-9e4e-6f8de2fd0a11"))
        assertFalse(BridgeValidation.isId(""))
        assertFalse(BridgeValidation.isId("../escape"))
        assertFalse(BridgeValidation.isId("id with spaces"))

        val id = BridgeValidation.parseId("""{"id":"abc-123","deleteFile":true}""")
        assertEquals("abc-123", id?.id)
        assertEquals(true, id?.deleteFile)
        assertNull(BridgeValidation.parseId("""{"id":"../escape"}"""))

        assertEquals("hello", BridgeValidation.parseText("""{"text":"hello"}""")?.text)
        assertNull(BridgeValidation.parseText("""{"text":42}"""))
        assertEquals("light", BridgeValidation.parseHaptic("""{"kind":"light"}""")?.kind)
        assertNull(BridgeValidation.parseHaptic("""{"kind":""}"""))
    }

    @Test
    fun dtoSerializerKeepsTheTypeScriptContract() {
        val entity = DownloadEntity(
            id = "download-1",
            url = "https://example.com/a.zip",
            fileName = "a.zip",
            filePath = "/tmp/a.zip",
            totalBytes = 200L,
            downloadedBytes = 50L,
            status = "Downloading",
            etag = null,
            lastModified = null,
            segmentCount = 8,
            bytesPerSecond = 1024L,
            sha256 = null,
            errorMessage = null,
            contentUri = null,
            mimeType = "application/zip",
            finalizeStep = null,
            finalizeProgress = 0f,
            createdAt = 123L,
            updatedAt = 456L,
        )

        val event = JsonParser.parseString(BridgeMapper.downloadsJson(listOf(entity))).asJsonObject
        assertEquals("downloads", event.get("kind").asString)
        val download = event.getAsJsonArray("downloads").get(0).asJsonObject
        assertEquals("download-1", download.get("id").asString)
        assertEquals("Downloading", download.get("status").asString)
        assertEquals(true, download.get("live").asBoolean)
        assertEquals(0.25f, download.get("fraction").asFloat, 0.001f)
        assertEquals(8, download.get("segmentCount").asInt)
        assertEquals("application/zip", download.get("mimeType").asString)
    }

    @Test
    fun settingsAndProbePayloadsStayValidJson() {
        val settings = BridgeSettings(
            wifiOnly = true,
            maxConcurrentDownloads = 2,
            defaultSegments = 16,
            speedLimitBytesPerSecond = 5_000L,
            keepDownloadsAlive = false,
            visualEffects = "off",
        )
        val settingsJson = JsonParser.parseString(BridgeMapper.settingsJson(settings)).asJsonObject
        assertTrue(settingsJson.get("wifiOnly").asBoolean)
        assertEquals(16, settingsJson.get("defaultSegments").asInt)
        assertEquals("off", settingsJson.get("visualEffects").asString)

        val probe = ProbeResult(
            finalUrl = "https://example.com/final.zip",
            contentLength = 42L,
            rangeSupported = true,
            etag = "tag",
            lastModified = null,
            fileName = "final.zip",
            mimeType = "application/zip",
        )
        val probeJson = JsonParser.parseString(BridgeMapper.probeJson(probe)).asJsonObject
        assertEquals("final.zip", probeJson.get("fileName").asString)
        assertEquals("null", BridgeMapper.probeJson(null))
        assertEquals("\"quote\\\"slash\\\\\"", BridgeMapper.quote("quote\"slash\\"))
    }
}
