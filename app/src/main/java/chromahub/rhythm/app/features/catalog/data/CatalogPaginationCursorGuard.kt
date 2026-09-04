package chromahub.rhythm.app.features.catalog.data

/** Fails closed when a malformed server page repeats a cursor and would otherwise loop forever. */
internal class CatalogPaginationCursorGuard(private val pageName: String) {
    private val seen = mutableSetOf<String>()

    fun advance(nextCursor: String?): String? = nextCursor?.also {
        require(it.isNotBlank()) { "$pageName pagination returned a blank cursor" }
        require(seen.add(it)) { "$pageName pagination repeated cursor" }
    }
}
