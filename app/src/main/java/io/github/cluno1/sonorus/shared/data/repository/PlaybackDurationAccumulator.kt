package io.github.cluno1.sonorus.shared.data.repository

/** Accumulates only running intervals from a monotonic clock. */
internal class PlaybackDurationAccumulator {
    private var accumulatedMs: Long = 0L
    private var runningSinceMs: Long? = null

    val isRunning: Boolean
        get() = runningSinceMs != null

    fun resume(nowMs: Long) {
        if (runningSinceMs == null) runningSinceMs = nowMs
    }

    fun pause(nowMs: Long) {
        val startedAt = runningSinceMs ?: return
        accumulatedMs += (nowMs - startedAt).coerceAtLeast(0L)
        runningSinceMs = null
    }

    fun elapsedMs(nowMs: Long): Long {
        val currentInterval = runningSinceMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L
        return accumulatedMs + currentInterval
    }

    fun consume(nowMs: Long): Long {
        pause(nowMs)
        return accumulatedMs.also {
            accumulatedMs = 0L
            runningSinceMs = null
        }
    }
}
