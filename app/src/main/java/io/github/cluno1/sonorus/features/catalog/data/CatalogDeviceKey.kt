package io.github.cluno1.sonorus.features.catalog.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec

internal interface CatalogSigner {
    fun publicKeySpki(): String
    fun sign(canonical: ByteArray): String
}

internal object CatalogDeviceKey : CatalogSigner {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "rhythm_catalog_device_signing_p256_v1"

    override fun publicKeySpki(): String = Base64.encodeToString(
        getOrCreateEntry().certificate.publicKey.encoded,
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )

    override fun sign(canonical: ByteArray): String {
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(getOrCreateEntry().privateKey)
            update(canonical)
            sign()
        }
        return Base64.encodeToString(
            signature,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    fun delete() {
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
        }
    }

    private fun getOrCreateEntry(): KeyStore.PrivateKeyEntry {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry)?.let { return it }
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE).apply {
            initialize(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setUserAuthenticationRequired(false)
                    .build(),
            )
            generateKeyPair()
        }
        return KeyStore.getInstance(ANDROID_KEYSTORE).run {
            load(null)
            getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        }
    }
}
