package chromahub.rhythm.app.features.local.data.device

import org.junit.Assert.assertTrue
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
}
