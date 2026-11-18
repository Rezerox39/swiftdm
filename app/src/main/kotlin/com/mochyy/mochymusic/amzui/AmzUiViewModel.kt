package com.mochyy.mochymusic.amzui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.amzui.data.AmzDownloadsRepository
import kotlinx.coroutines.flow.StateFlow

/** Single ViewModel for the canvas UI. The repository is the only path to the download engine. */
class AmzUiViewModel(
    private val repository: AmzDownloadsRepository,
) : ViewModel() {

    val downloads: StateFlow<List<com.mochyy.mochymusic.amzui.data.AmzDownloadItem>> = repository.downloads
    val active: StateFlow<List<com.mochyy.mochymusic.amzui.data.AmzDownloadItem>> = repository.active
    val hero: StateFlow<com.mochyy.mochymusic.amzui.data.AmzHeroSnapshot> = repository.hero
    val detected: StateFlow<List<com.mochyy.mochymusic.amzui.data.AmzDetectedFile>> = repository.detected
    val stats: StateFlow<com.mochyy.mochymusic.amzui.data.AmzLibraryStats> = repository.stats

    init {
        repository.start()
    }

    fun add(url: String) = repository.add(url)

    fun pause(id: String) = repository.pause(id)

    fun resume(id: String) = repository.resume(id)

    fun cancel(id: String) = repository.cancel(id)

    fun togglePause() = repository.togglePause()

    fun detail(id: String): StateFlow<com.mochyy.mochymusic.amzui.data.AmzDownloadItem?> = repository.detail(id)

    fun scanDownloads() = repository.scanDownloads()

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }
}
