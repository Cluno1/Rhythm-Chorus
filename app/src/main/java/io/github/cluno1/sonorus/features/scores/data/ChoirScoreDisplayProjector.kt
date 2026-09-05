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
import kotlin.math.roundToInt

/**
 * Creates a display-only choir reduction. Playback always keeps the original tracks.
 *
 * The current SATB assets contain one MusicXML part per voice. alphaTab maps each part to
 * a separate Track, so selecting several tracks cannot place their notes on one staff. This
 * projection combines S+A on a treble staff and T+B on a bass staff, using MusicXML voices.
 * Durations are softly quantized to a sixteenth-note grid so near-aligned transcriptions snap
 * to the same beat until the ingestion pipeline can provide exact voice/backup alignment.
 */
internal object ChoirScoreDisplayProjector {
    fun project(source: ByteArray, selectedTrackIndexes: Set<Int>): ByteArray {
        require(selectedTrackIndexes.isNotEmpty())

        val document = parse(source)
        val root = document.documentElement ?: return source
        val partList = root.directChild("part-list") ?: return source
        val partDefinitions = partList.directChildren("score-part")
        val parts = root.directChildren("part")
        val selected = selectedTrackIndexes
            .filter { it in parts.indices && it in partDefinitions.indices }
            .sorted()
        if (selected.isEmpty()) return source

        val groups = choirGroups(selected, parts.size)
        val divisions = findDivisions(parts, selected) ?: DEFAULT_DIVISIONS
        val snapGrid = (divisions / SNAP_DIVISOR).coerceAtLeast(1)
        val mergedDefinitions = mutableListOf<Element>()
        val mergedParts = mutableListOf<Element>()

        groups.forEachIndexed { groupIndex, group ->
            val sourceIndexes = group.sourceIndexes
            val id = "rhythm-merged-${groupIndex + 1}"
            val label = sourceIndexes.joinToString("+") { SATB_LABELS.getOrElse(it) { (it + 1).toString() } }
            mergedDefinitions += clonePartDefinition(
                source = partDefinitions[sourceIndexes.first()],
                id = id,
                label = label
            )
            mergedParts += mergeParts(
                document = document,
                sourceParts = sourceIndexes.map { parts[it] },
                id = id,
                divisions = divisions,
                snapGrid = snapGrid,
                clef = group.clef
            )
        }

        partList.removeAllChildren()
        mergedDefinitions.forEach(partList::appendChild)
        root.directChildren("part").forEach(root::removeChild)
        mergedParts.forEach(root::appendChild)
        return serialize(document)
    }

    private fun choirGroups(selected: List<Int>, partCount: Int): List<ChoirGroup> {
        if (partCount == SATB_LABELS.size) {
            return listOf(
                ChoirGroup(selected.filter { it <= ALTO_INDEX }, Clef("G", "2")),
                ChoirGroup(selected.filter { it >= TENOR_INDEX }, Clef("F", "4"))
            ).filter { it.sourceIndexes.isNotEmpty() }
        }
        return listOf(ChoirGroup(selected, clef = null))
    }

    private fun clonePartDefinition(source: Element, id: String, label: String): Element {
        val result = source.cloneNode(true) as Element
        result.setAttribute("id", id)
        result.setDirectChildText("part-name", label)
        result.setDirectChildText("part-abbreviation", label)
        result.getElementsByTagName("score-instrument").elements().forEachIndexed { index, element ->
            element.setAttribute("id", "$id-instrument-${index + 1}")
        }
        result.getElementsByTagName("midi-instrument").elements().forEachIndexed { index, element ->
            element.setAttribute("id", "$id-instrument-${index + 1}")
        }
        return result
    }

    private fun mergeParts(
        document: Document,
        sourceParts: List<Element>,
        id: String,
        divisions: Int,
        snapGrid: Int,
        clef: Clef?
    ): Element {
        val result = document.createElement("part").apply { setAttribute("id", id) }
        val measuresByVoice = sourceParts.map { it.directChildren("measure") }
        val measureCount = measuresByVoice.maxOfOrNull { it.size } ?: 0
        repeat(measureCount) { measureIndex ->
            val sourceMeasures = measuresByVoice.mapNotNull { it.getOrNull(measureIndex) }
            val anchor = sourceMeasures.firstOrNull() ?: return@repeat
            result.appendChild(
                mergeMeasure(
                    document = document,
                    anchor = anchor,
                    sourceMeasures = sourceMeasures,
                    divisions = divisions,
                    snapGrid = snapGrid,
                    clef = clef
                )
            )
        }
        return result
    }

