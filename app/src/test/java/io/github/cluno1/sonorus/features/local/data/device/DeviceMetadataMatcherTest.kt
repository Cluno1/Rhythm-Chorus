package io.github.cluno1.sonorus.features.local.data.device

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceMetadataMatcherTest {
    private val teenagers = DeviceMatchInput("Teenagers", "My Chemical Romance", "The Black Parade", 161_000)

    @Test fun `exact metadata and duration is high confidence`() {
        assertTrue(DeviceMetadataMatcher.score(teenagers, "Teenagers", "My Chemical Romance", "The Black Parade", 161.2) > 0.95)
    }

    @Test fun `wrong live version and artist is rejected`() {
        assertTrue(DeviceMetadataMatcher.score(teenagers, "Teenagers (Live)", "Another Artist", "Live", 188.0) < DeviceMetadataRepository.MIN_AUTO_CONFIDENCE)
    }

    @Test fun `duration demotes an otherwise similar wrong recording`() {
        val right = DeviceMetadataMatcher.score(teenagers, "Teenagers", "My Chemical Romance", "The Black Parade", 161.0)
        val wrongDuration = DeviceMetadataMatcher.score(teenagers, "Teenagers", "My Chemical Romance", "The Black Parade", 218.0)
        assertTrue(right > wrongDuration)
        assertTrue(wrongDuration < DeviceMetadataRepository.MIN_AUTO_CONFIDENCE)
    }

    @Test fun `minor title decoration remains matchable`() {
        assertTrue(DeviceMetadataMatcher.score(teenagers, "Teenagers (Album Version)", "My Chemical Romance", "The Black Parade", 162.0) > 0.9)
    }

    @Test fun `ambiguous close candidates require explicit selection`() {
        assertTrue(!DeviceMetadataMatcher.isAutomaticMatch(0.91, 0.90))
        assertTrue(DeviceMetadataMatcher.isAutomaticMatch(0.91, 0.80))
    }

    @Test fun `first lyrics candidate at ninety eight percent is accepted regardless of runner up`() {
        assertTrue(
            DeviceMetadataMatcher.isAutomaticMatch(
                best = 0.98,
                runnerUp = 0.98,
                unconditionalThreshold = DeviceMetadataRepository.EXACT_AUTO_CONFIDENCE,
            )
        )
        assertTrue(
            !DeviceMetadataMatcher.isAutomaticMatch(
                best = 0.979,
                runnerUp = 0.97,
                unconditionalThreshold = DeviceMetadataRepository.EXACT_AUTO_CONFIDENCE,
            )
        )
    }

    @Test fun `traditional Chinese metadata remains comparable to simplified input`() {
        val input = DeviceMatchInput("李香兰", "张学友", "音乐之旅Live演唱会", 553_000)
        val live = DeviceMetadataMatcher.score(input, "李香蘭", "張學友", "音樂之旅Live演唱會", 553.2)
        val studio = DeviceMetadataMatcher.score(input, "李香蘭", "張學友", "吻別", 397.0)
        assertTrue(live > studio)
        assertTrue(
            "live=$live title=${DeviceMetadataMatcher.similarityForTesting(input.title, "李香蘭")} " +
                "artist=${DeviceMetadataMatcher.similarityForTesting(input.artist, "張學友")} " +
                "album=${DeviceMetadataMatcher.similarityForTesting(input.album, "音樂之旅Live演唱會")}",
            live >= 0.85
        )
        assertTrue(studio < DeviceMetadataRepository.MIN_AUTO_CONFIDENCE)
    }

    @Test fun `live and studio version conflict is penalized`() {
        val input = DeviceMatchInput("Song", "Artist", "Album Live", 240_000)
        val matching = DeviceMetadataMatcher.score(input, "Song", "Artist", "Album Live", 240.0)
        val studio = DeviceMetadataMatcher.score(input, "Song", "Artist", "Album", 240.0)
        assertTrue(matching > studio)
    }

    @Test fun `artist names ignore case accents and punctuation`() {
        assertEquals(1.0, DeviceMetadataMatcher.artistNameScore("Beyoncé", "BEYONCE"), 0.0)
        assertTrue(DeviceMetadataMatcher.artistNameScore("My Chemical Romance", "My-Chemical Romance") > 0.95)
        assertTrue(DeviceMetadataMatcher.artistNameScore("Artist A", "Different Artist") < 0.5)
    }
}
