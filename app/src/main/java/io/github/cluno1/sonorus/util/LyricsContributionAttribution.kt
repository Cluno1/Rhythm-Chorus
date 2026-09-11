package io.github.cluno1.sonorus.util

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object LyricsContributionAttribution {
    private const val JSON_MARKER = "_rhythmContribution"
    private val contribution = Regex(
        "^\\[contributor:(uploader|modifier)\\|([^|]*)\\|([^]]*)]$",
        RegexOption.IGNORE_CASE,
    )
    private val standardBy = Regex("^\\[by:([^]]+)]$", RegexOption.IGNORE_CASE)
    private val ttmlContribution = Regex(
        "\\s*<!--\\s*rhythm-contribution\\s+role=\"(uploader|modifier)\".*?-->\\s*",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun append(
        lyrics: String,
        contributorName: String,
        updatedAt: String,
        format: String,
    ): String {
        val name = sanitizeMetadataValue(contributorName, 80)
        if (name.isBlank()) return lyrics
        val timestamp = sanitizeMetadataValue(updatedAt, 64)
        if (timestamp.isBlank()) return lyrics

        return when {
            format.equals("WORD_BY_WORD", ignoreCase = true) ->
                appendToWordByWordJson(lyrics, name, timestamp)
            looksLikeTtml(lyrics) -> appendToTtml(lyrics, name, timestamp)
            else -> appendToText(lyrics, name, timestamp)
        }
    }

    private fun appendToText(lyrics: String, name: String, timestamp: String): String {
        val lines = lyrics.replace("\r\n", "\n").replace('\r', '\n').lines()
        val hasUploader = lines.any { line ->
            contribution.matchEntire(line.trim())?.groupValues?.get(1)
                ?.equals("uploader", ignoreCase = true) == true || standardBy.matches(line.trim())
        }
        val role = if (hasUploader) "modifier" else "uploader"
        val credit = "[contributor:$role|$name|$timestamp]"
        if (lines.any { it.trim() == credit }) return lyrics
        val retained = lines.joinToString("\n").trimEnd()
        return if (retained.isBlank()) credit else "$retained\n$credit"
    }

    private fun appendToWordByWordJson(lyrics: String, name: String, timestamp: String): String {
        return try {
            val source = JsonParser.parseString(lyrics)
            if (!source.isJsonArray) return appendToText(lyrics, name, timestamp)
            val output = JsonArray()
            var hasUploader = false
            source.asJsonArray.forEach { element ->
                val contribution = element.takeIf { it.isJsonObject }
                    ?.asJsonObject
                    ?.get(JSON_MARKER)
                    ?.takeIf { it.isJsonObject }
                    ?.asJsonObject
                val role = contribution?.get("role")?.takeIf { it.isJsonPrimitive }?.asString
                if (role.equals("uploader", ignoreCase = true)) hasUploader = true
                output.add(element)
            }
            val details = JsonObject().apply {
                addProperty("role", if (hasUploader) "modifier" else "uploader")
                addProperty("name", name)
                addProperty("updatedAt", timestamp)
            }
            output.add(JsonObject().apply { add(JSON_MARKER, details) })
            Gson().toJson(output)
        } catch (_: Exception) {
            appendToText(lyrics, name, timestamp)
        }
    }

    private fun appendToTtml(lyrics: String, name: String, timestamp: String): String {
        var hasUploader = false
        ttmlContribution.findAll(lyrics).forEach { match ->
            if (match.groupValues[1].equals("uploader", ignoreCase = true)) {
                hasUploader = true
            }
        }
        val safeName = sanitizeCommentValue(name)
        val safeTimestamp = sanitizeCommentValue(timestamp)
        val role = if (hasUploader) "modifier" else "uploader"
        val credit = "<!-- rhythm-contribution role=\"$role\" name=\"$safeName\" updated-at=\"$safeTimestamp\" -->"
        if (lyrics.contains(credit)) return lyrics
        val retained = lyrics.trimEnd()
        return if (retained.isBlank()) credit else "$retained\n$credit"
    }

    private fun looksLikeTtml(lyrics: String): Boolean {
        val trimmed = lyrics.trimStart()
        return trimmed.startsWith("<") && (
            trimmed.contains("<tt") ||
                trimmed.contains("http://www.w3.org/ns/ttml")
            )
    }

    private fun sanitizeMetadataValue(value: String, maxLength: Int): String = value
        .replace(']', '）')
        .replace('|', '｜')
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
        .take(maxLength)

    private fun sanitizeCommentValue(value: String): String = value
        .replace("--", "—")
        .replace('"', '”')
        .replace('<', '‹')
        .replace('>', '›')
}