    private fun mergeMeasure(
        document: Document,
        anchor: Element,
        sourceMeasures: List<Element>,
        divisions: Int,
        snapGrid: Int,
        clef: Clef?
    ): Element {
        val result = document.createElement("measure")
        for (index in 0 until anchor.attributes.length) {
            val attribute = anchor.attributes.item(index)
            result.setAttribute(attribute.nodeName, attribute.nodeValue)
        }

        val suffix = mutableListOf<Node>()
        anchor.directElements().forEach { child ->
            if (child.tagName !in MUSIC_EVENTS) {
                val clone = child.cloneNode(true)
                if (clone is Element) {
                    clone.forceSingleStaff()
                    if (clone.tagName == "attributes" && clef != null) clone.forceClef(clef)
                }
                if (child.tagName == "barline" && child.getAttribute("location") == "right") {
                    suffix += clone
                } else {
                    result.appendChild(clone)
                }
            }
        }

        sourceMeasures.forEachIndexed { voiceIndex, sourceMeasure ->
            var writtenDuration = 0
            sourceMeasure.directElements()
                .filter { it.tagName in MUSIC_EVENTS && it.tagName != "backup" }
                .forEach { event ->
                    val clone = event.cloneNode(true) as Element
                    val snappedDuration = clone.snapDuration(snapGrid)
                    when (clone.tagName) {
                        "note" -> {
                            clone.setOrderedChildText(
                                name = "voice",
                                value = (voiceIndex + 1).toString(),
                                before = NOTE_ELEMENTS_AFTER_VOICE
                            )
                            if (sourceMeasures.size > 1 && !clone.hasDirectChild("rest")) {
                                clone.setOrderedChildText(
                                    name = "stem",
                                    value = if (voiceIndex == 0) "up" else "down",
                                    before = NOTE_ELEMENTS_AFTER_STEM
                                )
                            }
                            clone.setOrderedChildText(
                                name = "staff",
                                value = "1",
                                before = NOTE_ELEMENTS_AFTER_STAFF
                            )
                            if (!clone.hasDirectChild("chord")) writtenDuration += snappedDuration
                        }

                        "forward" -> writtenDuration += snappedDuration
                    }
                    result.appendChild(clone)
                }

            if (voiceIndex < sourceMeasures.lastIndex && writtenDuration > 0) {
                result.appendChild(
                    document.createElement("backup").apply {
                        appendChild(document.createElement("duration").apply {
                            textContent = writtenDuration.toString()
                        })
                    }
                )
            }
        }
        suffix.forEach(result::appendChild)

        // Keep a usable divisions value even when the anchor measure did not repeat attributes.
        result.directChild("attributes")?.directChild("divisions")?.let {
            if (it.textContent.toIntOrNull() == null) it.textContent = divisions.toString()
        }
        return result
    }

    private fun Element.snapDuration(grid: Int): Int {
        val duration = directChild("duration") ?: return 0
        val original = duration.textContent.trim().toIntOrNull() ?: return 0
        if (original <= 0) return original
        val snapped = ((original.toDouble() / grid).roundToInt() * grid).coerceAtLeast(grid)
        duration.textContent = snapped.toString()
        return snapped
    }

    private fun Element.forceSingleStaff() {
        getElementsByTagName("staves").elements().forEach { it.textContent = "1" }
        getElementsByTagName("staff").elements().forEach { it.textContent = "1" }
    }

    private fun Element.forceClef(clef: Clef) {
        val clefElement = directChild("clef") ?: ownerDocument.createElement("clef").also(::appendChild)
        clefElement.setDirectChildText("sign", clef.sign)
        clefElement.setDirectChildText("line", clef.line)
    }

    private fun findDivisions(parts: List<Element>, selected: List<Int>): Int? = selected
        .asSequence()
        .map { parts[it] }
        .flatMap { it.getElementsByTagName("divisions").elements().asSequence() }
        .mapNotNull { it.textContent.trim().toIntOrNull() }
        .firstOrNull { it > 0 }

    private fun parse(source: ByteArray): Document {
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
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(source))
    }

    private fun serialize(document: Document): ByteArray {
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

    private fun Element.setDirectChildText(name: String, value: String) {
        val child = directChild(name) ?: ownerDocument.createElement(name).also(::appendChild)
        child.textContent = value
    }

    private fun Element.setOrderedChildText(name: String, value: String, before: Set<String>) {
        val existing = directChild(name)
        if (existing != null) {
            existing.textContent = value
            return
        }
        val child = ownerDocument.createElement(name).apply { textContent = value }
        val next = directElements().firstOrNull { it.tagName in before }
        if (next == null) appendChild(child) else insertBefore(child, next)
    }

    private fun Element.hasDirectChild(name: String): Boolean = directChild(name) != null

    private fun Element.directChild(name: String): Element? = directElements().firstOrNull {
        it.tagName == name
    }

    private fun Element.directChildren(name: String): List<Element> = directElements().filter {
        it.tagName == name
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

    private fun Element.removeAllChildren() {
        while (hasChildNodes()) removeChild(firstChild)
    }

    private val MUSIC_EVENTS = setOf("note", "backup", "forward")
    private val NOTE_ELEMENTS_AFTER_VOICE = setOf(
        "type", "dot", "accidental", "time-modification", "stem", "notehead", "staff",
        "beam", "notations", "lyric", "play", "listen"
    )
    private val NOTE_ELEMENTS_AFTER_STEM = setOf(
        "notehead", "staff", "beam", "notations", "lyric", "play", "listen"
    )
    private val NOTE_ELEMENTS_AFTER_STAFF = setOf(
        "beam", "notations", "lyric", "play", "listen"
    )
    private data class ChoirGroup(val sourceIndexes: List<Int>, val clef: Clef?)
    private data class Clef(val sign: String, val line: String)
    private val SATB_LABELS = listOf("S", "A", "T", "B")
    private const val ALTO_INDEX = 1
    private const val TENOR_INDEX = 2
    private const val SNAP_DIVISOR = 4 // quarter-note divisions / 4 = sixteenth note
    private const val DEFAULT_DIVISIONS = 480
    private const val ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD"
    private const val ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema"
    private const val ACCESS_EXTERNAL_STYLESHEET =
        "http://javax.xml.XMLConstants/property/accessExternalStylesheet"
}
