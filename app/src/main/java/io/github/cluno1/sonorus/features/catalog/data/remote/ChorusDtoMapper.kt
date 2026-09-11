package io.github.cluno1.sonorus.features.catalog.data.remote

import io.github.cluno1.sonorus.features.catalog.domain.ChorusCatalog
import io.github.cluno1.sonorus.features.catalog.domain.ChorusMix
import io.github.cluno1.sonorus.features.catalog.domain.ChorusPart
import io.github.cluno1.sonorus.features.catalog.domain.ChorusPlayback
import io.github.cluno1.sonorus.features.catalog.domain.ChorusProject
import io.github.cluno1.sonorus.features.catalog.domain.ChorusSyncAnchor
import io.github.cluno1.sonorus.features.catalog.domain.ChorusTrack
import java.util.UUID

internal object ChorusDtoMapper {
    private val hashPattern = Regex("^[0-9a-fA-F]{64}$")
    private val projectStates = setOf("draft", "open", "closed", "archived")
    private val trackStates = setOf(
        "draft", "processing", "pending_review", "published", "rejected", "withdrawn", "failed",
    )
    private val alignmentStates = setOf("pending", "automatic", "manual", "verified", "failed")

    fun catalog(dto: WorkChorusDto): ChorusCatalog {
        val workId = uuid(dto.workId, "chorus.work_id")
        return ChorusCatalog(
            workId = workId,
            projects = requireNotNull(dto.projects) { "chorus.projects is missing" }.map(::project)
                .also { projects ->
                    require(projects.all { it.workId == workId }) {
                        "chorus project does not belong to the Work"
                    }
                },
        )
    }

    fun project(dto: ChorusProjectDto): ChorusProject {
        val id = uuid(dto.id, "chorus_project.id")
        val arrangementId = uuid(dto.arrangementId, "chorus_project.arrangement_id")
        val tracks = requireNotNull(dto.tracks) { "chorus_project.tracks is missing" }.map(::track)
        require(tracks.all { it.chorusProjectId == id }) { "chorus track belongs to another project" }
        return ChorusProject(
            id = id,
            workId = uuid(dto.workId, "chorus_project.work_id"),
            arrangementId = arrangementId,
            alignmentScoreRevisionId = uuid(
                dto.alignmentScoreRevisionId,
                "chorus_project.alignment_score_revision_id",
            ),
            timelineHash = hash(dto.timelineHash, "chorus_project.timeline_hash"),
            title = text(dto.title, "chorus_project.title"),
            status = text(dto.status, "chorus_project.status").also {
                require(it in projectStates) { "chorus project status is invalid" }
            },
            revision = positive(dto.revision, "chorus_project.revision"),
            parts = requireNotNull(dto.parts) { "chorus_project.parts is missing" }.map { part ->
                ChorusPart(
                    id = uuid(part.id, "chorus_part.id"),
                    code = text(part.code, "chorus_part.code"),
                    name = text(part.name, "chorus_part.name"),
                    displayOrder = positive(part.displayOrder, "chorus_part.display_order"),
                )
            },
            tracks = tracks,
        )
    }

