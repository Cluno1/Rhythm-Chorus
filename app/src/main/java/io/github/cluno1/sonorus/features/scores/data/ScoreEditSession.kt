package io.github.cluno1.sonorus.features.scores.data

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** A stable reference from an alphaTab runtime note back to canonical MusicXML. */
internal data class ScoreNoteRef(
    val id: String,
    val trackIndex: Int,
    val measureIndex: Int,
    val voiceIndex: Int,
    val beatIndex: Int,
    val noteIndex: Int,
    val midiPitch: Int,
    val lyric: String?
)

internal data class ScorePitch(
    val step: String,
    val alter: Int,
    val octave: Int
) {
    val midiPitch: Int
        get() = (octave + 1) * 12 + STEP_TO_PITCH_CLASS.getValue(step) + alter

    companion object {
        private val STEP_TO_PITCH_CLASS = mapOf(
            "C" to 0,
            "D" to 2,
            "E" to 4,
            "F" to 5,
            "G" to 7,
            "A" to 9,
            "B" to 11
        )

        fun fromMidi(midiPitch: Int): ScorePitch {
            require(midiPitch in 0..127) { "Pitch is outside the MIDI range" }
            val pitchClass = Math.floorMod(midiPitch, 12)
            return ScorePitch(
                step = SHARP_STEPS[pitchClass],
                alter = SHARP_ALTERS[pitchClass],
                octave = Math.floorDiv(midiPitch, 12) - 1
            )
        }

        private val SHARP_STEPS = arrayOf("C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B")
        private val SHARP_ALTERS = intArrayOf(0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0)
    }
}

/**
 * Owns an editable canonical MusicXML document and an invertible command history.
 * alphaTab scores are disposable projections and are never mutated by this class.
 */
internal class ScoreEditSession private constructor(
    private val document: CanonicalScoreDocument,
    val baseBytes: ByteArray
) {
    private val undoStack = ArrayDeque<ScoreEditCommand>()
    private val redoStack = ArrayDeque<ScoreEditCommand>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val isDirty: Boolean get() = undoStack.isNotEmpty()
    val notes: List<ScoreNoteRef> get() = document.notes()

    fun note(noteId: String): ScoreNoteRef? = notes.firstOrNull { it.id == noteId }

    fun changePitch(noteId: String, semitones: Int) {
        if (semitones == 0) return
        val before = document.pitch(noteId)
        val after = ScorePitch.fromMidi(before.midiPitch + semitones)
        execute(ChangePitch(noteId, before, after))
    }

    fun setLyrics(noteId: String, lyric: String) {
        val before = document.lyric(noteId)
        val after = lyric.trim().takeIf(String::isNotEmpty)
        if (before == after) return
        execute(SetLyrics(noteId, before, after))
    }

    fun undo(): Boolean {
        val command = undoStack.removeLastOrNull() ?: return false
        command.revert(document)
        redoStack.addLast(command)
        document.validate()
        return true
    }

    fun redo(): Boolean {
        val command = redoStack.removeLastOrNull() ?: return false
        command.apply(document)
        undoStack.addLast(command)
        document.validate()
        return true
    }

    fun toByteArray(): ByteArray = document.serialize()

    private fun execute(command: ScoreEditCommand) {
        command.apply(document)
        runCatching { document.validate() }
            .onFailure { command.revert(document) }
            .getOrThrow()
        undoStack.addLast(command)
        redoStack.clear()
    }

    companion object {
        fun create(source: ByteArray): ScoreEditSession {
            val document = CanonicalScoreDocument.parse(source)
            document.ensureStableIds()
            document.validate()
            return ScoreEditSession(document, document.serialize())
        }
    }
}

private sealed interface ScoreEditCommand {
    fun apply(document: CanonicalScoreDocument)
    fun revert(document: CanonicalScoreDocument)
}

private data class ChangePitch(
    val noteId: String,
    val before: ScorePitch,
    val after: ScorePitch
) : ScoreEditCommand {
    override fun apply(document: CanonicalScoreDocument) = document.setPitch(noteId, after)
    override fun revert(document: CanonicalScoreDocument) = document.setPitch(noteId, before)
}

private data class SetLyrics(
    val noteId: String,
    val before: String?,
    val after: String?
) : ScoreEditCommand {
    override fun apply(document: CanonicalScoreDocument) = document.setLyric(noteId, after)
    override fun revert(document: CanonicalScoreDocument) = document.setLyric(noteId, before)
}

