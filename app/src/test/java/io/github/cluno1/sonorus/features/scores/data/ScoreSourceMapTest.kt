@file:OptIn(kotlin.contracts.ExperimentalContracts::class, ExperimentalUnsignedTypes::class)

package io.github.cluno1.sonorus.features.scores.data

import alphaTab.PlayerMode
import alphaTab.Settings
import alphaTab.core.ecmaScript.Uint8Array
import alphaTab.importer.ScoreLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class ScoreSourceMapTest {
    @Test
    fun `every alphaTab note in bundled canonical scores maps to a stable MusicXML id`() {
        listOf(
            "gmusic_321_rev1_ocr.musicxml",
            "gmusic_321_midi_20210702.musicxml"
        ).forEach { assetName ->
            val bytes = assetFile(assetName).readBytes()
            val session = ScoreEditSession.create(bytes)
            val score = ScoreLoader.loadScoreFromBytes(
                Uint8Array(session.toByteArray().toUByteArray()),
                Settings().apply { player.playerMode = PlayerMode.Disabled }
            )
            val runtimeNotes = score.tracks.toList().flatMap { track ->
                track.staves.toList().flatMap { staff ->
                    staff.bars.toList().flatMap { bar ->
                        bar.voices.toList().flatMap { voice ->
                            voice.beats.toList().flatMap { it.notes.toList() }
                        }
                    }
                }
            }

            val mappedIds = runtimeNotes.map { note ->
                ScoreSourceMap.findNoteId(note, session.notes).also {
                    assertNotNull("$assetName runtime note ${note.id} was not mapped", it)
                }
            }
            assertEquals(session.notes.size, runtimeNotes.size)
            assertEquals(runtimeNotes.size, mappedIds.toSet().size)
        }
    }

    private fun assetFile(name: String): File {
        val candidates = listOf(
            File("app/src/main/assets", name),
            File("src/main/assets", name)
        )
        return candidates.firstOrNull(File::isFile)
            ?: error("Could not find bundled score asset $name")
    }
}
