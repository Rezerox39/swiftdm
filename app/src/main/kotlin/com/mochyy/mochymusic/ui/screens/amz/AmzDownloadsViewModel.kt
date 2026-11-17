package com.mochyy.mochymusic.ui.screens.amz

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi.armz.data.DownloadRepository
import com.abhi.armz.engine.DownloadHandle
import com.mochyy.mochymusic.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AmzDownloadsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DownloadRepository,
) : ViewModel() {

    private val handles = mutableMapOf<String, DownloadHandle>()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _segments = MutableStateFlow(8)
    val segments: StateFlow<Int> = _segments.asStateFlow()

    val downloads: StateFlow<List<AmzDownloadRow>> = repository.observeDownloads()
        .map { list -> list.map(AmzDownloadsUiMapper::toRow) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setUrl(value: String) {
        _url.value = value
        clearMessage()
    }

    fun setSegments(value: Int) {
        _segments.value = value.coerceIn(1, 32)
    }

    fun addDownload() {
        val candidate = _url.value.trim()
        if (!candidate.startsWith("http://", ignoreCase = true) &&
            !candidate.startsWith("https://", ignoreCase = true)
        ) {
            _message.value = context.getString(R.string.armz_downloads_invalid_url)
            return
        }
        viewModelScope.launch {
            runCatching {
                val handle = repository.startNew(candidate, _segments.value, null, null)
                handles[handle.id] = handle
                _url.value = ""
                clearMessage()
            }.onFailure(::reportFailure)
        }
    }

    fun pause(id: String) = launchHandle(id, "pause") { it.pause() }

    fun resume(id: String) {
        val existing = handles[id]
        if (existing != null) {
            launchHandle(id, "resume") { it.resume() }
            return
        }
        viewModelScope.launch {
            runCatching {
                val resumed = repository.resume(id)
                if (resumed == null) {
                    _message.value = context.getString(R.string.armz_downloads_resume_failed)
                } else {
                    handles[id] = resumed
                    clearMessage()
                }
            }.onFailure(::reportFailure)
        }
    }

    fun cancel(id: String) = launchHandle(id, "cancel") {
        it.cancel()
        handles.remove(id)
    }

    fun remove(id: String) {
        viewModelScope.launch {
            runCatching {
                handles.remove(id)?.cancel()
                repository.delete(id)
                clearMessage()
            }.onFailure(::reportFailure)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun launchHandle(id: String, action: String, block: suspend (DownloadHandle) -> Unit) {
        viewModelScope.launch {
            val handle = handles[id]
            if (handle == null) {
                _message.value = context.getString(R.string.armz_downloads_action_failed, action)
                return@launch
            }
            runCatching { block(handle) }
                .onSuccess { clearMessage() }
                .onFailure(::reportFailure)
        }
    }

    private fun reportFailure(failure: Throwable) {
        Timber.e(failure, "ARMz download action failed")
        _message.value = failure.message ?: context.getString(R.string.armz_downloads_action_failed, "action")
    }
}
