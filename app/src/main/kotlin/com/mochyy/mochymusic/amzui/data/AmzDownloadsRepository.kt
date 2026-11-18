package com.mochyy.mochymusic.amzui.data

import kotlinx.coroutines.flow.StateFlow

/** UI-only seam. The real engine is wired after the canvas screens are approved. */
interface AmzDownloadsRepository {
    val downloads: StateFlow<List<AmzDownloadItem>>
    val active: StateFlow<List<AmzDownloadItem>>
    val hero: StateFlow<AmzHeroSnapshot>
    val detected: StateFlow<List<AmzDetectedFile>>
    val stats: StateFlow<AmzLibraryStats>

    fun start()
    fun stop()
    fun add(url: String)
    fun pause(id: String)
    fun resume(id: String)
    fun cancel(id: String)
    fun togglePause()
    fun detail(id: String): StateFlow<AmzDownloadItem?>
    fun scanDownloads()
}
