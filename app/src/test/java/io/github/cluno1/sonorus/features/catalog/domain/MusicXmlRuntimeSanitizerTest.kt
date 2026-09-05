package io.github.cluno1.sonorus.features.catalog.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicXmlRuntimeSanitizerTest {
    @Test
    fun stripsStandardExternalDoctypeOnlyFromRuntimeCopy() {
        val original = """<?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE score-partwise PUBLIC "-//Recordare//DTD MusicXML 4.0 Partwise//EN" "http://www.musicxml.org/dtds/partwise.dtd">
            <score-partwise version="4.0"><part-list/></score-partwise>
        """.trimIndent().toByteArray()
        val originalSnapshot = original.copyOf()

        val runtime = MusicXmlRuntimeSanitizer.forAlphaTab(original)

        assertTrue(original.contentEquals(originalSnapshot))
        assertTrue(runtime.toString(Charsets.UTF_8).contains("<score-partwise"))
        assertFalse(runtime.toString(Charsets.UTF_8).contains("<!DOCTYPE"))
    }

    @Test
    fun rejectsEntityDeclarationsInsteadOfExpandingThem() {
        val malicious = """<!DOCTYPE score-partwise [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <score-partwise><credit><credit-words>&xxe;</credit-words></credit></score-partwise>
        """.trimIndent().toByteArray()
        assertThrows(IllegalArgumentException::class.java) {
            MusicXmlRuntimeSanitizer.forAlphaTab(malicious)
        }
    }
}
