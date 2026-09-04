package chromahub.rhythm.app.features.catalog.domain

data class WorkSummary(
    val id: String,
    val canonicalTitle: String,
    val language: String?,
    val status: String,
    val revision: Int,
    val aliases: List<WorkAlias>,
    val credits: List<WorkCredit>,
    val createdAt: String,
    val updatedAt: String,
)

data class WorkAlias(val namespace: String, val externalId: String)

data class WorkCredit(
    val id: String,
    val contributorId: String,
    val displayName: String,
    val role: String,
    val position: Int,
)

data class WorkBundle(
    val work: WorkSummary,
    val arrangements: List<Arrangement>,
    val bundleVersion: Int,
)

data class Arrangement(
    val id: String,
    val workId: String,
    val name: String,
    val voicing: String?,
    val keySignature: String?,
    val basedOnId: String?,
    val preferredScoreId: String?,
    val revision: Int,
    val parts: List<Part>,
    val scores: List<Score>,
    val renditions: List<Rendition>,
)

data class Part(
    val id: String,
    val code: String,
    val name: String,
    val displayOrder: Int,
    val midiChannel: Int?,
)

data class Score(
    val id: String,
    val arrangementId: String,
    val label: String,
    val origin: String,
    val derivedFromRevisionId: String?,
    val headRevisionId: String?,
    val publishedRevisionId: String?,
    val revision: Int,
)

data class ScoreRevision(
    val id: String,
    val scoreId: String,
    val revisionNo: Int,
    val basedOnRevisionId: String?,
    val editMessage: String?,
    val assets: List<ScoreRevisionAsset>,
    val createdAt: String,
) {
    val primaryMusicXml: ScoreRevisionAsset?
        get() = assets.singleOrNull { it.role == "primary_musicxml" }
}

data class ScoreRevisionAsset(
    val assetId: String,
    val role: String,
    val sha256: String,
    val byteSize: Long,
    val mediaType: String,
)

data class Rendition(
    val id: String,
    val arrangementId: String,
    val label: String,
    val kind: String,
    val ensemble: String?,
    val recordedAt: String?,
    val location: String?,
    val durationMs: Long?,
    val revision: Int,
    val assets: List<RenditionAsset>,
)

data class RenditionAsset(
    val id: String,
    val assetId: String,
    val role: String,
    val partId: String?,
    val codecProfile: String?,
    val sha256: String,
    val byteSize: Long,
    val mediaType: String,
)

data class PlaybackDescriptor(
    val renditionId: String,
    val assetId: String,
    val mediaType: String,
    val byteSize: Long,
    val delivery: String,
    val relativeUrl: String,
    val cacheKey: String,
    val etag: String,
    val supportsRange: Boolean,
    val expiresAt: String?,
)

data class AssetDeliveryDescriptor(
    val assetId: String,
    val mediaType: String,
    val byteSize: Long,
    val sha256: String,
    val delivery: String,
    val relativeUrl: String,
    val cacheKey: String,
    val etag: String,
    val supportsRange: Boolean,
    val expiresAt: String?,
)

data class CatalogPage(
    val items: List<WorkSummary>,
    val nextCursor: String?,
    val fromCache: Boolean = false,
)

data class CatalogChange(
    val sequence: Long,
    val entityType: String,
    val entityId: String,
    val entityRevision: Int,
    val operation: String,
    val workIds: List<String>,
    val tombstone: Boolean,
    val createdAt: String,
)

data class CatalogChanges(
    val changes: List<CatalogChange>,
    val nextCursor: Long,
    val hasMore: Boolean,
)

/** A real-audio rendition projected by the server into the native Songs library. */
data class CatalogLibrarySong(
    val workId: String,
    val arrangementId: String,
    val renditionId: String,
    val albumId: String,
    val title: String,
    val artist: String?,
    val albumTitle: String,
    val durationMs: Long?,
    val trackNo: Int?,
    val coverUrl: String?,
    val lyrics: String?,
)

/** A server-owned album/release. Work remains hidden from the native browsing surface. */
data class CatalogLibraryAlbum(
    val id: String,
    val key: String,
    val title: String,
    val artist: String?,
    val coverUrl: String?,
    val songCount: Int,
    val songs: List<CatalogLibrarySong> = emptyList(),
)

data class CatalogLibrarySnapshot(
    val songs: List<CatalogLibrarySong>,
    val albums: List<CatalogLibraryAlbum>,
    val fromCache: Boolean = false,
)

/** Stable projections consumed by Rhythm UI without turning catalog entities into legacy Songs. */
data class RhythmBrowseItem(
    val workId: String,
    val title: String,
    val subtitle: String,
)

data class RhythmNowPlayingItem(
    val workId: String,
    val arrangementId: String,
    val renditionId: String,
    val assetId: String?,
    val title: String,
    val subtitle: String,
    val lyrics: String? = null,
)

data class RhythmQueueEntry(
    val nowPlaying: RhythmNowPlayingItem,
    val playback: CatalogPlaybackItem,
)
