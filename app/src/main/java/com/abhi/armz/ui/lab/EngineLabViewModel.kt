package com.abhi.armz.ui.lab

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi.armz.data.DownloadRepository
import com.abhi.armz.engine.DownloadHandle
import com.abhi.armz.engine.DownloadRequest
import com.abhi.armz.engine.DownloadSnapshot
import com.abhi.armz.engine.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class LabFile(
    val name: String,
    val sizeBytes: Long,
)

data class EngineLabUiState(
    val url: String = DefaultBenchmarkUrl,
    val segments: Int = 8,
    val snapshot: DownloadSnapshot? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val files: List<LabFile> = emptyList(),
) {
    companion object {
        const val DefaultBenchmarkUrl: String = "https://proof.ovh.net/files/100Mb.dat"
    }
}

@HiltViewModel
class EngineLabViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DownloadRepository,
) : ViewModel() {

    private val mutableState = MutableStateFlow(EngineLabUiState())
    val state: StateFlow<EngineLabUiState> = mutableState.asStateFlow()

    private var handle: DownloadHandle? = null
    private var collector: Job? = null
    private val destination = File(context.filesDir, "engine-lab").apply { mkdirs() }

    fun onUrlChange(url: String) {
        mutableState.update { it.copy(url = url, error = null) }
    }

    fun onSegmentsChange(delta: Int) {
        mutableState.update {
            it.copy(segments = (it.segments + delta).coerceIn(1, 16))
        }
    }

    fun start() {
        val current = mutableState.value
        if (current.busy) return
        if (handle?.state?.value?.status?.isActive == true) return
        if (current.url.isBlank()) {
            mutableState.update { it.copy(error = "Enter a URL first") }
            return
        }
        mutableState.update { it.copy(busy = true, error = null, snapshot = null) }
        collector?.cancel()
        viewModelScope.launch {
            runCatching {
                val request = DownloadRequest(
                    id = UUID.randomUUID().toString(),
                    url = current.url.trim(),
                    destinationDir = destination,
                    segments = current.segments,
                )
                repository.startLab(request)
            }.onSuccess { newHandle ->
                handle = newHandle
                collector = viewModelScope.launch {
                    newHandle.state.collect { snapshot ->
                        val files = destination.listFiles()
                            ?.filter { it.isFile }
                            ?.sortedBy { it.name }
                            ?.map { LabFile(it.name, it.length()) }
                            .orEmpty()
                        mutableState.update {
                            it.copy(snapshot = snapshot, busy = false, files = files)
                        }
                        if (snapshot.status == DownloadStatus.Completed) {
                            Timber.i(
                                "EngineLab completed id=%s dir=%s files=%s",
                                snapshot.id,
                                destination.absolutePath,
                                files.joinToString { "${it.name}:${it.sizeBytes}" },
                            )
                        }
                    }
                }
            }.onFailure { failure ->
                mutableState.update {
                    it.copy(
                        busy = false,
                        error = failure.message ?: "Could not start the download",
                    )
                }
            }
        }
    }

    fun pause() {
        viewModelScope.launch { handle?.pause() }
    }

    fun resume() {
        viewModelScope.launch { handle?.resume() }
    }

    fun cancel() {
        viewModelScope.launch { handle?.cancel() }
    }
}
