/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.cluno1.sonorus.features.local.data.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class DeviceScanRoot(
    val treeUri: String,
    val displayPath: String,
)

/** Persisted read-only SAF roots used to supplement MediaStore in either filtering mode. */
class DeviceScanFolderAccess(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun roots(): List<DeviceScanRoot> {
        val json = prefs.getString(KEY_ROOTS, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<DeviceScanRoot>>(
                json,
                object : TypeToken<List<DeviceScanRoot>>() {}.type,
            ).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun add(
        treeUri: Uri,
        displayPath: String = DeviceScanFolderAccess.displayPath(context, treeUri),
    ): DeviceScanRoot {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        val normalizedPath = normalizePath(displayPath)
        val root = DeviceScanRoot(treeUri.toString(), normalizedPath)
        val (replaced, retained) = roots().partition {
            it.treeUri == treeUri.toString() ||
                normalizePath(it.displayPath).equals(normalizedPath, ignoreCase = true)
        }
        replaced.filterNot { it.treeUri == root.treeUri }.forEach(::releaseRoot)
        val updated = retained + root
        prefs.edit { putString(KEY_ROOTS, gson.toJson(updated)) }
        return root
    }

    fun removeByDisplayPath(displayPath: String) {
        val normalizedPath = normalizePath(displayPath)
        val (removed, retained) = roots().partition {
            normalizePath(it.displayPath).equals(normalizedPath, ignoreCase = true)
        }
        removed.forEach(::releaseRoot)
        prefs.edit { putString(KEY_ROOTS, gson.toJson(retained)) }
    }

    fun clear() {
        roots().forEach(::releaseRoot)
        prefs.edit { remove(KEY_ROOTS) }
    }

    private fun releaseRoot(root: DeviceScanRoot) {
        val uri = runCatching { Uri.parse(root.treeUri) }.getOrNull() ?: return
        if (uri in DeviceFolderAccess(context).roots()) return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "device_scan_folders"
        private const val KEY_ROOTS = "roots"

        fun displayPath(context: Context, treeUri: Uri): String {
            return rawPath(treeUri)
                ?: DocumentFile.fromTreeUri(context, treeUri)?.name?.takeIf(String::isNotBlank)
                ?: treeUri.toString()
        }

        fun rawPath(uri: Uri): String? {
            val documentId = runCatching {
                DocumentsContract.getDocumentId(uri)
            }.recoverCatching {
                DocumentsContract.getTreeDocumentId(uri)
            }.getOrNull() ?: return null
            val volume = documentId.substringBefore(':', missingDelimiterValue = "")
            val relativePath = documentId.substringAfter(':', missingDelimiterValue = "").trim('/')
            val root = when {
                volume.equals("primary", ignoreCase = true) -> "/storage/emulated/0"
                volume.equals("home", ignoreCase = true) -> "/storage/emulated/0/Documents"
                volume.matches(Regex("^[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}$")) -> "/storage/$volume"
                else -> return null
            }
            return normalizePath(if (relativePath.isBlank()) root else "$root/$relativePath")
        }

        fun normalizePath(path: String): String {
            val normalized = path.trim().replace('\\', '/').replace(Regex("/{2,}"), "/")
            return if (normalized.length > 1) normalized.trimEnd('/') else normalized
        }
    }
}

/** Admission policy for playback of files found through a user-authorized SAF tree. */
object DeviceDocumentPolicy {
    const val MEDIA_ID_PREFIX = "rhythm-device-document:"

    fun mediaId(uri: Uri): String =
        MEDIA_ID_PREFIX + UUID.nameUUIDFromBytes(uri.toString().toByteArray(Charsets.UTF_8))

    fun allowsPersistedRead(context: Context, mediaId: String, uri: Uri?): Boolean {
        uri ?: return false
        if (mediaId != mediaId(uri) || !"content".equals(uri.scheme, ignoreCase = true)) return false
        if (!DocumentsContract.isDocumentUri(context, uri)) return false

        val itemTreeId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: return false
        val configuredRoot = DeviceScanFolderAccess(context).roots().any { root ->
            val rootUri = runCatching { Uri.parse(root.treeUri) }.getOrNull() ?: return@any false
            rootUri.authority == uri.authority &&
                runCatching { DocumentsContract.getTreeDocumentId(rootUri) }.getOrNull() == itemTreeId
        }
        if (!configuredRoot) return false
        val hasMatchingGrant = context.contentResolver.persistedUriPermissions.any { permission ->
            permission.isReadPermission &&
                permission.uri.authority == uri.authority &&
                runCatching { DocumentsContract.getTreeDocumentId(permission.uri) }.getOrNull() == itemTreeId
        }
        if (!hasMatchingGrant) return false

        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
    }
}
