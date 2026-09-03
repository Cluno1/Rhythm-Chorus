package chromahub.rhythm.app.features.catalog.data.remote

import chromahub.rhythm.app.features.catalog.domain.Arrangement
import chromahub.rhythm.app.features.catalog.domain.CatalogChange
import chromahub.rhythm.app.features.catalog.domain.CatalogChanges
import chromahub.rhythm.app.features.catalog.domain.CatalogPage
import chromahub.rhythm.app.features.catalog.domain.Part
import chromahub.rhythm.app.features.catalog.domain.PlaybackDescriptor
import chromahub.rhythm.app.features.catalog.domain.Rendition
import chromahub.rhythm.app.features.catalog.domain.RenditionAsset
import chromahub.rhythm.app.features.catalog.domain.Score
import chromahub.rhythm.app.features.catalog.domain.ScoreRevision
import chromahub.rhythm.app.features.catalog.domain.ScoreRevisionAsset
import chromahub.rhythm.app.features.catalog.domain.WorkAlias
import chromahub.rhythm.app.features.catalog.domain.WorkBundle
import chromahub.rhythm.app.features.catalog.domain.WorkCredit
import chromahub.rhythm.app.features.catalog.domain.WorkSummary
import java.util.UUID

internal object CatalogDtoMapper {
    private val sha256 = Regex("^[0-9a-fA-F]{64}$")

    fun page(dto: WorkPageDto): CatalogPage = CatalogPage(
        items = dto.items.required("items").map(::work),
        nextCursor = dto.nextCursor,
    )

    fun work(dto: WorkDto): WorkSummary = WorkSummary(
        id = uuid(dto.id, "work.id"),
        canonicalTitle = text(dto.canonicalTitle, "work.canonical_title"),
        language = dto.language,
        status = text(dto.status, "work.status"),
        revision = positive(dto.revision, "work.revision"),
        aliases = dto.aliases.required("work.aliases").map {
            WorkAlias(text(it.namespace, "alias.namespace"), text(it.externalId, "alias.external_id"))
        },
        credits = dto.credits.required("work.credits").map {
            WorkCredit(
                id = uuid(it.id, "credit.id"),
                contributorId = uuid(it.contributorId, "credit.contributor_id"),
                displayName = text(it.displayName, "credit.display_name"),
                role = text(it.role, "credit.role"),
                position = positive(it.position, "credit.position"),
            )
        },
        createdAt = text(dto.createdAt, "work.created_at"),
        updatedAt = text(dto.updatedAt, "work.updated_at"),
    )

    fun bundle(dto: WorkBundleDto): WorkBundle {
        val mappedWork = work(dto.work.required("work"))
        val arrangements = dto.arrangements.required("arrangements").map { arrangementDto ->
            val arrangement = Arrangement(
                id = uuid(arrangementDto.id, "arrangement.id"),
                workId = uuid(arrangementDto.workId, "arrangement.work_id"),
                name = text(arrangementDto.name, "arrangement.name"),
                voicing = arrangementDto.voicing,
                keySignature = arrangementDto.keySignature,
                basedOnId = optionalUuid(arrangementDto.basedOnId, "arrangement.based_on_id"),
                preferredScoreId = optionalUuid(arrangementDto.preferredScoreId, "arrangement.preferred_score_id"),
                revision = positive(arrangementDto.revision, "arrangement.revision"),
                parts = arrangementDto.parts.required("arrangement.parts").map(::part),
                scores = arrangementDto.scores.required("arrangement.scores").map(::score),
                renditions = arrangementDto.renditions.required("arrangement.renditions").map(::rendition),
            )
            require(arrangement.workId == mappedWork.id) { "arrangement does not belong to work" }
            require(arrangement.scores.all { it.arrangementId == arrangement.id }) { "score does not belong to arrangement" }
            require(arrangement.renditions.all { it.arrangementId == arrangement.id }) { "rendition does not belong to arrangement" }
            val partIds = arrangement.parts.map { it.id }.toSet()
            require(arrangement.renditions.flatMap { it.assets }.all { it.partId == null || it.partId in partIds }) {
                "rendition asset refers to an unknown part"
            }
            arrangement
        }
        return WorkBundle(mappedWork, arrangements, nonNegative(dto.bundleVersion, "bundle_version"))
    }

    fun scoreRevision(dto: ScoreRevisionDto): ScoreRevision {
        val result = ScoreRevision(
            id = uuid(dto.id, "score_revision.id"),
            scoreId = uuid(dto.scoreId, "score_revision.score_id"),
            revisionNo = positive(dto.revisionNo, "score_revision.revision_no"),
            basedOnRevisionId = optionalUuid(dto.basedOnRevisionId, "score_revision.based_on_revision_id"),
            editMessage = dto.editMessage,
            assets = dto.assets.required("score_revision.assets").map {
                ScoreRevisionAsset(
                    assetId = uuid(it.assetId, "score_asset.asset_id"),
                    role = text(it.role, "score_asset.role"),
                    sha256 = hash(it.sha256, "score_asset.sha256"),
                    byteSize = positiveLong(it.byteSize, "score_asset.byte_size"),
                    mediaType = text(it.mediaType, "score_asset.media_type"),
                )
            },
            createdAt = text(dto.createdAt, "score_revision.created_at"),
        )
        require(result.assets.count { it.role == "primary_musicxml" } == 1) {
            "score revision must contain exactly one primary_musicxml"
        }
        return result
    }

