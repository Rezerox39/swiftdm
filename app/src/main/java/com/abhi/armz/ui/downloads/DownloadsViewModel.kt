package com.abhi.armz.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi.armz.data.DownloadRepository
import com.abhi.armz.data.local.DownloadEntity
import com.abhi.armz.engine.DownloadHandle
import com.abhi.armz.engine.DownloadSnapshot
import com.abhi.armz.engine.DownloadStatus
import com.abhi.armz.engine.FinalizeStep
import com.abhi.armz.engine.ProbeResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProbeUiState(
    val url: String = "",
    val loading: Boolean = false,
    val result: ProbeResult? = null,
    val error: String? = null,
)

data class DownloadItem(
    val id: String,
    val url: String,
    val fileName: String,
    val filePath: String,
    val contentUri: String?,
    val mimeType: String?,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val bytesPerSecond: Long,
    val etaSeconds: Long?,
    val error: String?,
    val finalizeStep: FinalizeStep?,
    val finalizeProgress: Float,
    val segmentCount: Int,
    val createdAt: Long,
    val live: Boolean,
) {
    val fraction: Float
        get() {
            if (totalBytes <= 0L) return 0f
            return (downloadedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
        }

    val canPause: Boolean
        get() = live && (status == DownloadStatus.Probing ||
            status == DownloadStatus.Downloading ||
            status == DownloadStatus.Finalizing)

    val canResume: Boolean
        get() = status == DownloadStatus.Paused ||
            status == DownloadStatus.Failed ||
            status == DownloadStatus.Cancelled ||
            (!live && status.isActive)

    val isDone: Boolean
        get() = status == DownloadStatus.Completed
}

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: DownloadRepository,
) : ViewModel() {

    private val liveSnapshots = MutableStateFlow<Map<String, DownloadSnapshot>>(emptyMap())
    private val handles = mutableMapOf<String, DownloadHandle>()
    private val tracking = mutableMapOf<String, Job>()

    val downloads: StateFlow<List<DownloadItem>> = combine(
        repository.observeDownloads(),
        liveSnapshots,
    ) { entities, live ->
        entities.map { entity -> entity.toItem(live[entity.id], live.containsKey(entity.id)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _probe = MutableStateFlow(ProbeUiState())
    val probe: StateFlow<ProbeUiState> = _probe.asStateFlow()

    private var probeJob: Job? = null

    fun probe(url: String) {
        probeJob?.cancel()
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            _probe.value = ProbeUiState()
            return
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            _probe.value = ProbeUiState(url = trimmed, error = "Enter an http or https link")
            return
        }
        probeJob = viewModelScope.launch {
            delay(350)
            _probe.value = ProbeUiState(url = trimmed, loading = true)
            runCatching { repository.probe(trimmed) }
                .onSuccess { result -> _probe.value = ProbeUiState(url = trimmed, result = result) }
                .onFailure { failure ->
                    _probe.value = ProbeUiState(
                        url = trimmed,
                        error = failure.message ?: "Could not inspect this link",
                    )
                }
        }
    }

    fun startDownload(url: String, segments: Int) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        val result = _probe.value.result?.takeIf { _probe.value.url == trimmed }
        viewModelScope.launch {
            runCatching {
                repository.startNew(
                    url = trimmed,
                    segments = segments,
                    fileName = result?.fileName,
                    mimeType = result?.mimeType,
                )
            }.onSuccess { handle ->
                _probe.value = ProbeUiState()
                track(handle)
            }.onFailure { failure ->
                _probe.value = _probe.value.copy(
                    error = failure.message ?: "Could not start the download",
                )
            }
        }
    }

    fun pause(item: DownloadItem) {
        viewModelScope.launch { handles[item.id]?.pause() }
    }

    fun resume(item: DownloadItem) {
        val existing = handles[item.id]
        viewModelScope.launch {
            if (existing != null) {
                existing.resume()
            } else {
                runCatching { repository.resume(item.id) }.getOrNull()?.let(::track)
            }
        }
    }

    fun delete(item: DownloadItem) {
        tracking.remove(item.id)?.cancel()
        handles.remove(item.id)
        liveSnapshots.update { it - item.id }
        viewModelScope.launch {
            runCatching { repository.delete(item.id) }
        }
    }

    fun clearProbe() {
        probeJob?.cancel()
        _probe.value = ProbeUiState()
    }

    private fun track(handle: DownloadHandle) {
        handles[handle.id] = handle
        tracking.remove(handle.id)?.cancel()
        tracking[handle.id] = viewModelScope.launch {
            handle.state.collect { snapshot ->
                liveSnapshots.update { it + (handle.id to snapshot) }
            }
        }
    }

    private fun DownloadEntity.toItem(
        snapshot: DownloadSnapshot?,
        hasSnapshot: Boolean,
    ): DownloadItem {
        val persistedStatus = runCatching { DownloadStatus.valueOf(status) }
            .getOrDefault(DownloadStatus.Failed)
        val rawStatus = snapshot?.status ?: persistedStatus
        val live = snapshot?.status?.isActive == true
        // A row that claims to be downloading but has no session in this process was interrupted.
        val effectiveStatus = if (!hasSnapshot && rawStatus.isActive) {
            DownloadStatus.Paused
        } else {
            rawStatus
        }
        return DownloadItem(
            id = id,
            url = url,
            fileName = snapshot?.fileName?.takeIf { it.isNotBlank() } ?: fileName,
            filePath = snapshot?.filePath ?: filePath,
            contentUri = snapshot?.contentUri ?: contentUri,
            mimeType = snapshot?.mimeType ?: mimeType,
            totalBytes = snapshot?.totalBytes ?: totalBytes,
            downloadedBytes = snapshot?.downloadedBytes ?: downloadedBytes,
            status = effectiveStatus,
            bytesPerSecond = snapshot?.bytesPerSecond ?: bytesPerSecond,
            etaSeconds = snapshot?.etaSeconds ?: etaSecondsFor(this),
            error = snapshot?.error?.message ?: errorMessage,
            finalizeStep = snapshot?.finalizeStep ?: finalizeStep?.let {
                runCatching { FinalizeStep.valueOf(it) }.getOrNull()
            },
            finalizeProgress = snapshot?.finalizeProgress ?: finalizeProgress,
            segmentCount = snapshot?.segmentCount ?: segmentCount,
            createdAt = createdAt,
            live = live,
        )
    }

    private fun etaSecondsFor(entity: DownloadEntity): Long? {
        if (entity.bytesPerSecond <= 0L || entity.totalBytes <= 0L) return null
        val remaining = entity.totalBytes - entity.downloadedBytes
        return if (remaining > 0L) remaining / entity.bytesPerSecond else null
    }
}
