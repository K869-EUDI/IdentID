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

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class NearbyTransferState {
    data object Idle : NearbyTransferState()

    data object Advertising : NearbyTransferState()

    data object Discovering : NearbyTransferState()

    data class ConnectionInitiated(
        val endpointId: String,
        val endpointName: String,
    ) : NearbyTransferState()

    data class Connected(
        val endpointId: String,
    ) : NearbyTransferState()

    data class PayloadReceived(
        val bytes: ByteArray,
    ) : NearbyTransferState()

    data object PayloadSent : NearbyTransferState()

    data class Disconnected(
        val endpointId: String,
    ) : NearbyTransferState()

    data class Error(
        val message: String,
    ) : NearbyTransferState()
}

interface NearbyTransferManager {
    val state: SharedFlow<NearbyTransferState>

    fun startAdvertising(
        context: Context,
        sessionId: String,
    )

    fun startDiscovery(
        context: Context,
        expectedSessionId: String,
    )

    fun acceptConnection(
        context: Context,
        endpointId: String,
    )

    fun rejectConnection(
        context: Context,
        endpointId: String,
    )

    fun sendPayload(
        context: Context,
        endpointId: String,
        data: ByteArray,
    )

    fun stopAllEndpoints(context: Context)
}

class NearbyTransferManagerImpl : NearbyTransferManager {
    companion object {
        private const val SERVICE_ID = "com.k689.identid.transfer"
    }

    private val _state = MutableSharedFlow<NearbyTransferState>(replay = 1, extraBufferCapacity = 10)
    override val state: SharedFlow<NearbyTransferState> = _state.asSharedFlow()

    private val payloadCallback =
        object : PayloadCallback() {
            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload,
            ) {
                val bytes = payload.asBytes()
                if (bytes != null) {
                    _state.tryEmit(NearbyTransferState.PayloadReceived(bytes))
                }
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate,
            ) {
            }
        }

    private fun createConnectionLifecycleCallback(context: Context) =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(
                endpointId: String,
                info: ConnectionInfo,
            ) {
                _state.tryEmit(NearbyTransferState.ConnectionInitiated(endpointId, info.endpointName))
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution,
            ) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        _state.tryEmit(NearbyTransferState.Connected(endpointId))
                    }

                    else -> {
                        _state.tryEmit(NearbyTransferState.Error("Connection failed: ${result.status.statusMessage}"))
                    }
                }
            }

            override fun onDisconnected(endpointId: String) {
                _state.tryEmit(NearbyTransferState.Disconnected(endpointId))
            }
        }

    override fun startAdvertising(
        context: Context,
        sessionId: String,
    ) {
        val client = Nearby.getConnectionsClient(context)
        // Stop any existing advertising/discovery/connections before starting fresh
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        _state.resetReplayCache()

        val advertisingOptions =
            AdvertisingOptions
                .Builder()
                .setStrategy(Strategy.P2P_POINT_TO_POINT)
                .build()

        client
            .startAdvertising(
                sessionId,
                SERVICE_ID,
                createConnectionLifecycleCallback(context),
                advertisingOptions,
            ).addOnSuccessListener {
                _state.tryEmit(NearbyTransferState.Advertising)
            }.addOnFailureListener { e ->
                _state.tryEmit(NearbyTransferState.Error("Advertising failed: ${e.localizedMessage}"))
            }
    }

    override fun startDiscovery(
        context: Context,
        expectedSessionId: String,
    ) {
        val client = Nearby.getConnectionsClient(context)
        // Stop any existing advertising/discovery/connections before starting fresh
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        _state.resetReplayCache()

        val discoveryOptions =
            DiscoveryOptions
                .Builder()
                .setStrategy(Strategy.P2P_POINT_TO_POINT)
                .build()

        client
            .startDiscovery(
                SERVICE_ID,
                object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(
                        endpointId: String,
                        info: DiscoveredEndpointInfo,
                    ) {
                        if (info.endpointName != expectedSessionId) return

                        Nearby
                            .getConnectionsClient(context)
                            .requestConnection(
                                "receiver",
                                endpointId,
                                createConnectionLifecycleCallback(context),
                            ).addOnFailureListener { e ->
                                _state.tryEmit(NearbyTransferState.Error("Connection request failed: ${e.localizedMessage}"))
                            }
                    }

                    override fun onEndpointLost(endpointId: String) {
                        _state.tryEmit(NearbyTransferState.Disconnected(endpointId))
                    }
                },
                discoveryOptions,
            ).addOnSuccessListener {
                _state.tryEmit(NearbyTransferState.Discovering)
            }.addOnFailureListener { e ->
                _state.tryEmit(NearbyTransferState.Error("Discovery failed: ${e.localizedMessage}"))
            }
    }

    override fun acceptConnection(
        context: Context,
        endpointId: String,
    ) {
        Nearby
            .getConnectionsClient(context)
            .acceptConnection(endpointId, payloadCallback)
    }

    override fun rejectConnection(
        context: Context,
        endpointId: String,
    ) {
        Nearby.getConnectionsClient(context).rejectConnection(endpointId)
    }

    override fun sendPayload(
        context: Context,
        endpointId: String,
        data: ByteArray,
    ) {
        val payload = Payload.fromBytes(data)
        Nearby
            .getConnectionsClient(context)
            .sendPayload(endpointId, payload)
            .addOnSuccessListener {
                _state.tryEmit(NearbyTransferState.PayloadSent)
            }.addOnFailureListener { e ->
                _state.tryEmit(NearbyTransferState.Error("Send failed: ${e.localizedMessage}"))
            }
    }

    override fun stopAllEndpoints(context: Context) {
        val client = Nearby.getConnectionsClient(context)
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        _state.resetReplayCache()
        _state.tryEmit(NearbyTransferState.Idle)
    }
}
