package com.abhi.armz.engine

import kotlin.math.max
import kotlin.math.min

data class CoverageReport(
    val totalBytes: Long,
    val coveredBytes: Long,
    val gaps: List<LongRange>,
    val overlaps: List<LongRange>,
) {
    val complete: Boolean
        get() = gaps.isEmpty() && overlaps.isEmpty() && coveredBytes == totalBytes

    fun describe(): String {
        if (complete) return "coverage complete ($coveredBytes/$totalBytes bytes)"
        val gapText = gaps.joinToString { "${it.first}-${it.last}" }.ifEmpty { "none" }
        val overlapText = overlaps.joinToString { "${it.first}-${it.last}" }.ifEmpty { "none" }
        return "coverage incomplete: $coveredBytes/$totalBytes bytes, " +
            "gaps=$gapText, overlaps=$overlapText"
    }
}

/**
 * Verifies that a set of segment ranges covers [0, totalBytes) exactly once. Ranges are half-open
 * so a gap at byte 100 of [0,100) + [200,300) is reported as 100..199.
 */
object CoverageVerifier {

    fun inspect(checkpoints: List<SegmentCheckpoint>, totalBytes: Long): CoverageReport {
        if (totalBytes <= 0L) {
            return CoverageReport(totalBytes = 0L, coveredBytes = 0L, gaps = emptyList(), overlaps = emptyList())
        }
        val intervals = checkpoints
            .mapNotNull { checkpoint ->
                val start = checkpoint.start.coerceIn(0L, totalBytes)
                val downloadedEnd = (checkpoint.start + checkpoint.downloaded)
                    .coerceAtMost(checkpoint.end)
                    .coerceIn(start, totalBytes)
                if (downloadedEnd > start) start until downloadedEnd else null
            }
            .sortedBy { it.first }

        val merged = ArrayList<LongRange>()
        val overlaps = ArrayList<LongRange>()
        for (interval in intervals) {
            val last = merged.lastOrNull()
            if (last == null) {
                merged += interval
                continue
            }
            if (interval.first < last.last + 1) {
                val overlapEnd = min(last.last, interval.last)
                overlaps += interval.first..overlapEnd
                if (interval.last > last.last) {
                    merged[merged.lastIndex] = last.first..interval.last
                }
            } else {
                merged += interval
            }
        }

        val gaps = ArrayList<LongRange>()
        var cursor = 0L
        for (interval in merged) {
            if (interval.first > cursor) gaps += cursor until interval.first
            cursor = max(cursor, interval.last + 1)
        }
        if (cursor < totalBytes) gaps += cursor until totalBytes

        val covered = merged.sumOf { (it.last - it.first + 1).toLong() }
        return CoverageReport(
            totalBytes = totalBytes,
            coveredBytes = covered,
            gaps = gaps,
            overlaps = overlaps,
        )
    }
}

internal fun SegmentPlanner.fromResume(
    checkpoints: List<SegmentCheckpoint>,
    totalBytes: Long,
    requestedSegments: Int,
): List<SegmentCheckpoint> {
    if (totalBytes <= 0L) return listOf(SegmentCheckpoint(0, 0, 0, 0))
    val covered = checkpoints
        .mapNotNull { checkpoint ->
            val start = checkpoint.start.coerceIn(0L, totalBytes)
            val end = (checkpoint.start + checkpoint.downloaded).coerceIn(start, totalBytes)
            if (end > start) start until end else null
        }
        .sortedBy { it.first }
        .fold(ArrayList<LongRange>()) { acc, interval ->
            val last = acc.lastOrNull()
            if (last != null && interval.first <= last.last + 1) {
                if (interval.last > last.last) acc[acc.lastIndex] = last.first..interval.last
            } else {
                acc += interval
            }
            acc
        }
    if (covered.isEmpty()) return plan(totalBytes, requestedSegments)
    val result = ArrayList<SegmentCheckpoint>()
    var index = 0
    var cursor = 0L
    for (interval in covered) {
        if (interval.first > cursor) {
            result += SegmentCheckpoint(index++, cursor, interval.first, 0L)
        }
        result += SegmentCheckpoint(
            index = index++,
            start = interval.first,
            end = interval.last + 1,
            downloaded = interval.last - interval.first + 1,
        )
        cursor = interval.last + 1
    }
    if (cursor < totalBytes) {
        result += SegmentCheckpoint(index, cursor, totalBytes, 0L)
    }
    return result
}
