/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.cluno1.sonorus.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistSeparatorTest {

    private val standardDelimiters = ";/"
    private val minimalDelimiters = ";"
    private val extendedDelimiters = ";/,+&"

    @Test
    fun splitArtistNames_standardDefaultDelimiters() {
        assertEquals(
            listOf("Artist1", "Artist2", "Artist3"),
            ArtistSeparator.splitArtistNames("Artist1/Artist2;Artist3", standardDelimiters, true)
        )
        assertEquals(
            listOf("Kendrick Lamar", "SZA"),
            ArtistSeparator.splitArtistNames("Kendrick Lamar; SZA", standardDelimiters, true)
        )
    }

    @Test
    fun splitArtistNames_preservesBandsWithCommasAndAmpersandsUnderStandardDefaults() {
        // Standard defaults (;/ ) do not split on , or & or +
        assertEquals(
            listOf("Tyler, The Creator"),
            ArtistSeparator.splitArtistNames("Tyler, The Creator", standardDelimiters, true)
        )
        assertEquals(
            listOf("Simon & Garfunkel"),
            ArtistSeparator.splitArtistNames("Simon & Garfunkel", standardDelimiters, true)
        )
        assertEquals(
            listOf("Earth, Wind & Fire"),
            ArtistSeparator.splitArtistNames("Earth, Wind & Fire", standardDelimiters, true)
        )
        assertEquals(
            listOf("+44"),
            ArtistSeparator.splitArtistNames("+44", standardDelimiters, true)
        )
        assertEquals(
            listOf("Florence + The Machine"),
            ArtistSeparator.splitArtistNames("Florence + The Machine", standardDelimiters, true)
        )
    }

    @Test
    fun splitArtistNames_minimalPreset_semicolonOnly() {
        assertEquals(
            listOf("Artist1/Artist2", "Artist3"),
            ArtistSeparator.splitArtistNames("Artist1/Artist2;Artist3", minimalDelimiters, true)
        )
        assertEquals(
            listOf("AC/DC", "Brian Johnson"),
            ArtistSeparator.splitArtistNames("AC/DC; Brian Johnson", minimalDelimiters, true)
        )
    }

    @Test
    fun splitArtistNames_customWordDelimiters() {
        val customDelimiters = ArtistSeparator.serializeDelimiters(listOf(";", "/", "feat.", "ft.", " x "))
        
        assertEquals(
            listOf("Artist1", "Artist2"),
            ArtistSeparator.splitArtistNames("Artist1 feat. Artist2", customDelimiters, true)
        )
        assertEquals(
            listOf("Artist1", "Artist2"),
            ArtistSeparator.splitArtistNames("Artist1 ft. Artist2", customDelimiters, true)
        )
        assertEquals(
            listOf("Artist1", "Artist2"),
            ArtistSeparator.splitArtistNames("Artist1 x Artist2", customDelimiters, true)
        )
        assertEquals(
            listOf("Daft Punk", "Pharrell Williams"),
            ArtistSeparator.splitArtistNames("Daft Punk feat. Pharrell Williams", customDelimiters, true)
        )
    }

    @Test
    fun splitArtistNames_cjkDelimiters() {
        val cjkDelimiters = ArtistSeparator.serializeDelimiters(listOf("、", "／", "・", "•"))

        assertEquals(
            listOf("YOASOBI", "Ayase"),
            ArtistSeparator.splitArtistNames("YOASOBI、Ayase", cjkDelimiters, true)
        )
        assertEquals(
            listOf("Artist A", "Artist B"),
            ArtistSeparator.splitArtistNames("Artist A／Artist B", cjkDelimiters, true)
        )
        assertEquals(
            listOf("Artist 1", "Artist 2"),
            ArtistSeparator.splitArtistNames("Artist 1・Artist 2", cjkDelimiters, true)
        )
    }

    @Test
    fun splitArtistNames_escapedDelimiter_staysTogether() {
        assertEquals(
            listOf("AC/DC"),
            ArtistSeparator.splitArtistNames("AC\\/DC", "/", true)
        )
    }

    @Test
    fun splitArtistNames_escapedAndRealDelimiters() {
        assertEquals(
            listOf("A/B", "C"),
            ArtistSeparator.splitArtistNames("A\\/B/C", "/", true)
        )
    }

    @Test
    fun splitArtistNames_multipleEscapedDelimiters() {
        assertEquals(
            listOf("A/B/C"),
            ArtistSeparator.splitArtistNames("A\\/B\\/C", "/", true)
        )
    }

    @Test
    fun splitArtistNames_disabled_returnsWholeString() {
        assertEquals(
            listOf("Artist1/Artist2"),
            ArtistSeparator.splitArtistNames("Artist1/Artist2", standardDelimiters, false)
        )
    }

    @Test
    fun splitArtistNames_emptyDelimiters_returnsWholeString() {
        assertEquals(
            listOf("Artist1/Artist2"),
            ArtistSeparator.splitArtistNames("Artist1/Artist2", "", true)
        )
    }

    @Test
    fun splitArtistNames_nullOrBlank_returnsEmpty() {
        assertEquals(emptyList<String>(), ArtistSeparator.splitArtistNames(null, standardDelimiters, true))
        assertEquals(emptyList<String>(), ArtistSeparator.splitArtistNames("", standardDelimiters, true))
        assertEquals(emptyList<String>(), ArtistSeparator.splitArtistNames("   ", standardDelimiters, true))
    }

    @Test
    fun splitArtistNames_trailingLeadingAndConsecutiveDelimiters_areFiltered() {
        assertEquals(listOf("Artist1"), ArtistSeparator.splitArtistNames("Artist1/", "/", true))
        assertEquals(listOf("Artist1"), ArtistSeparator.splitArtistNames("/Artist1", "/", true))
        assertEquals(listOf("Artist1"), ArtistSeparator.splitArtistNames("/ Artist1 /", "/", true))
        assertEquals(listOf("A", "B"), ArtistSeparator.splitArtistNames("A//B", "/", true))
        assertEquals(listOf("A", "B"), ArtistSeparator.splitArtistNames("A /// B", "/", true))
    }

    @Test
    fun splitArtistNames_deduplicatesIdenticalCollaborators() {
        assertEquals(
            listOf("Artist1", "Artist2"),
            ArtistSeparator.splitArtistNames("Artist1 / Artist2 / Artist1", "/", true)
        )
    }

    @Test
    fun splitArtistNames_trimSpacesAroundNames() {
        assertEquals(
            listOf("Artist1", "Artist2"),
            ArtistSeparator.splitArtistNames(" Artist1 / Artist2 ", "/", true)
        )
    }

    @Test
    fun splitArtistNames_escapedBackslashBeforeNonDelimiter_isKeptLiteral() {
        assertEquals(
            listOf("AC\\DC"),
            ArtistSeparator.splitArtistNames("AC\\DC", "/", true)
        )
    }

    @Test
    fun parseAndSerializeDelimiters_roundtrip() {
        val simpleList = listOf(";", "/")
        val simpleSerialized = ArtistSeparator.serializeDelimiters(simpleList)
        assertEquals(";", simpleSerialized.substring(0, 1))
        assertEquals(simpleList, ArtistSeparator.parseDelimiters(simpleSerialized))

        val complexList = listOf(";", "/", "feat.", "ft.", " // ")
        val complexSerialized = ArtistSeparator.serializeDelimiters(complexList)
        val parsed = ArtistSeparator.parseDelimiters(complexSerialized)
        assertEquals(listOf(";", "/", "feat.", "ft.", "//"), parsed)
    }

    @Test
    fun getPrimaryArtist_returnsFirstOrOriginal() {
        assertEquals("Artist1", ArtistSeparator.getPrimaryArtist("Artist1 / Artist2", standardDelimiters, true))
        assertEquals("Unknown Artist", ArtistSeparator.getPrimaryArtist(null, standardDelimiters, true))
    }

    @Test
    fun formatArtists_formatsProperly() {
        assertEquals("Artist1", ArtistSeparator.formatArtists(listOf("Artist1")))
        assertEquals("Artist1, Artist2", ArtistSeparator.formatArtists(listOf("Artist1", "Artist2")))
        assertEquals("Artist1, Artist2, Artist3 & 2 more", ArtistSeparator.formatArtists(listOf("Artist1", "Artist2", "Artist3", "Artist4", "Artist5")))
    }

    @Test
    fun escapeArtistName_escapesConfiguredDelimiters() {
        assertEquals("AC\\/DC", ArtistSeparator.escapeArtistName("AC/DC", standardDelimiters))
        assertEquals("A\\;B", ArtistSeparator.escapeArtistName("A;B", standardDelimiters))
    }
}
