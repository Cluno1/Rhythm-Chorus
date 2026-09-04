package chromahub.rhythm.app.features.local.presentation.screens

import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumDetailActionPolicyTest {
    @Test
    fun catalogAlbumDoesNotExposeSongMoreAction() {
        assertNull(albumSongMoreAction(allowSongOptions = false) {})
    }

    @Test
    fun localAlbumKeepsSongMoreAction() {
        var invoked = false
        val action = albumSongMoreAction(allowSongOptions = true) { invoked = true }

        assertNotNull(action)
        action?.invoke()
        assertTrue(invoked)
    }
}
