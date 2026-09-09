@file:OptIn(kotlin.contracts.ExperimentalContracts::class, ExperimentalUnsignedTypes::class)

package io.github.cluno1.sonorus.features.scores.presentation

import alphaTab.model.BeatStyle
import alphaTab.model.BeatSubElement
import alphaTab.model.Color
import alphaTab.model.NoteStyle
import alphaTab.model.NoteSubElement
import alphaTab.model.Track
import alphaTab.model.VoiceStyle
import alphaTab.model.VoiceSubElement

/** Applies display-only SATB colors without changing the playback score. */
internal fun applyScorePartColors(
    tracks: List<Track>,
    notationLayout: ScoreNotationLayout,
    colorMode: ScorePartColorMode,
    voiceColorIndexesByTrack: Map<Int, List<Int>> = emptyMap(),
) {
    tracks.forEach { track ->
        val maxVoiceCount = track.staves
            .flatMap { staff -> staff.bars.toList() }
            .maxOfOrNull { bar -> bar.voices.length.toInt() }
            ?: 1
        val voicePartIndexes = voiceColorIndexesByTrack[track.index.toInt()]
            ?.forVoiceCount(maxVoiceCount, track.index.toInt())
            ?: resolveScoreVoicePartIndexes(
                trackName = track.name.ifBlank { track.shortName },
                trackIndex = track.index.toInt(),
                voiceCount = maxVoiceCount,
                notationLayout = notationLayout
            )

        track.staves.forEach { staff ->
            staff.bars.forEach { bar ->
                bar.voices.forEach { voice ->
                    val partIndex = voicePartIndexes.getOrElse(voice.index.toInt()) {
                        track.index.toInt()
                    }
                    val color = scorePartColor(partIndex, colorMode)
                    voice.style = (voice.style ?: VoiceStyle()).apply {
                        colors.set(VoiceSubElement.Glyphs, color)
                    }
                    voice.beats.forEach { beat ->
                        beat.style = (beat.style ?: BeatStyle()).apply {
                            BeatSubElement.values().forEach { element -> colors.set(element, color) }
                        }
                        beat.notes.forEach { note ->
                            note.style = (note.style ?: NoteStyle()).apply {
                                NoteSubElement.values().forEach { element -> colors.set(element, color) }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun resolveScoreVoicePartIndexes(
    trackName: String,
    trackIndex: Int,
    voiceCount: Int,
    notationLayout: ScoreNotationLayout
): List<Int> {
    if (notationLayout == ScoreNotationLayout.MERGED_STAVES) {
        val indexesFromName = trackName
            .split('+')
            .mapNotNull { label -> SATB_LABELS.indexOf(label.trim()).takeIf { it >= 0 } }
        if (indexesFromName.size == voiceCount) return indexesFromName
    }
    return List(voiceCount) { trackIndex }
}

internal fun scorePartColorHex(partIndex: Int, colorMode: ScorePartColorMode): String =
    if (colorMode == ScorePartColorMode.DEFAULT) BLACK_HEX else PART_COLOR_HEX
        .getOrElse(partIndex) { BLACK_HEX }

internal fun List<Int>.forVoiceCount(voiceCount: Int, fallbackIndex: Int): List<Int> = when {
    size == voiceCount -> this
    size == 1 -> List(voiceCount) { first() }
    else -> List(voiceCount) { fallbackIndex }
}

private fun scorePartColor(partIndex: Int, colorMode: ScorePartColorMode): Color =
    checkNotNull(Color.fromJson(scorePartColorHex(partIndex, colorMode)))

private val SATB_LABELS = listOf("S", "A", "T", "B")
private val PART_COLOR_HEX = listOf(
    "#4f6bff", // S
    "#ed168c", // A
    "#f59f00", // T
    "#12b886", // B
    "#7e57c2", // Lead / first extra voice
    "#0097a7", // Additional voice
)
private const val BLACK_HEX = "#000000"
