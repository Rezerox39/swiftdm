package com.abhi.swiftdm.engine

/** Rolling-window throughput meter fed with cumulative byte counters. */
class SpeedMeter(
    private val windowMs: Long = 3_000L,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private data class Sample(val at: Long, val total: Long)

    private val samples = ArrayDeque<Sample>()

    @Synchronized
    fun onProgress(totalBytes: Long) {
        val now = clock()
        samples.addLast(Sample(now, totalBytes))
        while (samples.size > 2 && now - samples.first().at > windowMs) {
            samples.removeFirst()
        }
        while (samples.size > 512) {
            samples.removeFirst()
        }
    }

    @Synchronized
    fun bytesPerSecond(): Long {
        if (samples.size < 2) return 0L
        val now = clock()
        while (samples.size > 2 && now - samples.first().at > windowMs) {
            samples.removeFirst()
        }
        val oldest = samples.first()
        val newest = samples.last()
        val elapsed = (newest.at - oldest.at).coerceAtLeast(1L)
        val delta = newest.total - oldest.total
        if (delta <= 0L) return 0L
        return delta * 1_000L / elapsed
    }

    @Synchronized
    fun reset() {
        samples.clear()
    }
}
