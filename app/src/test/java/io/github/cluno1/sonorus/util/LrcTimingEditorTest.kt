package io.github.cluno1.sonorus.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LrcTimingEditorTest {
    @Test
    fun estimatedTemplatePreservesSectionsAndReplacesExistingHeaders() {
        assertEquals(
            "[00:00.000]第一行\n\n[01:00.000]第二行\n[02:00.000]第三行",
            LrcTimingEditor.generateTemplate(
                "第一行\n\n[00:09.000]第二行\n第三行",
                durationMs = 120_000,
                estimateFromDuration = true,
            ),
        )
    }

    @Test
    fun zeroTemplateMakesEveryNonBlankLineEditable() {
        assertEquals(
            "[00:00.000]One\n[00:00.000]Two",
            LrcTimingEditor.generateTemplate("One\nTwo", null, estimateFromDuration = false),
        )
    }

    @Test
    fun stampingAdvancesAndKeepsTimesMonotonicAndWithinDuration() {
        val original = "[00:03.000]One\n\n[00:00.000]Two"
        val result = requireNotNull(LrcTimingEditor.stampLine(original, 2, 2_000, 10_000))
        assertEquals("[00:03.000]One\n\n[00:03.000]Two", result.text)
        assertEquals(2, result.stampedLineIndex)
        assertNull(result.nextLineIndex)

        val clamped = requireNotNull(LrcTimingEditor.stampLine("Last", 0, 12_000, 10_000))
        assertEquals("[00:10.000]Last", clamped.text)
    }

    @Test
    fun lineAndCursorHelpersRespectBlankLines() {
        val text = "One\n\nThree"
        assertEquals(2, LrcTimingEditor.lineIndexAtOffset(text, text.length))
        assertEquals(5, LrcTimingEditor.lineStartOffset(text, 2))
        assertEquals(0, LrcTimingEditor.previousEditableLine(text, 2))
    }
}
