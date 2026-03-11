/*
 * Copyright (c) 2025 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package com.k689.identid.controller.transfer

import android.util.Base64
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.model.transfer.TransferableDocument
import com.k689.identid.model.transfer.TransferableTransactionLog
import com.k689.identid.model.transfer.WalletTransferData
import com.k689.identid.storage.dao.BookmarkDao
import com.k689.identid.storage.dao.RevokedDocumentDao
import com.k689.identid.storage.dao.TransactionLogDao
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface WalletTransferController {
    suspend fun collectTransferData(): WalletTransferData

    fun generateSessionKey(): ByteArray

    fun encryptData(
        data: WalletTransferData,
        sessionKey: ByteArray,
    ): ByteArray

    fun decryptData(
        encryptedBytes: ByteArray,
        sessionKey: ByteArray,
    ): WalletTransferData
}

class WalletTransferControllerImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val bookmarkDao: BookmarkDao,
    private val transactionLogDao: TransactionLogDao,
    private val revokedDocumentDao: RevokedDocumentDao,
) : WalletTransferController {
    companion object {
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val KEY_SIZE = 32
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun collectTransferData(): WalletTransferData {
        val locale = java.util.Locale.getDefault()

        // Only transfer documents that the user actually has issued, are not revoked, and are not expired
        val revokedIds = revokedDocumentDao.retrieveAll().map { it.identifier }.toSet()

        val now = java.time.Instant.now()
        val allIssuedDocs = walletCoreDocumentsController.getAllIssuedDocuments()

        val issuedFormats =
            allIssuedDocs
                .filter { doc ->
                    doc.id !in revokedIds &&
                        (doc.getValidUntil().getOrNull()?.isAfter(now) != false)
                }.mapNotNull { doc ->
                    when (doc.format) {
                        is eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat -> {
                            (doc.format as eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat).docType
                        }

                        is eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat -> {
                            (doc.format as eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat).vct
                        }
                    }
                }.toSet()

        val scopedDocs =
            when (val result = walletCoreDocumentsController.getScopedDocuments(locale)) {
                is com.k689.identid.controller.core.FetchScopedDocumentsPartialState.Success -> {
                    result.documents
                        .filter { doc -> doc.formatType in issuedFormats }
                        // Prefer non-deferred configurations, then deduplicate by formatType
                        .sortedBy { doc -> if (doc.configurationId.contains("deferred")) 1 else 0 }
                        .distinctBy { doc -> doc.formatType }
                        .map { doc ->
                            TransferableDocument(
                                name = doc.name,
                                configurationId = doc.configurationId,
                                credentialIssuerId = doc.credentialIssuerId,
                                formatType = doc.formatType,
                                isPid = doc.isPid,
                            )
                        }
                }

                else -> {
                    emptyList()
                }
            }

        val transactionLogs =
            transactionLogDao.retrieveAll().map { log ->
                TransferableTransactionLog(
                    identifier = log.identifier,
                    value = log.value,
                )
            }

        val bookmarks = bookmarkDao.retrieveAll().map { it.identifier }
        val revokedDocs = revokedDocumentDao.retrieveAll().map { it.identifier }

        return WalletTransferData(
            documents = scopedDocs,
            transactionLogs = transactionLogs,
            bookmarks = bookmarks,
            revokedDocuments = revokedDocs,
        )
    }

    override fun generateSessionKey(): ByteArray {
        val key = ByteArray(KEY_SIZE)
        SecureRandom().nextBytes(key)
        return key
    }

    override fun encryptData(
        data: WalletTransferData,
        sessionKey: ByteArray,
    ): ByteArray {
        val plaintext = json.encodeToString(WalletTransferData.serializer(), data).toByteArray(Charsets.UTF_8)
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val keySpec = SecretKeySpec(sessionKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext)

        // Prepend IV to ciphertext
        return iv + ciphertext
    }

    override fun decryptData(
        encryptedBytes: ByteArray,
        sessionKey: ByteArray,
    ): WalletTransferData {
        val iv = encryptedBytes.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encryptedBytes.copyOfRange(GCM_IV_LENGTH, encryptedBytes.size)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val keySpec = SecretKeySpec(sessionKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        val plaintext = cipher.doFinal(ciphertext)

        return json.decodeFromString(WalletTransferData.serializer(), plaintext.toString(Charsets.UTF_8))
    }

    fun encodeSessionKeyToBase64(key: ByteArray): String = Base64.encodeToString(key, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    fun decodeSessionKeyFromBase64(encoded: String): ByteArray = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
