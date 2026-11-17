package com.mochyy.mochymusic.downloader

import java.net.URI

/** Pure link decisions shared by the address bar, share receiver, and unit tests. */
object DownloadLinkPolicy {
    private val Domain = Regex(
        "^(?:[a-z0-9-]+\\.)+[a-z]{2,}(?::\\d+)?(?:/.*)?$",
        RegexOption.IGNORE_CASE,
    )
    private val DirectExtensions = setOf(
        "apk", "zip", "rar", "7z", "tar", "gz", "tgz", "xz", "bz2", "iso", "img", "bin",
        "mp4", "mkv", "webm", "avi", "mov", "m4v", "mp3", "m4a", "aac", "flac", "wav",
        "ogg", "opus", "pdf", "epub", "mobi", "azw3", "cbz", "cbr", "txt", "doc", "docx",
        "xls", "xlsx", "ppt", "pptx", "exe", "msi", "dmg", "deb", "rpm", "jar",
    )

    fun normalize(raw: String): String? {
        val value = raw.trim().trim('"', '\'', '<', '>')
        if (value.isBlank()) return null
        if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) {
            return value
        }
        if (value.startsWith("www.", ignoreCase = true)) return "https://$value"
        if (Domain.matches(value)) return "https://$value"
        return null
    }

    fun isHttpUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        val scheme = uri.scheme?.lowercase()
        (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    /** True when a navigation is a file fetch rather than an ordinary web page. */
    fun shouldCapture(url: String): Boolean {
        if (!isHttpUrl(url)) return false
        val lower = url.lowercase()
        if (lower.contains("response-content-disposition=attachment")) return true
        if (hasDownloadQuery(url)) return true

        val path = runCatching { URI(url).path ?: "" }.getOrDefault("")
        val name = path.substringAfterLast('/', "")
        val extension = name.substringAfterLast('.', "")
        return extension.isNotBlank() && extension.lowercase() in DirectExtensions
    }

    fun fileNameFrom(contentDisposition: String?): String? {
        if (contentDisposition.isNullOrBlank()) return null
        val encoded = contentDisposition
            .split(';')
            .map { it.trim() }
            .firstOrNull { it.startsWith("filename*=", ignoreCase = true) }
            ?.substringAfter('=', "")
            ?.trim('"')
            ?.substringAfter("''", "")

        if (!encoded.isNullOrBlank()) {
            val decoded = runCatching { java.net.URLDecoder.decode(encoded, Charsets.UTF_8.name()) }
                .getOrDefault(encoded)
            if (decoded.isNotBlank()) return decoded
        }
        return contentDisposition
            .split(';')
            .map { it.trim() }
            .firstOrNull { it.startsWith("filename=", ignoreCase = true) }
            ?.substringAfter('=', "")
            ?.trim('"')
            ?.takeIf { it.isNotBlank() }
    }

    private fun hasDownloadQuery(url: String): Boolean {
        val query = runCatching { URI(url).rawQuery }.getOrNull() ?: return false
        return query.split('&').any { part ->
            val key = part.substringBefore('=', "").lowercase()
            val value = part.substringAfter('=', "").lowercase()
            when (key) {
                "download", "dl" -> value == "1" || value == "true" || value.isBlank()
                "response-content-disposition" -> value.contains("attachment") || value.contains("filename")
                else -> false
            }
        }
    }
}
