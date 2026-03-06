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
import com.k689.identid.provider.resources.ResourceProvider
import com.k689.identid.storage.dao.BookmarkDao
import com.k689.identid.storage.dao.RevokedDocumentDao
import com.k689.identid.storage.dao.TransactionLogDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class ReceiveWalletPartialState {
    data object Discovering : ReceiveWalletPartialState()

    data class ConnectionInitiated(
        val endpointId: String,
        val endpointName: String,
    ) : ReceiveWalletPartialState()

    data class Connected(val endpointId: String) : ReceiveWalletPartialState()

    data class DataReceived(val data: WalletTransferData) : ReceiveWalletPartialState()

    data class Error(val message: String) : ReceiveWalletPartialState()

    data class Disconnected(val endpointId: String) : ReceiveWalletPartialState()
}

sealed class DocumentImportResult {
    data class Success(val documentId: String, val documentName: String) : DocumentImportResult()
    data class Failed(val documentName: String, val error: String) : DocumentImportResult()
}

interface ReceiveWalletInteractor {
    fun parseSessionInfo(encoded: String): TransferSessionInfo?

    fun connectToSender(context: Context, sessionInfo: TransferSessionInfo): Flow<ReceiveWalletPartialState>

    fun decryptReceivedData(encryptedBytes: ByteArray, sessionInfo: TransferSessionInfo): WalletTransferData?

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
        nearbyTransferManager.startDiscovery(context)

        return nearbyTransferManager.state.map { state ->
            when (state) {
                is NearbyTransferState.Discovering -> {
                    ReceiveWalletPartialState.Discovering
                }

                is NearbyTransferState.ConnectionInitiated -> {
                    // Auto-accept connection
                    nearbyTransferManager.acceptConnection(context, state.endpointId)
                    ReceiveWalletPartialState.ConnectionInitiated(
                        endpointId = state.endpointId,
                        endpointName = state.endpointName,
                    )
                }

                is NearbyTransferState.Connected -> {
                    ReceiveWalletPartialState.Connected(endpointId = state.endpointId)
                }

                is NearbyTransferState.PayloadReceived -> {
                    val data = decryptReceivedData(state.bytes, sessionInfo)
                    if (data != null) {
                        ReceiveWalletPartialState.DataReceived(data)
                    } else {
                        ReceiveWalletPartialState.Error("Failed to decrypt transfer data")
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
            val sessionKey = transferSessionManager.getSessionKey(sessionInfo)
            walletTransferController.decryptData(encryptedBytes, sessionKey)
        } catch (_: Exception) {
            null
        }

    override suspend fun importDocuments(
        documents: List<TransferableDocument>,
    ): List<DocumentImportResult> {
        val results = mutableListOf<DocumentImportResult>()
        for (doc in documents) {
            try {
                walletCoreDocumentsController.issueDocument(
                    issuanceMethod = IssuanceMethod.OPENID4VCI,
                    configId = doc.configurationId,
                    issuerId = doc.credentialIssuerId,
                ).collect { state ->
                    when (state) {
                        is IssueDocumentPartialState.Success -> {
                            results.add(
                                DocumentImportResult.Success(
                                    documentId = state.documentId,
                                    documentName = doc.name,
                                ),
                            )
                        }

                        is IssueDocumentPartialState.Failure -> {
                            results.add(
                                DocumentImportResult.Failed(
                                    documentName = doc.name,
                                    error = state.errorMessage,
                                ),
                            )
                        }

                        is IssueDocumentPartialState.UserAuthRequired -> {
                            // Auto-complete device authentication for import flow
                            state.resultHandler.onAuthenticationSuccess()
                        }

                        is IssueDocumentPartialState.DeferredSuccess -> {
                            results.add(
                                DocumentImportResult.Success(
                                    documentId = doc.configurationId,
                                    documentName = doc.name,
                                ),
                            )
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
