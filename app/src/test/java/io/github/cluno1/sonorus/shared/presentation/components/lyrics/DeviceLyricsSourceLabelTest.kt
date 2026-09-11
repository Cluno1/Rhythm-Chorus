package io.github.cluno1.sonorus.shared.presentation.components.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceLyricsSourceLabelTest {
    @Test
    fun readsManagedMusicLibraryLanguageWithoutExposingInternalName() {
        assertEquals("en", managedLibraryLanguage("RHYTHM_LIBRARY|en"))
        assertEquals("en", managedLibraryLanguage("Catalog · en"))
        assertEquals("und", managedLibraryLanguage("Catalog"))
        assertNull(managedLibraryLanguage("LRCLib"))
    }
}
