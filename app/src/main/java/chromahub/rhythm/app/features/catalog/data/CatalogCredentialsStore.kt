package chromahub.rhythm.app.features.catalog.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores the private catalog bearer token under a non-exportable Android Keystore key. */
class CatalogCredentialsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun save(serverUrl: String, token: String) {
        val normalizedUrl = serverUrl.trim().trimEnd('/')
        val normalizedToken = token.trim()
        require(normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://"))
        require(normalizedToken.isNotEmpty())

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val encrypted = cipher.doFinal(normalizedToken.toByteArray(Charsets.UTF_8))
        preferences.edit(commit = true) {
            putString(KEY_SERVER_URL, normalizedUrl)
            putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        }
    }

    fun loadServerUrl(): String? = preferences.getString(KEY_SERVER_URL, null)
        ?.trim()
        ?.trimEnd('/')
        ?.takeIf { it.isNotEmpty() }

    fun loadToken(): String? {
        val ciphertext = preferences.getString(KEY_CIPHERTEXT, null) ?: return null
        val iv = preferences.getString(KEY_IV, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateKey(),
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
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
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
        const val KEY_ALIAS = "rhythm_catalog_bearer_token_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
