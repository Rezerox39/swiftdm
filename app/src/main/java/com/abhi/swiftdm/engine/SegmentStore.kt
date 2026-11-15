package com.abhi.swiftdm.engine

import kotlin.math.min

/**
 * Thread-safe registry of byte ranges for one download. Ranges are half-open: [start, end).
 *
 * New work is claimed by workers; a worker that finishes early can split the largest remaining
 * active range so the tail of a slow connection is picked up by an idle worker. Total bytes are
 * only counted once because ranges never overlap.
 */
internal class SegmentStore(
    initial: List<SegmentCheckpoint>,
    val totalBytes: Long,
) {

    class Segment(
        val index: Int,
        val start: Long,
        var end: Long,
        var cursor: Long,
    ) {
        var state: SegmentState = if (cursor >= end) SegmentState.Done else SegmentState.Pending
    }

    private val lock = Any()
    private val segments = mutableListOf<Segment>()
    private var nextIndex = 0

    init {
        for (checkpoint in initial) {
            if (checkpoint.end <= checkpoint.start) continue
            val cursor = (checkpoint.start + checkpoint.downloaded).coerceIn(checkpoint.start, checkpoint.end)
            segments += Segment(checkpoint.index, checkpoint.start, checkpoint.end, cursor)
        }
        if (segments.isEmpty()) {
            segments += Segment(0, 0, totalBytes, 0)
        }
        nextIndex = (segments.maxOfOrNull { it.index } ?: -1) + 1
    }

    val size: Int
        get() = synchronized(lock) { segments.size }

    fun snapshot(): List<SegmentSnapshot> = synchronized(lock) {
        segments
            .sortedBy { it.index }
            .map {
                SegmentSnapshot(
                    index = it.index,
                    start = it.start,
                    end = it.end,
                    downloaded = (it.cursor - it.start).coerceAtLeast(0),
                    state = it.state,
                )
            }
    }

    fun checkpoints(): List<SegmentCheckpoint> = synchronized(lock) {
        segments
            .sortedBy { it.index }
            .map { SegmentCheckpoint(it.index, it.start, it.end, (it.cursor - it.start).coerceAtLeast(0)) }
    }

    fun downloadedTotal(): Long = synchronized(lock) {
        segments.sumOf { (it.cursor - it.start).coerceAtLeast(0) }
    }

    fun isComplete(): Boolean = synchronized(lock) {
        segments.all { it.cursor >= it.end }
    }

    fun claimNext(): Int? = synchronized(lock) {
        segments
            .firstOrNull {
                it.state != SegmentState.Done &&
                    it.state != SegmentState.Failed &&
                    it.state != SegmentState.Active &&
                    it.cursor < it.end
            }
            ?.also { it.state = SegmentState.Active }
            ?.index
    }

    fun cursorOf(index: Int): Long = synchronized(lock) {
        segment(index)?.cursor ?: 0L
    }

    fun endOf(index: Int): Long = synchronized(lock) {
        segment(index)?.end ?: 0L
    }

    fun advance(index: Int, bytes: Int) {
        if (bytes <= 0) return
        synchronized(lock) {
            val segment = segment(index) ?: return
            segment.cursor = min(segment.end, segment.cursor + bytes)
        }
    }

    fun markDone(index: Int) {
        synchronized(lock) {
            val segment = segment(index) ?: return
            segment.cursor = segment.end
            segment.state = SegmentState.Done
        }
    }

    fun markFailed(index: Int) {
        synchronized(lock) {
            val segment = segment(index) ?: return
            segment.state = SegmentState.Failed
        }
    }

    /**
     * Splits the largest still-active range in half and returns the new range, already claimed.
     * Returns null when no active range is at least `2 * minRemaining` bytes long.
     */
    fun splitLargest(minRemaining: Long): Int? = synchronized(lock) {
        val donor = segments
            .filter { it.state == SegmentState.Active && it.end - it.cursor >= minRemaining * 2L }
            .maxByOrNull { it.end - it.cursor }
            ?: return null
        val remaining = donor.end - donor.cursor
        val midpoint = donor.cursor + remaining / 2L
        if (midpoint <= donor.cursor || midpoint >= donor.end) return null
        val previousEnd = donor.end
        donor.end = midpoint
        val created = Segment(nextIndex++, midpoint, previousEnd, midpoint).also {
            it.state = SegmentState.Active
        }
        segments += created
        created.index
    }

    private fun segment(index: Int): Segment? = segments.firstOrNull { it.index == index }
}

internal object SegmentPlanner {

    private const val MinimumSegmentBytes: Long = 64L * 1024L

    fun plan(totalBytes: Long, requestedSegments: Int): List<SegmentCheckpoint> {
        if (totalBytes <= 0) return listOf(SegmentCheckpoint(0, 0, 0, 0))
        val maximumUseful = (totalBytes / MinimumSegmentBytes).coerceAtLeast(1L).toInt()
        val count = requestedSegments
            .coerceIn(DownloadRequest.MinSegments, DownloadRequest.MaxSegments)
            .coerceAtMost(maximumUseful)
        val base = totalBytes / count
        val remainder = totalBytes % count
        val result = ArrayList<SegmentCheckpoint>(count)
        var start = 0L
        for (index in 0 until count) {
            val length = base + if (index < remainder) 1L else 0L
            val end = start + length
            result += SegmentCheckpoint(index, start, end, 0L)
            start = end
        }
        return result
    }
}
