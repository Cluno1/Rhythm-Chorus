package io.github.cluno1.sonorus.features.catalog.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

class ChorusDtoMapperTest {
    private val workId = "00000000-0000-4000-8000-000000000001"
    private val projectId = "00000000-0000-4000-8000-000000000002"
    private val arrangementId = "00000000-0000-4000-8000-000000000003"
    private val revisionId = "00000000-0000-4000-8000-000000000004"
    private val scoreId = "00000000-0000-4000-8000-000000000008"
    private val timelineId = "00000000-0000-4000-8000-000000000009"
    private val partId = "00000000-0000-4000-8000-000000000005"
    private val trackId = "00000000-0000-4000-8000-000000000006"
    private val renditionId = "00000000-0000-4000-8000-000000000007"

    @Test
    fun projectMapsOwnedPublishedTrackAndTimingAnchors() {
        val project = ChorusDtoMapper.project(projectDto())

        assertEquals(projectId, project.id)
        assertEquals(scoreId, project.scoreId)
        assertEquals(timelineId, project.timelines.single().id)
        assertEquals("Soprano", project.parts.single().name)
        assertEquals(trackId, project.tracks.single().id)
        assertEquals(timelineId, project.tracks.single().chorusTimelineId)
        assertEquals(960L, project.tracks.single().anchors.last().scoreTick)
        assertTrue(project.tracks.single().ownedByRequester)
    }

    @Test
    fun catalogRejectsProjectFromAnotherWork() {
        val otherWork = "00000000-0000-4000-8000-000000000099"
        assertThrows(IllegalArgumentException::class.java) {
            ChorusDtoMapper.catalog(WorkChorusDto(otherWork, listOf(projectDto())))
        }
    }

    @Test
    fun chorusApiExposesOnlyTheNarrowResourceVerbs() {
        val methods = CatalogChorusApi::class.java.declaredMethods
            .filterNot { it.name.endsWith("\$default") }
        assertTrue(methods.any { it.getAnnotation(GET::class.java) != null })
        assertTrue(methods.any { it.getAnnotation(POST::class.java) != null })
        assertTrue(methods.any { it.getAnnotation(PATCH::class.java) != null })
        assertTrue(methods.any { it.getAnnotation(PUT::class.java) != null })
        assertTrue(methods.any { it.getAnnotation(DELETE::class.java) != null })
    }

    private fun projectDto() = ChorusProjectDto(
        id = projectId,
        workId = workId,
        arrangementId = arrangementId,
        scoreId = scoreId,
        alignmentScoreRevisionId = revisionId,
        timelineHash = "a".repeat(64),
        title = "Community chorus",
        status = "open",
        revision = 1,
        parts = listOf(ChorusPartDto(partId, "S", "Soprano", 1)),
        timelines = listOf(
            ChorusTimelineDto(
                id = timelineId,
                chorusProjectId = projectId,
                scoreRevisionId = revisionId,
                timelineHash = "a".repeat(64),
                revision = 1,
            ),
        ),
        tracks = listOf(
            ChorusTrackDto(
                id = trackId,
                chorusProjectId = projectId,
                chorusTimelineId = timelineId,
                renditionId = renditionId,
                uploaderDisplayName = "Singer",
                ownedByRequester = true,
                partId = partId,
                contributionKind = "vocal_part",
                displayLabel = "Take 1",
                takeNo = 1,
                alignmentState = "verified",
                alignmentOffsetMs = -10,
                status = "published",
                gainDb = 0.0,
                pan = 0.0,
                durationMs = 2_000,
                waveformPeaks = listOf(0.1f, 0.8f),
                rejectionReason = null,
                revision = 3,
                anchors = listOf(
                    ChorusSyncAnchorDto(0, 0, 0),
                    ChorusSyncAnchorDto(1, 960, 2_000),
                ),
            ),
        ),
    )
}
