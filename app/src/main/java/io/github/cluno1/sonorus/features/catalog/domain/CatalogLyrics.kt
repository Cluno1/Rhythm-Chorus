package io.github.cluno1.sonorus.features.catalog.domain

import java.util.Locale

data class CatalogLyricsVariant(
    val language: String,
    val lyrics: String,
)

fun normalizeCatalogLyricsLanguageTag(value: String): String {
    val normalized = value.trim().replace('_', '-')
    require(normalized.isNotEmpty()) { "lyrics language must not be blank" }
    val languageTag = Locale.forLanguageTag(normalized).toLanguageTag()
    require(languageTag.isNotEmpty()) { "lyrics language is invalid" }
    return languageTag
}

fun CatalogLibrarySong.lyricsVariants(): List<CatalogLyricsVariant> = catalogLyricsVariants(
    lyrics = lyrics,
    lyricsLanguage = lyricsLanguage,
    translations = lyricsTranslations,
)

fun RhythmNowPlayingItem.lyricsVariants(): List<CatalogLyricsVariant> = catalogLyricsVariants(
    lyrics = lyrics,
    lyricsLanguage = lyricsLanguage,
    translations = lyricsTranslations,
)

fun selectCatalogLyricsVariant(
    variants: List<CatalogLyricsVariant>,
    preferredLanguageTags: List<String>,
): CatalogLyricsVariant? {
    if (variants.isEmpty()) return null
    val normalizedVariants = variants.map { it to LanguageParts.from(it.language) }

    preferredLanguageTags.forEach { preferredTag ->
        val preferred = runCatching { LanguageParts.from(preferredTag) }.getOrNull()
            ?: return@forEach
        normalizedVariants.firstOrNull { (_, candidate) -> candidate.tag.equals(preferred.tag, true) }
            ?.let { return it.first }
        preferred.script?.let { script ->
            normalizedVariants.firstOrNull { (_, candidate) ->
                candidate.language == preferred.language && candidate.script == script
            }?.let { return it.first }
        }
        normalizedVariants.firstOrNull { (_, candidate) -> candidate.language == preferred.language }
            ?.let { return it.first }
    }
    return variants.first()
}

private fun catalogLyricsVariants(
    lyrics: String?,
    lyricsLanguage: String?,
    translations: List<CatalogLyricsTranslation>?,
): List<CatalogLyricsVariant> = buildList {
    lyrics?.takeIf(String::isNotBlank)?.let { primary ->
        val language = runCatching {
            normalizeCatalogLyricsLanguageTag(lyricsLanguage ?: "und")
        }.getOrDefault("und")
        add(CatalogLyricsVariant(language, primary))
    }
    translations.orEmpty().forEach { translation ->
        val language = runCatching { normalizeCatalogLyricsLanguageTag(translation.language) }.getOrNull()
            ?: return@forEach
        if (translation.lyrics.isNotBlank() && none { it.language.equals(language, true) }) {
            add(CatalogLyricsVariant(language, translation.lyrics))
        }
    }
}

private data class LanguageParts(
    val tag: String,
    val language: String,
    val script: String?,
) {
    companion object {
        fun from(value: String): LanguageParts {
            val tag = normalizeCatalogLyricsLanguageTag(value)
            val locale = Locale.forLanguageTag(tag)
            val language = locale.language.lowercase(Locale.ROOT)
            require(language.isNotEmpty()) { "lyrics language is invalid" }
            val script = locale.script.takeIf(String::isNotEmpty)
                ?: inferredChineseScript(language, locale.country)
            return LanguageParts(tag, language, script?.lowercase(Locale.ROOT))
        }

        private fun inferredChineseScript(language: String, country: String): String? {
            if (language != "zh") return null
            return when (country.uppercase(Locale.ROOT)) {
                "CN", "SG", "MY" -> "hans"
                "TW", "HK", "MO" -> "hant"
                else -> null
            }
        }
    }
}
