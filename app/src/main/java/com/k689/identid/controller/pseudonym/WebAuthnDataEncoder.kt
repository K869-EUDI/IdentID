package com.k689.identid.controller.pseudonym

import com.upokecenter.cbor.CBORObject
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.interfaces.ECPublicKey

object WebAuthnDataEncoder {
    // WebAuthn flags
    const val FLAG_UP: Byte = 0x01 // User Present
    const val FLAG_UV: Byte = 0x04 // User Verified
    const val FLAG_AT: Byte = 0x40 // Attested Credential Data included
    val FLAG_UP_UV_AT: Byte = (FLAG_UP.toInt() or FLAG_UV.toInt() or FLAG_AT.toInt()).toByte()
    val FLAG_UP_UV: Byte = (FLAG_UP.toInt() or FLAG_UV.toInt()).toByte()

    // AAGUID: 16 zero bytes for self attestation
    private val AAGUID = ByteArray(16)

    fun rpIdHash(rpId: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(rpId.toByteArray(Charsets.UTF_8))

    fun encodeCosePublicKey(publicKey: ECPublicKey): ByteArray {
        val ecPoint = publicKey.w
        val x = ecPoint.affineX.toByteArray().trimLeadingZeroPadding(32)
        val y = ecPoint.affineY.toByteArray().trimLeadingZeroPadding(32)

        val coseKey = CBORObject.NewMap()
        coseKey[CBORObject.FromObject(1)] = CBORObject.FromObject(2) // kty: EC2
        coseKey[CBORObject.FromObject(3)] = CBORObject.FromObject(-7) // alg: ES256
        coseKey[CBORObject.FromObject(-1)] = CBORObject.FromObject(1) // crv: P-256
        coseKey[CBORObject.FromObject(-2)] = CBORObject.FromObject(x) // x coordinate
        coseKey[CBORObject.FromObject(-3)] = CBORObject.FromObject(y) // y coordinate

        return coseKey.EncodeToBytes()
    }

    fun encodeAttestedCredentialData(
        credentialId: ByteArray,
        cosePublicKey: ByteArray,
    ): ByteArray {
        val buffer = ByteBuffer.allocate(16 + 2 + credentialId.size + cosePublicKey.size)
        buffer.put(AAGUID)
        buffer.putShort(credentialId.size.toShort())
        buffer.put(credentialId)
        buffer.put(cosePublicKey)
        return buffer.array()
    }

    fun encodeAuthenticatorData(
        rpIdHash: ByteArray,
        flags: Byte,
        signCount: Int,
        attestedCredentialData: ByteArray? = null,
    ): ByteArray {
        val size = 32 + 1 + 4 + (attestedCredentialData?.size ?: 0)
        val buffer = ByteBuffer.allocate(size)
        buffer.put(rpIdHash)
        buffer.put(flags)
        buffer.putInt(signCount)
        attestedCredentialData?.let { buffer.put(it) }
        return buffer.array()
    }

    fun encodeSelfAttestationObject(
        authData: ByteArray,
        signature: ByteArray,
    ): ByteArray {
        val attObj = CBORObject.NewMap()
        attObj["fmt"] = CBORObject.FromObject("packed")
        attObj["authData"] = CBORObject.FromObject(authData)

        val attStmt = CBORObject.NewMap()
        attStmt["alg"] = CBORObject.FromObject(-7) // ES256
        attStmt["sig"] = CBORObject.FromObject(signature)
        attObj["attStmt"] = attStmt

        return attObj.EncodeToBytes()
    }

    private fun ByteArray.trimLeadingZeroPadding(targetSize: Int): ByteArray {
        return when {
            size == targetSize -> this
            size > targetSize -> copyOfRange(size - targetSize, size)
            else -> ByteArray(targetSize - size) + this
        }
    }
}
