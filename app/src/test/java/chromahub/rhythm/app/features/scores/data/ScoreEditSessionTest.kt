package chromahub.rhythm.app.features.scores.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreEditSessionTest {
    @Test
    fun `injects stable ids and maps rests chords and voices without index drift`() {
        val session = ScoreEditSession.create(SAMPLE_SCORE.encodeToByteArray())

        assertEquals(4, session.notes.size)
        assertEquals(
            listOf(
                listOf(0, 0, 0, 0),
                listOf(0, 0, 0, 1),
                listOf(0, 0, 2, 0),
                listOf(1, 0, 0, 0)
            ),
            session.notes.map {
                listOf(it.voiceIndex, it.measureIndex, it.beatIndex, it.noteIndex)
            }
        )
        val ids = session.notes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("n_p1_m") })
        assertTrue(session.toByteArray().decodeToString().contains("id=\"m_p1_1\""))
    }

    @Test
    fun `pitch and lyric commands round trip through undo redo`() {
        val session = ScoreEditSession.create(SAMPLE_SCORE.encodeToByteArray())
        val noteId = session.notes.first().id
        val original = session.note(noteId)!!

        session.changePitch(noteId, 1)
        session.setLyrics(noteId, "新词")

        assertEquals(original.midiPitch + 1, session.note(noteId)?.midiPitch)
        assertEquals("新词", session.note(noteId)?.lyric)
        assertTrue(session.isDirty)
        assertTrue(session.canUndo)

        assertTrue(session.undo())
        assertEquals("原词", session.note(noteId)?.lyric)
        assertTrue(session.undo())
        assertEquals(original.midiPitch, session.note(noteId)?.midiPitch)
        assertFalse(session.canUndo)

        assertTrue(session.redo())
        assertTrue(session.redo())
        assertEquals("新词", session.note(noteId)?.lyric)
        assertNotEquals(session.baseBytes.decodeToString(), session.toByteArray().decodeToString())
    }

    @Test
    fun `stable ids survive a new edit session`() {
        val first = ScoreEditSession.create(SAMPLE_SCORE.encodeToByteArray())
        val ids = first.notes.map { it.id }

        val second = ScoreEditSession.create(first.toByteArray())

        assertEquals(ids, second.notes.map { it.id })
    }

    private companion object {
        val SAMPLE_SCORE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <score-partwise version="4.0">
              <part-list><score-part id="P1"><part-name>S</part-name></score-part></part-list>
              <part id="P1">
                <measure number="1">
                  <attributes><divisions>4</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>
                  <note><pitch><step>C</step><octave>4</octave></pitch><duration>4</duration><voice>1</voice><type>quarter</type><lyric><text>原词</text></lyric></note>
                  <note><chord/><pitch><step>E</step><octave>4</octave></pitch><duration>4</duration><voice>1</voice><type>quarter</type></note>
                  <note><rest/><duration>4</duration><voice>1</voice><type>quarter</type></note>
                  <note><pitch><step>G</step><octave>4</octave></pitch><duration>4</duration><voice>1</voice><type>quarter</type></note>
                  <backup><duration>12</duration></backup>
                  <note><pitch><step>C</step><octave>3</octave></pitch><duration>12</duration><voice>2</voice><type>half</type><dot/></note>
                </measure>
              </part>
            </score-partwise>
        """.trimIndent()
    }
}
