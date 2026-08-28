/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility object for parsing multiple artists from a single artist string.
 * 
 * Supports configurable delimiters (e.g., ;, /, ,, +, &, custom words like 'feat.', 'ft.')
 * and backslash escape sequences.
 * 
 * Example usage:
 * - "Artist1; Artist2" -> ["Artist1", "Artist2"]
 * - "Artist1/Artist2" -> ["Artist1", "Artist2"]
 * - "AC\\/DC" -> ["AC/DC"] (escaped slash)
 * - "Artist1 feat. Artist2" -> ["Artist1", "Artist2"] (when feat. delimiter enabled)
 */
object ArtistSeparator {
    private const val TAG = "ArtistSeparator"
    const val DEFAULT_DELIMITERS = ";/"
    private const val ESCAPE_CHAR = '\\'
    private const val PLACEHOLDER_PREFIX = "\u0000\u0001"
    private const val PLACEHOLDER_SUFFIX = '\u0002'

    private val regexCache = ConcurrentHashMap<String, Regex>()

    /**
     * Parses a raw delimiters string (single characters, JSON array, or newline/pipe delimited)
     * into a list of individual delimiter strings.
     */
    fun parseDelimiters(delimiters: String?): List<String> {
        if (delimiters.isNullOrBlank()) {
            return listOf(";", "/")
        }

        val trimmed = delimiters.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            val fromJson = runCatching {
                GsonUtils.gson.fromJson(trimmed, Array<String>::class.java)?.toList()
            }.getOrNull()
            if (!fromJson.isNullOrEmpty()) {
                return fromJson.filter { it.isNotEmpty() }.distinct()
            }
        }

        if (trimmed.contains('\n') || trimmed.contains('|')) {
            val tokens = trimmed.split(Regex("[\n|]"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            if (tokens.isNotEmpty()) {
                return tokens
            }
        }

        // Legacy / compact representation: each character is a delimiter
        return trimmed.map { it.toString() }.distinct()
    }

    /**
     * Serializes a list of delimiter tokens into a string for storage in AppSettings.
     */
    fun serializeDelimiters(delimiters: List<String>): String {
        val clean = delimiters.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (clean.isEmpty()) return DEFAULT_DELIMITERS
        
        val isSimpleCharsOnly = clean.all { it.length == 1 && !it[0].isWhitespace() && it != "[" && it != "]" }
        return if (isSimpleCharsOnly) {
            clean.joinToString("")
        } else {
            GsonUtils.gson.toJson(clean)
        }
    }

    private fun getOrCreateRegex(tokens: List<String>): Regex {
        val key = tokens.sorted().joinToString("|||")
        return regexCache.getOrPut(key) {
            val sorted = tokens.sortedByDescending { it.length }
            val patternString = sorted.joinToString("|") { Regex.escape(it) }
            patternString.toRegex(RegexOption.IGNORE_CASE)
        }
    }

    /**
     * Splits artist names using configurable delimiters with escape sequence support
     * and high-performance precompiled Regex caching.
     */
    fun splitArtistNames(
        artistName: String?,
        delimiters: String = DEFAULT_DELIMITERS,
        enabled: Boolean = true
    ): List<String> {
        if (artistName.isNullOrBlank()) {
            return emptyList()
        }
        
        if (!enabled || delimiters.isEmpty()) {
            return listOf(artistName.trim()).filter { it.isNotBlank() }
        }

        val tokens = parseDelimiters(delimiters)
        if (tokens.isEmpty()) {
            return listOf(artistName.trim()).filter { it.isNotBlank() }
        }

        // Protect escaped "\<delim>" occurrences so they are never treated as split points
        val escapedTokens = mutableListOf<String>()
        val escaped = StringBuilder(artistName.length)
        val sortedTokens = tokens.sortedByDescending { it.length }
        var i = 0

        while (i < artistName.length) {
            val c = artistName[i]
            if (c == ESCAPE_CHAR && i + 1 < artistName.length) {
                val remaining = artistName.substring(i + 1)
                val matchedToken = sortedTokens.firstOrNull { remaining.startsWith(it, ignoreCase = true) }
                if (matchedToken != null) {
                    val originalSlice = artistName.substring(i + 1, i + 1 + matchedToken.length)
                    escapedTokens.add(originalSlice)
                    escaped.append(PLACEHOLDER_PREFIX)
                    escaped.append((escapedTokens.size - 1).toChar())
                    escaped.append(PLACEHOLDER_SUFFIX)
                    i += 1 + matchedToken.length
                    continue
                }
            }
            escaped.append(c)
            i++
        }

        val regex = getOrCreateRegex(tokens)
        return regex.split(escaped.toString())
            .map { segment ->
                var restored = segment
                for ((index, originalToken) in escapedTokens.withIndex()) {
                    val tokenPlaceholder = PLACEHOLDER_PREFIX + index.toChar() + PLACEHOLDER_SUFFIX
                    restored = restored.replace(tokenPlaceholder, originalToken)
                }
                restored.trim()
            }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /**
     * Split an artist string into multiple artists using the provided delimiters.
     * Delegates to [splitArtistNames] for unified behavior.
     */
    fun splitArtists(
        artistString: String?,
        delimiters: String = DEFAULT_DELIMITERS,
        enabled: Boolean = true
    ): List<String> {
        return splitArtistNames(artistString, delimiters, enabled)
    }
    
    /**
     * Get the primary (first) artist from a split artist string.
     * This is useful for display purposes when you need a single artist name.
     */
    fun getPrimaryArtist(
        artistString: String?,
        delimiters: String = DEFAULT_DELIMITERS,
        enabled: Boolean = true
    ): String {
        val artists = splitArtistNames(artistString, delimiters, enabled)
        return artists.firstOrNull() ?: artistString?.trim() ?: "Unknown Artist"
    }
    
    /**
     * Format multiple artists for display.
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
     */
    fun escapeArtistName(artistName: String, delimiters: String = DEFAULT_DELIMITERS): String {
        val tokens = parseDelimiters(delimiters).sortedByDescending { it.length }
        var result = artistName
        for (token in tokens) {
            result = result.replace(token, "$ESCAPE_CHAR$token")
        }
        return result
    }
}
