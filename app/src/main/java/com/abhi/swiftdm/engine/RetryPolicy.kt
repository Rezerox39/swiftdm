package com.abhi.swiftdm.engine

/**
 * Exponential backoff: 1s, 2s, 4s, 8s for the default five attempts. The delay is capped so a
 * very persistent server cannot park a download indefinitely.
 */
data class RetryPolicy(
    val maxAttempts: Int = 5,
    val initialDelayMs: Long = 1_000L,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
        require(initialDelayMs >= 0L) { "initialDelayMs must be >= 0" }
    }

    /** Delay before the next attempt after [attempt] failed attempts (1-based). */
    fun delayMillisAfter(attempt: Int): Long {
        val shift = (attempt - 1).coerceIn(0, 3)
        return (initialDelayMs shl shift).coerceAtMost(8_000L)
    }
}
