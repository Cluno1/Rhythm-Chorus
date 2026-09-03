@file:OptIn(kotlin.contracts.ExperimentalContracts::class, ExperimentalUnsignedTypes::class)

package chromahub.rhythm.app.features.scores.presentation

import alphaTab.model.Beat
import alphaTab.model.Track
import kotlin.math.abs

internal data class ScorePlaybackBeatPosition(
    val partIndex: Int,
    val measureIndex: Int,
    val playbackStart: Double,
)

internal fun activeScorePlaybackPositions(beats: List<Beat>): List<ScorePlaybackBeatPosition> =
    beats.map { beat ->
        ScorePlaybackBeatPosition(
            partIndex = beat.voice.bar.staff.track.index.toInt(),
            measureIndex = beat.voice.bar.index.toInt(),
            playbackStart = beat.playbackStart,
        )
    }.distinct()

internal fun findScorePlaybackHighlightBeats(
    tracks: List<Track>,
    notationLayout: ScoreNotationLayout,
    activePositions: List<ScorePlaybackBeatPosition>,
): List<Beat> {
    if (activePositions.isEmpty()) return emptyList()

    val positionsByPartAndMeasure = activePositions.groupBy { position ->
        position.partIndex to position.measureIndex
    }
    val highlightedBeats = mutableListOf<Beat>()

    tracks.forEach { track ->
        val maxVoiceCount = track.staves
            .flatMap { staff -> staff.bars.toList() }
            .maxOfOrNull { bar -> bar.voices.length.toInt() }
            ?: 1
        val voicePartIndexes = resolveScoreVoicePartIndexes(
            trackName = track.name.ifBlank { track.shortName },
            trackIndex = track.index.toInt(),
            voiceCount = maxVoiceCount,
            notationLayout = notationLayout,
        )

        track.staves.forEach { staff ->
            staff.bars.forEach { bar ->
                bar.voices.forEach voiceLoop@{ voice ->
                    val partIndex = voicePartIndexes.getOrElse(voice.index.toInt()) {
                        track.index.toInt()
                    }
                    val activeStarts = positionsByPartAndMeasure[
                        partIndex to bar.index.toInt()
                    ].orEmpty().map { it.playbackStart }
                    if (activeStarts.isEmpty()) return@voiceLoop

                    val noteBeats = voice.beats.toList().filter { it.notes.length > 0 }
                    highlightedBeats += activeStarts.mapNotNull { playbackStart ->
                        findClosestPlaybackBeat(noteBeats, playbackStart)
                    }
                }
            }
        }
    }
    return highlightedBeats.distinctBy { it.id }
}

internal fun <T> findClosestPlaybackItem(
    items: List<T>,
    targetStart: Double,
    playbackStart: (T) -> Double,
    tolerance: Double = PLAYBACK_BEAT_SNAP_TOLERANCE,
): T? = items
    .minByOrNull { item -> abs(playbackStart(item) - targetStart) }
    ?.takeIf { item -> abs(playbackStart(item) - targetStart) <= tolerance }

private fun findClosestPlaybackBeat(beats: List<Beat>, targetStart: Double): Beat? =
    findClosestPlaybackItem(beats, targetStart, Beat::playbackStart)

internal const val PLAYBACK_BEAT_SNAP_TOLERANCE = 241.0
