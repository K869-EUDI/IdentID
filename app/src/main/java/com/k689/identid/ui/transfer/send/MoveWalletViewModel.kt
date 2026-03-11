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

package com.k689.identid.ui.transfer.send

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.k689.identid.interactor.transfer.MoveWalletInteractor
import com.k689.identid.interactor.transfer.MoveWalletPartialState
import com.k689.identid.navigation.TransferScreens
import com.k689.identid.service.TransferNfcPayloadStore
import com.k689.identid.ui.component.content.ContentErrorConfig
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class MoveWalletState(
    val isLoading: Boolean = true,
    val error: ContentErrorConfig? = null,
    val qrCode: String = "",
    val connectedEndpointId: String? = null,
    val isDataSent: Boolean = false,
) : ViewState

sealed class MoveWalletEvent : ViewEvent {
    data class Init(
        val context: Context,
    ) : MoveWalletEvent()

    data class GoBack(
        val context: Context,
    ) : MoveWalletEvent()

    data class AcceptConnection(
        val context: Context,
        val endpointId: String,
    ) : MoveWalletEvent()

    data class SendData(
        val context: Context,
    ) : MoveWalletEvent()
}

sealed class MoveWalletEffect : ViewSideEffect {
    sealed class Navigation : MoveWalletEffect() {
        data object Pop : Navigation()

        data class SwitchScreen(
            val screenRoute: String,
        ) : Navigation()
    }
}

@KoinViewModel
class MoveWalletViewModel(
    private val interactor: MoveWalletInteractor,
) : MviViewModel<MoveWalletEvent, MoveWalletState, MoveWalletEffect>() {
    private var interactorJob: Job? = null

    override fun setInitialState(): MoveWalletState = MoveWalletState()

    override fun handleEvents(event: MoveWalletEvent) {
        when (event) {
            is MoveWalletEvent.Init -> {
                if (interactorJob?.isActive != true) {
                    startSession(event.context)
                }
            }

            is MoveWalletEvent.GoBack -> {
                cleanUp(event.context)
                setEffect { MoveWalletEffect.Navigation.Pop }
            }

            is MoveWalletEvent.AcceptConnection -> {
                interactor.acceptConnection(event.context, event.endpointId)
            }

            is MoveWalletEvent.SendData -> {
                val endpointId = viewState.value.connectedEndpointId ?: return
                viewModelScope.launch {
                    interactor.encryptAndSendData(event.context, endpointId)
                }
            }
        }
    }

    private fun startSession(context: Context) {
        setState { copy(isLoading = true, error = null) }

        interactorJob =
            viewModelScope.launch {
                interactor.createSessionAndAdvertise(context).collect { state ->
                    when (state) {
                        is MoveWalletPartialState.SessionReady -> {
                            TransferNfcPayloadStore.setPayload(state.qrContent)
                            setState {
                                copy(
                                    isLoading = false,
                                    error = null,
                                    qrCode = state.qrContent,
                                )
                            }
                        }

                        is MoveWalletPartialState.ConnectionInitiated -> {
                            if (viewState.value.connectedEndpointId == null) {
                                interactor.acceptConnection(context, state.endpointId)
                            } else {
                                interactor.rejectConnection(context, state.endpointId)
                            }
                        }

                        is MoveWalletPartialState.Connected -> {
                            setState {
                                copy(connectedEndpointId = state.endpointId)
                            }
                            // Navigate to approval screen
                            setEffect {
                                MoveWalletEffect.Navigation.SwitchScreen(
                                    TransferScreens.MoveWalletApproval.screenRoute,
                                )
                            }
                        }

                        is MoveWalletPartialState.DataSent -> {
                            setState { copy(isDataSent = true) }
                        }

                        is MoveWalletPartialState.Error -> {
                            setState {
                                copy(
                                    isLoading = false,
                                    error =
                                        ContentErrorConfig(
                                            onRetry = { setEvent(MoveWalletEvent.Init(context)) },
                                            errorSubTitle = state.message,
                                            onCancel = {
                                                cleanUp(context)
                                                setEffect { MoveWalletEffect.Navigation.Pop }
                                            },
                                        ),
                                )
                            }
                        }

                        is MoveWalletPartialState.Disconnected -> {
                            setState { copy(connectedEndpointId = null) }
                        }

                        is MoveWalletPartialState.WaitingForConnection -> {
                            // No-op, waiting
                        }
                    }
                }
            }
    }

    private fun cleanUp(context: Context) {
        interactorJob?.cancel()
        interactorJob = null
        TransferNfcPayloadStore.clear()
        interactor.stopTransfer(context)
    }
}
