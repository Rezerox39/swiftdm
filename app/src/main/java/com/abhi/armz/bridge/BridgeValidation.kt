package com.abhi.armz.bridge

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI

/** Kotlin-side validation for every string that crosses the JS boundary. */
object BridgeValidation {
    fun parseObject(raw: String): JsonObject? = runCatching {
        JsonParser.parseString(raw).asJsonObject
    }.getOrNull()

    fun parseStart(raw: String): BridgeStartInput? = runCatching {
        val objectValue = parseObject(raw) ?: return null
        val url = objectValue.string("url")?.trim().orEmpty()
        if (!isHttpUrl(url)) return null

        val segmentsElement = objectValue.get("segments")
        val segments = if (segmentsElement == null) {
            null
        } else {
            if (!segmentsElement.isJsonPrimitive || !segmentsElement.asJsonPrimitive.isNumber) {
                return null
            }
            segmentsElement.asInt
        }
        if (segments != null && segments !in 1..16) return null

        val headers = when {
            !objectValue.has("headers") || objectValue.get("headers").isJsonNull -> emptyMap()
            else -> {
                val headerObject = objectValue.getAsJsonObject("headers")
                if (headerObject.size() > 32) return null
                headerObject.entrySet().associate { (key, value) ->
                    require(key.isNotBlank() && key.length <= 128)
                    require(value.isJsonPrimitive && value.asJsonPrimitive.isString)
                    key to value.asString
                }
            }
        }

        BridgeStartInput(url = url, headers = headers, segments = segments)
    }.getOrNull()

    fun parseId(raw: String): BridgeIdInput? = runCatching {
        val objectValue = parseObject(raw) ?: return null
        val id = objectValue.string("id")?.trim().orEmpty()
        if (!isId(id)) return null
        val deleteFile = objectValue.get("deleteFile")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }
            ?.asBoolean
            ?: false
        BridgeIdInput(id = id, deleteFile = deleteFile)
    }.getOrNull()

    fun parseText(raw: String): BridgeTextInput? = runCatching {
        val objectValue = parseObject(raw) ?: return null
        val text = objectValue.string("text") ?: return null
        if (text.length > 16_384) return null
        BridgeTextInput(text)
    }.getOrNull()

    fun parseHaptic(raw: String): BridgeHapticInput? = runCatching {
        val objectValue = parseObject(raw) ?: return null
        val kind = objectValue.string("kind")?.trim().orEmpty()
        if (kind.isBlank() || kind.length > 32) return null
        BridgeHapticInput(kind)
    }.getOrNull()

    fun isHttpUrl(raw: String): Boolean {
        if (raw.isBlank() || raw.length > 8_192) return false
        val uri = runCatching { URI(raw) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
    }

    fun isId(raw: String): Boolean =
        raw.isNotBlank() && raw.length <= 128 && raw.all { it.isLetterOrDigit() || it == '-' || it == '_' }

    private fun JsonObject.string(key: String): String? =
        get(key)
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString
}
