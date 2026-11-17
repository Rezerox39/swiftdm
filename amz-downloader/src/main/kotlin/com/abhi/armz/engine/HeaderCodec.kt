package com.abhi.armz.engine

import java.net.URLDecoder
import java.net.URLEncoder

/** Compact, dependency-free persistence for the small header set captured by the browser. */
object HeaderCodec {
    private const val PairSeparator = '&'
    private const val KeyValueSeparator = '='

    fun encode(headers: Map<String, String>): String? {
        if (headers.isEmpty()) return null
        return headers
            .filter { it.key.isNotBlank() }
            .toSortedMap(compareBy { it.lowercase() })
            .entries
            .joinToString(PairSeparator.toString()) { (key, value) ->
                "${encodePart(key)}$KeyValueSeparator${encodePart(value)}"
            }
            .takeIf { it.isNotBlank() }
    }

    fun decode(encoded: String?): Map<String, String> {
        if (encoded.isNullOrBlank()) return emptyMap()
        return encoded
            .split(PairSeparator)
            .mapNotNull { part ->
                val split = part.indexOf(KeyValueSeparator)
                if (split <= 0) return@mapNotNull null
                val key = decodePart(part.substring(0, split))
                val value = decodePart(part.substring(split + 1))
                key.takeIf { it.isNotBlank() }?.to(value)
            }
            .toMap()
    }

    private fun encodePart(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun decodePart(value: String): String =
        runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault(value)
}
