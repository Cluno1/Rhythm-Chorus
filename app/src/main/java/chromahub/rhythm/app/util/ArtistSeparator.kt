package chromahub.rhythm.app.util

import android.util.Log

/**
 * Utility object for parsing multiple artists from a single artist string.
 * 
 * Supports configurable delimiters (e.g., /, ;, ,, +, &) and backslash escape sequences.
 * 
 * Example usage:
 * - "Artist1/Artist2" -> ["Artist1", "Artist2"]
 * - "Artist1; Artist2" -> ["Artist1", "Artist2"]
 * - "Artist1\\/Artist2" -> ["Artist1/Artist2"] (escaped slash)
 * - "Artist1 & Artist2" -> ["Artist1", "Artist2"]
 */
object ArtistSeparator {
    private const val TAG = "ArtistSeparator"
    private const val ESCAPE_CHAR = '\\'

    @Volatile
    private var cachedDelimiters: String? = null
    
    @Volatile
    private var cachedRegex: Regex? = null
    
    private val lock = Any()

    private fun getOrCreateRegex(delimiters: String): Regex {
        val cached = cachedRegex
        if (cachedDelimiters == delimiters && cached != null) {
            return cached
        }
        synchronized(lock) {
            if (cachedDelimiters == delimiters && cachedRegex != null) {
                return cachedRegex!!
            }
            
            val selectedDelimiterChars = delimiters.toSet()
            val wordSeparators = mutableListOf<String>().apply {
                if (selectedDelimiterChars.contains('&')) add(" & ")
                add(" and ")
                if (selectedDelimiterChars.contains(',')) add(", ")
                add(" feat. ")
                add(" feat ")
                add(" ft. ")
                add(" ft ")
                add(" featuring ")
                add(" x ")
                add(" X ")
                add(" × ")
                add(" vs ")
                add(" vs. ")
                add(" with ")
            }
            
            val charSeparators = delimiters.map { it.toString() }
            val allSeparators = (wordSeparators + charSeparators)
                .distinct()
                .sortedByDescending { it.length }
                
            val patternString = allSeparators.joinToString("|") { Regex.escape(it) }
            val regex = patternString.toRegex(RegexOption.IGNORE_CASE)
            
            cachedDelimiters = delimiters
            cachedRegex = regex
            return regex
        }
    }

    /**
     * Splits artist names using precompiled high-performance Regex caching.
     * Supports both character-level delimiters and word-level separators.
     */
    fun splitArtistNames(
        artistName: String?,
        delimiters: String = "/;,+&",
        enabled: Boolean = true
    ): List<String> {
        if (artistName.isNullOrBlank()) {
            return emptyList()
        }
        
        if (!enabled || delimiters.isEmpty()) {
            return listOf(artistName.trim()).filter { it.isNotBlank() }
        }

        // Protect escaped "\<delim>" pairs with unique tokens so the delimiter never becomes a split point.
        val placeholder = "\u0000\u0001"
        val escapedChars = mutableListOf<Char>()
        val escaped = StringBuilder(artistName.length)
        var i = 0
        while (i < artistName.length) {
            val c = artistName[i]
            if (c == ESCAPE_CHAR && i + 1 < artistName.length && delimiters.contains(artistName[i + 1])) {
                escapedChars.add(artistName[i + 1])
                escaped.append(placeholder)
                escaped.append((escapedChars.size - 1).toChar())
                i += 2
            } else {
                escaped.append(c)
                i++
            }
        }

        val regex = getOrCreateRegex(delimiters)
        return regex.split(escaped.toString())
            .map { segment ->
                var restored = segment
                for ((index, char) in escapedChars.withIndex()) {
                    restored = restored.replace(placeholder + index.toChar(), char.toString())
                }
                restored.trim()
            }
            .filter { it.isNotBlank() }
    }

    /**
     * Split an artist string into multiple artists using the provided delimiters.
     * 
     * @param artistString The artist string to split (e.g., "Artist1/Artist2")
     * @param delimiters String containing delimiter characters (e.g., "/;,+&")
     * @param enabled Whether artist splitting is enabled
     * @return List of artist names, or single-item list if splitting is disabled or no delimiters found
     */
    fun splitArtists(
        artistString: String?,
        delimiters: String = "/;,+&",
        enabled: Boolean = true
    ): List<String> {
        // Return empty list for null/blank input
        if (artistString.isNullOrBlank()) {
            return emptyList()
        }
        
        // If splitting is disabled, return original string as single artist
        if (!enabled || delimiters.isEmpty()) {
            return listOf(artistString.trim())
        }
        
        val artists = mutableListOf<String>()
        val currentArtist = StringBuilder()
        var i = 0
        
        while (i < artistString.length) {
            val char = artistString[i]
            
            // Check for escape character
            if (char == ESCAPE_CHAR && i + 1 < artistString.length) {
                // Next character is escaped, add it literally
                currentArtist.append(artistString[i + 1])
                i += 2
                continue
            }
            
            // Check if current character is a delimiter
            if (delimiters.contains(char)) {
                // Found a delimiter, add current artist if not empty
                val artist = currentArtist.toString().trim()
                if (artist.isNotEmpty()) {
                    artists.add(artist)
                }
                currentArtist.clear()
                i++
                continue
            }
            
            // Regular character, add to current artist
            currentArtist.append(char)
            i++
        }
        
        // Add the last artist if not empty
        val lastArtist = currentArtist.toString().trim()
        if (lastArtist.isNotEmpty()) {
            artists.add(lastArtist)
        }
        
        // If no artists were found, return original string
        if (artists.isEmpty()) {
            return listOf(artistString.trim())
        }
        
        Log.d(TAG, "Split '$artistString' into ${artists.size} artists: $artists")
        return artists
    }
    
    /**
     * Get the primary (first) artist from a split artist string.
     * This is useful for display purposes when you need a single artist name.
     * 
     * @param artistString The artist string to parse
     * @param delimiters String containing delimiter characters
     * @param enabled Whether artist splitting is enabled
     * @return The first artist name, or the original string if no splitting occurred
     */
    fun getPrimaryArtist(
        artistString: String?,
        delimiters: String = "/;,+&",
        enabled: Boolean = true
    ): String {
        val artists = splitArtists(artistString, delimiters, enabled)
        return artists.firstOrNull() ?: artistString?.trim() ?: "Unknown Artist"
    }
    
    /**
     * Format multiple artists for display.
     * 
     * @param artists List of artist names
     * @param separator Separator to use for joining (default: ", ")
     * @param maxArtists Maximum number of artists to show before using "& more"
     * @return Formatted artist string
     */
    fun formatArtists(
        artists: List<String>,
        separator: String = ", ",
        maxArtists: Int = 3
    ): String {
        if (artists.isEmpty()) return "Unknown Artist"
        if (artists.size == 1) return artists[0]
        
        return if (artists.size <= maxArtists) {
            artists.joinToString(separator)
        } else {
            val visible = artists.take(maxArtists)
            val remaining = artists.size - maxArtists
            "${visible.joinToString(separator)} & $remaining more"
        }
    }
    
    /**
     * Escape delimiters in an artist name to prevent splitting.
     * 
     * @param artistName The artist name to escape
     * @param delimiters String containing delimiter characters to escape
     * @return Escaped artist name
     */
    fun escapeArtistName(artistName: String, delimiters: String = "/;,+&"): String {
        val escaped = StringBuilder()
        for (char in artistName) {
            if (delimiters.contains(char) || char == ESCAPE_CHAR) {
                escaped.append(ESCAPE_CHAR)
            }
            escaped.append(char)
        }
        return escaped.toString()
    }
}
