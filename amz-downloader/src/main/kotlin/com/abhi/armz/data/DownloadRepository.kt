package com.abhi.armz.data

import android.content.Context
import android.os.Environment
import androidx.room.withTransaction
import com.abhi.armz.data.local.ArmzDatabase
import com.abhi.armz.data.local.DownloadDao
import com.abhi.armz.data.local.DownloadEntity
import com.abhi.armz.data.local.SegmentEntity
import com.abhi.armz.data.publish.ArmzStorage
import com.abhi.armz.engine.CheckpointSink
import com.abhi.armz.engine.DownloadEngine
import com.abhi.armz.engine.DownloadHandle
import com.abhi.armz.engine.DownloadRequest
import com.abhi.armz.engine.DownloadSnapshot
import com.abhi.armz.engine.DownloadStatus
import com.abhi.armz.engine.HeaderCodec
import com.abhi.armz.engine.ProbeResult
import com.abhi.armz.engine.LocalFilePublisher
import com.abhi.armz.engine.PublishedFile
import com.abhi.armz.engine.ResumeState
import com.abhi.armz.engine.SegmentCheckpoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinator between the pure engine and Room. Persistence is deliberately diffed: checkpoints
 * arrive twice a second, but only segments whose bytes/state actually changed are written.
 */
@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ArmzDatabase,
    private val dao: DownloadDao,
    private val engine: DownloadEngine,
    private val storage: ArmzStorage,
) {

    private val checkpointSink = object : CheckpointSink {
        override suspend fun persist(snapshot: DownloadSnapshot) = persistSnapshot(snapshot)

        override suspend fun clearSegments(downloadId: String) = dao.deleteSegments(downloadId)
    }

    fun observeDownloads(): Flow<List<DownloadEntity>> = dao.observeAll()

    suspend fun find(id: String): DownloadEntity? = dao.find(id)

    suspend fun probe(url: String): ProbeResult = engine.probe(url)

    /** Debug lab runs against the local publisher so the on-disk folder can be inspected. */
    suspend fun startLab(request: DownloadRequest): DownloadHandle =
        engine.start(
            request = request,
            checkpointSink = checkpointSink,
            publisherOverride = LocalFilePublisher(),
        )

    suspend fun startNew(
        url: String,
        segments: Int,
        fileName: String?,
        mimeType: String?,
        headers: Map<String, String> = emptyMap(),
        source: String? = null,
    ): DownloadHandle {
        val request = DownloadRequest(
            id = UUID.randomUUID().toString(),
            url = url.trim(),
            destinationDir = partsDirectory(),
            segments = segments,
            fileNameOverride = fileName,
            mimeTypeHint = mimeType,
            headers = headers,
        )
        persistSnapshot(
            DownloadSnapshot(
                id = request.id,
                url = request.url,
                fileName = request.fileNameOverride ?: "",
                filePath = "",
                totalBytes = request.resume?.totalBytes ?: -1L,
                downloadedBytes = 0L,
                status = DownloadStatus.Probing,
                bytesPerSecond = 0L,
                etaSeconds = null,
                rangeSupported = false,
                etag = request.resume?.etag,
                lastModified = request.resume?.lastModified,
                segmentCount = request.safeSegments,
                segments = emptyList(),
                sha256 = null,
                error = null,
            ),
            requestHeaders = HeaderCodec.encode(headers),
            source = source,
        )
        return start(request)
    }

    private suspend fun start(request: DownloadRequest): DownloadHandle =
        engine.start(request = request, checkpointSink = checkpointSink)

    /**
     * Rebuilds an engine request from a persisted row. Part files live in app-specific external
     * storage, so this also works after the process is killed. Completed rows do not resume.
     */
    suspend fun resume(id: String): DownloadHandle? {
        val record = dao.find(id) ?: return null
        if (record.status == "Completed") return null
        val file = record.filePath.takeIf { it.isNotBlank() }?.let(::File)
        val directory = file?.parentFile ?: partsDirectory()
        val exists = file?.exists() == true
        val segments = if (exists) {
            dao.segments(id).map {
                SegmentCheckpoint(
                    index = it.segmentIndex,
                    start = it.startByte,
                    end = it.endByte,
                    downloaded = it.downloadedBytes,
                )
            }
        } else {
            emptyList()
        }
        val request = DownloadRequest(
            id = record.id,
            url = record.url,
            destinationDir = directory,
            segments = record.segmentCount,
            fileNameOverride = record.fileName.takeIf { it.isNotBlank() },
            headers = HeaderCodec.decode(record.requestHeaders),
            mimeTypeHint = record.mimeType,
            resume = if (exists) {
                ResumeState(
                    filePath = record.filePath,
                    totalBytes = record.totalBytes,
                    etag = record.etag,
                    lastModified = record.lastModified,
                    segments = segments,
                )
            } else {
                null
            },
        )
        return start(request)
    }

    /** Rows left active by a killed process. The app controller resumes these after restart. */
    suspend fun interrupted(): List<DownloadEntity> = dao.interrupted()

    /** Marks a row active before the service starts, so it cannot stop on a stale paused status. */
    suspend fun markActive(id: String) = dao.markActive(id, "Probing", System.currentTimeMillis())

    /** Removes only the history row; the published download stays on the device. */
    suspend fun deleteRecord(id: String) {
        dao.deleteSegments(id)
        dao.deleteDownload(id)
    }

    suspend fun delete(id: String) {
        val record = dao.find(id)
        if (record != null) {
            if (!record.contentUri.isNullOrBlank() || record.filePath.isNotBlank()) {
                runCatching {
                    storage.delete(
                        PublishedFile(
                            uri = record.contentUri,
                            path = record.filePath,
                            displayName = record.fileName,
                            mimeType = record.mimeType ?: "application/octet-stream",
                        )
                    )
                }
            }
            if (record.filePath.isNotBlank()) {
                runCatching { File(record.filePath).delete() }
            }
        }
        dao.deleteDownload(id)
    }

    private fun partsDirectory(): File {
        val external = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return File(external ?: File(context.filesDir, "Download"), "parts").apply { mkdirs() }
    }

    private suspend fun persistSnapshot(
        snapshot: DownloadSnapshot,
        requestHeaders: String? = null,
        source: String? = null,
    ) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val existing = dao.find(snapshot.id)
            dao.upsertDownload(
                DownloadEntity(
                    id = snapshot.id,
                    url = snapshot.url,
                    fileName = snapshot.fileName,
                    filePath = snapshot.filePath,
                    totalBytes = snapshot.totalBytes,
                    downloadedBytes = snapshot.downloadedBytes,
                    status = snapshot.status.name,
                    etag = snapshot.etag,
                    lastModified = snapshot.lastModified,
                    segmentCount = snapshot.segmentCount,
                    bytesPerSecond = snapshot.bytesPerSecond,
                    sha256 = snapshot.sha256,
                    errorMessage = snapshot.error?.message,
                    contentUri = snapshot.contentUri,
                    mimeType = snapshot.mimeType,
                    requestHeaders = requestHeaders ?: existing?.requestHeaders,
                    source = source ?: existing?.source,
                    finalizeStep = snapshot.finalizeStep?.name,
                    finalizeProgress = snapshot.finalizeProgress,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
            )

            if (snapshot.status == com.abhi.armz.engine.DownloadStatus.Completed) {
                // Published downloads keep their row/history, never their checkpoint rows.
                dao.deleteSegments(snapshot.id)
                return@withTransaction
            }

            val persisted = dao.segments(snapshot.id).associateBy { it.segmentIndex }
            val incoming = snapshot.segments.associateBy { it.index }
            val changed = incoming.values.mapNotNull { segment ->
                val entity = SegmentEntity(
                    downloadId = snapshot.id,
                    segmentIndex = segment.index,
                    startByte = segment.start,
                    endByte = segment.end,
                    downloadedBytes = segment.downloaded,
                    state = segment.state.name,
                )
                val old = persisted[segment.index]
                if (old == entity) null else entity
            }
            if (changed.isNotEmpty()) dao.upsertSegments(changed)
            persisted.keys.filter { it !in incoming }.forEach { index ->
                dao.deleteSegment(snapshot.id, index)
            }
        }
    }
}