    fun track(dto: ChorusTrackDto): ChorusTrack = ChorusTrack(
        id = uuid(dto.id, "chorus_track.id"),
        chorusProjectId = uuid(dto.chorusProjectId, "chorus_track.chorus_project_id"),
        renditionId = uuid(dto.renditionId, "chorus_track.rendition_id"),
        uploaderDisplayName = text(dto.uploaderDisplayName, "chorus_track.uploader_display_name"),
        ownedByRequester = requireNotNull(dto.ownedByRequester) {
            "chorus_track.owned_by_requester is missing"
        },
        partId = dto.partId?.let { uuid(it, "chorus_track.part_id") },
        contributionKind = text(dto.contributionKind, "chorus_track.contribution_kind"),
        displayLabel = text(dto.displayLabel, "chorus_track.display_label"),
        takeNo = positive(dto.takeNo, "chorus_track.take_no"),
        alignmentState = text(dto.alignmentState, "chorus_track.alignment_state").also {
            require(it in alignmentStates) { "chorus track alignment state is invalid" }
        },
        alignmentOffsetMs = requireNotNull(dto.alignmentOffsetMs) {
            "chorus_track.alignment_offset_ms is missing"
        },
        status = text(dto.status, "chorus_track.status").also {
            require(it in trackStates) { "chorus track status is invalid" }
        },
        gainDb = requireNotNull(dto.gainDb) { "chorus_track.gain_db is missing" },
        pan = requireNotNull(dto.pan) { "chorus_track.pan is missing" },
        durationMs = dto.durationMs?.also { require(it > 0) },
        waveformPeaks = dto.waveformPeaks.orEmpty().also { peaks ->
            require(peaks.size <= 400 && peaks.all { it in 0f..1f }) {
                "chorus track waveform is invalid"
            }
        },
        rejectionReason = dto.rejectionReason,
        revision = positive(dto.revision, "chorus_track.revision"),
        anchors = requireNotNull(dto.anchors) { "chorus_track.anchors is missing" }.map {
            ChorusSyncAnchor(
                anchorOrder = it.anchorOrder,
                scoreTick = it.scoreTick,
                mediaMs = it.mediaMs,
                confidence = it.confidence,
                source = it.source,
            )
        },
    )

    fun mix(dto: ChorusMixDto): ChorusMix {
        val state = text(dto.state, "chorus_mix.state")
        require(state in setOf("queued", "processing", "ready", "failed", "obsolete")) {
            "chorus mix state is invalid"
        }
        val delivery = dto.delivery?.let { item ->
            val assetId = uuid(item.assetId, "chorus_mix.delivery.asset_id")
            val sha256 = hash(item.sha256, "chorus_mix.delivery.sha256")
            require(item.cacheKey == "rhythm:asset:$assetId:$sha256") {
                "chorus mix cache key does not match its Asset"
            }
            ChorusPlayback(
                assetId = assetId,
                mediaType = text(item.mediaType, "chorus_mix.delivery.media_type"),
                byteSize = requireNotNull(item.byteSize) { "chorus_mix.delivery.byte_size is missing" }
                    .also { require(it > 0) },
                sha256 = sha256,
                url = text(item.url, "chorus_mix.delivery.url"),
                cacheKey = item.cacheKey,
                expiresAt = item.expiresAt,
            )
        }
        require((state == "ready") == (delivery != null)) {
            "ready chorus mixes must have exactly one delivery"
        }
        val trackIds = requireNotNull(dto.selectedTrackIds) {
            "chorus_mix.selected_track_ids is missing"
        }.map { uuid(it, "chorus_mix.track_id") }
        require(trackIds.size == dto.selectedTrackCount && trackIds.isNotEmpty()) {
            "chorus mix selected track count is invalid"
        }
        return ChorusMix(
            id = uuid(dto.id, "chorus_mix.id"),
            chorusProjectId = uuid(dto.chorusProjectId, "chorus_mix.chorus_project_id"),
            selectionHash = hash(dto.selectionHash, "chorus_mix.selection_hash"),
            selectedTrackIds = trackIds,
            state = state,
            durationMs = dto.durationMs,
            errorSummary = dto.errorSummary,
            playback = delivery,
        )
    }

    private fun uuid(value: String?, field: String): String = runCatching {
        UUID.fromString(text(value, field)).toString()
    }.getOrElse { error("$field is not a UUID") }

    private fun hash(value: String?, field: String): String = text(value, field).lowercase().also {
        require(hashPattern.matches(it)) { "$field is not a SHA-256 hash" }
    }

    private fun text(value: String?, field: String): String = value?.trim()?.takeIf(String::isNotEmpty)
        ?: error("$field is missing")

    private fun positive(value: Int?, field: String): Int = requireNotNull(value) { "$field is missing" }
        .also { require(it > 0) { "$field must be positive" } }
}
