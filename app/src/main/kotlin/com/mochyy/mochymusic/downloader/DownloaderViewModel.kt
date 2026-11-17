package com.mochyy.mochymusic.downloader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi.armz.data.DownloadRepository
import com.mochyy.mochymusic.ui.component.TopToaster
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class DownloaderUiState(
    val input: String = "",
    val segments: Int = 32,
    val filter: DownloaderFilter = DownloaderFilter.All,
    val rows: List<DownloaderRow> = emptyList(),
    val stats: DownloaderStats = DownloaderStats(0, 0, 0, 0L, 0L),
    val desktopBrowser: Boolean = false,
)

@HiltViewModel
class DownloaderViewModel @Inject constructor(
    private val repository: DownloadRepository,
    private val controller: DownloadController,
) : ViewModel() {

    private val input = MutableStateFlow("")
    private val segments = MutableStateFlow(32)
    private val filter = MutableStateFlow(DownloaderFilter.All)
    private val desktopBrowser = MutableStateFlow(false)

    private val rows: StateFlow<List<DownloaderRow>> = repository.observeDownloads()
        .map { entities -> entities.map(DownloaderUiMapper::toRow) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<DownloaderUiState> = combine(
        input,
        segments,
        filter,
        rows,
        desktopBrowser,
    ) { inputValue, segmentValue, filterValue, rowValues, desktop ->
        val visible = DownloaderUiMapper.filter(rowValues, filterValue)
        DownloaderUiState(
            input = inputValue,
            segments = segmentValue,
            filter = filterValue,
            rows = visible,
            stats = DownloaderUiMapper.stats(rowValues),
            desktopBrowser = desktop,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloaderUiState())

    init {
        viewModelScope.launch {
            runCatching { controller.resumeInterrupted() }
                .onFailure { Timber.w(it, "Resuming interrupted AMz downloads failed") }
        }
    }

    fun onInputChanged(value: String) {
        input.value = value
    }

    fun onSegmentsChanged(value: Int) {
        segments.value = value.coerceIn(1, 32)
    }

    fun onFilterChanged(value: DownloaderFilter) {
        filter.value = value
    }

    fun setDesktopBrowser(value: Boolean) {
        desktopBrowser.value = value
    }

    fun submitLink(raw: String = input.value, source: String = "manual") {
        val url = DownloadLinkPolicy.normalize(raw)
        if (url == null) {
            TopToaster.error("Enter a valid http(s) download link")
            return
        }
        viewModelScope.launch {
            runCatching {
                controller.add(url, segments.value, source = source)
                input.value = ""
                TopToaster.show("Added to downloads")
            }.onFailure { error ->
                Timber.w(error, "Adding AMz download failed")
                TopToaster.error(error.message ?: "Could not add download")
            }
        }
    }

    fun addBrowserDownload(
        url: String,
        userAgent: String?,
        cookie: String?,
        referer: String?,
        contentDisposition: String?,
        mimeType: String?,
    ) {
        val headers = buildMap {
            if (!userAgent.isNullOrBlank()) put("User-Agent", userAgent)
            if (!cookie.isNullOrBlank()) put("Cookie", cookie)
            if (!referer.isNullOrBlank()) put("Referer", referer)
        }
        val fileName = DownloadLinkPolicy.fileNameFrom(contentDisposition)
        viewModelScope.launch {
            runCatching {
                controller.add(
                    url = url,
                    segments = segments.value,
                    headers = headers,
                    fileName = fileName,
                    mimeType = mimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" },
                    source = "browser",
                )
                TopToaster.show("Captured from browser")
            }.onFailure { error ->
                Timber.w(error, "Capturing browser download failed")
                TopToaster.error(error.message ?: "Could not capture link")
            }
        }
    }

    /** Links shared from another app land in the composer; direct files start immediately. */
    fun receiveSharedText(text: String) {
        val match = Regex("https?://[^\\s<>\"']+").find(text) ?: return
        val url = match.value.trimEnd('.', ',', ')', ']')
        if (DownloadLinkPolicy.shouldCapture(url)) {
            submitLink(url, source = "share")
        } else {
            input.value = url
            TopToaster.show("Link ready · tap Download")
        }
    }

    fun pause(id: String) = action("Paused") { controller.pause(id) }
    fun resume(id: String) = action("Resuming") { controller.resume(id) }
    fun cancel(id: String) = action("Cancelled") { controller.cancel(id) }
    fun remove(id: String) = action("Removed") { controller.remove(id) }
    fun pauseAll() = action("All active downloads paused") { controller.pauseAll() }
    fun resumeAll() = action("Resuming downloads") { controller.resumeAll() }
    fun clearFinished() = action("Finished downloads cleared") { controller.clearFinished() }

    private fun action(message: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching(block)
                .onSuccess { TopToaster.show(message) }
                .onFailure { error -> TopToaster.error(error.message ?: "Action failed") }
        }
    }

    suspend fun probe(url: String) = runCatching { repository.probe(url) }
}
