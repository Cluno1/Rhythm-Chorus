package chromahub.rhythm.app.features.scores.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScorePlaybackHighlighterTest {
    private data class TimedItem(val label: String, val start: Double)

    @Test
    fun closestPlaybackItem_acceptsMergedProjectionSnapWithinSixteenthGrid() {
        val items = listOf(
            TimedItem("first", 0.0),
            TimedItem("second", 960.0),
            TimedItem("third", 1920.0),
        )

        val result = findClosestPlaybackItem(items, 1080.0, TimedItem::start)

        assertEquals("second", result?.label)
    }

    @Test
    fun closestPlaybackItem_rejectsDistantNoteWhenPlaybackBeatIsARest() {
        val items = listOf(
            TimedItem("first", 0.0),
            TimedItem("last", 2880.0),
        )

        val result = findClosestPlaybackItem(items, 1440.0, TimedItem::start)

        assertNull(result)
    }
}
