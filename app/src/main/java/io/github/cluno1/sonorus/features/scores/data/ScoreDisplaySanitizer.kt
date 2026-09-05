package io.github.cluno1.sonorus.features.scores.data

internal object ScoreDisplaySanitizer {
    private val directionBlock = Regex(
        pattern = """<direction\b[^>]*>[\s\S]*?</direction>"""
    )

    /**
     * Builds an alphaTab-only display projection with one visible tempo direction.
     *
     * alphaTab currently renders tempo labels from <sound tempo="…"> even when the
     * surrounding direction has print-object="no". Removing later tempo directions from
     * the display projection is therefore the only reliable way to prevent label overlap.
     * The bundled source bytes remain untouched for a future playback pipeline.
     */
    fun keepFirstVisibleMetronome(bytes: ByteArray): ByteArray {
        val xml = bytes.decodeToString()
        var foundVisibleTempo = false
        val sanitized = directionBlock.replace(xml) { match ->
            val direction = match.value
            val hasTempo = direction.contains("<metronome") || direction.contains("<sound tempo=")
            if (!hasTempo) {
                direction
            } else if (!foundVisibleTempo) {
                foundVisibleTempo = true
                direction
            } else {
                ""
            }
        }
        return sanitized.encodeToByteArray()
    }
}