private class CanonicalScoreDocument private constructor(private val document: Document) {
    fun ensureStableIds() {
        val usedIds = mutableSetOf<String>()
        document.getElementsByTagName("*").elements()
            .filter { it.tagName != "measure" && it.tagName != "note" }
            .mapNotNull { it.getAttribute("id").takeIf(String::isNotBlank) }
            .filterTo(usedIds) { it.matches(XML_ID_PATTERN) }

        parts().forEachIndexed { partIndex, part ->
            part.directChildren("measure").forEachIndexed { measureIndex, measure ->
                measure.ensureUniqueId("m_p${partIndex + 1}_${measureIndex + 1}", usedIds)
                measure.directChildren("note").forEachIndexed { eventIndex, note ->
                    note.ensureUniqueId(
                        "n_p${partIndex + 1}_m${measureIndex + 1}_e${eventIndex + 1}",
                        usedIds
                    )
                }
            }
        }
    }

    fun notes(): List<ScoreNoteRef> = buildList {
        parts().forEachIndexed { partIndex, part ->
            part.directChildren("measure").forEachIndexed { measureIndex, measure ->
                val voiceIndexes = linkedMapOf<String, Int>()
                val nextBeatByVoice = mutableMapOf<Int, Int>()
                val currentBeatByVoice = mutableMapOf<Int, Int>()
                val nextNoteByVoiceBeat = mutableMapOf<Pair<Int, Int>, Int>()

                measure.directChildren("note").forEach { note ->
                    val voiceName = note.directChildText("voice") ?: "1"
                    val voiceIndex = voiceIndexes.getOrPut(voiceName) { voiceIndexes.size }
                    val isChord = note.directChild("chord") != null
                    val beatIndex = if (isChord) {
                        currentBeatByVoice[voiceIndex] ?: 0
                    } else {
                        nextBeatByVoice.getOrDefault(voiceIndex, 0).also { nextBeat ->
                            currentBeatByVoice[voiceIndex] = nextBeat
                            nextBeatByVoice[voiceIndex] = nextBeat + 1
                        }
                    }
                    val pitch = note.readPitch() ?: return@forEach
                    val noteKey = voiceIndex to beatIndex
                    val noteIndex = nextNoteByVoiceBeat.getOrDefault(noteKey, 0)
                    nextNoteByVoiceBeat[noteKey] = noteIndex + 1
                    add(
                        ScoreNoteRef(
                            id = note.getAttribute("id"),
                            trackIndex = partIndex,
                            measureIndex = measureIndex,
                            voiceIndex = voiceIndex,
                            beatIndex = beatIndex,
                            noteIndex = noteIndex,
                            midiPitch = pitch.midiPitch,
                            lyric = note.readLyric()
                        )
                    )
                }
            }
        }
    }

    fun pitch(noteId: String): ScorePitch = requireNote(noteId).readPitch()
        ?: error("MusicXML note $noteId has no pitch")

    fun lyric(noteId: String): String? = requireNote(noteId).readLyric()

    fun setPitch(noteId: String, pitch: ScorePitch) {
        require(pitch.midiPitch in 0..127)
        val pitchElement = requireNotNull(requireNote(noteId).directChild("pitch"))
        pitchElement.setDirectChildText("step", pitch.step)
        val octave = requireNotNull(pitchElement.directChild("octave"))
        val alter = pitchElement.directChild("alter")
        if (pitch.alter == 0) {
            alter?.let(pitchElement::removeChild)
        } else if (alter != null) {
            alter.textContent = pitch.alter.toString()
        } else {
            pitchElement.insertBefore(
                document.createElement("alter").apply { textContent = pitch.alter.toString() },
                octave
            )
        }
        octave.textContent = pitch.octave.toString()
    }

    fun setLyric(noteId: String, lyric: String?) {
        val note = requireNote(noteId)
        val lyricElements = note.directChildren("lyric")
        val target = lyricElements.firstOrNull { it.getAttribute("number") == "1" }
            ?: lyricElements.firstOrNull()
        if (lyric == null) {
            target?.let(note::removeChild)
            return
        }
        val lyricElement = target ?: document.createElement("lyric").apply {
            setAttribute("number", "1")
            val next = note.directElements().firstOrNull { it.tagName in NOTE_ELEMENTS_AFTER_LYRIC }
            if (next == null) note.appendChild(this) else note.insertBefore(this, next)
        }
        val text = lyricElement.directChild("text") ?: document.createElement("text").also {
            lyricElement.appendChild(it)
        }
        text.textContent = lyric
    }

