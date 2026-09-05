package chromahub.rhythm.app.features.catalog.data.local

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale

/**
 * Persistent, app-private cache for Catalog audio and score assets.
 *
 * The cache is shared by all configured Catalog connections but every entry is namespaced by a
 * one-way server/token fingerprint. The complete cache has a hard 10 GiB LRU ceiling. Device
 * MediaStore songs never enter this directory.
 */
internal class CatalogOfflineCache private constructor(
    private val root: File,
    private val gson: Gson,
    private val maxBytes: Long,
) {
    constructor(context: Context) : this(
        File(context.applicationContext.filesDir, DIRECTORY),
        Gson(),
        MAX_BYTES,
    )

    internal constructor(root: File, maxBytes: Long = MAX_BYTES) : this(root, Gson(), maxBytes)

    data class CachedAsset(
        val file: File,
        val mediaType: String,
        val assetId: String,
        val sha256: String,
        val byteSize: Long,
    ) {
        val uri: Uri get() = Uri.fromFile(file)
    }

    fun findAudio(namespace: String, renditionId: String): CachedAsset? = synchronized(LOCK) {
        val index = readIndex()
        val entry = index.lastOrNull {
            it.namespace == namespace && it.kind == KIND_AUDIO && it.renditionId == renditionId
        } ?: return@synchronized null
        validatedAsset(index, entry)
    }

    fun readAsset(namespace: String, assetId: String, sha256: String, byteSize: Long): ByteArray? =
        synchronized(LOCK) {
            val index = readIndex()
            val entry = index.lastOrNull {
                it.namespace == namespace &&
                    it.assetId == assetId &&
                    it.sha256.equals(sha256, ignoreCase = true) &&
                    it.byteSize == byteSize
            } ?: return@synchronized null
            val cached = validatedAsset(index, entry) ?: return@synchronized null
            val bytes = runCatching { cached.file.readBytes() }.getOrNull() ?: return@synchronized null
            if (!digest(bytes).equals(sha256, ignoreCase = true)) {
                removeBroken(index, entry, cached.file)
                return@synchronized null
            }
            bytes
        }

    fun store(
        namespace: String,
        kind: String,
        assetId: String,
        sha256: String,
        byteSize: Long,
        mediaType: String,
        renditionId: String? = null,
        revisionId: String? = null,
        input: InputStream,
    ): CachedAsset {
        require(namespace.matches(HASH_PATTERN)) { "cache namespace is invalid" }
        require(kind == KIND_AUDIO || kind == KIND_SCORE) { "cache kind is invalid" }
        require(assetId.matches(UUID_PATTERN)) { "asset ID is invalid" }
        require(sha256.matches(HASH_PATTERN)) { "asset SHA-256 is invalid" }
        require(byteSize in 1..maxBytes) { "asset exceeds the offline cache limit" }
        renditionId?.let { require(it.matches(UUID_PATTERN)) { "rendition ID is invalid" } }
        revisionId?.let { require(it.matches(UUID_PATTERN)) { "revision ID is invalid" } }

        root.mkdirs()
        val namespaceRoot = File(root, namespace).apply { mkdirs() }
        val relativePath = "$namespace/$kind-$assetId-${sha256.lowercase(Locale.ROOT)}.asset"
        val target = File(root, relativePath)
        val temporary = File(namespaceRoot, ".${target.name}.${System.nanoTime()}.tmp")
        val digest = MessageDigest.getInstance("SHA-256")
        var copied = 0L
        try {
            temporary.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    copied += read
                    if (copied > byteSize) throw IOException("asset is larger than its descriptor")
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
            }
            if (copied != byteSize) throw IOException("asset size mismatch")
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actualHash.equals(sha256, ignoreCase = true)) throw IOException("asset SHA-256 mismatch")

            return synchronized(LOCK) {
                val index = readIndex()
                val existing = index.lastOrNull {
                    it.namespace == namespace &&
                        it.assetId == assetId &&
                        it.sha256.equals(sha256, ignoreCase = true)
                }
                if (existing != null) {
                    validatedAsset(index, existing)?.let {
                        temporary.delete()
                        return@synchronized it
                    }
                }

                reserve(index, byteSize, relativePath)
                target.parentFile?.mkdirs()
                if (!temporary.renameTo(target)) {
                    temporary.inputStream().use { source ->
                        target.outputStream().use { destination -> source.copyTo(destination) }
                    }
                    if (!temporary.delete()) temporary.deleteOnExit()
                }
                val entry = Entry(
                    namespace = namespace,
                    kind = kind,
                    assetId = assetId,
                    sha256 = sha256.lowercase(Locale.ROOT),
                    byteSize = byteSize,
                    mediaType = mediaType,
                    renditionId = renditionId,
                    revisionId = revisionId,
                    relativePath = relativePath,
                    lastAccessedAt = System.currentTimeMillis(),
                )
                val superseded = index.filter {
                    it.namespace == namespace &&
                        (it.relativePath == relativePath ||
                            (renditionId != null && it.kind == KIND_AUDIO && it.renditionId == renditionId) ||
                            (revisionId != null && it.kind == KIND_SCORE && it.revisionId == revisionId))
                }
                superseded.forEach { old ->
                    if (old.relativePath != relativePath) File(root, old.relativePath).delete()
                }
                index.removeAll(superseded.toSet())
                index += entry
                writeIndex(index)
                entry.toCachedAsset(target)
            }
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    fun usedBytes(): Long = synchronized(LOCK) {
        val index = readIndex()
        pruneMissing(index)
        index.sumOf { it.byteSize }
    }

    private fun validatedAsset(index: MutableList<Entry>, entry: Entry): CachedAsset? {
        val file = File(root, entry.relativePath)
        if (!file.isFile || file.length() != entry.byteSize) {
            removeBroken(index, entry, file)
            return null
        }
        entry.lastAccessedAt = System.currentTimeMillis()
        file.setLastModified(entry.lastAccessedAt)
        writeIndex(index)
        return entry.toCachedAsset(file)
    }

    private fun reserve(index: MutableList<Entry>, incomingBytes: Long, incomingPath: String) {
        pruneMissing(index)
        var total = index.sumOf { it.byteSize }
        val replaced = index.firstOrNull { it.relativePath == incomingPath }
        if (replaced != null) total -= replaced.byteSize
        val victims = index
            .filterNot { it.relativePath == incomingPath }
            .sortedBy { it.lastAccessedAt }
            .iterator()
        while (total + incomingBytes > maxBytes && victims.hasNext()) {
            val victim = victims.next()
            File(root, victim.relativePath).delete()
            index.remove(victim)
            total -= victim.byteSize
        }
        if (total + incomingBytes > maxBytes) throw IOException("offline cache is full")
    }

    private fun pruneMissing(index: MutableList<Entry>) {
        val removed = index.removeAll { entry ->
            val file = File(root, entry.relativePath)
            !file.isFile || file.length() != entry.byteSize
        }
        val knownPaths = index.mapTo(mutableSetOf()) { it.relativePath }
        root.walkTopDown()
            .filter { it.isFile && it.extension == "asset" }
            .filter { it.relativeTo(root).invariantSeparatorsPath !in knownPaths }
            .forEach(File::delete)
        if (removed) writeIndex(index)
    }

    private fun removeBroken(index: MutableList<Entry>, entry: Entry, file: File) {
        file.delete()
        index.remove(entry)
        writeIndex(index)
    }

    private fun readIndex(): MutableList<Entry> {
        if (!indexFile.isFile) return mutableListOf()
        return runCatching {
            gson.fromJson<List<Entry>>(
                indexFile.readText(),
                object : TypeToken<List<Entry>>() {}.type,
            ).orEmpty().toMutableList()
        }.getOrElse {
            // A corrupt index must never turn files into unaccounted, unbounded cache usage.
            root.walkTopDown()
                .filter { file -> file.isFile && file.extension == "asset" }
                .forEach(File::delete)
            indexFile.delete()
            mutableListOf()
        }
    }

    private fun writeIndex(index: List<Entry>) {
        root.mkdirs()
        val temporary = File(root, ".index-v1.json.tmp")
        temporary.writeText(gson.toJson(index))
        if (!temporary.renameTo(indexFile)) {
            indexFile.writeText(temporary.readText())
            temporary.delete()
        }
    }

    private val indexFile: File get() = File(root, INDEX_FILE)

    private data class Entry(
        val namespace: String,
        val kind: String,
        val assetId: String,
        val sha256: String,
        val byteSize: Long,
        val mediaType: String,
        val renditionId: String?,
        val revisionId: String?,
        val relativePath: String,
        var lastAccessedAt: Long,
    ) {
        fun toCachedAsset(file: File) = CachedAsset(file, mediaType, assetId, sha256, byteSize)
    }

    companion object {
        const val MAX_BYTES: Long = 10L * 1024L * 1024L * 1024L
        const val KIND_AUDIO = "audio"
        const val KIND_SCORE = "score"

        private const val DIRECTORY = "catalog-offline"
        private const val INDEX_FILE = "index-v1.json"
        private val LOCK = Any()
        private val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        private val HASH_PATTERN = Regex("^[0-9a-fA-F]{64}$")

        fun namespace(serverUrl: String, token: String): String = digest(
            (serverUrl.trimEnd('/') + "\u0000" + token.trim()).toByteArray(Charsets.UTF_8),
        )

        private fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}
