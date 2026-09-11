package io.github.cluno1.sonorus.util

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
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
}
