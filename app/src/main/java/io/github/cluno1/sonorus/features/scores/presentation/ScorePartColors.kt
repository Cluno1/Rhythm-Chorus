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
    colorMode: ScorePartColorMode
) {
    tracks.forEach { track ->
        val maxVoiceCount = track.staves
            .flatMap { staff -> staff.bars.toList() }
            .maxOfOrNull { bar -> bar.voices.length.toInt() }
            ?: 1
        val voicePartIndexes = resolveScoreVoicePartIndexes(
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
    if (colorMode == ScorePartColorMode.DEFAULT) BLACK_HEX else SATB_COLOR_HEX
        .getOrElse(partIndex) { BLACK_HEX }

private fun scorePartColor(partIndex: Int, colorMode: ScorePartColorMode): Color =
    checkNotNull(Color.fromJson(scorePartColorHex(partIndex, colorMode)))

private val SATB_LABELS = listOf("S", "A", "T", "B")
private val SATB_COLOR_HEX = listOf("#4f6bff", "#ed168c", "#f59f00", "#12b886")
private const val BLACK_HEX = "#000000"
