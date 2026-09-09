package io.github.cluno1.sonorus.features.scores.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class ChoirScoreDisplayProjectorTest {
    @Test
    fun `combines soprano and alto as two voices on one staff`() {
        val result = project(setOf(0, 1))
        val parts = result.documentElement.directChildren("part")

        assertEquals(1, parts.size)
        assertEquals(setOf("1", "2"), parts.first().descendants("voice").map { it.textContent }.toSet())
        assertEquals(setOf("1"), parts.first().descendants("staff").map { it.textContent }.toSet())
        assertEquals(1, parts.first().descendants("backup").size)
        assertEquals(listOf("up", "down"), parts.first().descendants("stem").map { it.textContent })
        parts.first().descendants("note").forEach { note ->
            val childNames = note.directChildren().map { it.tagName }
            assertTrue(childNames.indexOf("voice") < childNames.indexOf("type"))
            assertTrue(childNames.indexOf("stem") < childNames.indexOf("staff"))
        }
    }

    @Test
    fun `keeps soprano and bass on upper and lower staves`() {
        val result = project(setOf(0, 3))
        val parts = result.documentElement.directChildren("part")

        assertEquals(2, parts.size)
        assertEquals(listOf("S", "B"), result.documentElement.descendants("part-name").map { it.textContent })
        assertTrue(parts.all { it.descendants("backup").isEmpty() })
        assertEquals(listOf("G", "F"), result.documentElement.descendants("sign").map { it.textContent })
    }

    @Test
    fun `snaps near durations to a sixteenth note grid`() {
        val result = project(setOf(0, 1), duration = 470)

        assertEquals(
            setOf("480"),
            result.documentElement.descendants("duration").map { it.textContent }.toSet()
        )
    }

    @Test
    fun `keeps lord god almighty lead separate and merges satb pairs`() {
        val projection = ChoirScoreDisplayProjector.projectWithMapping(
            source = scoreXml(
                duration = 480,
                names = List(5) { "SmartMusic SoftSynth" },
                clefSigns = listOf("G", "G", "G", "F", "F"),
            ).encodeToByteArray(),
            selectedTrackIndexes = (0 until 5).toSet(),
        )
        val result = parse(projection.musicXml)
        val parts = result.documentElement.directChildren("part")

        assertEquals(3, parts.size)
        assertEquals(
            listOf("Lead", "S+A", "T+B"),
            result.documentElement.descendants("part-name").map { it.textContent },
        )
        assertEquals(
            listOf(emptySet(), setOf("1", "2"), setOf("1", "2")),
            parts.map { part -> part.descendants("voice").map { it.textContent }.toSet() },
        )
        assertEquals(
            listOf(listOf(0), listOf(1, 2), listOf(3, 4)),
            projection.groups.map { it.sourcePartIndexes },
        )
        assertEquals(
            listOf(listOf(4), listOf(0, 1), listOf(2, 3)),
            projection.groups.map { it.colorPartIndexes },
        )
    }

    @Test
    fun `keeps two and three part scores on separate staves`() {
        listOf(
            listOf("G", "F"),
            listOf("G", "G", "F"),
        ).forEach { clefs ->
            val projection = ChoirScoreDisplayProjector.projectWithMapping(
                source = scoreXml(
                    duration = 480,
                    names = List(clefs.size) { "SmartMusic SoftSynth" },
                    clefSigns = clefs,
                ).encodeToByteArray(),
                selectedTrackIndexes = clefs.indices.toSet(),
            )

            assertEquals(clefs.size, projection.groups.size)
            assertTrue(projection.groups.all { it.sourcePartIndexes.size == 1 })
        }
    }

    @Test
    fun `merges a recognized satb block and preserves sixth part extras`() {
        val projection = ChoirScoreDisplayProjector.projectWithMapping(
            source = scoreXml(
                duration = 480,
                names = List(6) { "[Staff 1]" },
                clefSigns = listOf("G", "G", "G", "F", "G", "G"),
            ).encodeToByteArray(),
            selectedTrackIndexes = (0 until 6).toSet(),
        )

        assertEquals(
            listOf(listOf(0, 1), listOf(2, 3), listOf(4), listOf(5)),
            projection.groups.map { it.sourcePartIndexes },
        )
        assertEquals(listOf("S+A", "T+B", "5", "6"), projection.groups.map { it.label })
    }

    @Test
    fun `preserves every unknown part instead of flattening them`() {
        val projection = ChoirScoreDisplayProjector.projectWithMapping(
            source = scoreXml(
                duration = 480,
                names = List(6) { "SmartMusic SoftSynth" },
                clefSigns = List(6) { "F" },
            ).encodeToByteArray(),
            selectedTrackIndexes = (0 until 6).toSet(),
        )

        assertEquals(6, projection.groups.size)
        assertTrue(projection.groups.all { it.sourcePartIndexes.size == 1 })
    }

    private fun project(selected: Set<Int>, duration: Int = 480) =
        parse(
            ChoirScoreDisplayProjector.project(
                source = scoreXml(duration).encodeToByteArray(),
                selectedTrackIndexes = selected
            )
        )

    private fun parse(source: ByteArray) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(source))

    private fun scoreXml(
        duration: Int,
        names: List<String> = List(4) { index -> "P$index" },
        clefSigns: List<String> = listOf("G", "G", "F", "F"),
    ): String {
        require(names.size == clefSigns.size)
        val definitions = names.indices.joinToString("") { index ->
            "<score-part id=\"P$index\"><part-name>${names[index]}</part-name></score-part>"
        }
        val parts = names.indices.joinToString("") { index ->
            val clefSign = clefSigns[index]
            """
                <part id="P$index">
                  <measure number="1">
                    <attributes>
                      <divisions>480</divisions><key><fifths>0</fifths></key>
                      <time><beats>4</beats><beat-type>4</beat-type></time>
                      <clef><sign>$clefSign</sign><line>${if (clefSign == "F") "4" else "2"}</line></clef>
                    </attributes>
                    <note><pitch><step>${if (index % 2 == 0) "D" else "C"}</step><octave>4</octave></pitch><duration>$duration</duration><type>quarter</type></note>
                    <barline location="right"><bar-style>light-heavy</bar-style></barline>
                  </measure>
                </part>
            """.trimIndent()
        }
        return """
            <score-partwise version="4.0"><part-list>$definitions</part-list>$parts</score-partwise>
        """.trimIndent()
    }

    private fun Element.directChildren(name: String): List<Element> = buildList {
        for (index in 0 until childNodes.length) {
            val node = childNodes.item(index)
            if (node.nodeType == Node.ELEMENT_NODE && node.nodeName == name) add(node as Element)
        }
    }

    private fun Element.directChildren(): List<Element> = buildList {
        for (index in 0 until childNodes.length) {
            val node = childNodes.item(index)
            if (node.nodeType == Node.ELEMENT_NODE) add(node as Element)
        }
    }

    private fun Element.descendants(name: String): List<Element> = buildList {
        val nodes = getElementsByTagName(name)
        for (index in 0 until nodes.length) add(nodes.item(index) as Element)
    }
}
