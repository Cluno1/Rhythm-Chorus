package chromahub.rhythm.app.features.catalog.domain

/** Prepares verified COS bytes for alphaTab without changing the cached Asset identity. */
object MusicXmlRuntimeSanitizer {
    private val standardExternalDoctype = Regex(
        """<!DOCTYPE\s+score-partwise\s+(?:PUBLIC\s+[\"'][^\"']*[\"']\s+[\"'][^\"']*[\"']|SYSTEM\s+[\"'][^\"']*[\"'])\s*>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun forAlphaTab(verifiedAssetBytes: ByteArray): ByteArray {
        require(verifiedAssetBytes.isNotEmpty()) { "MusicXML is empty" }
        val source = verifiedAssetBytes.toString(Charsets.UTF_8)
        require(!source.contains("<!ENTITY", ignoreCase = true)) {
            "MusicXML entity declarations are not allowed"
        }
        val runtime = standardExternalDoctype.replaceFirst(source, "")
        require(!runtime.contains("<!DOCTYPE", ignoreCase = true)) {
            "Unsupported MusicXML DOCTYPE"
        }
        return runtime.toByteArray(Charsets.UTF_8)
    }
}
