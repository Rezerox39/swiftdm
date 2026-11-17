package com.abhi.armz.data.publish

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.abhi.armz.engine.FilePublisher
import com.abhi.armz.engine.PublishNames
import com.abhi.armz.engine.PublishedFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes a finished part file into public Downloads on API 29+, using MediaStore's pending
 * flag so half-written files never appear in the gallery/file manager. API 26-28 falls back to
 * the app-specific external Downloads/ARMz directory, exposed through FileProvider.
 */
@Singleton
class ArmzStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) : FilePublisher {

    override val requiresCopy: Boolean = true

    override suspend fun publish(
        partFile: File,
        displayName: String,
        mimeType: String?,
        onProgress: (copied: Long, total: Long) -> Unit,
    ): PublishedFile = withContext(Dispatchers.IO) {
        val resolvedMime = PublishNames.mimeTypeFor(displayName, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishToMediaStore(partFile, displayName, resolvedMime, onProgress)
        } else {
            publishToAppExternal(partFile, displayName, resolvedMime, onProgress)
        }
    }

    private fun publishToMediaStore(
        partFile: File,
        displayName: String,
        mimeType: String,
        onProgress: (Long, Long) -> Unit,
    ): PublishedFile {
        val resolver = context.contentResolver
        val finalName = uniqueMediaStoreName(displayName)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values)
            ?: throw IOException("Could not create a Downloads entry")
        var published = false
        try {
            val total = partFile.length()
            var copied = 0L
            resolver.openFileDescriptor(uri, "w")?.use { descriptor ->
                FileInputStream(partFile).use { input ->
                    FileOutputStream(descriptor.fileDescriptor).use { output ->
                        val buffer = ByteArray(CopyBuffer)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            onProgress(copied, total)
                        }
                        output.flush()
                        output.fd.sync()
                    }
                }
            } ?: throw IOException("Could not open the Downloads entry for writing")
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )
            published = true
            if (!partFile.delete()) {
                throw IOException("Published but could not remove the part file")
            }
            onProgress(total, total)
            return PublishedFile(
                uri = uri.toString(),
                path = "",
                displayName = finalName,
                mimeType = mimeType,
            )
        } catch (cancellation: CancellationException) {
            runCatching { resolver.delete(uri, null, null) }
            throw cancellation
        } catch (failure: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw failure
        } finally {
            if (!published) runCatching { resolver.delete(uri, null, null) }
        }
    }

    private fun publishToAppExternal(
        partFile: File,
        displayName: String,
        mimeType: String,
        onProgress: (Long, Long) -> Unit,
    ): PublishedFile {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "Downloads")
        val directory = File(root, FALLBACK_FOLDER).apply { mkdirs() }
        val finalName = PublishNames.uniqueName(displayName) { File(directory, it).exists() }
        val target = File(directory, finalName)
        val total = partFile.length()
        var copied = 0L
        partFile.inputStream().use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(CopyBuffer)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copied += read
                    onProgress(copied, total)
                }
                output.flush()
            }
        }
        if (!partFile.delete()) {
            target.delete()
            throw IOException("Could not remove the part file")
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            target,
        )
        onProgress(total, total)
        return PublishedFile(
            uri = uri.toString(),
            path = target.absolutePath,
            displayName = finalName,
            mimeType = mimeType,
        )
    }

    private fun uniqueMediaStoreName(displayName: String): String {
        val resolver = context.contentResolver
        return runCatching {
            PublishNames.uniqueName(displayName) { candidate ->
                resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND " +
                        "${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                    arrayOf(candidate, RELATIVE_PATH),
                    null,
                )?.use { cursor -> cursor.moveToFirst() } ?: false
            }
        }.getOrDefault(displayName)
    }

    override suspend fun delete(published: PublishedFile): Boolean = withContext(Dispatchers.IO) {
        val uri = published.uri
        if (!uri.isNullOrBlank()) {
            runCatching { context.contentResolver.delete(Uri.parse(uri), null, null) > 0 }
                .getOrDefault(false)
        } else if (published.path.isNotBlank()) {
            File(published.path).delete()
        } else {
            false
        }
    }

    private companion object {
        const val RELATIVE_PATH = "Download/ARMz/"
        const val FALLBACK_FOLDER = "ARMz"
        const val CopyBuffer = 128 * 1024
    }
}
