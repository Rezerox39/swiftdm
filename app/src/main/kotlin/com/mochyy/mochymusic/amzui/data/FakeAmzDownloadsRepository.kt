package com.mochyy.mochymusic.amzui.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

/** Reproduces the demo behavior from the canvas master prompt. */
class FakeAmzDownloadsRepository : AmzDownloadsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    private val _downloads = MutableStateFlow(
        listOf(
            AmzDownloadItem(
                id = "queue-wallpapers",
                name = "Wallpapers_Pack.zip",
                kind = AmzFileKind.ARCHIVE,
                sizeLabel = "1.2 GB",
                pct = 0f,
                active = false,
            ),
            AmzDownloadItem(
                id = "queue-lecture",
                name = "Lecture_07.mp4",
                kind = AmzFileKind.VIDEO,
                sizeLabel = "148 MB",
                pct = 0f,
                active = false,
            ),
        ),
    )
    private val _hero = MutableStateFlow(AmzHeroSnapshot())
    private val _detected = MutableStateFlow(
        listOf(
            AmzDetectedFile(
                id = "detected-ubuntu-desktop",
                name = "ubuntu-24.04-desktop-amd64.iso",
                kind = AmzFileKind.DISK,
                sizeLabel = "5.9 GB",
                url = "https://releases.ubuntu.com/24.04/ubuntu-24.04-desktop-amd64.iso",
            ),
            AmzDetectedFile(
                id = "detected-ubuntu-server",
                name = "ubuntu-24.04-live-server-amd64.iso",
                kind = AmzFileKind.DISK,
                sizeLabel = "2.6 GB",
                url = "https://releases.ubuntu.com/24.04/ubuntu-24.04-live-server-amd64.iso",
            ),
            AmzDetectedFile(
                id = "detected-sha256",
                name = "SHA256SUMS",
                kind = AmzFileKind.OTHER,
                sizeLabel = "1 KB",
                url = "https://releases.ubuntu.com/24.04/SHA256SUMS",
            ),
        ),
    )

    private var addedCount = 0

    override val downloads: StateFlow<List<AmzDownloadItem>> = _downloads.asStateFlow()
    override val active: StateFlow<List<AmzDownloadItem>> = _downloads
        .map { items -> items.filter { it.active } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    override val hero: StateFlow<AmzHeroSnapshot> = _hero.asStateFlow()
    override val detected: StateFlow<List<AmzDetectedFile>> = _detected.asStateFlow()
    override val stats: StateFlow<AmzLibraryStats> =
        MutableStateFlow(AmzLibraryStats()).asStateFlow()

    override fun start() {
        if (tickJob?.isActive == true) return
        tickJob = scope.launch {
            while (isActive) {
                tick()
                delay(TickMs)
            }
        }
    }

    override fun stop() {
        tickJob?.cancel()
        tickJob = null
    }

    override fun add(url: String) {
        val spec = AddCycle[addedCount % AddCycle.size]
        val item = AmzDownloadItem(
            id = "added-${System.currentTimeMillis()}-$addedCount",
            name = spec.first,
            kind = spec.second,
            sizeLabel = spec.third,
            pct = 0f,
            active = true,
            source = url,
        )
        _downloads.value = (listOf(item) + _downloads.value).take(MaxQueue)
        addedCount += 1
        _hero.value = _hero.value.copy(
            clip = false,
            added = addedCount,
            nextName = AddCycle[addedCount % AddCycle.size].first,
        )
    }

    override fun pause(id: String) = setActive(id, false)

    override fun resume(id: String) = setActive(id, true)

    override fun cancel(id: String) {
        _downloads.value = _downloads.value.filterNot { it.id == id }
    }

    override fun togglePause() {
        _hero.value = _hero.value.copy(paused = !_hero.value.paused)
    }

    override fun detail(id: String): StateFlow<AmzDownloadItem?> = _downloads
        .map { items -> items.firstOrNull { it.id == id } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    override fun scanDownloads() {
        // Permission is requested by the UI; the demo indexes nothing until the real engine lands.
    }

    private fun setActive(id: String, active: Boolean) {
        _downloads.value = _downloads.value.map { item ->
            if (item.id == id && !item.done) item.copy(active = active) else item
        }
    }

    private fun tick() {
        val current = _hero.value
        val nextSpeed = if (current.paused) {
            current.speed
        } else {
            (current.speed + (Random.nextFloat() - 0.5f) * 4f).coerceIn(12f, 26f)
        }
        val nextPct = if (current.paused) current.pct else min(99.9f, current.pct + 0.12f)
        val nextSpeeds = (current.speeds.drop(1) + if (current.paused) 0f else nextSpeed)
            .takeLast(SpeedSamples)
        _hero.value = current.copy(
            pct = nextPct,
            speed = nextSpeed,
            speeds = nextSpeeds,
        )
        _downloads.value = _downloads.value.map { item ->
            if (!item.active) {
                item
            } else {
                val pct = min(100f, item.pct + QueueStep)
                item.copy(
                    pct = pct,
                    active = pct < 100f,
                    done = pct >= 100f,
                )
            }
        }
    }

    fun dispose() {
        stop()
        scope.cancel()
    }

    private companion object {
        const val TickMs = 300L
        const val SpeedSamples = 18
        const val QueueStep = 1.4f
        const val MaxQueue = 4
        val AddCycle = listOf(
            Triple("Lecture_08.mp4", AmzFileKind.VIDEO, "212 MB"),
            Triple("Report_Q4.pdf", AmzFileKind.DOC, "18 MB"),
            Triple("Podcast_Ep12.zip", AmzFileKind.ARCHIVE, "96 MB"),
        )
    }
}
