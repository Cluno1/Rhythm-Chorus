package chromahub.rhythm.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistSeparatorTest {

    private val defaultDelimiters = "/;,+&"

    @Test
    fun splitArtistNames_basicCharDelimiters() {
        assertEquals(
            listOf("Artist1", "Artist2", "Artist3"),
            ArtistSeparator.splitArtistNames("Artist1/Artist2;Artist3", defaultDelimiters, true)
        )
    }

    @Test
    fun splitArtistNames_wordSeparators() {
        assertEquals(
            listOf("Artist1", "Artist2"),
            ArtistSeparator.splitArtistNames("Artist1 feat. Artist2", defaultDelimiters, true)
        )
        assertEquals(
            listOf("Artist1", "Artist2"),
            ArtistSeparator.splitArtistNames("Artist1 AND Artist2", defaultDelimiters, true)
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
    fun splitArtistNames_escapedDelimiterMixedWithWordSeparator() {
        assertEquals(
            listOf("AC/DC", "Brian"),
            ArtistSeparator.splitArtistNames("AC\\/DC ft. Brian", "/", true)
        )
    }

    @Test
    fun splitArtistNames_disabled_returnsWholeString() {
        assertEquals(
            listOf("Artist1/Artist2"),
            ArtistSeparator.splitArtistNames("Artist1/Artist2", defaultDelimiters, false)
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
        assertEquals(emptyList<String>(), ArtistSeparator.splitArtistNames(null, defaultDelimiters, true))
        assertEquals(emptyList<String>(), ArtistSeparator.splitArtistNames("", defaultDelimiters, true))
        assertEquals(emptyList<String>(), ArtistSeparator.splitArtistNames("   ", defaultDelimiters, true))
    }

    @Test
    fun splitArtistNames_trailingAndConsecutiveDelimiters_areFiltered() {
        assertEquals(listOf("Artist1"), ArtistSeparator.splitArtistNames("Artist1/", "/", true))
        assertEquals(listOf("A", "B"), ArtistSeparator.splitArtistNames("A//B", "/", true))
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
}
