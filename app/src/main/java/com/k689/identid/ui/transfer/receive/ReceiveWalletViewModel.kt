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

package com.k689.identid.ui.transfer.receive

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.k689.identid.interactor.transfer.DocumentImportResult
import com.k689.identid.interactor.transfer.ReceiveWalletInteractor
import com.k689.identid.interactor.transfer.ReceiveWalletPartialState
import com.k689.identid.model.transfer.TransferSessionInfo
import com.k689.identid.model.transfer.TransferableDocument
import com.k689.identid.model.transfer.WalletTransferData
import com.k689.identid.navigation.DashboardScreens
import com.k689.identid.navigation.TransferScreens
import com.k689.identid.ui.component.content.ContentErrorConfig
import com.k689.identid.ui.mvi.MviViewModel
import com.k689.identid.ui.mvi.ViewEvent
import com.k689.identid.ui.mvi.ViewSideEffect
import com.k689.identid.ui.mvi.ViewState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class ReceiveWalletState(
    val isLoading: Boolean = false,
    val error: ContentErrorConfig? = null,
    val isConnected: Boolean = false,
    val receivedData: WalletTransferData? = null,
    val sessionInfo: TransferSessionInfo? = null,
    val importResults: List<DocumentImportResult> = emptyList(),
    val isImporting: Boolean = false,
    val isImportComplete: Boolean = false,
) : ViewState

sealed class ReceiveWalletEvent : ViewEvent {
    data object GoBack : ReceiveWalletEvent()
    data object ChooseQr : ReceiveWalletEvent()
    data object ChooseNfc : ReceiveWalletEvent()
    data class SessionScanned(val context: Context, val encoded: String) : ReceiveWalletEvent()
    data class ImportDocuments(val context: Context) : ReceiveWalletEvent()
    data class OnResumeIssuance(val uri: String) : ReceiveWalletEvent()
    data object NavigateToDashboard : ReceiveWalletEvent()
}

sealed class ReceiveWalletEffect : ViewSideEffect {
    sealed class Navigation : ReceiveWalletEffect() {
        data object Pop : Navigation()
        data class SwitchScreen(val screenRoute: String) : Navigation()
        data class NavigateToDashboard(val screenRoute: String) : Navigation()
    }
}

@KoinViewModel
class ReceiveWalletViewModel(
    private val interactor: ReceiveWalletInteractor,
) : MviViewModel<ReceiveWalletEvent, ReceiveWalletState, ReceiveWalletEffect>() {

    private var connectionJob: Job? = null

    override fun setInitialState(): ReceiveWalletState = ReceiveWalletState()

    override fun handleEvents(event: ReceiveWalletEvent) {
        when (event) {
            is ReceiveWalletEvent.GoBack -> {
                connectionJob?.cancel()
                setEffect { ReceiveWalletEffect.Navigation.Pop }
            }

            is ReceiveWalletEvent.ChooseQr -> {
                setEffect {
                    ReceiveWalletEffect.Navigation.SwitchScreen(
                        TransferScreens.ReceiveWalletQr.screenRoute,
                    )
                }
            }

            is ReceiveWalletEvent.ChooseNfc -> {
                setEffect {
                    ReceiveWalletEffect.Navigation.SwitchScreen(
                        TransferScreens.ReceiveWalletNfc.screenRoute,
                    )
                }
            }

            is ReceiveWalletEvent.SessionScanned -> {
                handleSessionScanned(event.context, event.encoded)
            }

            is ReceiveWalletEvent.ImportDocuments -> {
                importReceivedDocuments(event.context)
            }

            is ReceiveWalletEvent.OnResumeIssuance -> {
                interactor.resumeOpenId4VciWithAuthorization(event.uri)
            }

            is ReceiveWalletEvent.NavigateToDashboard -> {
                setEffect {
                    ReceiveWalletEffect.Navigation.NavigateToDashboard(
                        DashboardScreens.Dashboard.screenRoute,
                    )
                }
            }
        }
    }

    private fun handleSessionScanned(context: Context, encoded: String) {
        val sessionInfo = interactor.parseSessionInfo(encoded)
        if (sessionInfo == null) {
            setState {
                copy(
                    error = ContentErrorConfig(
                        errorSubTitle = "Invalid QR code. Please try again.",
                        onRetry = {
                            setState { copy(error = null) }
                            setEffect {
                                ReceiveWalletEffect.Navigation.SwitchScreen(
                                    TransferScreens.ReceiveWalletQr.screenRoute,
                                )
                            }
                        },
                        onCancel = { setEvent(ReceiveWalletEvent.GoBack) },
                    ),
                )
            }
            return
        }

        setState { copy(isLoading = true, sessionInfo = sessionInfo) }

        connectionJob = viewModelScope.launch {
            interactor.connectToSender(context, sessionInfo).collect { state ->
                when (state) {
                    is ReceiveWalletPartialState.Discovering -> {
                        // Still searching
                    }

                    is ReceiveWalletPartialState.Connected -> {
                        setState { copy(isConnected = true) }
                    }

                    is ReceiveWalletPartialState.DataReceived -> {
                        setState {
                            copy(
                                isLoading = false,
                                receivedData = state.data,
                            )
                        }
                        // Navigate to document list
                        setEffect {
                            ReceiveWalletEffect.Navigation.SwitchScreen(
                                TransferScreens.ReceiveWalletDocumentList.screenRoute,
                            )
                        }
                    }

                    is ReceiveWalletPartialState.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = ContentErrorConfig(
                                    errorSubTitle = state.message,
                                    onRetry = { setEvent(ReceiveWalletEvent.GoBack) },
                                    onCancel = { setEvent(ReceiveWalletEvent.GoBack) },
                                ),
                            )
                        }
                    }

                    is ReceiveWalletPartialState.Disconnected -> {
                        if (viewState.value.receivedData == null) {
                            setState {
                                copy(
                                    isLoading = false,
                                    error = ContentErrorConfig(
                                        errorSubTitle = "Connection lost. Please try again.",
                                        onRetry = { setEvent(ReceiveWalletEvent.GoBack) },
                                        onCancel = { setEvent(ReceiveWalletEvent.GoBack) },
                                    ),
                                )
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun importReceivedDocuments(context: Context) {
        val data = viewState.value.receivedData ?: return
        setState { copy(isImporting = true) }

        viewModelScope.launch {
            try {
                // Import documents via re-issuance
                val results = interactor.importDocuments(data.documents)

                // Import local data (transaction logs, bookmarks, revoked docs)
                interactor.importLocalData(data)

                interactor.stopTransfer(context)

                setState {
                    copy(
                        isImporting = false,
                        isImportComplete = true,
                        importResults = results,
                    )
                }
            } catch (e: Exception) {
                setState {
                    copy(
                        isImporting = false,
                        error = ContentErrorConfig(
                            errorSubTitle = e.localizedMessage ?: "Import failed",
                            onRetry = { setEvent(ReceiveWalletEvent.ImportDocuments(context)) },
                            onCancel = { setEvent(ReceiveWalletEvent.GoBack) },
                        ),
                    )
                }
            }
        }
    }
}