    fun validate() {
        require(document.documentElement?.tagName == "score-partwise") {
            "Only score-partwise MusicXML is editable"
        }
        val ids = mutableSetOf<String>()
        document.getElementsByTagName("*").elements()
            .filter { it.tagName == "measure" || it.tagName == "note" }
            .forEach { element ->
            val id = element.getAttribute("id").takeIf(String::isNotBlank) ?: return@forEach
            require(id.matches(XML_ID_PATTERN)) { "Invalid MusicXML id: $id" }
            require(ids.add(id)) { "Duplicate MusicXML id: $id" }
        }
        notes().forEach { note -> require(note.midiPitch in 0..127) }
    }

    fun serialize(): ByteArray {
        val output = ByteArrayOutputStream()
        val factory = TransformerFactory.newInstance().apply {
            runCatching { setAttribute(ACCESS_EXTERNAL_DTD, "") }
            runCatching { setAttribute(ACCESS_EXTERNAL_STYLESHEET, "") }
        }
        factory.newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
            setOutputProperty(OutputKeys.INDENT, "no")
        }.transform(DOMSource(document), StreamResult(output))
        return output.toByteArray()
    }

    private fun requireNote(noteId: String): Element = document.getElementsByTagName("note")
        .elements()
        .firstOrNull { it.getAttribute("id") == noteId }
        ?: error("Unknown MusicXML note id: $noteId")

    private fun parts(): List<Element> = document.documentElement.directChildren("part")

    private fun Element.ensureUniqueId(candidate: String, usedIds: MutableSet<String>) {
        val current = getAttribute("id")
        if (current.matches(XML_ID_PATTERN) && usedIds.add(current)) return
        var next = candidate
        var suffix = 2
        while (!usedIds.add(next)) next = "${candidate}_${suffix++}"
        setAttribute("id", next)
    }

    private fun Element.readPitch(): ScorePitch? {
        val pitch = directChild("pitch") ?: return null
        val step = pitch.directChildText("step")?.uppercase() ?: return null
        if (step !in setOf("A", "B", "C", "D", "E", "F", "G")) return null
        return ScorePitch(
            step = step,
            alter = pitch.directChildText("alter")?.toIntOrNull() ?: 0,
            octave = pitch.directChildText("octave")?.toIntOrNull() ?: return null
        )
    }

    private fun Element.readLyric(): String? = directChildren("lyric")
        .firstOrNull { it.getAttribute("number") == "1" }
        ?.directChildText("text")
        ?: directChildren("lyric").firstOrNull()?.directChildText("text")

    companion object {
        fun parse(source: ByteArray): CanonicalScoreDocument {
            require(source.isNotEmpty())
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                runCatching { isXIncludeAware = false }
                runCatching { isExpandEntityReferences = false }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
                runCatching { setAttribute(ACCESS_EXTERNAL_DTD, "") }
                runCatching { setAttribute(ACCESS_EXTERNAL_SCHEMA, "") }
            }
            return CanonicalScoreDocument(
                factory.newDocumentBuilder().parse(ByteArrayInputStream(source))
            )
        }

        private val XML_ID_PATTERN = Regex("[A-Za-z_][A-Za-z0-9._-]*")
        private val NOTE_ELEMENTS_AFTER_LYRIC = setOf("play", "listen")
        private const val ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD"
        private const val ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema"
        private const val ACCESS_EXTERNAL_STYLESHEET =
            "http://javax.xml.XMLConstants/property/accessExternalStylesheet"
    }
}

private fun Element.directChild(name: String): Element? = directElements().firstOrNull {
    it.tagName == name
}

private fun Element.directChildren(name: String): List<Element> = directElements().filter {
    it.tagName == name
}

private fun Element.directChildText(name: String): String? = directChild(name)
    ?.textContent
    ?.trim()
    ?.takeIf(String::isNotEmpty)

private fun Element.setDirectChildText(name: String, value: String) {
    val child = directChild(name) ?: ownerDocument.createElement(name).also(::appendChild)
    child.textContent = value
}

private fun Element.directElements(): List<Element> = buildList {
    for (index in 0 until childNodes.length) {
        val node = childNodes.item(index)
        if (node.nodeType == Node.ELEMENT_NODE) add(node as Element)
    }
}

private fun org.w3c.dom.NodeList.elements(): List<Element> = buildList {
    for (index in 0 until length) {
        val node = item(index)
        if (node.nodeType == Node.ELEMENT_NODE) add(node as Element)
    }
}
