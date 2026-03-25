package com.k689.identid.controller.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class PseudonymKeyManager {
    companion object {
        private const val KEYSTORE_TYPE = "AndroidKeyStore"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val EC_CURVE = "secp256r1"
    }

    private val keyStore: KeyStore =
        KeyStore.getInstance(KEYSTORE_TYPE).apply { load(null) }

    fun generateKeyPair(alias: String): PublicKey {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generateKeyPairInternal(alias, strongBox = true)
            } catch (_: android.security.keystore.StrongBoxUnavailableException) {
                // StrongBox not available, fall back to TEE
            }
        }
        return generateKeyPairInternal(alias, strongBox = false)
    }

    private fun generateKeyPairInternal(alias: String, strongBox: Boolean): PublicKey {
        val builder = KeyGenParameterSpec
            .Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }
        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_TYPE,
        )
        generator.initialize(builder.build())
        return generator.generateKeyPair().public
    }

    fun sign(alias: String, data: ByteArray): ByteArray {
        val privateKey = keyStore.getKey(alias, null) as PrivateKey
        return Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initSign(privateKey)
            update(data)
            sign()
        }
    }

    fun getPublicKey(alias: String): PublicKey? {
        return keyStore.getCertificate(alias)?.publicKey
    }

    fun deleteKey(alias: String) {
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    fun keyExists(alias: String): Boolean = keyStore.containsAlias(alias)
}
