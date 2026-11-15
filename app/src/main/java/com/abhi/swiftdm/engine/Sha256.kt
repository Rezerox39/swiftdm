package com.abhi.swiftdm.engine

import java.io.File
import java.security.MessageDigest

/** SHA-256 helpers used to prove a finished file matches the source byte-for-byte. */
object Sha256 {

    private val HexDigits = "0123456789abcdef".toCharArray()

    fun of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return toHex(digest.digest())
    }

    fun of(bytes: ByteArray): String = toHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    fun matches(file: File, expectedHex: String): Boolean =
        of(file).equals(expectedHex, ignoreCase = true)

    private fun toHex(bytes: ByteArray): String {
        val chars = CharArray(bytes.size * 2)
        var index = 0
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            chars[index++] = HexDigits[value ushr 4]
            chars[index++] = HexDigits[value and 0x0F]
        }
        return String(chars)
    }
}

internal fun ensureFreeSpace(directory: File, requiredBytes: Long) {
    directory.mkdirs()
    if (requiredBytes <= 0L) return
    val usable = directory.usableSpace
    if (usable <= 0L) return
    val reserve = 16L * 1024L * 1024L
    if (usable < requiredBytes + reserve) {
        throw EngineException.Storage(
            "Not enough free space: need ${requiredBytes / (1024 * 1024)} MB, " +
                "have ${usable / (1024 * 1024)} MB"
        )
    }
}
