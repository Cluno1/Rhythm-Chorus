package chromahub.rhythm.app.features.catalog.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import chromahub.rhythm.app.features.catalog.data.remote.CatalogEndpoint
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class CatalogDeviceCredentials(
    val serverUrl: String,
    val userId: String,
    val deviceId: String,
    val sessionId: String,
    val accessToken: String,
    val accessTokenExpiresAtEpochSeconds: Long,
    val sessionExpiresAt: String,
)

/** Stores the short-lived access token encrypted under a non-exportable Android Keystore key. */
class CatalogCredentialsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    /** Kept so an existing WireGuard/private-server configuration works after the upgrade. */
    fun save(serverUrl: String, token: String) {
        val normalizedUrl = CatalogEndpoint.normalize(serverUrl)
        val normalizedToken = token.trim()
        require(normalizedToken.isNotEmpty())
        saveEncryptedToken(normalizedToken)
        preferences.edit(commit = true) {
            putString(KEY_SERVER_URL, normalizedUrl)
            remove(KEY_USER_ID)
            remove(KEY_DEVICE_ID)
            remove(KEY_SESSION_ID)
            remove(KEY_ACCESS_EXPIRES_AT)
            remove(KEY_SESSION_EXPIRES_AT)
        }
    }

    fun saveDevice(credentials: CatalogDeviceCredentials) {
        saveEncryptedToken(credentials.accessToken)
        preferences.edit(commit = true) {
            putString(KEY_SERVER_URL, CatalogEndpoint.normalize(credentials.serverUrl))
            putString(KEY_USER_ID, credentials.userId)
            putString(KEY_DEVICE_ID, credentials.deviceId)
            putString(KEY_SESSION_ID, credentials.sessionId)
            putLong(KEY_ACCESS_EXPIRES_AT, credentials.accessTokenExpiresAtEpochSeconds)
            putString(KEY_SESSION_EXPIRES_AT, credentials.sessionExpiresAt)
        }
    }

    fun updateAccessToken(token: String, expiresAtEpochSeconds: Long) {
        check(loadDeviceId() != null) { "device credentials are not configured" }
        saveEncryptedToken(token)
        preferences.edit(commit = true) {
            putLong(KEY_ACCESS_EXPIRES_AT, expiresAtEpochSeconds)
        }
    }

    fun loadServerUrl(): String? = preferences.getString(KEY_SERVER_URL, null)
        ?.trim()
        ?.trimEnd('/')
        ?.takeIf { it.isNotEmpty() }

    fun loadDeviceId(): String? = preferences.getString(KEY_DEVICE_ID, null)
        ?.takeIf { it.isNotBlank() }

    fun loadDevice(): CatalogDeviceCredentials? {
        val serverUrl = loadServerUrl() ?: return null
        val token = loadToken() ?: return null
        return CatalogDeviceCredentials(
            serverUrl = serverUrl,
            userId = preferences.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() }
                ?: return null,
            deviceId = loadDeviceId() ?: return null,
            sessionId = preferences.getString(KEY_SESSION_ID, null)?.takeIf { it.isNotBlank() }
                ?: return null,
            accessToken = token,
            accessTokenExpiresAtEpochSeconds = preferences.getLong(KEY_ACCESS_EXPIRES_AT, 0L),
            sessionExpiresAt = preferences.getString(KEY_SESSION_EXPIRES_AT, null)
                ?.takeIf { it.isNotBlank() }
                ?: return null,
        )
    }

    fun loadToken(): String? {
        val ciphertext = preferences.getString(KEY_CIPHERTEXT, null) ?: return null
        val iv = preferences.getString(KEY_IV, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateEncryptionKey(),
                    GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP)),
                )
            }
            cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP))
                .toString(Charsets.UTF_8)
                .takeIf { it.isNotBlank() }
        }.getOrElse {
            clear()
            null
        }
    }

    fun clear() {
        preferences.edit(commit = true) { clear() }
        CatalogDeviceKey.delete()
    }

    private fun saveEncryptedToken(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateEncryptionKey())
        }
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        preferences.edit(commit = true) {
            putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        }
    }

    private fun getOrCreateEncryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ENCRYPTION_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    ENCRYPTION_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "rhythm_catalog_credentials"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_CIPHERTEXT = "token_ciphertext"
        const val KEY_IV = "token_iv"
        const val KEY_USER_ID = "device_user_id"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_SESSION_ID = "device_session_id"
        const val KEY_ACCESS_EXPIRES_AT = "access_token_expires_at"
        const val KEY_SESSION_EXPIRES_AT = "device_session_expires_at"
        const val ENCRYPTION_KEY_ALIAS = "rhythm_catalog_bearer_token_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
