package io.github.cluno1.sonorus.util

import com.google.gson.JsonParser
import io.github.cluno1.sonorus.shared.data.model.LyricsContribution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsContributionAttributionTest {
    @Test
    fun firstTextContributionAddsUploaderMetadataWithoutCreatingTimingTargets() {
        val result = LyricsContributionAttribution.append(
            lyrics = "First\nSecond",
            contributorName = "Grace] User",
            updatedAt = "2026-09-11 16:30:00 +08:00",
            format = "LINE_BY_LINE",
        )

        assertEquals(
            "First\nSecond\n[contributor:uploader|Grace） User|2026-09-11 16:30:00 +08:00]",
            result,
        )
        assertEquals(2, requireNotNull(LrcTimingEditor.timingTarget(result, 0)).total)
    }

    @Test
    fun laterTextContributionAppendsModifierWithoutReplacingHistory() {
        val result = LyricsContributionAttribution.append(
            lyrics = "Line\n[contributor:uploader|First|Earlier]\n[contributor:modifier|Old|Old time]",
            contributorName = "Second",
            updatedAt = "Now",
            format = "LINE_BY_LINE",
        )

        assertTrue(result.contains("[contributor:uploader|First|Earlier]"))
        assertTrue(result.contains("[contributor:modifier|Old|Old time]"))
        assertTrue(result.endsWith("[contributor:modifier|Second|Now]"))
    }

    @Test
    fun blankNameLeavesLyricsByteForByteUnchanged() {
        val original = "Line\n"
        assertEquals(
            original,
            LyricsContributionAttribution.append(original, "  ", "Now", "LINE_BY_LINE"),
        )
    }

    @Test
    fun wordByWordContributionRemainsValidAndInvisibleToLyricsParser() {
        val original = """[{"text":[{"text":"Word","timestamp":1000,"endtime":1500}]}]"""
        val first = LyricsContributionAttribution.append(original, "First", "Earlier", "WORD_BY_WORD")
        val result = LyricsContributionAttribution.append(first, "Second", "Now", "WORD_BY_WORD")

        assertTrue(JsonParser.parseString(result).isJsonArray)
        assertTrue(result.contains("\"role\":\"uploader\""))
        assertTrue(result.contains("\"role\":\"modifier\""))
        assertTrue(result.contains("\"name\":\"First\""))
        assertTrue(result.contains("\"name\":\"Second\""))
        assertEquals(1, RhythmLyricsParser.parseWordByWordLyrics(result).size)
    }

    @Test
    fun ttmlContributionUsesAnXmlCommentAfterTheDocument() {
        val original = "<tt xmlns=\"http://www.w3.org/ns/ttml\"><body/></tt>"
        val result = LyricsContributionAttribution.append(original, "User", "Now", "SOURCE")

        assertTrue(result.startsWith(original))
        assertTrue(result.endsWith("<!-- rhythm-contribution role=\"uploader\" name=\"User\" updated-at=\"Now\" -->"))
        assertTrue(RhythmLyricsParser.isTtmlContent(result))
    }

    @Test
    fun extractsAllInAllUploaderAndRemovesMetadataFromVisibleLrc() {
        val original = """[00:00.000]You are my strength when I am weak
[01:04.031]You are my all in all.
[contributor:uploader|cluno|2026-09-11 17:12:16 +08:00]"""

        val result = LyricsContributionAttribution.extract(original, "lrc")

        assertFalse(result.visibleLyrics.contains("contributor:"))
        assertEquals(1, result.contributions.size)
        assertEquals("uploader", result.contributions.single().role)
        assertEquals("cluno", result.contributions.single().name)
        assertEquals("2026-09-11 17:12:16 +08:00", result.contributions.single().updatedAt)
    }

    @Test
    fun extractsOrderedWordByWordContributionHistoryWithoutParserObjects() {
        val original = """[
            {"text":[{"text":"Word","timestamp":1000,"endtime":1500}]},
            {"_rhythmContribution":{"role":"uploader","name":"First","updatedAt":"Earlier"}},
            {"_rhythmContribution":{"role":"modifier","name":"Second","updatedAt":"Now"}}
        ]""".trimIndent()

        val result = LyricsContributionAttribution.extract(original, "word_by_word_json")

        assertEquals(listOf("First", "Second"), result.contributions.map { it.name })
        assertEquals(listOf("uploader", "modifier"), result.contributions.map { it.role })
        assertFalse(result.visibleLyrics.contains("_rhythmContribution"))
        assertEquals(1, RhythmLyricsParser.parseWordByWordLyrics(result.visibleLyrics).size)
    }

    @Test
    fun extractsTtmlContributionHistoryAndKeepsDocumentVisible() {
        val original = """<tt xmlns="http://www.w3.org/ns/ttml"><body/></tt>
<!-- rhythm-contribution role="uploader" name="First" updated-at="Earlier" -->
<!-- rhythm-contribution role="modifier" name="Second" updated-at="Now" -->"""

        val result = LyricsContributionAttribution.extract(original, "ttml")

        assertEquals(listOf("First", "Second"), result.contributions.map { it.name })
        assertEquals("<tt xmlns=\"http://www.w3.org/ns/ttml\"><body/></tt>", result.visibleLyrics)
    }

    @Test
    fun hiddenHistoryIsPreservedAndNextNamedContributionBecomesModifier() {
        val existing = listOf(LyricsContribution("uploader", "First", "Earlier"))
        val restored = LyricsContributionAttribution.preserveHistory(
            lyrics = "[00:00.000]Edited line",
            existing = existing,
            format = "LINE_BY_LINE",
        )
        val updated = LyricsContributionAttribution.append(
            lyrics = restored,
            contributorName = "Second",
            updatedAt = "Now",
            format = "LINE_BY_LINE",
        )

        val result = LyricsContributionAttribution.extract(updated, "lrc")
        assertEquals(listOf("uploader", "modifier"), result.contributions.map { it.role })
        assertEquals(listOf("First", "Second"), result.contributions.map { it.name })
        assertEquals("[00:00.000]Edited line", result.visibleLyrics)
    }
}
