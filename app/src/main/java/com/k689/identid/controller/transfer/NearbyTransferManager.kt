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
import android.util.Log
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
    data class ConnectionInitiated(val endpointId: String, val endpointName: String) : NearbyTransferState()
    data class Connected(val endpointId: String) : NearbyTransferState()
    data class PayloadReceived(val bytes: ByteArray) : NearbyTransferState()
    data object PayloadSent : NearbyTransferState()
    data class Disconnected(val endpointId: String) : NearbyTransferState()
    data class Error(val message: String) : NearbyTransferState()
}

interface NearbyTransferManager {
    val state: SharedFlow<NearbyTransferState>

    fun startAdvertising(context: Context, sessionId: String)

    fun startDiscovery(context: Context)

    fun acceptConnection(context: Context, endpointId: String)

    fun rejectConnection(context: Context, endpointId: String)

    fun sendPayload(context: Context, endpointId: String, data: ByteArray)

    fun stopAllEndpoints(context: Context)
}

class NearbyTransferManagerImpl : NearbyTransferManager {

    companion object {
        private const val SERVICE_ID = "com.k689.identid.transfer"
    }

    private val _state = MutableSharedFlow<NearbyTransferState>(replay = 1, extraBufferCapacity = 10)
    override val state: SharedFlow<NearbyTransferState> = _state.asSharedFlow()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                _state.tryEmit(NearbyTransferState.PayloadReceived(bytes))
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS &&
                update.totalBytes == update.bytesTransferred
            ) {
                // Only emit PayloadSent for outgoing transfers
            }
        }
    }

    private fun createConnectionLifecycleCallback(context: Context) = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            _state.tryEmit(NearbyTransferState.ConnectionInitiated(endpointId, info.endpointName))
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
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

    override fun startAdvertising(context: Context, sessionId: String) {
        Log.d("NearbyTransfer", "startAdvertising called with sessionId=$sessionId")
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        Nearby.getConnectionsClient(context)
            .startAdvertising(
                sessionId,
                SERVICE_ID,
                createConnectionLifecycleCallback(context),
                advertisingOptions,
            )
            .addOnSuccessListener {
                Log.d("NearbyTransfer", "Advertising started successfully")
                _state.tryEmit(NearbyTransferState.Advertising)
            }
            .addOnFailureListener { e ->
                Log.e("NearbyTransfer", "Advertising failed", e)
                _state.tryEmit(NearbyTransferState.Error("Advertising failed: ${e.localizedMessage}"))
            }
    }

    override fun startDiscovery(context: Context) {
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        Nearby.getConnectionsClient(context)
            .startDiscovery(
                SERVICE_ID,
                object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                        Nearby.getConnectionsClient(context)
                            .requestConnection(
                                "receiver",
                                endpointId,
                                createConnectionLifecycleCallback(context),
                            )
                    }

                    override fun onEndpointLost(endpointId: String) {
                        _state.tryEmit(NearbyTransferState.Disconnected(endpointId))
                    }
                },
                discoveryOptions,
            )
            .addOnSuccessListener {
                _state.tryEmit(NearbyTransferState.Discovering)
            }
            .addOnFailureListener { e ->
                _state.tryEmit(NearbyTransferState.Error("Discovery failed: ${e.localizedMessage}"))
            }
    }

    override fun acceptConnection(context: Context, endpointId: String) {
        Nearby.getConnectionsClient(context)
            .acceptConnection(endpointId, payloadCallback)
    }

    override fun rejectConnection(context: Context, endpointId: String) {
        Nearby.getConnectionsClient(context)
            .rejectConnection(endpointId)
    }

    override fun sendPayload(context: Context, endpointId: String, data: ByteArray) {
        val payload = Payload.fromBytes(data)
        Nearby.getConnectionsClient(context)
            .sendPayload(endpointId, payload)
            .addOnSuccessListener {
                _state.tryEmit(NearbyTransferState.PayloadSent)
            }
            .addOnFailureListener { e ->
                _state.tryEmit(NearbyTransferState.Error("Send failed: ${e.localizedMessage}"))
            }
    }

    override fun stopAllEndpoints(context: Context) {
        Nearby.getConnectionsClient(context).stopAllEndpoints()
        _state.tryEmit(NearbyTransferState.Idle)
    }
}
