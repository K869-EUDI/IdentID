package com.k689.identid.controller.pseudonym

import android.util.Base64
import com.k689.identid.controller.crypto.PseudonymKeyManager
import com.k689.identid.model.storage.Pseudonym
import com.k689.identid.storage.dao.PseudonymDao
import java.security.interfaces.ECPublicKey
import java.util.UUID

data class PseudonymCreationResult(
    val pseudonym: Pseudonym,
    val attestationObject: ByteArray,
    val authData: ByteArray,
    val credentialIdBytes: ByteArray,
    val publicKeyBytes: ByteArray,
)

data class PseudonymAuthResult(
    val authenticatorData: ByteArray,
    val signature: ByteArray,
    val userHandle: ByteArray,
)

class PseudonymRepository(
    private val pseudonymDao: PseudonymDao,
    private val keyManager: PseudonymKeyManager,
) {
    suspend fun createPseudonym(
        rpId: String,
        rpName: String,
        userId: ByteArray,
        userName: String,
        clientDataHash: ByteArray,
    ): PseudonymCreationResult {
        val id = UUID.randomUUID().toString()
        val credentialIdBytes = UUID.randomUUID().toString().toByteArray(Charsets.UTF_8)
        val credentialIdB64 = Base64.encodeToString(credentialIdBytes, Base64.URL_SAFE or Base64.NO_WRAP)
        val keystoreAlias = "pseudonym_$id"

        val publicKey = keyManager.generateKeyPair(keystoreAlias) as ECPublicKey
        val publicKeyEncoded = publicKey.encoded
        val publicKeyB64 = Base64.encodeToString(publicKeyEncoded, Base64.NO_WRAP)

        val cosePublicKey = WebAuthnDataEncoder.encodeCosePublicKey(publicKey)
        val attestedCredData = WebAuthnDataEncoder.encodeAttestedCredentialData(credentialIdBytes, cosePublicKey)
        val rpIdHash = WebAuthnDataEncoder.rpIdHash(rpId)
        val authData = WebAuthnDataEncoder.encodeAuthenticatorData(
            rpIdHash = rpIdHash,
            flags = WebAuthnDataEncoder.FLAG_UP_UV_AT,
            signCount = 0,
            attestedCredentialData = attestedCredData,
        )

        // Self attestation: sign authData || clientDataHash with the credential key
        val signedData = authData + clientDataHash
        val signature = keyManager.sign(keystoreAlias, signedData)
        val attestationObject = WebAuthnDataEncoder.encodeSelfAttestationObject(authData, signature)

        val now = System.currentTimeMillis()
        val pseudonym = Pseudonym(
            id = id,
            rpId = rpId,
            rpName = rpName,
            credentialId = credentialIdB64,
            publicKey = publicKeyB64,
            keystoreAlias = keystoreAlias,
            userId = Base64.encodeToString(userId, Base64.URL_SAFE or Base64.NO_WRAP),
            userName = userName,
            createdAt = now,
        )
        pseudonymDao.store(pseudonym)

        return PseudonymCreationResult(
            pseudonym = pseudonym,
            attestationObject = attestationObject,
            authData = authData,
            credentialIdBytes = credentialIdBytes,
            publicKeyBytes = publicKeyEncoded,
        )
    }

    suspend fun authenticate(
        pseudonymId: String,
        clientDataHash: ByteArray,
    ): PseudonymAuthResult? {
        val pseudonym = pseudonymDao.getById(pseudonymId) ?: return null

        val rpIdHash = WebAuthnDataEncoder.rpIdHash(pseudonym.rpId)
        val authData = WebAuthnDataEncoder.encodeAuthenticatorData(
            rpIdHash = rpIdHash,
            flags = WebAuthnDataEncoder.FLAG_UP_UV,
            signCount = 1,
        )

        val signedData = authData + clientDataHash
        val signature = keyManager.sign(pseudonym.keystoreAlias, signedData)

        // Update lastUsedAt
        pseudonymDao.update(pseudonym.copy(lastUsedAt = System.currentTimeMillis()))

        val userHandle = Base64.decode(pseudonym.userId, Base64.URL_SAFE or Base64.NO_WRAP)

        return PseudonymAuthResult(
            authenticatorData = authData,
            signature = signature,
            userHandle = userHandle,
        )
    }

    suspend fun getPseudonymsForRp(rpId: String): List<Pseudonym> =
        pseudonymDao.getByRpId(rpId)

    suspend fun getAllPseudonyms(): List<Pseudonym> =
        pseudonymDao.getAll()

    suspend fun getPseudonymById(id: String): Pseudonym? =
        pseudonymDao.getById(id)

    suspend fun getPseudonymByCredentialId(credentialId: String): Pseudonym? =
        pseudonymDao.getByCredentialId(credentialId)

    suspend fun updateAlias(id: String, alias: String?) {
        val pseudonym = pseudonymDao.getById(id) ?: return
        pseudonymDao.update(pseudonym.copy(userAlias = alias))
    }

    suspend fun deletePseudonym(id: String) {
        val pseudonym = pseudonymDao.getById(id) ?: return
        keyManager.deleteKey(pseudonym.keystoreAlias)
        pseudonymDao.delete(id)
    }

    suspend fun deleteAllPseudonyms() {
        val all = pseudonymDao.getAll()
        all.forEach { keyManager.deleteKey(it.keystoreAlias) }
        pseudonymDao.deleteAll()
    }
}
