package io.github.cluno1.sonorus.features.scores.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreDisplaySanitizerTest {
    @Test
    fun `keeps only the first tempo direction in the display projection`() {
        val source = """
            <score-partwise>
              <direction>
                <direction-type><metronome><per-minute>90</per-minute></metronome></direction-type>
                <sound tempo="90" />
              </direction>
              <direction>
                <direction-type>
                  <metronome parentheses="no"><per-minute>89.47</per-minute></metronome>
                </direction-type>
                <offset>-256</offset>
                <sound tempo="89.47" />
              </direction>
              <direction>
                <direction-type><metronome><per-minute>88.94</per-minute></metronome></direction-type>
                <sound tempo="88.94" />
              </direction>
            </score-partwise>
        """.trimIndent()

        val result = ScoreDisplaySanitizer.keepFirstVisibleMetronome(source.encodeToByteArray())
            .decodeToString()

        assertEquals(1, Regex("<metronome\\b").findAll(result).count())
        assertEquals(1, Regex("<sound tempo=").findAll(result).count())
        assertEquals(1, Regex("<direction(?:\\s|>)").findAll(result).count())
        assertTrue(result.contains("<per-minute>90</per-minute>"))
        assertTrue(!result.contains("89.47"))
        assertTrue(!result.contains("88.94"))
    }

    @Test
    fun `does not alter a score without printed tempo marks`() {
        val source = "<score-partwise><part /></score-partwise>".encodeToByteArray()

        assertEquals(
            source.decodeToString(),
            ScoreDisplaySanitizer.keepFirstVisibleMetronome(source).decodeToString()
        )
    }
}
