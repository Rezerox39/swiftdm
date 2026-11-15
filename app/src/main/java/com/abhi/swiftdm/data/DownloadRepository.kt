package com.abhi.swiftdm.data

import androidx.room.withTransaction
import com.abhi.swiftdm.data.local.DownloadDao
import com.abhi.swiftdm.data.local.DownloadEntity
import com.abhi.swiftdm.data.local.SegmentEntity
import com.abhi.swiftdm.data.local.SwiftDmDatabase
import com.abhi.swiftdm.engine.CheckpointSink
import com.abhi.swiftdm.engine.DownloadEngine
import com.abhi.swiftdm.engine.DownloadHandle
import com.abhi.swiftdm.engine.DownloadRequest
import com.abhi.swiftdm.engine.DownloadSnapshot
import com.abhi.swiftdm.engine.ResumeState
import com.abhi.swiftdm.engine.SegmentCheckpoint
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin coordinator between the pure engine and Room. The engine stays free of Android imports;
 * all persistence happens through [CheckpointSink] callbacks at most twice a second.
 */
@Singleton
class DownloadRepository @Inject constructor(
    private val database: SwiftDmDatabase,
    private val dao: DownloadDao,
    private val engine: DownloadEngine,
) {

    fun observeDownloads(): Flow<List<DownloadEntity>> = dao.observeAll()

    suspend fun find(id: String): DownloadEntity? = dao.find(id)

    suspend fun start(request: DownloadRequest): DownloadHandle =
        engine.start(
            request = request,
            checkpointSink = CheckpointSink { snapshot -> persist(snapshot) },
        )

    /**
     * Rebuilds an engine request from a persisted row. Used by the debug lab, and later by the
     * foreground service, to continue after the process dies.
     */
    suspend fun resume(id: String): DownloadHandle? {
        val record = dao.find(id) ?: return null
        val file = File(record.filePath)
        val directory = file.parentFile ?: return null
        val segments = dao.segments(id).map {
            SegmentCheckpoint(
                index = it.segmentIndex,
                start = it.startByte,
                end = it.endByte,
                downloaded = it.downloadedBytes,
            )
        }
        val request = DownloadRequest(
            id = record.id,
            url = record.url,
            destinationDir = directory,
            segments = record.segmentCount,
            fileNameOverride = file.name,
            resume = ResumeState(
                filePath = record.filePath,
                totalBytes = record.totalBytes,
                etag = record.etag,
                lastModified = record.lastModified,
                segments = segments,
            ),
        )
        return start(request)
    }

    suspend fun delete(id: String) {
        dao.find(id)?.let { record ->
            File(record.filePath).delete()
        }
        dao.deleteDownload(id)
    }

    private suspend fun persist(snapshot: DownloadSnapshot) {
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
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
            )
            dao.deleteSegments(snapshot.id)
            if (snapshot.segments.isNotEmpty()) {
                dao.insertSegments(
                    snapshot.segments.map { segment ->
                        SegmentEntity(
                            downloadId = snapshot.id,
                            segmentIndex = segment.index,
                            startByte = segment.start,
                            endByte = segment.end,
                            downloadedBytes = segment.downloaded,
                            state = segment.state.name,
                        )
                    }
                )
            }
        }
    }
}
