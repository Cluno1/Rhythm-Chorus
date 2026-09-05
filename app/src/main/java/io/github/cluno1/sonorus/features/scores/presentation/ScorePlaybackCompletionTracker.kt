package io.github.cluno1.sonorus.features.scores.presentation

/** Tracks natural completion separately from an ordinary pause. */
internal class ScorePlaybackCompletionTracker {
    private var finished = false

    fun markFinished() {
        finished = true
    }

    fun reset() {
        finished = false
    }

    fun consumeFinished(): Boolean {
        val wasFinished = finished
        finished = false
        return wasFinished
    }
}
