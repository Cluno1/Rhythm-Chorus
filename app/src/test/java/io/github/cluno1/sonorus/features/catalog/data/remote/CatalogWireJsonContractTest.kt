package io.github.cluno1.sonorus.features.catalog.data.remote

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogWireJsonContractTest {
    private val gson = GsonBuilder().create()

    @Test
    fun parsesProductionLibrarySongShapeIncludingSameNameFields() {
        val page = gson.fromJson(
            """
            {
              "items": [{
                "work_id": "11111111-1111-4111-8111-111111111111",
                "arrangement_id": "22222222-2222-4222-8222-222222222222",
                "rendition_id": "33333333-3333-4333-8333-333333333333",
                "rendition_revision": 7,
                "album_id": "44444444-4444-4444-8444-444444444444",
                "title": "需要人陪",
                "artist": "Test Artist",
                "album_title": "Test Album",
                "duration_ms": 123456,
                "track_no": 1,
                "cover_url": null,
                "lyrics": "Test lyrics",
                "lyrics_language": "zh-Hans",
                "lyrics_translations": [{"language": "en", "lyrics": "Translation"}],
                "cover_asset_id": null,
                "lyrics_source_images": [],
                "lyric_source_count": 0,
                "lyrics_formats": [{"language": "zh-Hans", "format": "plain"}]
              }],
              "next_cursor": "next-page"
            }
            """.trimIndent(),
            LibrarySongPageDto::class.java,
        )

        assertEquals("next-page", page.nextCursor)
        assertEquals(1, page.items?.size)
        val song = checkNotNull(page.items?.single())
        assertEquals("需要人陪", song.title)
        assertEquals("Test Artist", song.artist)
        assertEquals("Test lyrics", song.lyrics)
        assertEquals("en", song.lyricsTranslations?.single()?.language)
        assertEquals("plain", song.lyricsFormats?.single()?.format)

        val mapped = CatalogDtoMapper.librarySongs(page).first.single()
        assertEquals("需要人陪", mapped.title)
        assertEquals(7, mapped.renditionRevision)
    }

    @Test
    fun parsesNestedChorusShapeIncludingSameNameFields() {
        val chorus = gson.fromJson(
            """
            {
              "work_id": "11111111-1111-4111-8111-111111111111",
              "projects": [{
                "id": "22222222-2222-4222-8222-222222222222",
                "work_id": "11111111-1111-4111-8111-111111111111",
                "arrangement_id": "33333333-3333-4333-8333-333333333333",
                "score_id": "44444444-4444-4444-8444-444444444444",
                "alignment_score_revision_id": "55555555-5555-4555-8555-555555555555",
                "timeline_hash": "${"a".repeat(64)}",
                "title": "SATB",
                "status": "active",
                "revision": 2,
                "parts": [{"id": "66666666-6666-4666-8666-666666666666", "code": "S", "name": "Soprano", "display_order": 1}],
                "timelines": [],
                "tracks": []
              }]
            }
            """.trimIndent(),
            WorkChorusDto::class.java,
        )

        assertEquals(1, chorus.projects?.size)
        val project = checkNotNull(chorus.projects?.single())
        assertEquals("SATB", project.title)
        assertEquals("active", project.status)
        assertEquals("Soprano", project.parts?.single()?.name)
    }

    @Test
    fun serializesSameNameWriteFieldsWithProtocolNames() {
        val lyricKeys = gson.toJsonTree(
            RenditionLyricReplaceDto(lyrics = "text", format = "plain"),
        ).asJsonObject.keySet()
        assertEquals(setOf("lyrics", "format"), lyricKeys)

        val anchorKeys = gson.toJsonTree(
            ChorusSyncAnchorDto(anchorOrder = 0, scoreTick = 10, mediaMs = 20),
        ).asJsonObject.keySet()
        assertTrue(anchorKeys.containsAll(setOf("anchor_order", "score_tick", "media_ms", "confidence", "source")))
    }
}
