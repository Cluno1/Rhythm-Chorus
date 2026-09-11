package io.github.cluno1.sonorus.util

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.cluno1.sonorus.shared.data.model.LyricsContribution

data class LyricsContributionExtraction(
    val visibleLyrics: String,
    val contributions: List<LyricsContribution>,
)

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
    private val ttmlContributionBlock = Regex(
        "\\s*<!--\\s*rhythm-contribution\\s+([^>]*?)-->\\s*",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val ttmlAttribute = Regex("([A-Za-z][A-Za-z-]*)=\"([^\"]*)\"")

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

    fun extract(lyrics: String, format: String): LyricsContributionExtraction = when {
        format.equals("WORD_BY_WORD", ignoreCase = true) ||
            format.equals("word_by_word_json", ignoreCase = true) -> extractFromWordByWordJson(lyrics)
        looksLikeTtml(lyrics) || format.equals("ttml", ignoreCase = true) -> extractFromTtml(lyrics)
        else -> extractFromText(lyrics)
    }

    fun visibleLyrics(lyrics: String, format: String): String = extract(lyrics, format).visibleLyrics

    fun preserveHistory(
        lyrics: String,
        existing: List<LyricsContribution>,
        format: String,
    ): String {
        val extracted = extract(lyrics, format)
        val combined = (existing + extracted.contributions).distinctBy {
            Triple(it.role.lowercase(), it.name, it.updatedAt)
        }
        if (combined.isEmpty()) return extracted.visibleLyrics
        return when {
            format.equals("WORD_BY_WORD", ignoreCase = true) ||
                format.equals("word_by_word_json", ignoreCase = true) ->
                encodeWordByWordHistory(extracted.visibleLyrics, combined)
            looksLikeTtml(extracted.visibleLyrics) || format.equals("ttml", ignoreCase = true) ->
                encodeTtmlHistory(extracted.visibleLyrics, combined)
            else -> encodeTextHistory(extracted.visibleLyrics, combined)
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

    private fun extractFromText(lyrics: String): LyricsContributionExtraction {
        val credits = mutableListOf<LyricsContribution>()
        val visibleLines = lyrics
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .filterNot { line ->
                val match = contribution.matchEntire(line.trim()) ?: return@filterNot false
                credits += LyricsContribution(
                    role = match.groupValues[1].lowercase(),
                    name = match.groupValues[2],
                    updatedAt = match.groupValues[3],
                )
                true
            }
        return LyricsContributionExtraction(
            visibleLyrics = visibleLines.joinToString("\n").trimEnd(),
            contributions = credits,
        )
    }

    private fun encodeTextHistory(
        lyrics: String,
        contributions: List<LyricsContribution>,
    ): String {
        val retained = lyrics.trimEnd()
        val history = contributions.joinToString("\n") {
            val role = if (it.role.equals("uploader", true)) "uploader" else "modifier"
            "[contributor:$role|${sanitizeMetadataValue(it.name, 80)}|" +
                "${sanitizeMetadataValue(it.updatedAt, 64)}]"
        }
        return if (retained.isBlank()) history else "$retained\n$history"
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

    private fun extractFromWordByWordJson(lyrics: String): LyricsContributionExtraction {
        return try {
            val source = JsonParser.parseString(lyrics)
            if (!source.isJsonArray) return extractFromText(lyrics)
            val visible = JsonArray()
            val credits = mutableListOf<LyricsContribution>()
            source.asJsonArray.forEach { element ->
                val details = element.takeIf { it.isJsonObject }
                    ?.asJsonObject
                    ?.get(JSON_MARKER)
                    ?.takeIf { it.isJsonObject }
                    ?.asJsonObject
                if (details == null) {
                    visible.add(element)
                    return@forEach
                }
                val role = details.stringValue("role")
                val name = details.stringValue("name")
                val updatedAt = details.stringValue("updatedAt")
                if ((role.equals("uploader", true) || role.equals("modifier", true)) && name.isNotBlank()) {
                    credits += LyricsContribution(role.lowercase(), name, updatedAt)
                }
            }
            LyricsContributionExtraction(Gson().toJson(visible), credits)
        } catch (_: Exception) {
            extractFromText(lyrics)
        }
    }

    private fun encodeWordByWordHistory(
        lyrics: String,
        contributions: List<LyricsContribution>,
    ): String {
        return try {
            val source = JsonParser.parseString(lyrics)
            if (!source.isJsonArray) return encodeTextHistory(lyrics, contributions)
            val output = source.asJsonArray.deepCopy()
            contributions.forEach { contribution ->
                val details = JsonObject().apply {
                    addProperty(
                        "role",
                        if (contribution.role.equals("uploader", true)) "uploader" else "modifier",
                    )
                    addProperty("name", sanitizeMetadataValue(contribution.name, 80))
                    addProperty("updatedAt", sanitizeMetadataValue(contribution.updatedAt, 64))
                }
                output.add(JsonObject().apply { add(JSON_MARKER, details) })
            }
            Gson().toJson(output)
        } catch (_: Exception) {
            encodeTextHistory(lyrics, contributions)
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

    private fun extractFromTtml(lyrics: String): LyricsContributionExtraction {
        val credits = mutableListOf<LyricsContribution>()
        val visible = ttmlContributionBlock.replace(lyrics) { match ->
            val attributes = ttmlAttribute.findAll(match.groupValues[1]).associate {
                it.groupValues[1].lowercase() to it.groupValues[2]
            }
            val role = attributes["role"].orEmpty()
            val name = attributes["name"].orEmpty()
            val updatedAt = attributes["updated-at"].orEmpty()
            if ((role.equals("uploader", true) || role.equals("modifier", true)) && name.isNotBlank()) {
                credits += LyricsContribution(role.lowercase(), name, updatedAt)
            }
            ""
        }.trimEnd()
        return LyricsContributionExtraction(visible, credits)
    }

    private fun encodeTtmlHistory(
        lyrics: String,
        contributions: List<LyricsContribution>,
    ): String {
        val retained = lyrics.trimEnd()
        val history = contributions.joinToString("\n") {
            val role = if (it.role.equals("uploader", true)) "uploader" else "modifier"
            "<!-- rhythm-contribution role=\"$role\" name=\"${sanitizeCommentValue(it.name)}\" " +
                "updated-at=\"${sanitizeCommentValue(it.updatedAt)}\" -->"
        }
        return if (retained.isBlank()) history else "$retained\n$history"
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

    private fun JsonObject.stringValue(name: String): String =
        get(name)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
}
