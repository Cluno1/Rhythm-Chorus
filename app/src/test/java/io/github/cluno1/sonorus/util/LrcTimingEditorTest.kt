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
    fun manualTemplateClearsLineTimestampsAndPreservesText() {
        assertEquals(
            "[ar:Artist]\nOne two\nTwo\n",
            LrcTimingEditor.generateTemplate(
                "[ar:Artist]\n[00:04.000]<00:04.000>One <00:04.500>two\n[00:08.000]Two\n[00:09.000]",
                null,
                estimateFromDuration = false,
            ),
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
    fun consecutiveStampsUseTheReturnedNextEditableLine() {
        val first = requireNotNull(
            LrcTimingEditor.stampLine("First\n\nSecond\nThird", 0, 1_000L),
        )
        assertEquals(2, first.nextLineIndex)

        val second = requireNotNull(
            LrcTimingEditor.stampLine(
                first.text,
                requireNotNull(first.nextLineIndex),
                2_500L,
            ),
        )
        assertEquals(
            "[00:01.000]First\n\n[00:02.500]Second\nThird",
            second.text,
        )
        assertEquals(3, second.nextLineIndex)
    }

    @Test
    fun lineAndCursorHelpersRespectBlankLines() {
        val text = "One\n\nThree"
        assertEquals(2, LrcTimingEditor.lineIndexAtOffset(text, text.length))
        assertEquals(5, LrcTimingEditor.lineStartOffset(text, 2))
        assertEquals(0, LrcTimingEditor.previousEditableLine(text, 2))
    }

    @Test
    fun timingTargetSkipsBlankLinesAndStripsExistingTimestamp() {
        val text = "[00:01.000]One\n\n[00:02.000]Three"
        assertEquals(
            LrcTimingTarget(lineIndex = 2, ordinal = 2, total = 2, text = "Three"),
            LrcTimingEditor.timingTarget(text, 1),
        )
        assertEquals(
            LrcTimingTarget(lineIndex = 2, ordinal = 2, total = 2, text = "Three"),
            LrcTimingEditor.timingTarget(text, 99),
        )
        assertNull(LrcTimingEditor.timingTarget("\n\n", 0))
    }

    @Test
    fun loopEndEnforcesMinimumWindowAndTrackDuration() {
        assertEquals(1_500L, LrcTimingEditor.loopEnd(1_000L, 1_100L, 10_000L))
        assertEquals(10_000L, LrcTimingEditor.loopEnd(9_000L, 12_000L, 10_000L))
        assertNull(LrcTimingEditor.loopEnd(9_750L, 9_900L, 10_000L))
    }

    @Test
    fun manualTemplateBecomesSyncedOnlyAfterARealStamp() {
        val generated = LrcTimingEditor.generateTemplate(
            "One\nTwo\nThree",
            durationMs = null,
            estimateFromDuration = false,
        )

        assertEquals(false, LrcTimingEditor.hasLineTimestamp(generated))
        val stamped = requireNotNull(LrcTimingEditor.stampLine(generated, 0, 1_500L))
        assertEquals("[00:01.500]One\nTwo\nThree", stamped.text)
        assertEquals(true, LrcTimingEditor.hasLineTimestamp(stamped.text))
    }
}
