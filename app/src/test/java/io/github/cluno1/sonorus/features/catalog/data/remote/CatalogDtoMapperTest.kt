package io.github.cluno1.sonorus.features.catalog.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogDtoMapperTest {
    private val workId = "11111111-1111-4111-8111-111111111111"
    private val arrangementId = "22222222-2222-4222-8222-222222222222"
    private val renditionId = "33333333-3333-4333-8333-333333333333"
    private val renditionAssetId = "44444444-4444-4444-8444-444444444444"
    private val assetId = "55555555-5555-4555-8555-555555555555"
    private val hash = "a".repeat(64)

    @Test
    fun mapsAndValidatesPlaybackIdentity() {
        val result = CatalogDtoMapper.playback(
            PlaybackDto(
                renditionId, assetId, "audio/mpeg", 12, "authenticated_url",
                "/v2/assets/$assetId/content", "rhythm:asset:$assetId:$hash", "\"sha256:$hash\"", true, null,
            ),
        )
        assertEquals(assetId, result.assetId)
        assertEquals("rhythm:asset:$assetId:$hash", result.cacheKey)
    }

    @Test
    fun rejectsMidiPlaybackDescriptor() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDtoMapper.playback(
                PlaybackDto(
                    renditionId, assetId, "audio/midi", 12, "authenticated_url",
                    "/v2/assets/$assetId/content", "rhythm:asset:$assetId:$hash", "etag", true, null,
                ),
            )
        }
    }

    @Test
    fun rejectsPlaybackCacheKeyForAnotherAsset() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDtoMapper.playback(
                PlaybackDto(
                    renditionId, assetId, "audio/mpeg", 12, "authenticated_url",
                    "/v2/assets/$assetId/content",
                    "rhythm:asset:66666666-6666-4666-8666-666666666666:$hash", "etag", true, null,
                ),
            )
        }
    }

    @Test
    fun rejectsCrossArrangementRelationships() {
        val work = WorkDto(workId, "Title", null, "active", 1, emptyList(), emptyList(), "now", "now")
        val rendition = RenditionDto(
            renditionId, "77777777-7777-4777-8777-777777777777", "Recording", "audio", null, null, null,
            1000, 1,
            listOf(RenditionAssetDto(renditionAssetId, assetId, "stream", null, null, hash, 12, "audio/mpeg")),
        )
        val arrangement = ArrangementDto(
            arrangementId, workId, "Main", null, null, null, null, 1,
            emptyList(), emptyList(), listOf(rendition),
        )
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDtoMapper.bundle(WorkBundleDto(work, listOf(arrangement), 1))
        }
    }
    @Test
    fun mapsSignedUrlDelivery() {
        val cosUrl =
            "https://bible-1328751369.cos.ap-guangzhou.myqcloud.com/music/221.mp3?q-signature=x"
        val result = CatalogDtoMapper.playback(
            PlaybackDto(
                renditionId, assetId, "audio/mpeg", 12, "signed_url",
                cosUrl, "rhythm:asset:$assetId:$hash", "\"sha256:$hash\"", true,
                "2026-09-04T10:00:00Z",
            ),
        )
        assertEquals("signed_url", result.delivery)
        assertEquals(cosUrl, result.relativeUrl)
    }

    @Test
    fun rejectsUnknownDeliveryMode() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDtoMapper.playback(
                PlaybackDto(
                    renditionId, assetId, "audio/mpeg", 12, "public_link",
                    "/v2/assets/$assetId/content", "rhythm:asset:$assetId:$hash", "etag", true, null,
                ),
            )
        }
    }

    @Test
    fun mapsSingleIhopeAlbumWithSeventyThreeSongs() {
        val albumId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        val songs = (1..73).map { index ->
            LibrarySongDto(
                workId = java.util.UUID.nameUUIDFromBytes("work-$index".toByteArray()).toString(),
                arrangementId = java.util.UUID.nameUUIDFromBytes("arrangement-$index".toByteArray()).toString(),
                renditionId = java.util.UUID.nameUUIDFromBytes("rendition-$index".toByteArray()).toString(),
                albumId = albumId,
                title = "Song $index",
                artist = null,
                albumTitle = "ihope",
                durationMs = index * 1_000L,
                trackNo = index,
                coverUrl = null,
                lyrics = null,
            )
        }
        val result = CatalogDtoMapper.libraryAlbumDetail(
            LibraryAlbumDetailDto(
                album = LibraryAlbumDto(albumId, "ihope", "ihope", null, null, 73),
                songs = songs,
            )
        )
        assertEquals("ihope", result.key)
        assertEquals(73, result.songs.size)
        assertNull(result.songs.first().artist)
    }

    @Test
    fun rejectsAlbumDetailWithWrongSongCount() {
        val albumId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDtoMapper.libraryAlbumDetail(
                LibraryAlbumDetailDto(
                    album = LibraryAlbumDto(albumId, "ihope", "ihope", null, null, 73),
                    songs = emptyList(),
                )
            )
        }
    }

    @Test
    fun mapsSignedMusicXmlAssetDelivery() {
        val url = "https://music.cos.ap-guangzhou.myqcloud.com/scores/a.musicxml" +
            "?q-sign-algorithm=sha1&q-signature=fresh"
        val result = CatalogDtoMapper.assetDelivery(
            AssetDeliveryDto(
                assetId = assetId,
                mediaType = "application/vnd.recordare.musicxml+xml",
                byteSize = 123,
                sha256 = hash,
                delivery = "signed_url",
                url = url,
                cacheKey = "rhythm:asset:$assetId:$hash",
                etag = "\"sha256:$hash\"",
                supportsRange = true,
                expiresAt = "2026-09-05T12:00:00Z",
            )
        )
        assertEquals("signed_url", result.delivery)
        assertEquals(hash, result.sha256)
    }

    @Test
    fun acceptsNullableLibraryDurationAndRejectsNegativeDuration() {
        val albumId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        val base = LibrarySongDto(
            workId = workId,
            arrangementId = arrangementId,
            renditionId = renditionId,
            albumId = albumId,
            title = "Unknown duration",
            artist = null,
            albumTitle = "ihope",
            durationMs = null,
            trackNo = null,
            coverUrl = null,
            lyrics = null,
        )

        assertNull(CatalogDtoMapper.librarySongs(LibrarySongPageDto(listOf(base), null)).first.single().durationMs)
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDtoMapper.librarySongs(LibrarySongPageDto(listOf(base.copy(durationMs = -1)), null))
        }
    }

    @Test
    fun mapsAllLocalizedLibraryLyrics() {
        val song = LibrarySongDto(
            workId = workId,
            arrangementId = arrangementId,
            renditionId = renditionId,
            albumId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
            title = "Localized song",
            artist = "Artist",
            albumTitle = "Album",
            durationMs = 1_000,
            trackNo = 1,
            coverUrl = null,
            lyrics = "Amazing grace",
            lyricsLanguage = "en",
            lyricsTranslations = listOf(
                LyricsTranslationDto("zh-hans", "奇异恩典"),
                LyricsTranslationDto("zh-Hant", "奇異恩典"),
            ),
        )

        val mapped = CatalogDtoMapper.librarySongs(
            LibrarySongPageDto(listOf(song), null),
        ).first.single()

        assertEquals("en", mapped.lyricsLanguage)
        assertEquals(listOf("zh-Hans", "zh-Hant"), mapped.lyricsTranslations?.map { it.language })
        assertEquals(listOf("奇异恩典", "奇異恩典"), mapped.lyricsTranslations?.map { it.lyrics })
    }

    @Test
    fun mapsStableSharedLyricSourceImageIdentity() {
        val pageId = "66666666-6666-4666-8666-666666666666"
        val documentId = "77777777-7777-4777-8777-777777777777"
        val linkId = "88888888-8888-4888-8888-888888888888"
        val song = LibrarySongDto(
            workId = workId,
            arrangementId = arrangementId,
            renditionId = renditionId,
            albumId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
            title = "Source image song",
            artist = null,
            albumTitle = "Album",
            durationMs = null,
            trackNo = null,
            coverUrl = null,
            lyrics = null,
            lyricsSourceImages = listOf(
                LyricSourceImageDto(
                    linkId = linkId,
                    sourcePageId = pageId,
                    imageAssetId = assetId,
                    documentId = documentId,
                    documentTitle = "IHOP Songbook 2024",
                    sourceKind = "pdf",
                    sourceRef = "2024-IHOP-Songbook.pdf",
                    physicalPageNumber = 20,
                    displayLabel = "PDF page 20",
                    displayOrder = 1,
                    widthPx = 1200,
                    heightPx = 1800,
                    renderDpi = 144,
                    ownerType = "work",
                    ownerId = workId,
                    languageRelations = listOf(
                        LyricSourceLanguageRelationDto("zh-Hans", "printed", null),
                    ),
                    note = null,
                ),
            ),
            lyricSourceCount = 1,
        )

        val source = CatalogDtoMapper.librarySongs(
            LibrarySongPageDto(listOf(song), null),
        ).first.single().lyricsSourceImages.orEmpty().single()

        assertEquals(pageId, source.sourcePageId)
        assertEquals(assetId, source.imageAssetId)
        assertEquals("zh-Hans", source.languageRelations.single().language)
        assertEquals(20, source.physicalPageNumber)
    }

    @Test
    fun rejectsDuplicateLocalizedLibraryLyrics() {
        val song = LibrarySongDto(
            workId = workId,
            arrangementId = arrangementId,
            renditionId = renditionId,
            albumId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
            title = "Localized song",
            artist = null,
            albumTitle = "Album",
            durationMs = null,
            trackNo = null,
            coverUrl = null,
            lyrics = "Default",
            lyricsLanguage = "zh-Hans",
            lyricsTranslations = listOf(LyricsTranslationDto("zh-hans", "Duplicate")),
        )

        assertThrows(IllegalArgumentException::class.java) {
            CatalogDtoMapper.librarySongs(LibrarySongPageDto(listOf(song), null))
        }
    }

    @Test
    fun rejectsNonMusicXmlAssetDeliveryMime() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogDtoMapper.assetDelivery(
                AssetDeliveryDto(
                    assetId = assetId,
                    mediaType = "text/html",
                    byteSize = 123,
                    sha256 = hash,
                    delivery = "authenticated_url",
                    url = "/v2/assets/$assetId/content",
                    cacheKey = "rhythm:asset:$assetId:$hash",
                    etag = "etag",
                    supportsRange = true,
                    expiresAt = null,
                )
            )
        }
    }
}
