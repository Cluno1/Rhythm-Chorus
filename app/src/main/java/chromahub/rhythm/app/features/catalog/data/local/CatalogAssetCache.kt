package chromahub.rhythm.app.features.catalog.data.local

import android.content.Context
import java.io.File
import java.security.MessageDigest

internal class CatalogAssetCache(context: Context) {
    private val root = File(context.applicationContext.filesDir, "catalog-assets").apply { mkdirs() }

    fun read(assetId: String, sha256: String, byteSize: Long): ByteArray? {
        val file = file(assetId, sha256)
        if (!file.isFile || file.length() != byteSize) return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        return bytes.takeIf { digest(it) == sha256.lowercase() }
    }

    fun write(assetId: String, sha256: String, bytes: ByteArray) {
        require(digest(bytes) == sha256.lowercase())
        root.mkdirs()
        val target = file(assetId, sha256)
        val temporary = File(root, ".${target.name}.tmp")
        temporary.writeBytes(bytes)
        check(temporary.renameTo(target) || runCatching {
            target.writeBytes(bytes)
            temporary.delete()
            true
        }.getOrDefault(false)) { "could not commit catalog asset cache" }
    }

    private fun file(assetId: String, sha256: String) = File(root, "$assetId-${sha256.lowercase()}")
    private fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}
