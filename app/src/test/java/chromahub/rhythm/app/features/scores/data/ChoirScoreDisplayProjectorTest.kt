package chromahub.rhythm.app.features.scores.data

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
        assertTrue(parts.all { it.descendants("voice").map(Element::getTextContent).toSet() == setOf("1") })
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

    private fun project(selected: Set<Int>, duration: Int = 480) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            ByteArrayInputStream(
                ChoirScoreDisplayProjector.project(
                    source = scoreXml(duration).encodeToByteArray(),
                    selectedTrackIndexes = selected
                )
            )
        )

    private fun scoreXml(duration: Int): String {
        val definitions = (0 until 4).joinToString("") { index ->
            "<score-part id=\"P$index\"><part-name>P$index</part-name></score-part>"
        }
        val parts = (0 until 4).joinToString("") { index ->
            """
                <part id="P$index">
                  <measure number="1">
                    <attributes>
                      <divisions>480</divisions><key><fifths>0</fifths></key>
                      <time><beats>4</beats><beat-type>4</beat-type></time>
                      <clef><sign>${if (index < 2) "G" else "F"}</sign><line>${if (index < 2) "2" else "4"}</line></clef>
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
