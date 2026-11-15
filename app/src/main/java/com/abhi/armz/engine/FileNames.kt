package com.abhi.armz.engine

import java.io.File
import java.net.URLDecoder

/** Resolves and sanitises file names coming from Content-Disposition or the request URL. */
object FileNames {

    private const val MaxNameLength = 160

    fun fromContentDisposition(header: String?): String? {
        if (header.isNullOrBlank()) return null
        var plain: String? = null
        for (rawPart in header.split(';')) {
            val part = rawPart.trim()
            if (part.startsWith("filename*=", ignoreCase = true)) {
                val value = part.substringAfter('=').trim().trim('"')
                val separator = value.indexOf("''")
                val encoded = if (separator >= 0) value.substring(separator + 2) else value
                val decoded = runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrNull()
                sanitize(decoded)?.let { return it }
            }
            if (plain == null && part.startsWith("filename=", ignoreCase = true)) {
                plain = part.substringAfter('=').trim().trim('"')
            }
        }
        return sanitize(plain)
    }

    fun fromUrl(url: String): String? {
        val withoutQuery = url.substringBefore('#').substringBefore('?')
        val path = withoutQuery.substringAfter("://", withoutQuery)
        val last = path.substringAfterLast('/', missingDelimiterValue = "")
        val decoded = runCatching { URLDecoder.decode(last, "UTF-8") }.getOrNull() ?: last
        return sanitize(decoded)
    }

    fun sanitize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim().trim('"').trim()
        if (trimmed.isEmpty() || trimmed == "." || trimmed == "..") return null
        val builder = StringBuilder(trimmed.length)
        for (character in trimmed) {
            val invalid = character.code < 32 ||
                character in "\\/:*?\"<>|\u0000" ||
                character == '\n' ||
                character == '\r'
            builder.append(if (invalid) '_' else character)
        }
        val cleaned = builder.toString().trim().trimEnd('.', ' ')
        if (cleaned.isEmpty()) return null
        return cleaned.take(MaxNameLength).ifBlank { null }
    }

    /** Returns a path inside [directory] that does not exist yet, appending " (n)" when needed. */
    fun unique(directory: File, name: String): File {
        val candidate = File(directory, name)
        if (!candidate.exists()) return candidate
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var counter = 1
        while (true) {
            val next = File(directory, "$base ($counter)$extension")
            if (!next.exists()) return next
            counter++
        }
    }
}
