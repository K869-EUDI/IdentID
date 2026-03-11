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

package com.k689.identid.interactor.transfer

import android.content.Context
import android.util.Base64
import com.k689.identid.controller.core.IssuanceMethod
import com.k689.identid.controller.core.IssueDocumentPartialState
import com.k689.identid.controller.core.WalletCoreDocumentsController
import com.k689.identid.controller.transfer.NearbyTransferManager
import com.k689.identid.controller.transfer.NearbyTransferState
import com.k689.identid.controller.transfer.TransferSessionManager
import com.k689.identid.controller.transfer.WalletTransferController
import com.k689.identid.model.storage.Bookmark
import com.k689.identid.model.storage.RevokedDocument
import com.k689.identid.model.storage.TransactionLog
import com.k689.identid.model.transfer.TransferSessionInfo
import com.k689.identid.model.transfer.TransferableDocument
import com.k689.identid.model.transfer.WalletTransferData
import com.k689.identid.model.transfer.WalletTransferEnvelope
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.storage.dao.BookmarkDao
import com.k689.identid.storage.dao.RevokedDocumentDao
import com.k689.identid.storage.dao.TransactionLogDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json

sealed class ReceiveWalletPartialState {
    data object Discovering : ReceiveWalletPartialState()

    data class ConnectionInitiated(
        val endpointId: String,
        val endpointName: String,
    ) : ReceiveWalletPartialState()

    data class Connected(
        val endpointId: String,
    ) : ReceiveWalletPartialState()

    data class DataReceived(
        val data: WalletTransferData,
    ) : ReceiveWalletPartialState()

    data class Error(
        val message: String,
    ) : ReceiveWalletPartialState()

    data class Disconnected(
        val endpointId: String,
    ) : ReceiveWalletPartialState()
}

sealed class DocumentImportResult {
    data class Success(
        val documentId: String,
        val documentName: String,
    ) : DocumentImportResult()

    data class Failed(
        val documentName: String,
        val error: String,
    ) : DocumentImportResult()
}

interface ReceiveWalletInteractor {
    fun parseSessionInfo(encoded: String): TransferSessionInfo?

    fun connectToSender(
        context: Context,
        sessionInfo: TransferSessionInfo,
    ): Flow<ReceiveWalletPartialState>

    fun decryptReceivedData(
        encryptedBytes: ByteArray,
        sessionInfo: TransferSessionInfo,
    ): WalletTransferData?

    suspend fun importDocuments(
        documents: List<TransferableDocument>,
    ): List<DocumentImportResult>

    suspend fun importLocalData(data: WalletTransferData)

    fun resumeOpenId4VciWithAuthorization(uri: String)

    fun stopTransfer(context: Context)
}

class ReceiveWalletInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val walletTransferController: WalletTransferController,
    private val nearbyTransferManager: NearbyTransferManager,
    private val transferSessionManager: TransferSessionManager,
    private val bookmarkDao: BookmarkDao,
    private val transactionLogDao: TransactionLogDao,
    private val revokedDocumentDao: RevokedDocumentDao,
) : ReceiveWalletInteractor {
    private val json = Json { ignoreUnknownKeys = true }

    override fun parseSessionInfo(encoded: String): TransferSessionInfo? =
        try {
            transferSessionManager.decodeSessionInfo(encoded)
        } catch (_: Exception) {
            null
        }

    override fun connectToSender(
        context: Context,
        sessionInfo: TransferSessionInfo,
    ): Flow<ReceiveWalletPartialState> {
        nearbyTransferManager.startDiscovery(context, expectedSessionId = sessionInfo.sessionId)

        return nearbyTransferManager.state.map { state ->
            when (state) {
                is NearbyTransferState.Discovering -> {
                    ReceiveWalletPartialState.Discovering
                }

                is NearbyTransferState.ConnectionInitiated -> {
                    if (state.endpointName != sessionInfo.sessionId) {
                        nearbyTransferManager.rejectConnection(context, state.endpointId)
                        ReceiveWalletPartialState.Discovering
                    } else {
                        nearbyTransferManager.acceptConnection(context, state.endpointId)
                        ReceiveWalletPartialState.ConnectionInitiated(
                            endpointId = state.endpointId,
                            endpointName = state.endpointName,
                        )
                    }
                }

                is NearbyTransferState.Connected -> {
                    ReceiveWalletPartialState.Connected(endpointId = state.endpointId)
                }

                is NearbyTransferState.PayloadReceived -> {
                    val data = decryptReceivedData(state.bytes, sessionInfo)
                    if (data != null) {
                        ReceiveWalletPartialState.DataReceived(data)
                    } else {
                        ReceiveWalletPartialState.Error("Failed to decrypt transfer data. Please retry transfer.")
                    }
                }

                is NearbyTransferState.Disconnected -> {
                    ReceiveWalletPartialState.Disconnected(endpointId = state.endpointId)
                }

                is NearbyTransferState.Error -> {
                    ReceiveWalletPartialState.Error(message = state.message)
                }

                else -> {
                    ReceiveWalletPartialState.Discovering
                }
            }
        }
    }

    override fun decryptReceivedData(
        encryptedBytes: ByteArray,
        sessionInfo: TransferSessionInfo,
    ): WalletTransferData? =
        try {
            val envelope =
                runCatching {
                    json.decodeFromString(
                        WalletTransferEnvelope.serializer(),
                        encryptedBytes.toString(Charsets.UTF_8),
                    )
                }.getOrNull()

            val payloadBytes =
                if (envelope != null) {
                    val payload = Base64.decode(envelope.encryptedPayloadBase64, Base64.NO_WRAP)
                    if (envelope.sessionId != null && envelope.sessionId != sessionInfo.sessionId) {
                        return null
                    }
                    payload
                } else {
                    encryptedBytes
                }

            val sessionKey = transferSessionManager.getSessionKey(sessionInfo)
            walletTransferController.decryptData(payloadBytes, sessionKey)
        } catch (_: Exception) {
            null
        }

    override suspend fun importDocuments(
        documents: List<TransferableDocument>,
    ): List<DocumentImportResult> {
        val results = mutableListOf<DocumentImportResult>()
        for (doc in documents) {
            try {
                // Use onEach + first to handle intermediate states (UserAuthRequired)
                // while stopping collection after a terminal state.
                // The underlying callbackFlow never closes its channel, so .collect{}
                // would block forever. .first{} cancels the flow once a terminal state
                // is received, allowing the loop to proceed to the next document.
                walletCoreDocumentsController
                    .issueDocument(
                        issuanceMethod = IssuanceMethod.OPENID4VCI,
                        configId = doc.configurationId,
                        issuerId = doc.credentialIssuerId,
                    ).onEach { state ->
                        if (state is IssueDocumentPartialState.UserAuthRequired) {
                            // Auto-complete device authentication for import flow
                            state.resultHandler.onAuthenticationSuccess()
                        }
                    }.first { state ->
                        when (state) {
                            is IssueDocumentPartialState.Success -> {
                                results.add(
                                    DocumentImportResult.Success(
                                        documentId = state.documentId,
                                        documentName = doc.name,
                                    ),
                                )
                                true
                            }

                            is IssueDocumentPartialState.Failure -> {
                                results.add(
                                    DocumentImportResult.Failed(
                                        documentName = doc.name,
                                        error = state.errorMessage,
                                    ),
                                )
                                true
                            }

                            is IssueDocumentPartialState.DeferredSuccess -> {
                                results.add(
                                    DocumentImportResult.Success(
                                        documentId = doc.configurationId,
                                        documentName = doc.name,
                                    ),
                                )
                                true
                            }

                            // UserAuthRequired is handled in onEach above; skip it here
                            else -> {
                                false
                            }
                        }
                    }
            } catch (e: Exception) {
                results.add(
                    DocumentImportResult.Failed(
                        documentName = doc.name,
                        error = e.localizedMessage ?: "Unknown error",
                    ),
                )
            }
        }
        return results
    }

    override fun resumeOpenId4VciWithAuthorization(uri: String) {
        walletCoreDocumentsController.resumeOpenId4VciWithAuthorization(uri)
    }

    override suspend fun importLocalData(data: WalletTransferData) {
        // Import transaction logs
        for (log in data.transactionLogs) {
            transactionLogDao.store(
                TransactionLog(
                    identifier = log.identifier,
                    value = log.value,
                ),
            )
        }

        // Import bookmarks
        for (bookmarkId in data.bookmarks) {
            bookmarkDao.store(Bookmark(identifier = bookmarkId))
        }

        // Import revoked documents
        for (revokedId in data.revokedDocuments) {
            revokedDocumentDao.store(RevokedDocument(identifier = revokedId))
        }
    }

    override fun stopTransfer(context: Context) {
        nearbyTransferManager.stopAllEndpoints(context)
    }
}
