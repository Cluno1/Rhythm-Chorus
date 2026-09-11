/* SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.cluno1.sonorus.features.local.presentation.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceManualMetadataScreenTest {
    @Test
    fun `duration accepts seconds minutes and hours`() {
        assertEquals(225, parseDurationSeconds("225"))
        assertEquals(225, parseDurationSeconds("3:45"))
        assertEquals(3_723, parseDurationSeconds("1:02:03"))
    }

    @Test
    fun `duration rejects malformed and out of range values`() {
        assertNull(parseDurationSeconds("3:99"))
        assertNull(parseDurationSeconds("1:2:99"))
        assertNull(parseDurationSeconds("0"))
        assertNull(parseDurationSeconds("86401"))
        assertNull(parseDurationSeconds("words"))
    }
}
