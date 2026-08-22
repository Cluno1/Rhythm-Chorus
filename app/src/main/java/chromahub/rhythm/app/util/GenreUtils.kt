/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.util

/**
 * Utility methods for parsing and matching multi-genre metadata.
 *
 * Supports comma, semicolon, and newline-separated values, e.g.:
 * - "Mahur, Shur"
 * - "Mahur;Shur"
 * - "Mahur\nShur"
 */
object GenreUtils {
    private val genreSplitRegex = Regex("[,;\\n\\r]+")

    fun splitGenres(rawGenre: String?): List<String> {
        if (rawGenre.isNullOrBlank()) return emptyList()

        // Some tags use NUL separators; normalize them to newline before splitting.
        val normalized = rawGenre.replace('\u0000', '\n')
        val seen = LinkedHashSet<String>()
        val genres = mutableListOf<String>()

        normalized
            .split(genreSplitRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) && it != "-" }
            .forEach { genre ->
                val key = genre.lowercase()
                if (seen.add(key)) {
                    genres.add(genre)
                }
            }

        return genres
    }

    fun matchesGenre(rawGenre: String?, genre: String): Boolean {
        val target = genre.trim()
        if (target.isEmpty()) return false
        return splitGenres(rawGenre).any { it.equals(target, ignoreCase = true) }
    }

    fun matchesGenreQuery(rawGenre: String?, query: String): Boolean {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) return false

        return splitGenres(rawGenre).any { genre ->
            val normalizedGenre = genre.lowercase()
            normalizedGenre == normalizedQuery ||
                normalizedGenre.startsWith(normalizedQuery) ||
                normalizedGenre.contains(normalizedQuery)
        }
    }
}
