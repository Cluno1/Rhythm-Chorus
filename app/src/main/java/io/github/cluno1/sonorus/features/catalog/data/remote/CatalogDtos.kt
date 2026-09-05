package io.github.cluno1.sonorus.features.catalog.data.remote

import com.google.gson.annotations.SerializedName

internal data class WorkPageDto(
    val items: List<WorkDto>?,
    @SerializedName("next_cursor") val nextCursor: String?,
)

internal data class LibrarySongPageDto(
    val items: List<LibrarySongDto>?,
    @SerializedName("next_cursor") val nextCursor: String?,
)

internal data class LibraryAlbumPageDto(
    val items: List<LibraryAlbumDto>?,
    @SerializedName("next_cursor") val nextCursor: String?,
)

internal data class LibraryAlbumDetailDto(
    val album: LibraryAlbumDto?,
    val songs: List<LibrarySongDto>?,
)

internal data class LibrarySongDto(
    @SerializedName("work_id") val workId: String?,
    @SerializedName("arrangement_id") val arrangementId: String?,
    @SerializedName("rendition_id") val renditionId: String?,
    @SerializedName("album_id") val albumId: String?,
    val title: String?,
    val artist: String?,
    @SerializedName("album_title") val albumTitle: String?,
    @SerializedName("duration_ms") val durationMs: Long?,
    @SerializedName("track_no") val trackNo: Int?,
    @SerializedName("cover_url") val coverUrl: String?,
    val lyrics: String?,
)

internal data class LibraryAlbumDto(
    val id: String?,
    val key: String?,
    val title: String?,
    val artist: String?,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("song_count") val songCount: Int?,
)

internal data class WorkAliasDto(val namespace: String?, @SerializedName("external_id") val externalId: String?)
internal data class WorkCreditDto(
    val id: String?,
    @SerializedName("contributor_id") val contributorId: String?,
    @SerializedName("display_name") val displayName: String?,
    val role: String?,
    val position: Int?,
)

internal data class WorkDto(
    val id: String?,
    @SerializedName("canonical_title") val canonicalTitle: String?,
    val language: String?,
    val status: String?,
    val revision: Int?,
    val aliases: List<WorkAliasDto>?,
    val credits: List<WorkCreditDto>?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
)

internal data class WorkBundleDto(
    val work: WorkDto?,
    val arrangements: List<ArrangementDto>?,
    @SerializedName("bundle_version") val bundleVersion: Int?,
)

internal data class ArrangementDto(
    val id: String?,
    @SerializedName("work_id") val workId: String?,
    val name: String?,
    val voicing: String?,
    @SerializedName("key_signature") val keySignature: String?,
    @SerializedName("based_on_id") val basedOnId: String?,
    @SerializedName("preferred_score_id") val preferredScoreId: String?,
    val revision: Int?,
    val parts: List<PartDto>?,
    val scores: List<ScoreDto>?,
    val renditions: List<RenditionDto>?,
)

internal data class PartDto(
    val id: String?, val code: String?, val name: String?,
    @SerializedName("display_order") val displayOrder: Int?,
    @SerializedName("midi_channel") val midiChannel: Int?,
)

internal data class ScoreDto(
    val id: String?,
    @SerializedName("arrangement_id") val arrangementId: String?,
    val label: String?, val origin: String?,
    @SerializedName("derived_from_revision_id") val derivedFromRevisionId: String?,
    @SerializedName("head_revision_id") val headRevisionId: String?,
    @SerializedName("published_revision_id") val publishedRevisionId: String?,
    val revision: Int?,
)

internal data class ScoreRevisionDto(
    val id: String?,
    @SerializedName("score_id") val scoreId: String?,
    @SerializedName("revision_no") val revisionNo: Int?,
    @SerializedName("based_on_revision_id") val basedOnRevisionId: String?,
    @SerializedName("edit_message") val editMessage: String?,
    val assets: List<ScoreAssetDto>?,
    @SerializedName("created_at") val createdAt: String?,
)

internal data class ScoreAssetDto(
    @SerializedName("asset_id") val assetId: String?, val role: String?, val sha256: String?,
    @SerializedName("byte_size") val byteSize: Long?,
    @SerializedName("media_type") val mediaType: String?,
)

internal data class RenditionDto(
    val id: String?,
    @SerializedName("arrangement_id") val arrangementId: String?,
    val label: String?, val kind: String?, val ensemble: String?,
    @SerializedName("recorded_at") val recordedAt: String?,
    val location: String?,
    @SerializedName("duration_ms") val durationMs: Long?,
    val revision: Int?, val assets: List<RenditionAssetDto>?,
)

internal data class RenditionAssetDto(
    val id: String?,
    @SerializedName("asset_id") val assetId: String?,
    val role: String?,
    @SerializedName("part_id") val partId: String?,
    @SerializedName("codec_profile") val codecProfile: String?,
    val sha256: String?,
    @SerializedName("byte_size") val byteSize: Long?,
    @SerializedName("media_type") val mediaType: String?,
)

internal data class PlaybackDto(
    @SerializedName("rendition_id") val renditionId: String?,
    @SerializedName("asset_id") val assetId: String?,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("byte_size") val byteSize: Long?,
    val delivery: String?, val url: String?,
    @SerializedName("cache_key") val cacheKey: String?,
    val etag: String?,
    @SerializedName("supports_range") val supportsRange: Boolean?,
    @SerializedName("expires_at") val expiresAt: String?,
)

internal data class AssetDeliveryDto(
    @SerializedName("asset_id") val assetId: String?,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("byte_size") val byteSize: Long?,
    val sha256: String?,
    val delivery: String?,
    val url: String?,
    @SerializedName("cache_key") val cacheKey: String?,
    val etag: String?,
    @SerializedName("supports_range") val supportsRange: Boolean?,
    @SerializedName("expires_at") val expiresAt: String?,
)

internal data class ChangesDto(
    val changes: List<ChangeDto>?,
    @SerializedName("next_cursor") val nextCursor: Long?,
    @SerializedName("has_more") val hasMore: Boolean?,
)

internal data class ChangeDto(
    val sequence: Long?,
    @SerializedName("entity_type") val entityType: String?,
    @SerializedName("entity_id") val entityId: String?,
    @SerializedName("entity_revision") val entityRevision: Int?,
    val operation: String?,
    @SerializedName("work_ids") val workIds: List<String>?,
    val tombstone: Boolean?,
    @SerializedName("created_at") val createdAt: String?,
)
