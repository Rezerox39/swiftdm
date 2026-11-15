package com.abhi.armz.engine

import java.io.File
import java.io.IOException
import java.util.Locale

data class PublishedFile(
    val uri: String?,
    val path: String,
    val displayName: String,
    val mimeType: String,
)

/**
 * Moves a finished part file into its final home. The default implementation publishes next to the
 * part file, which keeps the engine JVM-testable; Android injects a MediaStore implementation.
 */
interface FilePublisher {
    val requiresCopy: Boolean

    suspend fun publish(
        partFile: File,
        displayName: String,
        mimeType: String?,
        onProgress: (copied: Long, total: Long) -> Unit,
    ): PublishedFile

    suspend fun delete(published: PublishedFile): Boolean = false
}

/** Pure JVM publisher used by tests and by builds without a platform storage backend. */
class LocalFilePublisher : FilePublisher {

    override val requiresCopy: Boolean = false

    override suspend fun publish(
        partFile: File,
        displayName: String,
        mimeType: String?,
        onProgress: (copied: Long, total: Long) -> Unit,
    ): PublishedFile {
        val directory = partFile.parentFile ?: throw IOException("Part file has no parent")
        val total = partFile.length()
        val name = PublishNames.uniqueName(displayName) { candidate -> File(directory, candidate).exists() }
        val target = File(directory, name)
        if (!partFile.renameTo(target)) {
            partFile.copyTo(target, overwrite = false)
            if (!partFile.delete()) {
                throw IOException("Could not remove ${partFile.name} after copying")
            }
        }
        onProgress(total, total)
        return PublishedFile(
            uri = null,
            path = target.absolutePath,
            displayName = target.name,
            mimeType = PublishNames.mimeTypeFor(target.name, mimeType),
        )
    }

    override suspend fun delete(published: PublishedFile): Boolean =
        published.path.isNotBlank() && File(published.path).delete()
}

object PublishNames {

    private val mimeByExtension: Map<String, String> = mapOf(
        "mp4" to "video/mp4",
        "m4v" to "video/mp4",
        "mkv" to "video/x-matroska",
        "webm" to "video/webm",
        "mov" to "video/quicktime",
        "avi" to "video/x-msvideo",
        "mp3" to "audio/mpeg",
        "m4a" to "audio/mp4",
        "aac" to "audio/aac",
        "flac" to "audio/flac",
        "wav" to "audio/wav",
        "ogg" to "audio/ogg",
        "opus" to "audio/opus",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "webp" to "image/webp",
        "gif" to "image/gif",
        "heic" to "image/heic",
        "pdf" to "application/pdf",
        "zip" to "application/zip",
        "7z" to "application/x-7z-compressed",
        "rar" to "application/vnd.rar",
        "tar" to "application/x-tar",
        "gz" to "application/gzip",
        "xz" to "application/x-xz",
        "apk" to "application/vnd.android.package-archive",
        "epub" to "application/epub+zip",
        "txt" to "text/plain",
        "csv" to "text/csv",
        "json" to "application/json",
        "html" to "text/html",
        "xml" to "application/xml",
        "iso" to "application/x-iso9660-image",
        "bin" to "application/octet-stream",
    )

    fun mimeTypeFor(fileName: String, hint: String? = null): String {
        val extension = extensionOf(fileName)
        val fromExtension = mimeByExtension[extension]
        if (fromExtension != null) return fromExtension
        val cleanedHint = hint?.substringBefore(';')?.trim()?.lowercase(Locale.US)
        if (!cleanedHint.isNullOrBlank() && cleanedHint != "application/octet-stream" &&
            cleanedHint != "binary/octet-stream"
        ) {
            return cleanedHint
        }
        return "application/octet-stream"
    }

    /** "name (1).ext", "name (2).ext", … with the extension preserved. */
    fun uniqueName(name: String, exists: (String) -> Boolean): String {
        if (!exists(name)) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var counter = 1
        while (true) {
            val candidate = "$base ($counter)$extension"
            if (!exists(candidate)) return candidate
            counter++
        }
    }

    fun extensionOf(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        if (dot <= 0 || dot == fileName.lastIndex) return ""
        return fileName.substring(dot + 1).lowercase(Locale.US)
    }
}
