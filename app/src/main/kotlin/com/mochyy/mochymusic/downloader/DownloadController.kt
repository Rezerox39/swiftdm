package com.mochyy.mochymusic.downloader

import android.content.Context
import com.abhi.armz.data.DownloadRepository
import com.abhi.armz.engine.DownloadHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide handle registry. Keeping it outside the ViewModel means navigation never drops a
 * running transfer, and the notification can resume work the UI started before it was killed.
 */
@Singleton
class DownloadController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DownloadRepository,
) {
    private val handles = ConcurrentHashMap<String, DownloadHandle>()

    suspend fun add(
        url: String,
        segments: Int,
        headers: Map<String, String> = emptyMap(),
        fileName: String? = null,
        mimeType: String? = null,
        source: String? = null,
    ): DownloadHandle {
        val handle = repository.startNew(
            url = url,
            segments = segments,
            fileName = fileName,
            mimeType = mimeType,
            headers = headers,
            source = source,
        )
        handles[handle.id] = handle
        DownloaderService.start(context)
        return handle
    }

    suspend fun pause(id: String) {
        handles[id]?.pause()
    }

    suspend fun resume(id: String) {
        var handle = handles[id]
        repository.markActive(id)
        if (handle == null) {
            handle = repository.resume(id)?.also { handles[id] = it }
        } else {
            handle.resume()
        }
        if (handle != null) DownloaderService.start(context)
    }

    suspend fun cancel(id: String) {
        handles[id]?.cancel()
        handles.remove(id)
    }

    suspend fun remove(id: String) {
        cancel(id)
        repository.delete(id)
    }

    suspend fun pauseAll() {
        repository.observeDownloads().first()
            .filter { it.status in ACTIVE_STATUSES }
            .forEach { pause(it.id) }
    }

    suspend fun resumeAll() {
        repository.observeDownloads().first()
            .filter { it.status in RESUMABLE_STATUSES }
            .forEach { resume(it.id) }
    }

    suspend fun clearFinished() {
        repository.observeDownloads().first()
            .filter { it.status in FINISHED_STATUSES }
            .forEach { id ->
                cancel(id)
                repository.deleteRecord(id)
            }
    }

    /** Called from app start and the boot receiver: old active sessions become live handles again. */
    suspend fun resumeInterrupted(): Int {
        val rows = repository.interrupted()
        rows.forEach { row ->
            if (!handles.containsKey(row.id)) {
                repository.resume(row.id)?.let { handles[row.id] = it }
            }
        }
        if (rows.isNotEmpty()) DownloaderService.start(context)
        return rows.size
    }

    private companion object {
        val ACTIVE_STATUSES = setOf("Probing", "Downloading", "Finalizing")
        val RESUMABLE_STATUSES = setOf("Paused", "Failed")
        val FINISHED_STATUSES = setOf("Completed", "Cancelled", "Failed")
    }
}