    fun playback(dto: PlaybackDto): PlaybackDescriptor {
        val assetId = uuid(dto.assetId, "playback.asset_id")
        val normalizedHash = dto.cacheKey?.substringAfterLast(':')?.let { hash(it, "playback.cache_key hash") }
            ?: error("playback.cache_key is missing")
        val expectedCacheKey = "rhythm:asset:$assetId:$normalizedHash"
        require(dto.cacheKey == expectedCacheKey) { "playback.cache_key does not match asset identity" }
        return PlaybackDescriptor(
            renditionId = uuid(dto.renditionId, "playback.rendition_id"),
            assetId = assetId,
            mediaType = text(dto.mediaType, "playback.media_type"),
            byteSize = positiveLong(dto.byteSize, "playback.byte_size"),
            delivery = text(dto.delivery, "playback.delivery"),
            relativeUrl = text(dto.url, "playback.url"),
            cacheKey = expectedCacheKey,
            etag = text(dto.etag, "playback.etag"),
            supportsRange = dto.supportsRange.required("playback.supports_range"),
            expiresAt = dto.expiresAt,
        )
    }

    fun changes(dto: ChangesDto): CatalogChanges = CatalogChanges(
        changes = dto.changes.required("changes").map {
            CatalogChange(
                sequence = positiveLong(it.sequence, "change.sequence"),
                entityType = text(it.entityType, "change.entity_type"),
                entityId = uuid(it.entityId, "change.entity_id"),
                entityRevision = nonNegative(it.entityRevision, "change.entity_revision"),
                operation = text(it.operation, "change.operation"),
                workIds = it.workIds.required("change.work_ids").map { id -> uuid(id, "change.work_id") },
                tombstone = it.tombstone.required("change.tombstone"),
                createdAt = text(it.createdAt, "change.created_at"),
            )
        },
        nextCursor = nonNegativeLong(dto.nextCursor, "next_cursor"),
        hasMore = dto.hasMore.required("has_more"),
    )

    private fun part(dto: PartDto) = Part(
        uuid(dto.id, "part.id"), text(dto.code, "part.code"), text(dto.name, "part.name"),
        positive(dto.displayOrder, "part.display_order"), dto.midiChannel?.also { require(it in 1..16) },
    )

    private fun score(dto: ScoreDto) = Score(
        uuid(dto.id, "score.id"), uuid(dto.arrangementId, "score.arrangement_id"),
        text(dto.label, "score.label"), text(dto.origin, "score.origin"),
        optionalUuid(dto.derivedFromRevisionId, "score.derived_from_revision_id"),
        optionalUuid(dto.headRevisionId, "score.head_revision_id"),
        optionalUuid(dto.publishedRevisionId, "score.published_revision_id"),
        positive(dto.revision, "score.revision"),
    )

    private fun rendition(dto: RenditionDto) = Rendition(
        id = uuid(dto.id, "rendition.id"),
        arrangementId = uuid(dto.arrangementId, "rendition.arrangement_id"),
        label = text(dto.label, "rendition.label"), kind = text(dto.kind, "rendition.kind"),
        ensemble = dto.ensemble, recordedAt = dto.recordedAt, location = dto.location,
        durationMs = dto.durationMs?.also { require(it >= 0) },
        revision = positive(dto.revision, "rendition.revision"),
        assets = dto.assets.required("rendition.assets").map {
            RenditionAsset(
                id = uuid(it.id, "rendition_asset.id"),
                assetId = uuid(it.assetId, "rendition_asset.asset_id"),
                role = text(it.role, "rendition_asset.role"),
                partId = optionalUuid(it.partId, "rendition_asset.part_id"),
                codecProfile = it.codecProfile,
                sha256 = hash(it.sha256, "rendition_asset.sha256"),
                byteSize = positiveLong(it.byteSize, "rendition_asset.byte_size"),
                mediaType = text(it.mediaType, "rendition_asset.media_type"),
            )
        },
    )

    private fun uuid(value: String?, field: String): String = text(value, field).also {
        runCatching { UUID.fromString(it) }.getOrElse { throw IllegalArgumentException("$field is not a UUID") }
    }.lowercase()
    private fun optionalUuid(value: String?, field: String) = value?.let { uuid(it, field) }
    private fun hash(value: String?, field: String) = text(value, field).also {
        require(sha256.matches(it)) { "$field is not SHA-256" }
    }.lowercase()
    private fun text(value: String?, field: String) = value?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("$field is missing")
    private fun positive(value: Int?, field: String) = value.required(field).also { require(it > 0) { "$field must be positive" } }
    private fun nonNegative(value: Int?, field: String) = value.required(field).also { require(it >= 0) { "$field must not be negative" } }
    private fun positiveLong(value: Long?, field: String) = value.required(field).also { require(it > 0) { "$field must be positive" } }
    private fun nonNegativeLong(value: Long?, field: String) = value.required(field).also { require(it >= 0) { "$field must not be negative" } }
    private fun <T> T?.required(field: String): T = this ?: throw IllegalArgumentException("$field is missing")
}
