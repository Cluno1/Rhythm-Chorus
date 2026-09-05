/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package chromahub.rhythm.app.features.local.data.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile

data class DeviceSiblingFile(val uri: Uri, val name: String)

/** Persisted, read-only Storage Access Framework roots used only for sibling metadata. */
class DeviceFolderAccess(private val context: Context) {
    private val prefs = context.getSharedPreferences("device_metadata_folders", Context.MODE_PRIVATE)

    fun roots(): Set<Uri> = prefs.getStringSet(KEY_ROOTS, emptySet()).orEmpty().mapNotNull {
        runCatching { Uri.parse(it) }.getOrNull()
    }.toSet()

    fun add(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        prefs.edit { putStringSet(KEY_ROOTS, roots().map(Uri::toString).toSet() + uri.toString()) }
    }

    fun clear() {
        roots().forEach { uri -> runCatching { context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        prefs.edit { remove(KEY_ROOTS) }
    }

    fun remove(uri: Uri) {
        runCatching { context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        prefs.edit { putStringSet(KEY_ROOTS, roots().filterNot { it == uri }.map(Uri::toString).toSet()) }
    }

    fun findSibling(audioUri: Uri, extensions: Set<String>, commonNames: Set<String> = emptySet()): DeviceSiblingFile? {
        val displayName = queryDisplayName(audioUri) ?: return null
        val expectedRelativePath = queryRelativePath(audioUri)
        val stem = displayName.substringBeforeLast('.', displayName)
        for (root in roots()) {
            findSiblingInRoot(root, displayName, expectedRelativePath, stem, extensions, commonNames)?.let { return it }
        }
        return null
    }

    private fun findSiblingInRoot(
        root: Uri,
        displayName: String,
        expectedRelativePath: String?,
        stem: String,
        extensions: Set<String>,
        commonNames: Set<String>
    ): DeviceSiblingFile? = runCatching {
            val tree = DocumentFile.fromTreeUri(context, root) ?: return@runCatching null
            val matchingAudio = mutableListOf<DocumentFile>()
            findDocuments(tree, displayName, 0, matchingAudio)
            val audio = matchingAudio.firstOrNull { file ->
                expectedRelativePath != null && runCatching {
                    DocumentsContract.getDocumentId(file.uri).substringAfter(':').endsWith(expectedRelativePath + displayName, true)
                }.getOrDefault(false)
            } ?: matchingAudio.singleOrNull() ?: return@runCatching null
            val siblings = audio.parentFile?.listFiles().orEmpty()
            val exact = siblings.firstOrNull { file ->
                val name = file.name.orEmpty()
                file.isFile && name.substringBeforeLast('.', name).equals(stem, true) && name.substringAfterLast('.', "").lowercase() in extensions
            }
            val common = siblings.firstOrNull { file ->
                val name = file.name.orEmpty()
                file.isFile && name.substringBeforeLast('.', name).lowercase() in commonNames && name.substringAfterLast('.', "").lowercase() in extensions
            }
            (exact ?: common)?.let { DeviceSiblingFile(it.uri, it.name.orEmpty()) }
    }.getOrNull()

    private fun findDocuments(directory: DocumentFile, displayName: String, depth: Int, result: MutableList<DocumentFile>) {
        if (depth > MAX_DEPTH) return
        directory.listFiles().forEach { if (it.isFile && it.name.equals(displayName, true)) result += it }
        directory.listFiles().forEach { child ->
            if (child.isDirectory) findDocuments(child, displayName, depth + 1, result)
        }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun queryRelativePath(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.trimStart('/') else null
        }
    }.getOrNull()

    private companion object {
        const val KEY_ROOTS = "roots"
        const val MAX_DEPTH = 12
    }
}
