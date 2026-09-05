package io.github.cluno1.sonorus.features.local.presentation.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeAlbumQueueProjectionTest {
    @Test
    fun newReleaseQueueUsesProjectedAlbumSongsAndDeduplicatesThem() {
        val result = flattenDistinctHomeAlbumSongs(
            albumSongs = listOf(listOf("song-a", "song-b"), listOf("song-b", "song-c")),
            identity = { it },
        )

        assertEquals(listOf("song-a", "song-b", "song-c"), result)
    }
}
