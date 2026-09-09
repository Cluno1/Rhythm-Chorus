package io.github.cluno1.sonorus.features.catalog.domain

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogLyricsTest {
    private val variants = listOf(
        CatalogLyricsVariant("en", "English"),
        CatalogLyricsVariant("zh-Hans", "简体"),
        CatalogLyricsVariant("zh-Hant", "繁體"),
    )

    @Test
    fun exactApplicationLanguageWinsOverBackendDefault() {
        assertEquals(
            "简体",
            selectCatalogLyricsVariant(variants, listOf("zh-Hans"))?.lyrics,
        )
    }

    @Test
    fun chineseRegionSelectsMatchingScript() {
        assertEquals(
            "简体",
            selectCatalogLyricsVariant(variants, listOf("zh-CN"))?.lyrics,
        )
        assertEquals(
            "繁體",
            selectCatalogLyricsVariant(variants, listOf("zh-TW"))?.lyrics,
        )
    }

    @Test
    fun unavailableApplicationLanguageFallsBackToBackendDefault() {
        assertEquals(
            "English",
            selectCatalogLyricsVariant(variants, listOf("de-DE"))?.lyrics,
        )
    }

    @Test
    fun oldQueuePayloadStillProducesDefaultLyrics() {
        val nowPlaying = Gson().fromJson(
            """{
                "workId":"work",
                "arrangementId":"arrangement",
                "renditionId":"rendition",
                "title":"Title",
                "subtitle":"Artist",
                "lyrics":"Legacy lyrics"
            }""".trimIndent(),
            RhythmNowPlayingItem::class.java,
        )

        assertEquals(
            listOf(CatalogLyricsVariant("und", "Legacy lyrics")),
            nowPlaying.lyricsVariants(),
        )
    }

    @Test
    fun queueJsonRoundTripKeepsEveryLyricsLanguage() {
        val gson = Gson()
        val original = RhythmNowPlayingItem(
            workId = "work",
            arrangementId = "arrangement",
            renditionId = "rendition",
            assetId = null,
            title = "Title",
            subtitle = "Artist",
            lyrics = "English",
            lyricsLanguage = "en",
            lyricsTranslations = listOf(
                CatalogLyricsTranslation("zh-Hans", "简体"),
                CatalogLyricsTranslation("zh-Hant", "繁體"),
            ),
        )

        val restored = gson.fromJson(gson.toJson(original), RhythmNowPlayingItem::class.java)

        assertEquals(variants, restored.lyricsVariants())
    }
}
