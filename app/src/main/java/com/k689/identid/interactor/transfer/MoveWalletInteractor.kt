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
import com.k689.identid.controller.transfer.NearbyTransferManager
import com.k689.identid.controller.transfer.NearbyTransferState
import com.k689.identid.controller.transfer.TransferSessionManager
import com.k689.identid.controller.transfer.WalletTransferController
import com.k689.identid.model.transfer.TransferSessionInfo
import com.k689.identid.model.transfer.WalletTransferEnvelope
import com.k689.identid.provider.resources.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

sealed class MoveWalletPartialState {
    data class SessionReady(
        val qrContent: String,
        val sessionInfo: TransferSessionInfo,
    ) : MoveWalletPartialState()

    data object WaitingForConnection : MoveWalletPartialState()

    data class ConnectionInitiated(
        val endpointId: String,
        val endpointName: String,
    ) : MoveWalletPartialState()

    data class Connected(
        val endpointId: String,
    ) : MoveWalletPartialState()

    data object DataSent : MoveWalletPartialState()

    data class Error(
        val message: String,
    ) : MoveWalletPartialState()

    data class Disconnected(
        val endpointId: String,
    ) : MoveWalletPartialState()
}

interface MoveWalletInteractor {
    fun createSessionAndAdvertise(context: Context): Flow<MoveWalletPartialState>

    fun getSessionInfo(): TransferSessionInfo?

    fun getConnectedEndpointId(): String?

    fun acceptConnection(
        context: Context,
        endpointId: String,
    )

    fun rejectConnection(
        context: Context,
        endpointId: String,
    )

    suspend fun encryptAndSendData(
        context: Context,
        endpointId: String,
    )

    fun stopTransfer(context: Context)
}

class MoveWalletInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletTransferController: WalletTransferController,
    private val nearbyTransferManager: NearbyTransferManager,
    private val transferSessionManager: TransferSessionManager,
) : MoveWalletInteractor {
    private var currentSession: TransferSessionInfo? = null
    private var connectedEndpointId: String? = null
    private val json = Json { ignoreUnknownKeys = true }

    override fun createSessionAndAdvertise(context: Context): Flow<MoveWalletPartialState> {
        val session = transferSessionManager.createSession()
        currentSession = session

        nearbyTransferManager.startAdvertising(context, session.sessionId)

        val qrContent = transferSessionManager.encodeSessionInfo(session)

        return nearbyTransferManager.state.map { state ->
            when (state) {
                is NearbyTransferState.Advertising -> {
                    MoveWalletPartialState.SessionReady(
                        qrContent = qrContent,
                        sessionInfo = session,
                    )
                }

                is NearbyTransferState.ConnectionInitiated -> {
                    MoveWalletPartialState.ConnectionInitiated(
                        endpointId = state.endpointId,
                        endpointName = state.endpointName,
                    )
                }

                is NearbyTransferState.Connected -> {
                    connectedEndpointId = state.endpointId
                    MoveWalletPartialState.Connected(endpointId = state.endpointId)
                }

                is NearbyTransferState.PayloadSent -> {
                    MoveWalletPartialState.DataSent
                }

                is NearbyTransferState.Disconnected -> {
                    MoveWalletPartialState.Disconnected(endpointId = state.endpointId)
                }

                is NearbyTransferState.Error -> {
                    MoveWalletPartialState.Error(message = state.message)
                }

                else -> {
                    MoveWalletPartialState.WaitingForConnection
                }
            }
        }
    }

    override fun getSessionInfo(): TransferSessionInfo? = currentSession

    override fun getConnectedEndpointId(): String? = connectedEndpointId

    override fun acceptConnection(
        context: Context,
        endpointId: String,
    ) {
        nearbyTransferManager.acceptConnection(context, endpointId)
    }

    override fun rejectConnection(
        context: Context,
        endpointId: String,
    ) {
        nearbyTransferManager.rejectConnection(context, endpointId)
    }

    override suspend fun encryptAndSendData(
        context: Context,
        endpointId: String,
    ) {
        val session = currentSession ?: return

        val transferData = walletTransferController.collectTransferData()
        val sessionKey = transferSessionManager.getSessionKey(session)
        val encrypted = walletTransferController.encryptData(transferData, sessionKey)
        val envelope =
            WalletTransferEnvelope(
                sessionId = session.sessionId,
                encryptedPayloadBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP),
            )
        val envelopeBytes =
            json
                .encodeToString(WalletTransferEnvelope.serializer(), envelope)
                .toByteArray(Charsets.UTF_8)
        nearbyTransferManager.sendPayload(context, endpointId, envelopeBytes)
    }

    override fun stopTransfer(context: Context) {
        nearbyTransferManager.stopAllEndpoints(context)
        currentSession = null
        connectedEndpointId = null
    }
}
