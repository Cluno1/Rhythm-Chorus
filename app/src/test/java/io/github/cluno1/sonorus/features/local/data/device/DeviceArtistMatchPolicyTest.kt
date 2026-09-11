package io.github.cluno1.sonorus.features.local.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceArtistMatchPolicyTest {
    @Test fun `exact first candidate is accepted even when another exact row follows`() {
        assertEquals(
            0,
            DeviceArtistMatchPolicy.bestAutomaticIndex(
                "周杰伦",
                listOf("周杰伦", "周杰伦", "林俊杰"),
            ),
        )
    }

    @Test fun `accent-normalized exact match is accepted`() {
        assertEquals(
            1,
            DeviceArtistMatchPolicy.bestAutomaticIndex(
                "Beyoncé",
                listOf("Beyoncé Tribute", "Beyonce"),
            ),
        )
    }

    @Test fun `popular but unrelated fallback is rejected`() {
        assertNull(
            DeviceArtistMatchPolicy.bestAutomaticIndex(
                "Local Unknown Artist",
                listOf("Taylor Swift", "The Weeknd", "Various Artists"),
            )
        )
    }

    @Test fun `shared word does not make an artist automatic`() {
        assertNull(
            DeviceArtistMatchPolicy.bestAutomaticIndex(
                "The Local Band",
                listOf("The Band", "Local Natives"),
            )
        )
    }
}
