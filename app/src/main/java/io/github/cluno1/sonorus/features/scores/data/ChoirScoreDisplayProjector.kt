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
        return projectWithMapping(source, selectedTrackIndexes).musicXml
    }

    fun projectWithMapping(
        source: ByteArray,
        selectedTrackIndexes: Set<Int>,
    ): ChoirDisplayProjection {
        require(selectedTrackIndexes.isNotEmpty())

        val document = parse(source)
        val root = document.documentElement ?: return ChoirDisplayProjection(source, emptyList())
        val partList = root.directChild("part-list") ?: return ChoirDisplayProjection(source, emptyList())
        val partDefinitions = partList.directChildren("score-part")
        val parts = root.directChildren("part")
        val selected = selectedTrackIndexes
            .filter { it in parts.indices && it in partDefinitions.indices }
            .sorted()
        if (selected.isEmpty()) return ChoirDisplayProjection(source, emptyList())

        val sourceParts = describeParts(partDefinitions, parts)
        val groups = choirGroups(selected, sourceParts)
        val divisions = findDivisions(parts, selected) ?: DEFAULT_DIVISIONS
        val snapGrid = (divisions / SNAP_DIVISOR).coerceAtLeast(1)
        val mergedDefinitions = mutableListOf<Element>()
        val mergedParts = mutableListOf<Element>()

        groups.forEachIndexed { groupIndex, group ->
            val sourceIndexes = group.sourceIndexes
            val isSingleton = sourceIndexes.size == 1
            val id = if (isSingleton) {
                parts[sourceIndexes.single()].getAttribute("id")
            } else {
                "rhythm-merged-${groupIndex + 1}"
            }
            mergedDefinitions += clonePartDefinition(
                source = partDefinitions[sourceIndexes.first()],
                id = id,
                label = group.label,
                rekeyInstruments = !isSingleton,
            )
            mergedParts += if (isSingleton) {
                (parts[sourceIndexes.single()].cloneNode(true) as Element).apply {
                    setAttribute("id", id)
                }
            } else {
                mergeParts(
                    document = document,
                    sourceParts = sourceIndexes.map { parts[it] },
                    id = id,
                    divisions = divisions,
                    snapGrid = snapGrid,
                    clef = group.clef,
                )
            }
        }

        partList.removeAllChildren()
        mergedDefinitions.forEach(partList::appendChild)
        root.directChildren("part").forEach(root::removeChild)
        mergedParts.forEach(root::appendChild)
        return ChoirDisplayProjection(
            musicXml = serialize(document),
            groups = groups.map { group ->
                ChoirDisplayGroup(
                    sourcePartIndexes = group.sourceIndexes,
                    colorPartIndexes = group.sourceIndexes.map { index ->
                        sourceParts[index].role?.colorIndex ?: fallbackColorIndex(index)
                    },
                    label = group.label,
                )
            },
        )
    }

    fun trackLabels(source: ByteArray): List<String> {
        val document = parse(source)
        val root = document.documentElement ?: return emptyList()
        val definitions = root.directChild("part-list")?.directChildren("score-part") ?: return emptyList()
        val parts = root.directChildren("part")
        return describeParts(definitions, parts).map(SourcePart::displayLabel)
    }

    fun trackColorIndexes(source: ByteArray): List<Int> {
        val document = parse(source)
        val root = document.documentElement ?: return emptyList()
        val definitions = root.directChild("part-list")?.directChildren("score-part") ?: return emptyList()
        val parts = root.directChildren("part")
        return describeParts(definitions, parts).map { part ->
            part.role?.colorIndex ?: fallbackColorIndex(part.index)
        }
    }

    private fun choirGroups(selected: List<Int>, parts: List<SourcePart>): List<ChoirGroup> {
        val selectedSet = selected.toSet()
        val grouped = mutableSetOf<Int>()
        val groups = mutableListOf<ChoirGroup>()

        fun addRolePair(first: PartRole, second: PartRole, clef: Clef) {
            val indexes = listOfNotNull(
                parts.singleOrNull { it.role == first }?.index,
                parts.singleOrNull { it.role == second }?.index,
            ).filter { it in selectedSet }
            if (indexes.isEmpty()) return
            grouped += indexes
            groups += ChoirGroup(
                sourceIndexes = indexes.sorted(),
                clef = clef,
                label = indexes.joinToString("+") { index ->
                    checkNotNull(parts[index].role).label
                },
            )
        }

        addRolePair(PartRole.SOPRANO, PartRole.ALTO, Clef("G", "2"))
        addRolePair(PartRole.TENOR, PartRole.BASS, Clef("F", "4"))

        selected.filterNot { it in grouped }.forEach { index ->
            groups += ChoirGroup(
                sourceIndexes = listOf(index),
                clef = parts[index].clef,
                label = parts[index].displayLabel,
            )
        }

        // Keep extras in their source order while still placing each SATB pair at the first
        // source index it owns. This yields Lead / S+A / T+B for five-part choir scores.
        return groups.sortedBy { it.sourceIndexes.minOrNull() ?: Int.MAX_VALUE }
    }

    private fun describeParts(
        definitions: List<Element>,
        parts: List<Element>,
    ): List<SourcePart> {
        val available = minOf(definitions.size, parts.size)
        val names = (0 until available).map { definitions[it].directChild("part-name")?.textContent.orEmpty() }
        val clefs = (0 until available).map { index -> parts[index].primaryClef() }
        val explicitRoles = names.map(::explicitPartRole)
        val roles = explicitRoles.toMutableList()

        val hasExplicitSatb = SATB_ROLES.all { role -> explicitRoles.count { it == role } == 1 }
        if (!hasExplicitSatb && available == SATB_ROLES.size) {
            SATB_ROLES.forEachIndexed { index, role -> roles[index] = role }
        } else if (
            !hasExplicitSatb &&
            available > SATB_ROLES.size &&
            explicitRoles.none { it in SATB_ROLES }
        ) {
            findGenericSatbBlock(clefs)?.let { blockStart ->
                SATB_ROLES.forEachIndexed { offset, role -> roles[blockStart + offset] = role }
                if (blockStart > 0) {
                    val possibleLead = blockStart - 1
                    if (roles[possibleLead] == null && clefs[possibleLead]?.sign == "G") {
                        roles[possibleLead] = PartRole.LEAD
                    }
                }
            }
        }

        return (0 until available).map { index ->
            val rawName = names[index].replace('\u00A0', ' ').trim()
            val role = roles[index]
            val displayLabel = when {
                rawName.isNotEmpty() && !rawName.isGenericPartName() -> rawName
                role != null -> role.label
                else -> (index + 1).toString()
            }
            SourcePart(index, displayLabel, clefs[index], role)
        }
    }

    private fun findGenericSatbBlock(clefs: List<Clef?>): Int? {
        val signs = clefs.map { it?.sign }
        val preferred = listOf("G", "G", "F", "F")
        val tenorTreble = listOf("G", "G", "G", "F")
        return signs.windowed(SATB_ROLES.size).indexOfFirst { it == preferred }
            .takeIf { it >= 0 }
            ?: signs.windowed(SATB_ROLES.size).indexOfFirst { it == tenorTreble }
                .takeIf { it >= 0 }
    }

    private fun explicitPartRole(name: String): PartRole? {
        val normalized = name.replace('\u00A0', ' ').trim().lowercase()
        val compact = normalized.replace(ROLE_SEPARATOR, "")
        return when (compact) {
            "s", "sop", "soprano", "女高音" -> PartRole.SOPRANO
            "a", "alto", "女低音" -> PartRole.ALTO
            "t", "tenor", "男高音" -> PartRole.TENOR
            "b", "bass", "男低音" -> PartRole.BASS
            "melody", "lead", "solo", "主旋律", "领唱", "領唱" -> PartRole.LEAD
            else -> null
        }
    }

    private fun String.isGenericPartName(): Boolean {
        val normalized = replace('\u00A0', ' ').trim()
        return normalized.isEmpty() ||
            normalized.contains("SmartMusic SoftSynth", ignoreCase = true) ||
            normalized.equals("Piano", ignoreCase = true) ||
            normalized.equals("Pno", ignoreCase = true) ||
            GENERIC_STAFF_NAME.matches(normalized) ||
            GENERIC_INSTRUMENT_NAME.matches(normalized)
    }

    private fun Element.primaryClef(): Clef? {
        val clef = getElementsByTagName("clef").elements().firstOrNull() ?: return null
        val sign = clef.directChild("sign")?.textContent?.trim().orEmpty()
        val line = clef.directChild("line")?.textContent?.trim().orEmpty()
        return sign.takeIf(String::isNotBlank)?.let { Clef(it, line) }
    }

    private fun clonePartDefinition(
        source: Element,
        id: String,
        label: String,
        rekeyInstruments: Boolean,
    ): Element {
        val result = source.cloneNode(true) as Element
        result.setAttribute("id", id)
        result.setDirectChildText("part-name", label)
        result.setDirectChildText("part-abbreviation", label)
        if (rekeyInstruments) {
            result.getElementsByTagName("score-instrument").elements().forEachIndexed { index, element ->
                element.setAttribute("id", "$id-instrument-${index + 1}")
            }
            result.getElementsByTagName("midi-instrument").elements().forEachIndexed { index, element ->
                element.setAttribute("id", "$id-instrument-${index + 1}")
            }
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
    private data class SourcePart(
        val index: Int,
        val displayLabel: String,
        val clef: Clef?,
        val role: PartRole?,
    )
    private data class ChoirGroup(
        val sourceIndexes: List<Int>,
        val clef: Clef?,
        val label: String,
    )
    private data class Clef(val sign: String, val line: String)
    private enum class PartRole(val label: String, val colorIndex: Int) {
        SOPRANO("S", 0),
        ALTO("A", 1),
        TENOR("T", 2),
        BASS("B", 3),
        LEAD("Lead", 4),
    }
    private val SATB_ROLES = listOf(
        PartRole.SOPRANO,
        PartRole.ALTO,
        PartRole.TENOR,
        PartRole.BASS,
    )
    private val ROLE_SEPARATOR = Regex("[\\s._-]+")
    private val GENERIC_STAFF_NAME = Regex("""\[?Staff\s+\d+]?""", RegexOption.IGNORE_CASE)
    private val GENERIC_INSTRUMENT_NAME = Regex("Instrument\\s*\\d+", RegexOption.IGNORE_CASE)
    private const val SNAP_DIVISOR = 4 // quarter-note divisions / 4 = sixteenth note
    private const val DEFAULT_DIVISIONS = 480
    private const val ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD"
    private const val ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema"
    private const val ACCESS_EXTERNAL_STYLESHEET =
        "http://javax.xml.XMLConstants/property/accessExternalStylesheet"
}

internal data class ChoirDisplayProjection(
    val musicXml: ByteArray,
    val groups: List<ChoirDisplayGroup>,
)

internal data class ChoirDisplayGroup(
    val sourcePartIndexes: List<Int>,
    val colorPartIndexes: List<Int>,
    val label: String,
)

private fun fallbackColorIndex(sourceIndex: Int): Int = sourceIndex
