@file:OptIn(kotlin.contracts.ExperimentalContracts::class, ExperimentalUnsignedTypes::class)

package io.github.cluno1.sonorus.features.scores.data

import alphaTab.model.Note

/** Maps a clicked alphaTab note to its stable canonical MusicXML note id. */
internal object ScoreSourceMap {
    fun findNoteId(runtimeNote: Note, canonicalNotes: List<ScoreNoteRef>): String? {
        val beat = runtimeNote.beat
        val exact = canonicalNotes.firstOrNull { note ->
            note.trackIndex == beat.voice.bar.staff.track.index.toInt() &&
                note.measureIndex == beat.voice.bar.index.toInt() &&
                note.voiceIndex == beat.voice.index.toInt() &&
                note.beatIndex == beat.index.toInt() &&
                note.noteIndex == runtimeNote.index.toInt()
        }
        if (exact != null) return exact.id

        // Some importers compact empty voices. Pitch is only a fallback discriminator; the
        // structural key above remains authoritative and is stable after pitch edits.
        return canonicalNotes.singleOrNull { note ->
            note.trackIndex == beat.voice.bar.staff.track.index.toInt() &&
                note.measureIndex == beat.voice.bar.index.toInt() &&
                note.beatIndex == beat.index.toInt() &&
                note.noteIndex == runtimeNote.index.toInt() &&
                note.midiPitch == runtimeNote.realValue.toInt()
        }?.id
    }
}
