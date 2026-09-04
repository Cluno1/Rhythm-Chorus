package chromahub.rhythm.app.features.catalog.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CatalogPaginationCursorGuardTest {
    @Test
    fun acceptsDistinctCursorsAndTerminalNull() {
        val guard = CatalogPaginationCursorGuard("songs")

        assertEquals("page-2", guard.advance("page-2"))
        assertEquals("page-3", guard.advance("page-3"))
        assertNull(guard.advance(null))
    }

    @Test
    fun rejectsRepeatedOrBlankCursor() {
        val guard = CatalogPaginationCursorGuard("songs")
        guard.advance("page-2")

        assertThrows(IllegalArgumentException::class.java) { guard.advance("page-2") }
        assertThrows(IllegalArgumentException::class.java) { guard.advance("  ") }
    }
}
